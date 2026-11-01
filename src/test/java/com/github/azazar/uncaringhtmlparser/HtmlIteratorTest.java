package com.github.azazar.uncaringhtmlparser;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HtmlIteratorTest {
    @Test void consumesCachedAndRemainingElements() {
        var iterator = HtmlParser.byTagName(CharBuffer.wrap("<a id='1'></a><a id='2'></a>"), "a");
        assertTrue(iterator.hasNext());
        List<String> ids = new ArrayList<>();
        iterator.forEachRemaining(element -> ids.add(element.id()));
        assertEquals(List.of("1", "2"), ids);
        assertFalse(iterator.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iterator::next);
        iterator.forEachRemaining(element -> fail("Already exhausted"));
    }

    @Test void continuesAfterNextAndActionFailure() {
        var iterator = HtmlParser.byTagName(CharBuffer.wrap("<a id='1'></a><a id='2'></a><a id='3'></a>"), "a");
        assertEquals("1", iterator.next().id());
        assertThrows(IllegalStateException.class, () -> iterator.forEachRemaining(element -> {
            assertEquals("2", element.id());
            throw new IllegalStateException();
        }));
        assertEquals("3", iterator.next().id());
        assertFalse(iterator.hasNext());
    }

    @Test void rejectsNullActionEvenWhenEmpty() {
        var iterator = HtmlParser.byTagName(CharBuffer.wrap(""), "a");
        assertThrows(NullPointerException.class, () -> iterator.forEachRemaining(null));
    }

    @Test void findFirstDoesNotTraverseFollowingElements() {
        AtomicInteger visited = new AtomicInteger();
        assertEquals("1", new Html("<a id='1'></a><a id='2'></a>").tag("a")
                .peek(element -> visited.incrementAndGet()).findFirst().orElseThrow().id());
        assertEquals(1, visited.get());
    }
}
