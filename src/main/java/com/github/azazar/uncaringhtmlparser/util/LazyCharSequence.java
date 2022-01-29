/*
 * Copyright (C) 2022 Mikhail Yevchenko <m.ṥῥẚɱ.ѓѐḿởύḙ@uo1.net>
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

import java.util.function.Supplier;

/**
 *
 * @author Mikhail Yevchenko <m.ṥῥẚɱ.ѓѐḿởύḙ@uo1.net>
 * @param <T>
 */
public class LazyCharSequence<T extends CharSequence> implements CharSequence {
    
    private Supplier<T> supplier;
    private T sequence = null;

    public LazyCharSequence(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (sequence == null)
            sequence = supplier.get();
        
        return sequence;
    }

    @Override
    public int length() {
        return get().length();
    }

    @Override
    public char charAt(int index) {
        return get().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return get().subSequence(start, end);
    }
    
    @Override
    public String toString() {
        return get().toString();
    }

}
