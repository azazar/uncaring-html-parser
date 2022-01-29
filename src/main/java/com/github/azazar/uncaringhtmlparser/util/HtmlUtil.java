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
        StringBuilder r = new StringBuilder(html.length());
        
        int pos = 0, ofs;
        
        while ((ofs = StringUtils.indexOf(html, '<', pos)) != -1) {
            r.append(html, pos, ofs);
            
            int end = StringUtils.indexOf(html, '>', ofs + 1);
            
            if (end == -1) {
                break;
            }
            
            pos = end + 1;
        }
        
        r.append(html, pos, html.length());
        
        return r;
    }
    
    public static String attributeValue(CharBuffer attrs, String attributeName) {
        int i = StringUtils.indexOfIgnoreCase(attrs, attributeName);

        if (i == -1)
            return null;

        i += attributeName.length();

        while (i < attrs.length() && isSpace(attrs.charAt(i))) i++;

        if (i == attrs.length() || attrs.charAt(i) != '=') return null;

        i++;

        while (i < attrs.length() && isSpace(attrs.charAt(i))) i++;

        if (i == attrs.length()) return null;

        char delim = attrs.charAt(i);

        int attrEnd;

        if (delim == '\'' || delim == '\"') {
            i++;

            attrEnd = StringUtils.indexOf(attrs, delim, i);

            if (attrEnd != -1)
                return StringEscapeUtils.unescapeHtml4(attrs.subSequence(i, attrEnd).toString());
        }
        else {
            attrEnd = i;
            while (attrEnd < attrs.length()) {
                if (isSpace(attrs.charAt(attrEnd))) {
                    break;
                }

                attrEnd++;
            }
            return StringEscapeUtils.unescapeHtml4(attrs.subSequence(i, attrEnd).toString());
        }
        
        return null;
    }
    
}
