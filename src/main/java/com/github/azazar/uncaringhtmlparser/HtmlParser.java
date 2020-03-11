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

import java.util.Iterator;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Mikhail Yevchenko <mail@uo1.net>
 */
public class HtmlParser {
    
    public static Iterator<HtmlElement> byTagName(String html, String tagName) {
        String tagSearch = "<" + tagName;

        return new Iterator<HtmlElement>() {

            int position = 0;
            HtmlElement next = null;
            
            private boolean parseNext() {
                while(position < html.length()) {
                    position = StringUtils.indexOfIgnoreCase(html, tagSearch, position);

                    if (position == -1) {
                        break;
                    }

                    position += tagSearch.length();

                    char ch = html.charAt(position);

                    if (ch != '>' && !Character.isWhitespace(ch)) {
                        continue;
                    }

                    int tagEnd = html.indexOf('>', position);

                    if (tagEnd == -1) {
                        break;
                    }

                    String tagAttrs = html.substring(position, tagEnd);

                    position = tagEnd + 1;
                    
                    next = new HtmlElement(tagName, tagAttrs, () -> html.substring(tagEnd + 1, StringUtils.indexOfIgnoreCase(html, "</" + tagName + ">", tagEnd + 1)));
                    return true;
                }

                position = html.length();
                return false;
            }
            
            @Override
            public boolean hasNext() {
                if (position == html.length())
                    return false;
                
                if (next == null) {
                    return parseNext();
                }
                
                return true;
            }

            @Override
            public HtmlElement next() {
                HtmlElement retVal = next;
                next = null;
                return retVal;
            }
        };
    }

}
