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
 * An element view sharing the source storage. Cached fragment boundaries and class
 * tokens are not invalidated when that storage changes.
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
    
    private Html html;
    
    /**
     * Returns the cached inner HTML view. The first access finds the fragment boundary;
     * reusing this element avoids repeating that work. Queries inside the view still scan it.
     * 
     * @return inner html
     */
    public Html html() {
        if (html == null) {
            html = new Html(innerHtml.get());
        }
        
        return html;
    }
    
    /**
     * Returns a fresh, single-use stream of matching descendants within {@link #html()}.
     * Retain selected elements or collected results to avoid repeating the search.
     * 
     * @param tagName
     * @return 
     */
    public Stream<HtmlElement> tag(String tagName) {
        return html().tag(tagName);
    }
    
    /**
     * Returns a fresh, single-use query within {@link #html()}.
     * The selector is parsed on each call and matching descendants are not cached.
     * 
     * @param selector
     * @return 
     */
    public Stream<HtmlElement> css(String selector) {
        return html().css(selector);
    }

    @Deprecated
    public Stream<HtmlElement> byTagName(String tagName) {
        return tag(tagName);
    }
    
    @Deprecated
    public Stream<HtmlElement> bySelector(String selector) {
        return css(selector);
    }

    /**
     * Scans and decodes an attribute value on each call. Retain the returned value
     * when it will be reused, or use {@link #hasAttr(String)} for presence only.
     * 
     * @param name attribute name
     * @return attribute value or null if attribute doesn't exist
     */
    public String attrOrNull(String name) {
        return HtmlUtil.attributeValue(attrs, name);
    }

    /**
     * Extracts an uncached attribute value; see {@link #attrOrNull(String)} for reuse.
     * 
     * @param name attribute name
     * @return attribute value or empty string if attribute doesn't exist
     */
    public String attr(String name) {
        String val = attrOrNull(name);
        return val == null ? "" : val;
    }

    /** Scans for an attribute without decoding or allocating its value. */
    public boolean hasAttr(String name) {
        return HtmlUtil.attributePosition(attrs, name) >= 0;
    }

    boolean hasAttr(String name, String value) {
        return HtmlUtil.attributeMatches(attrs, name, value);
    }
    
    /** Extracts an uncached ID value; retain it for repeated use. */
    public String id() {
        return attrOrNull("id");
    }
    
    private String[] classes;
    
    private static final String[] NO_CLASSES = {};
    
    /**
     * Decodes and caches class tokens on the first call. Reuse this element for
     * multiple class checks; later changes to the source class attribute are not observed.
     */
    public boolean hasClass(String className) {
        if (classes == null) {
            String classAttr = attrOrNull("class");
            
            if (classAttr != null) {
                classes = StringUtils.split(classAttr);
            } else {
                classes = NO_CLASSES;
            }
        }
        
        for (String className_ : classes) {
            if (className.equals(className_))
                return true;
        }
        
        return false;
    }
    
    /**
     * Removes tags and returns an uncached text snapshot on each call.
     * Retain the returned string when the same text is needed again.
     */
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
