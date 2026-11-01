package com.github.azazar.uncaringhtmlparser;

import java.nio.CharBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HtmlParserScanningTest {
    @Test
    void observesBufferLimitChangesBetweenElements() {
        CharBuffer buffer = CharBuffer.wrap("<a><a>");
        buffer.limit(3);
        var elements = HtmlParser.byTagName(buffer, "a");
        assertNotNull(elements.next());
        buffer.limit(6);
        assertNotNull(elements.next());
        assertFalse(elements.hasNext());
        assertEquals(0, buffer.position());
    }

    @Test
    void readsInnerHtmlLazilyAndCachesIt() {
        char[] source = "<a>old</a>".toCharArray();
        var element = HtmlParser.byTagName(CharBuffer.wrap(source), "a").next();
        source[3] = 'n';
        source[4] = 'e';
        source[5] = 'w';
        assertEquals("new", element.html().getHtml());
        assertSame(element.html(), element.html());
    }

    @Test
    void ignoresQuotedTagDelimitersInSlicedBuffer() {
        CharBuffer buffer = CharBuffer.wrap("prefix<a title='>' data=\"x>y\">text</a>suffix");
        buffer.position(6);
        buffer.limit(buffer.limit() - 6);
        var element = HtmlParser.byTagName(buffer, "a").next();
        assertEquals(">", element.attr("title"));
        assertEquals("x>y", element.attr("data"));
        assertEquals("text", element.getTextContent());
        assertEquals(6, buffer.position());
    }
}
