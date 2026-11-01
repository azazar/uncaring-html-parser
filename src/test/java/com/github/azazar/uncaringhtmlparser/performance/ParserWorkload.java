package com.github.azazar.uncaringhtmlparser.performance;

import com.github.azazar.uncaringhtmlparser.Html;
import com.github.azazar.uncaringhtmlparser.util.HtmlUtil;
import java.nio.CharBuffer;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/** Target JVM launched by the JUnit debugger test. Markers delimit the measured region. */
public final class ParserWorkload {
    static final String[] SELECTORS = {"a", "a.link", "a[href]", "div a", "missing", "A", "a[missing]", "a[class='link']", "plain-text", "tagged-text"};
    private static volatile long sink;

    public static void begin(String selector, int repetition) { }
    public static void end() { }
    public static void allocation(String selector, long bytes) { }

    private static void query(String html, String selector) {
        sink = new Html(html).css(selector).count();
    }

    private static void text(String html) {
        sink = HtmlUtil.stripHtml(CharBuffer.wrap(html)).length();
    }

    public static void main(String[] args) {
        ThreadMXBean memory = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!memory.isThreadAllocatedMemorySupported()) {
            throw new AssertionError("Thread allocation measurement is unavailable");
        }
        memory.setThreadAllocatedMemoryEnabled(true);
        String html = "<div><a href='https://example.org' class='link'>text</a></div>".repeat(5);
        for (int index = 0; index < SELECTORS.length - 2; index++) {
            String selector = SELECTORS[index];
            for (int warmup = 0; warmup < 10; warmup++) query(html, selector);
            for (int repetition = 0; repetition < 2; repetition++) {
                begin(selector, repetition);
                query(html, selector);
                end();
                // Separate invocation keeps allocation accounting outside instruction stepping.
                long before = memory.getCurrentThreadAllocatedBytes();
                query(html, selector);
                long bytes = memory.getCurrentThreadAllocatedBytes() - before;
                allocation(selector, bytes);
                if (sink != (selector.contains("missing") ? 0 : 5)) {
                    throw new AssertionError("Incorrect query result: " + selector + " = " + sink);
                }
            }
        }

        String[] textInputs = {"plain text", "plain <b>text</b>"};
        for (int index = 0; index < textInputs.length; index++) {
            String input = textInputs[index];
            String selector = SELECTORS[SELECTORS.length - textInputs.length + index];
            for (int warmup = 0; warmup < 10; warmup++) text(input);
            for (int repetition = 0; repetition < 2; repetition++) {
                begin(selector, repetition);
                text(input);
                end();
                long before = memory.getCurrentThreadAllocatedBytes();
                text(input);
                long bytes = memory.getCurrentThreadAllocatedBytes() - before;
                allocation(selector, bytes);
                if (sink != 10) throw new AssertionError("Incorrect text length: " + sink);
            }
        }
    }
}
