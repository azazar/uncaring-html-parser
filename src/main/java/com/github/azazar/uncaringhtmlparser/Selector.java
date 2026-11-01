/*
 * Copyright (C) 2020 Mikhail Yevchenko <mail@uo1.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.azazar.uncaringhtmlparser;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *
 * @author Mikhail Yevchenko <mail@uo1.net>
 */
public class Selector {

    /** Parses on each call; retain a {@link #compile(String) compiled selector} for repeated queries. */
    public static Stream<HtmlElement> queryAll(String selector, Html html) {
        return compile(selector).queryAll(html);
    }

    private static boolean isTag(char c, int index) {
        if (index == 0)
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';

        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
    }

    /** Parses once for reuse across documents or fragments; each query creates a fresh lazy stream. */
    public static Compiled compile(String selector) {
        int start = 0;
        while (start < selector.length() && selector.charAt(start) == ' ') start++;
        if (start == selector.length()) throw new IllegalArgumentException("The provided selector is empty");
        return new Compiled(selector, start);
    }

    /** A reusable selector plan. It holds no document or query results. */
    public static final class Compiled {
        private final String tagName;
        private final Predicate<HtmlElement> condition;
        private final Compiled next;

        private Compiled(String selector, int start) {
            int end = selector.indexOf(' ', start);
            String selectorPart = end < 0 ? selector.substring(start) : selector.substring(start, end);
            int i = 0;

            int length = selectorPart.length();
            while(i < length && isTag(selectorPart.charAt(i), i)) {
                i++;
            }

            if (i == 0) {
                throw new UnsupportedOperationException("Selectors without tag name aren't supported by parser");
            }

            tagName = selectorPart.substring(0, i);

            Predicate<HtmlElement> condition = null;

            selectorPart = selectorPart.substring(i);

            if (!selectorPart.isEmpty() && selectorPart.charAt(0) == '#') {
                String id = selectorPart.substring(1);

                if (id.isEmpty()) {
                    throw new IllegalArgumentException("Empty id specified");
                }

                condition = and(condition, e -> e.getClass() == HtmlElement.class
                        ? e.hasAttr("id", id) : id.equals(e.id()));
                selectorPart = "";
            }

            while (!selectorPart.isEmpty()) {
                char kind = selectorPart.charAt(0);
                if (kind != '[' && kind != '.') break;
                int partEnd = -1;
                for (int p = 1; p < selectorPart.length(); p++) {
                    char c = selectorPart.charAt(p);
                    if (c == '.' || c == '[' || c == '(') {
                        partEnd = p;
                        break;
                    }
                }
                String part;

                if (partEnd == -1) {
                    part = selectorPart;
                    selectorPart = "";
                }
                else {
                    part = selectorPart.substring(0, partEnd);
                    selectorPart = selectorPart.substring(partEnd);
                }

                if (kind == '.') {
                    String clazz = part.substring(1);
                    condition = and(condition, e -> e.hasClass(clazz));
                }
                else {
                    part = part.substring(1, part.length() - 1).trim();
                    i = part.indexOf('=');

                    if (i == -1) {
                        String attrName = part;
                        condition = and(condition, e -> e.hasAttr(attrName));
                    }
                    else {
                        String attrName = part.substring(0, i).trim();
                        String value = part.substring(i + 1).trim();

                        if (!value.isEmpty()) {
                            char quote = value.charAt(0);
                            if ((quote == '\'' || quote == '"') && value.charAt(value.length() - 1) == quote) {
                                value = value.substring(1, value.length() - 1);
                            }
                        }

                        String value_ = value;

                        condition = and(condition, e -> e.getClass() == HtmlElement.class
                                ? e.hasAttr(attrName, value_) : value_.equals(e.attrOrNull(attrName)));
                    }
                }
            }

            if (!selectorPart.isEmpty()) {
                throw new UnsupportedOperationException("Unsupported selector part (" + selectorPart + ')');
            }

            this.condition = condition;
            int nextStart = end;
            if (nextStart >= 0) {
                while (nextStart < selector.length() && selector.charAt(nextStart) == ' ') nextStart++;
            }
            next = nextStart < 0 || nextStart == selector.length() ? null : new Compiled(selector, nextStart);
        }

        private static Predicate<HtmlElement> and(Predicate<HtmlElement> first, Predicate<HtmlElement> second) {
            return first == null ? second : first.and(second);
        }

        /** Executes against the supplied document without caching elements or consuming the returned stream. */
        public Stream<HtmlElement> queryAll(Html html) {
            Stream<HtmlElement> elements = html.tag(tagName);
            if (condition != null) elements = elements.filter(condition);
            for (Compiled part = next; part != null; part = part.next) {
                String tag = part.tagName;
                elements = elements.flatMap(element -> element.tag(tag));
                if (part.condition != null) elements = elements.filter(part.condition);
            }
            return elements;
        }
    }
}
