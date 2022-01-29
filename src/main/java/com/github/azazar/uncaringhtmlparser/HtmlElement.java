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
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Mikhail Yevchenko <mail@uo1.net>
 */
public class HtmlElement {
    
    private String tagName;
    private CharBuffer attrs;
    private LazyCharSequence<CharBuffer> innerHtml;

    public HtmlElement(String tagName, CharBuffer attrs, LazyCharSequence<CharBuffer> innerHtml) {
        this.tagName = tagName;
        this.attrs = attrs;
        this.innerHtml = innerHtml;
    }
    
    private Html html = null;
    
    /**
     * Returns inner html
     * 
     * @return inner html
     */
    public Html html() {
        if (html == null) {CharBuffer.wrap("asd");
            html = new Html(innerHtml.get());
        }
        
        return html;
    }
    
    /**
     * Returns all elements matching specified tag name
     * 
     * @param tagName
     * @return 
     */
    public Stream<HtmlElement> byTagName(String tagName) {
        return html().byTagName(tagName);
    }
    
    /**
     * Extracts attribute value
     * 
     * @param name attribute name
     * @return attribute value or null if attribute doesn't exist
     */
    public String attrOrNull(String name) {
        return HtmlUtil.attributeValue(attrs, name);
    }

    /**
     * Extracts attribute value
     * 
     * @param name attribute name
     * @return attribute value or empty string if attribute doesn't exist
     */
    public String attr(String name) {
        String val = attrOrNull(name);
        return val == null ? "" : val;
    }

    public boolean hasAttr(String name) {
        return StringUtils.containsIgnoreCase(attrs, " " + name);
    }
    
    public String id() {
        return attrOrNull("id");
    }
    
    private String[] classes = null;
    
    private static final String[] NO_CLASSES = {};
    
    public boolean hasClass(String className) {
        if (classes == null) {
            String classAttr = attrOrNull("class");
            
            classes = NO_CLASSES;
            
            if (classAttr != null) {
                classes = StringUtils.split(classAttr.trim(), ' ');
            }
        }
        
        for (String className_ : classes) {
            if (className.equals(className_))
                return true;
        }
        
        return false;
    }
    
    public String getTextContent() {
        return HtmlUtil.stripHtml(innerHtml.get()).toString();
    }

    /**
     * 
     * @return tag html
     */
    @Override
    public String toString() {
        return "<" + tagName + (attrs.length() == 0 ? "" : " " + attrs) + (innerHtml.length() == 0 ? "/>" : ">\n" + innerHtml + "\n</" + tagName + ">");
    }

}
