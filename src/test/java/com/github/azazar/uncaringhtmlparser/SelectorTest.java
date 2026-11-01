package com.github.azazar.uncaringhtmlparser;

import java.nio.CharBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SelectorTest {
    @Test
    void descendantSelectorsStayInsideMatchingParents() {
        Html html = new Html("<a id='before'></a><section class='selected'>"
                + "<a id='direct'></a><div><a id='nested'></a></div></section>"
                + "<section><div><a id='other'></a></div></section><a id='after'></a>");
        assertEquals(List.of("direct", "nested"),
                html.css("section.selected a").map(HtmlElement::id).toList());
        assertEquals(List.of("nested"),
                html.css("section.selected div a").map(HtmlElement::id).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void rejectsEmptySelectors(String selector) {
        assertThrows(IllegalArgumentException.class, () -> new Html("<a></a>").css(selector));
    }

    @Test
    void ignoresExtraSelectorSpaces() {
        Html html = new Html("<section><div><a class='link'>match</a></div></section>");
        assertEquals(1, html.css("  section   div  a.link  ").count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a(", "a.link(", ".link"})
    void rejectsUnsupportedSuffixes(String selector) {
        assertThrows(UnsupportedOperationException.class, () -> new Html("<a></a>").css(selector));
    }

    @Test
    void rejectsEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> new Html("<a></a>").css("a#"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a.link.extra[href='target']",
        "a.link.extra[href=target]",
        "a.link.extra[href=\"target\"]",
        "a[href='target'].extra.link",
        "section div a.link.extra[href='target']",
        "section div a[href='target'].extra.link"
    })
    void combinesFiltersAndDescendants(String selector) {
        Html html = new Html("<section><div>"
                + "<a class='link extra' href='target'>match</a>"
                + "<a class='other' href='target'>wrong class</a>"
                + "<a class='link extra' href='other'>wrong value</a>"
                + "<a class='link extra'>missing value</a>"
                + "</div></section>");
        assertEquals("match", html.css(selector).findFirst().orElseThrow().getTextContent());
        assertEquals(1, html.css(selector).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a[id=]", "a[id='']", "a[id=\"\"]"})
    void matchesEmptyAttributeValue(String selector) {
        Html html = new Html("<a id=''></a><a id='other'></a><a></a>");
        assertEquals(1, html.css(selector).count());
    }

    @Test
    void scansLazily() {
        char[] source = "<b></b>".toCharArray();
        try (var elements = new Html(CharBuffer.wrap(source)).css("a")) {
            source[1] = 'a';
            source[5] = 'a';
            assertEquals(1, elements.count());
        }
    }
}
