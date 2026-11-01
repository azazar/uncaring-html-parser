package com.github.azazar.uncaringhtmlparser;

import java.nio.CharBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HtmlParserScanningTest {
    @Test
    void unterminatedParentHasNoDescendants() {
        var parent = new Html("<section><a id='inside'></a>")
                .tag("section").findFirst().orElseThrow();
        assertTrue(parent.tag("a").findFirst().isEmpty());
        assertEquals(0, parent.css("a").count());
    }

    @Test
    void fixesBoundsBeforeTraversalButReadsDescendantsLazily() {
        String original = "<section><b id='inside'></b></section><a id='outside'></a>";
        char[] source = original.toCharArray();
        var parent = new Html(CharBuffer.wrap(source)).tag("section").findFirst().orElseThrow();
        try (var descendants = parent.tag("a")) {
            source[original.indexOf("</section>") + 2] = 'x';
            source[original.indexOf("<b") + 1] = 'a';
            source[original.indexOf("</b") + 2] = 'a';
            assertEquals(List.of("inside"), descendants.map(HtmlElement::id).toList());
        }
    }

    @Test
    void nestedSameNameParentsKeepTheOuterBoundary() {
        var parent = new Html("<section><section><a id='nested'></a></section>"
                + "<a id='direct'></a></section><a id='outside'></a>")
                .tag("section").findFirst().orElseThrow();
        assertEquals(List.of("nested", "direct"), parent.tag("a").map(HtmlElement::id).toList());
    }

    @Test
    void readsOnlyTheSelectedFragmentAndDefersReadingItsContents() {
        String opening = "<section id='selected'>";
        String contents = "<a id='one'></a><div><a id='two'></a></div>";
        GuardedText source = new GuardedText(opening + contents + "</section><a id='outside'></a>");
        source.end = opening.length();
        var element = new Html(CharBuffer.wrap(source)).tag("section").findFirst().orElseThrow();
        assertEquals("selected", element.id());

        source.end = opening.length() + contents.length() + "</section>".length();
        Html fragment = element.html();
        source.start = opening.length();
        source.end = source.start + contents.length();
        for (int repeat = 0; repeat < 2; repeat++) {
            assertEquals(List.of("one", "two"), element.tag("a").map(HtmlElement::id).toList());
            assertEquals(List.of("two"), element.css("div a").map(HtmlElement::id).toList());
            assertEquals(contents, fragment.getHtml());
        }
    }

    @Test
    void respectsInputBufferPositionAndLimitWithoutReadingOutside() {
        String outside = "<a id='outside'></a>";
        String inside = "<a id='inside'>text</a>";
        GuardedText source = new GuardedText(outside + inside + outside);
        source.start = outside.length();
        source.end = source.start + inside.length();
        CharBuffer buffer = CharBuffer.wrap(source);
        buffer.position(source.start);
        buffer.limit(source.end);
        Html fragment = new Html(buffer);
        assertEquals(List.of("inside"), fragment.css("a").map(HtmlElement::id).toList());
        assertEquals("text", fragment.tag("a").findFirst().orElseThrow().getTextContent());
        assertEquals(source.start, buffer.position());
        assertEquals(source.end, buffer.limit());
    }

    @Test
    void attributeAndContentViewsShareTheOriginalStorage() {
        String original = "<a id='old'>old</a>";
        char[] source = original.toCharArray();
        var element = new Html(CharBuffer.wrap(source)).tag("a").findFirst().orElseThrow();
        Html fragment = element.html();
        assertEquals("old", element.id());
        assertEquals("old", fragment.getHtml());
        "new".getChars(0, 3, source, original.indexOf("old"));
        "new".getChars(0, 3, source, original.lastIndexOf("old"));
        assertEquals("new", element.id());
        assertEquals("new", fragment.getHtml());
    }

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

    private static final class GuardedText implements CharSequence {
        private final String text;
        private int start;
        private int end;

        private GuardedText(String text) {
            this.text = text;
            end = text.length();
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            assertTrue(index >= start && index < end, "Read outside fragment: " + index);
            return text.charAt(index);
        }

        @Override
        public CharSequence subSequence(int from, int to) {
            assertTrue(from >= start && to <= end, "Copied outside fragment");
            return text.substring(from, to);
        }

        @Override
        public String toString() {
            return subSequence(0, length()).toString();
        }
    }
}
