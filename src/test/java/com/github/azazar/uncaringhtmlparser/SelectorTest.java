package com.github.azazar.uncaringhtmlparser;

import java.nio.CharBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SelectorTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "a.link.extra[href='target']",
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
