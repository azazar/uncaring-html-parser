package com.github.azazar.uncaringhtmlparser;

import com.github.azazar.uncaringhtmlparser.util.HtmlUtil;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttributeMatchingTest {
    @Test void matchesRealPageAttributes() throws Exception {
        Html html;
        try (var input = getClass().getResourceAsStream("/performance/w3.html")) {
            html = new Html(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        int checked = 0;
        for (String tag : List.of("html", "body", "a", "div", "span", "link", "meta", "script", "img",
                "section", "li", "ul", "nav", "header", "footer", "h1", "h2", "h3", "p", "button", "input", "form")) {
            for (HtmlElement element : html.tag(tag).toList()) {
                for (String name : List.of("href", "class", "lang", "id", "aria-label", "src")) {
                    String value = element.attrOrNull(name);
                    if (value != null) {
                        assertTrue(element.hasAttr(name, value), name);
                        assertFalse(element.hasAttr(name, value + "!missing!"), name);
                        checked++;
                    }
                }
            }
        }
        assertEquals(266, checked);
    }

    @Test void selectorsKeepOverriddenValueAndIdAccess() {
        HtmlElement element = new HtmlElement("a", CharBuffer.wrap("id='raw'"), null) {
            @Override public String id() { return "virtual-id"; }
            @Override public String attrOrNull(String name) { return "virtual-value"; }
        };
        Html document = new Html("") {
            @Override public Stream<HtmlElement> tag(String name) { return Stream.of(element); }
        };
        assertEquals(1, document.css("a#virtual-id").count());
        assertEquals(1, document.css("a[id='virtual-value']").count());
        assertEquals(0, document.css("a#raw").count());
        assertEquals(0, document.css("a[id='raw']").count());
    }

    @Test void directMatchingKeepsExtractionSemantics() {
        for (String attrs : new String[]{"", "id", "id=", "id=''", "id=\"\"", "id=plain", "id='plain'",
                "id=\"plain\"", "id='unfinished", "id=\"unfinished", "id=a/b", "data-id=x id=plain",
                "ID = '\u2003Русский'", "id=\u0000", "id='a&amp;b'", "id='&#97;'", "id='&#x61;'",
                "id='&unknown;'", "id='first' id='second'", "id='&#x1F600;'"}) {
            for (String value : new String[]{"", "plain", "a/b", "\u2003Русский", "\u0000", "a&b", "a", "&unknown;", "first", "second", "😀", "different"}) {
                CharBuffer buffer = CharBuffer.wrap("prefix" + attrs + "suffix");
                buffer.position(6);
                buffer.limit(buffer.limit() - 6);
                assertEquals(value.equals(HtmlUtil.attributeValue(buffer, "id")),
                        HtmlUtil.attributeMatches(buffer, "id", value), attrs + " / " + value);
                assertEquals(6, buffer.position());
            }
        }
        CharBuffer invalid = CharBuffer.wrap("id='prefix&#1114112;'");
        assertThrows(IllegalArgumentException.class, () -> HtmlUtil.attributeValue(invalid, "id"));
        assertThrows(IllegalArgumentException.class, () -> HtmlUtil.attributeMatches(invalid, "id", "different"));
    }

    @Test void matchesValuesAndIdsWithoutChangingDecoding() {
        for (String value : new String[]{"plain", "", "a&amp;b", "&#97;", "&#x61;", "&unknown;", "Ая", "a/"}) {
            Html html = new Html("<a ID='" + value + "'></a><a id=\"" + value + "\"></a>");
            String expected = html.tag("a").findFirst().orElseThrow().id();
            assertEquals(2, html.css("a[id='" + expected + "']").count(), value);
            if (!expected.isEmpty()) assertEquals(2, html.css("a#" + expected).count(), value);
            assertEquals(0, html.css("a[id='different']").count(), value);
        }
    }

    @Test void rejectsMissingBooleanAndUnclosedValues() {
        for (String attrs : new String[]{"", "id", "id=", "id='unfinished", "id=\"unfinished"}) {
            Html html = new Html("<a " + attrs + "></a>");
            assertEquals(0, html.css("a[id='']").count());
            assertEquals(0, html.css("a#unfinished").count());
        }
        assertEquals(1, new Html("<a id=plain></a>").css("a#plain").count());
        assertEquals(0, new Html("<a data-id=plain></a>").css("a#plain").count());
    }

    @Test void matchingReadsCurrentAttributeBuffer() {
        char[] source = "prefix<a id='old'></a>suffix".toCharArray();
        CharBuffer buffer = CharBuffer.wrap(source);
        buffer.position(6);
        buffer.limit(source.length - 6);
        Html html = new Html(buffer);
        try (var elements = html.css("a#new")) {
            "new".getChars(0, 3, source, 13);
            assertEquals(1, elements.count());
        }
        assertEquals(6, buffer.position());
    }
}
