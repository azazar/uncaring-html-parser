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
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Mikhail Yevchenko <mail@uo1.net>
 */
public class HtmlElement {
    
    private String tagName;
    private String attrs;
    private Supplier<String> innerHtmlSupplier;

    public HtmlElement(String tagName, String attrs, Supplier<String> innerHtmlSupplier) {
        this.tagName = tagName;
        this.attrs = attrs;
        this.innerHtmlSupplier = innerHtmlSupplier;
    }
    
    private Html html = null;
    
    /**
     * Returns inner html
     * 
     * @return inner html
     */
    public Html html() {
        if (html == null) {
            html = new Html(innerHtmlSupplier.get());
        }
        
        return html;
    }
    
    /**
     * Returns all elements matching specified tag name
     * 
     * @param tagName
     * @return 
     */
    public HtmlElements byTagName(String tagName) {
        return html().elements(tagName);
    }
    
    /**
     * Extracts attribute value
     * 
     * @param name attribute name
     * @return attribute value or null if attribute doesn't exist
     */
    public String attr(String name) {
        return HtmlUtil.attributeValue(attrs, name);
    }
    
    public String id() {
        return attr("id");
    }
    
    private String[] classes = null;
    
    private static final String[] NO_CLASSES = {};
    
    public boolean hasClass(String className) {
        if (classes == null) {
            String classAttr = attr("class");
            
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
        return HtmlUtil.stripHtml(innerHtmlSupplier.get());
    }

    /**
     * 
     * @return tag html
     */
    @Override
    public String toString() {
        String innerHtml = innerHtmlSupplier.get();

        return "<" + tagName + (attrs.isEmpty() ? "" : " " + attrs) + (innerHtml.isEmpty() ? "/>" : ">\n" + innerHtml + "\n</" + tagName + ">");
    }

}
