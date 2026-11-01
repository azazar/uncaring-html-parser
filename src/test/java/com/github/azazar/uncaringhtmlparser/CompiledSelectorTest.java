package com.github.azazar.uncaringhtmlparser;

import java.nio.CharBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CompiledSelectorTest {
    @ParameterizedTest
    @ValueSource(strings = {"a", "a.link", "a#one", "a[href]", "a[href='target']", "a[href=\"target\"]", "a[href=target]", "div a.link[href='target']", "  div   a  "})
    void reusableQueriesKeepExistingResults(String selector) {
        var compiled = Selector.compile(selector);
        for (String html : List.of("<div><a id='one' class='link' href='target'></a><a id='two'></a></div>",
                "<a id='outside'></a><div><a id='three' class='link' href='target'></a></div>")) {
            Html document = new Html(html);
            for (int repeat = 0; repeat < 3; repeat++) {
                assertEquals(document.css(selector).map(HtmlElement::id).toList(),
                        compiled.queryAll(document).map(HtmlElement::id).toList());
            }
        }
    }

    @Test
    void compilationDoesNotCaptureOrEagerlyReadHtml() {
        var compiled = Selector.compile("a.link");
        char[] source = "<b class='link'></b>".toCharArray();
        Html document = new Html(CharBuffer.wrap(source));
        try (var elements = compiled.queryAll(document)) {
            source[1] = 'a';
            source[source.length - 2] = 'a';
            assertEquals(1, elements.count());
        }
        source[1] = 'b';
        assertEquals(0, compiled.queryAll(document).count());
    }

    @Test
    void respectsCustomTagLookup() {
        AtomicInteger calls = new AtomicInteger();
        Html custom = new Html("") {
            @Override public Stream<HtmlElement> tag(String name) {
                calls.incrementAndGet();
                return new Html("<a id='custom'></a>").tag(name);
            }
        };
        assertEquals("custom", Selector.compile("a").queryAll(custom).findFirst().orElseThrow().id());
        assertEquals(1, calls.get());
    }

    @Test
    void respectsCustomDescendantLookup() {
        AtomicInteger calls = new AtomicInteger();
        HtmlElement parent = new HtmlElement("div", CharBuffer.wrap(""), null) {
            @Override public Stream<HtmlElement> tag(String name) {
                calls.incrementAndGet();
                return new Html("<a id='custom'></a>").tag(name);
            }
        };
        Html custom = new Html("") {
            @Override public Stream<HtmlElement> tag(String name) {
                assertEquals("div", name);
                return Stream.of(parent);
            }
        };
        assertEquals("custom", Selector.compile("div a").queryAll(custom).findFirst().orElseThrow().id());
        assertEquals(1, calls.get());
    }

    @Test
    void rejectsInvalidSelectorsAtCompilation() {
        assertThrows(IllegalArgumentException.class, () -> Selector.compile("  "));
        assertThrows(IllegalArgumentException.class, () -> Selector.compile("a#"));
        assertThrows(UnsupportedOperationException.class, () -> Selector.compile(".link"));
        assertThrows(UnsupportedOperationException.class, () -> Selector.compile("a("));
    }
}
