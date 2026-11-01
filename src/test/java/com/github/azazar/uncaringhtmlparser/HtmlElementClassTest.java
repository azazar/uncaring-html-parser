package com.github.azazar.uncaringhtmlparser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HtmlElementClassTest {
    @Test void classTokensKeepWhitespaceAndEntitySemantics() {
        var element = new Html("<a class=' first\tsecond\nthird&#32;fourth\u2003fifth '></a>")
                .tag("a").findFirst().orElseThrow();
        for (int repeat = 0; repeat < 3; repeat++) {
            for (String name : new String[]{"first", "second", "third", "fourth", "fifth"}) {
                assertTrue(element.hasClass(name));
            }
            assertFalse(element.hasClass("fir"));
            assertFalse(element.hasClass("First"));
            assertFalse(element.hasClass(""));
        }
    }

    @Test void missingEmptyAndBooleanClassesStayEmpty() {
        for (String attribute : new String[]{"", "class", "class=''", "class=' \t\n'"}) {
            var element = new Html("<a " + attribute + "></a>").tag("a").findFirst().orElseThrow();
            assertFalse(element.hasClass("x"));
            assertFalse(element.hasClass(""));
        }
    }
}
