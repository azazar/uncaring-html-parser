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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Mikhail Yevchenko <mail@uo1.net>
 */
public class Selector {
    
    public static Stream<HtmlElement> queryAll(String selector, Html html) {
        return queryAll(Arrays.asList(StringUtils.split(selector, ' ')), html::tag);
    }
    
    private static boolean isTag(char c, int index) {
        if (index == 0)
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';

        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
    }
    
    private static Stream<HtmlElement> queryAll(List<String> selector, Function<String, Stream<HtmlElement>> find) {
        if (selector.isEmpty())
            throw new IllegalArgumentException("The provided selector is empty");
        
        String selectorPart = selector.get(0);
        
        selector = selector.subList(1, selector.size());
        
        int i = 0;
        
        while(i < selectorPart.length() && isTag(selectorPart.charAt(i), i)) {
            i++;
        }
        
        if (i == 0) {
            throw new UnsupportedOperationException("Selectors without tag name aren't supported by parser");
        }
        
        String tagName = selectorPart.substring(0, i);
        
        Stream<HtmlElement> elements = find.apply(tagName);
        
        selectorPart = selectorPart.substring(i);
        
        if (selectorPart.startsWith("#")) {
            String id = selectorPart.substring(1);
            
            if (id.isEmpty()) {
                throw new IllegalArgumentException("Empty id specified");
            }
            
            elements = elements.filter(e -> id.equals(e.id()));
            selectorPart = "";
        }
        
        while (selectorPart.startsWith("[") || selectorPart.startsWith(".")) {
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
            
            if (part.startsWith(".")) {
                String clazz = part.substring(1);
                elements = elements.filter(e -> e.hasClass(clazz));
            }
            else if (part.startsWith("[")) {
                part = part.substring(1, part.length() - 1).trim();
                i = part.indexOf('=');
                
                if (i == -1) {
                    String attrName = part;
                    elements = elements.filter(e -> e.hasAttr(attrName));
                }
                else {
                    String attrName = part.substring(0, i).trim();
                    String value = part.substring(i + 1).trim();

                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    String value_ = value;

                    elements = elements.filter(e -> value_.equals(e.attrOrNull(attrName)));
                }
            }
            else {
                throw new UnsupportedOperationException("Unsupported selector part (" + part + ')');
            }
        }

        if (!selectorPart.isEmpty()) {
            throw new UnsupportedOperationException("Unsupported selector part (" + selectorPart + ')');
        }
        
        if (selector.isEmpty())
            return elements;
        
        Stream<HtmlElement> parents = elements;
        return queryAll(selector, name -> parents.flatMap(e -> e.tag(name)));
    }
    
}
