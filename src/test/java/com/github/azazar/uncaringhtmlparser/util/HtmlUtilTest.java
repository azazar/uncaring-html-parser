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

import java.nio.CharBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Mikhail Yevchenko <m.ṥῥẚɱ.ѓѐḿởύḙ@uo1.net>
 */
public class HtmlUtilTest {
    
    public HtmlUtilTest() {
    }

    @Test
    public void testStripHtml() {
        assertEquals("весь текст", HtmlUtil.stripHtml("<i class=\"icon-check book-status-icon icon-v-middle\"></i>весь текст").toString());
        assertEquals(" весь текст", HtmlUtil.stripHtml("<i class=\"icon-check book-status-icon icon-v-middle\"></i> весь текст").toString());
    }

    @Test
    public void testAttributeScannerSkipsBooleanAndQuotedValues() {
        for (String separator : new String[]{" ", "\t", "\r\n", "/"}) {
            CharBuffer attrs = CharBuffer.wrap(separator + "disabled" + separator
                    + "data-id='wrong' title=\" id='fake'\" ID \t=\n'right'");
            assertEquals("right", HtmlUtil.attributeValue(attrs, "id"));
            assertTrue(HtmlUtil.attributePosition(attrs, "disabled") >= 0);
            assertNull(HtmlUtil.attributeValue(attrs, "disabled"));
            assertEquals(-1, HtmlUtil.attributePosition(attrs, "missing"));
            assertEquals(0, attrs.position());
        }
    }
    
}
