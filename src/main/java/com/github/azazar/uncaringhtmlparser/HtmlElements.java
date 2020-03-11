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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * @author Mikhail Yevchenko <mail@uo1.net>
 */
public class HtmlElements implements Iterable<HtmlElement> {
    
    private Iterable<HtmlElement> elements;

    public HtmlElements(Iterable<HtmlElement> elements) {
        this.elements = elements;
    }

    @Override
    public Iterator<HtmlElement> iterator() {
        return elements.iterator();
    }
    
    public Stream<HtmlElement> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

}
