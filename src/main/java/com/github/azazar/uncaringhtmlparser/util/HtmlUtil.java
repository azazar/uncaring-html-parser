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
package com.github.azazar.uncaringhtmlparser.util;

import java.nio.CharBuffer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author Mikhail Yevchenko <mail@uo1.net>
 */
public class HtmlUtil {
    
    public static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }
    
    public static CharSequence stripHtml(CharSequence html) {
        int pos = 0, ofs = StringUtils.indexOf(html, '<', 0);
        if (ofs == -1) return html.toString();

        StringBuilder r = new StringBuilder(html.length());
        do {
            r.append(html, pos, ofs);
            
            int end = StringUtils.indexOf(html, '>', ofs + 1);
            
            if (end == -1) {
                pos = ofs;
                break;
            }
            
            pos = end + 1;
        } while ((ofs = StringUtils.indexOf(html, '<', pos)) != -1);
        
        r.append(html, pos, html.length());
        
        return r;
    }
    
    public static int attributePosition(CharBuffer attrs, String attributeName) {
        int length = attrs.length();
        int i = 0;
        while (i < length) {
            while (i < length) {
                char c = attrs.charAt(i);
                if (!isSpace(c) && c != '/') break;
                i++;
            }
            int start = i;
            while (i < length) {
                char c = attrs.charAt(i);
                if (isSpace(c) || c == '=' || c == '/') break;
                i++;
            }
            boolean match = i - start == attributeName.length()
                    && matches(attrs, start, attributeName);
            if (match) {
                return start;
            }
            while (i < length && isSpace(attrs.charAt(i))) i++;
            if (i < length && attrs.charAt(i) == '=') {
                i++;
                while (i < length && isSpace(attrs.charAt(i))) i++;
                if (i < length) {
                    char quote = attrs.charAt(i);
                    if (quote == '\'' || quote == '"') {
                        i++;
                        while (i < length && attrs.charAt(i) != quote) i++;
                        if (i < length) i++;
                    } else while (i < length && !isSpace(attrs.charAt(i))) i++;
                }
            }
        }
        return -1;
    }

    /** Compares a known in-bounds region without allocating a subsequence. */
    public static boolean matches(CharBuffer attrs, int start, String name) {
        for (int j = 0; j < name.length(); j++) {
            char a = attrs.charAt(start + j), b = name.charAt(j);
            if (a != b && Character.toLowerCase(a) != Character.toLowerCase(b)) return false;
        }
        return true;
    }

    private static String decode(CharBuffer attrs, int start, int end) {
        String value = attrs.subSequence(start, end).toString();
        return value.indexOf('&') < 0 ? value : StringEscapeUtils.unescapeHtml4(value);
    }

    public static String attributeValue(CharBuffer attrs, String attributeName) {
        int i = attributePosition(attrs, attributeName);
        if (i < 0) return null;

        int length = attrs.length();
        i += attributeName.length();

        while (i < length && isSpace(attrs.charAt(i))) i++;

        if (i == length || attrs.charAt(i) != '=') return null;

        i++;

        while (i < length && isSpace(attrs.charAt(i))) i++;

        if (i == length) return null;

        char delim = attrs.charAt(i);

        int attrEnd;

        if (delim == '\'' || delim == '\"') {
            i++;

            attrEnd = i;
            while (attrEnd < length && attrs.charAt(attrEnd) != delim) attrEnd++;

            if (attrEnd < length)
                return decode(attrs, i, attrEnd);
        }
        else {
            attrEnd = i;
            while (attrEnd < length) {
                if (isSpace(attrs.charAt(attrEnd))) {
                    break;
                }

                attrEnd++;
            }
            return decode(attrs, i, attrEnd);
        }
        
        return null;
    }
    
}
