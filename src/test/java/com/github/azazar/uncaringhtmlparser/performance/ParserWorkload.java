package com.github.azazar.uncaringhtmlparser.performance;

import com.github.azazar.uncaringhtmlparser.Html;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/** Target JVM launched by the JUnit debugger test. Markers delimit the measured region. */
public final class ParserWorkload {
    static final String[] SELECTORS = {"a", "a.link", "a[href]", "div a", "missing", "A", "a[missing]", "a[class='link']"};
    private static volatile long sink;

    public static void begin(String selector, int repetition) { }
    public static void end() { }
    public static void allocation(String selector, long bytes) { }

    private static void query(String html, String selector) {
        sink = new Html(html).css(selector).count();
    }

    public static void main(String[] args) {
        ThreadMXBean memory = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!memory.isThreadAllocatedMemorySupported()) {
            throw new AssertionError("Thread allocation measurement is unavailable");
        }
        memory.setThreadAllocatedMemoryEnabled(true);
        String html = "<div><a href='https://example.org' class='link'>text</a></div>".repeat(5);
        for (String selector : SELECTORS) {
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
    }
}
