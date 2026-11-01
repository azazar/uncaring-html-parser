package com.github.azazar.uncaringhtmlparser.performance;

import com.github.azazar.uncaringhtmlparser.Html;

/** Target JVM launched by the JUnit debugger test. Markers delimit the measured region. */
public final class ParserWorkload {
    static final String[] SELECTORS = {"a", "a.link", "a[href]", "div a", "missing", "A", "a[missing]", "a[class='link']"};
    private static volatile long sink;

    public static void begin(String selector, int repetition) { }
    public static void end() { }

    private static void query(String html, String selector) {
        sink = new Html(html).css(selector).count();
    }

    public static void main(String[] args) {
        String html = "<div><a href='https://example.org' class='link'>text</a></div>".repeat(5);
        for (String selector : SELECTORS) {
            for (int warmup = 0; warmup < 10; warmup++) query(html, selector);
            for (int repetition = 0; repetition < 2; repetition++) {
                begin(selector, repetition);
                query(html, selector);
                end();
                if (sink != (selector.contains("missing") ? 0 : 5)) {
                    throw new AssertionError("Incorrect query result: " + selector + " = " + sink);
                }
            }
        }
    }
}
