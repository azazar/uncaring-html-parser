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

import com.github.azazar.uncaringhtmlparser.util.HtmlUtil;
import com.github.azazar.uncaringhtmlparser.util.LazyCharSequence;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *
 * @author Mikhail Yevchenko <mail@uo1.net>
 */
public class HtmlParser {
    
    private static final CharBuffer EMPTY_BUFFER = CharBuffer.allocate(0);
    
    private static int findTag(CharBuffer html, String name, int start, int limit) {
        for (int i = start; i < limit; i++) {
            if (html.charAt(i) == '<' && HtmlUtil.matches(html, i + 1, name)) return i;
        }
        return -1;
    }

    public static Iterator<HtmlElement> byTagName(CharBuffer html, String tagName) {
        int tagSearchLength = tagName.length() + 1;

        return new Iterator<HtmlElement>() {

            int position;
            HtmlElement next;
            
            private boolean parseNext() {
                int length = html.length();
                while(position < length) {
                    position = findTag(html, tagName, position, length - tagSearchLength + 1);

                    if (position == -1) {
                        break;
                    }

                    position += tagSearchLength;

                    if (position == length) break;
                    char ch = html.charAt(position);

                    if (ch != '>' && ch != '/' && !Character.isWhitespace(ch)) {
                        continue;
                    }

                    int endOfTag = position;
                    CharBuffer tagAttrs;
                    if (ch != '>') {
                        char quote = 0;
                        for (endOfTag++; endOfTag < length; endOfTag++) {
                            char c = html.charAt(endOfTag);
                            if (quote != 0) {
                                if (c == quote) quote = 0;
                            }
                            else if (c == '\'' || c == '"') quote = c;
                            else if (c == '>') break;
                        }
                        if (endOfTag == length) break;
                        tagAttrs = html.subSequence(position, endOfTag);
                    } else {
                        tagAttrs = EMPTY_BUFFER;
                    }
                    final int tagEnd = endOfTag;

                    position = tagEnd + 1;
                    
                    next = new HtmlElement(tagName, tagAttrs, new LazyCharSequence<>(() -> {
                        int depth = 0;
                        int end = -1;
                        
                        int rightBoundary = html.length() - tagName.length() - 2;
                        for(int i = tagEnd + 1; i < rightBoundary;) {
                            if (html.charAt(i) != '<') {
                                i++;
                                continue;
                            }
                            
                            i++;
                            
                            if (html.charAt(i) == '/') {
                                i++;
                                
                                if (HtmlUtil.matches(html, i, tagName)) {
                                    depth--;

                                    if (depth == -1) {
                                        end = i - 2;
                                        break;
                                    }
                                }
                                
                                continue;
                            }
                            else {
                                if (HtmlUtil.matches(html, i, tagName)) {
                                    depth++;
                                    
                                    i += tagName.length() + 1;
                                }
                                else {
                                    i++;
                                }
                            }
                        }
                        
                        if (end == -1)
                            return EMPTY_BUFFER;

                        return html.subSequence(tagEnd + 1, end);
                    }));

                    return true;
                }

                position = length;
                return false;
            }
            
            @Override
            public boolean hasNext() {
                if (next != null) return true;
                if (position == html.length())
                    return false;
                
                return parseNext();
            }

            @Override
            public HtmlElement next() {
                if (next == null && !hasNext()) throw new java.util.NoSuchElementException();
                HtmlElement retVal = next;
                next = null;
                return retVal;
            }

            @Override
            public void forEachRemaining(Consumer<? super HtmlElement> action) {
                Objects.requireNonNull(action);
                while (hasNext()) {
                    HtmlElement element = next;
                    next = null;
                    action.accept(element);
                }
            }
        };
    }

}
