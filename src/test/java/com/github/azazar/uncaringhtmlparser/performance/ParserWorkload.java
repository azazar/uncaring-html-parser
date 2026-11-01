package com.github.azazar.uncaringhtmlparser.performance;

import com.github.azazar.uncaringhtmlparser.Html;
import com.github.azazar.uncaringhtmlparser.HtmlElement;
import com.github.azazar.uncaringhtmlparser.Selector;
import com.github.azazar.uncaringhtmlparser.util.HtmlUtil;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/** Target JVM launched by the JUnit debugger test. Markers delimit the measured region. */
public final class ParserWorkload {
    static final String[] SELECTORS = {"standalone-tags", "standalone-value", "standalone-class",
            "standalone-missing", "nested-descendant", "repeated-fragment", "compile-selector", "compiled-tags",
            "plain-text", "tagged-text"};
    private static volatile long sink;
    private static volatile Selector.Compiled compiledSink;

    public static void begin(String selector, int repetition) { }
    public static void end() { }
    public static void allocation(String selector, long bytes) { }

    private static void queryTags(String[] tags, String selector) {
        long count = 0;
        for (String tag : tags) {
            Html html = new Html(tag);
            count += selector == null ? html.tag("a").count() : html.css(selector).count();
        }
        sink = count;
    }

    private static void queryFragment(HtmlElement fragment) {
        sink = fragment.css("a[href]").count()
                + fragment.css("img[src]").count()
                + fragment.tag("h2").count();
    }

    private static void queryCompiledTags(String[] tags, Selector.Compiled selector) {
        long count = 0;
        for (String tag : tags) count += selector.queryAll(new Html(tag)).count();
        sink = count;
    }

    private static void text(String html) {
        sink = HtmlUtil.stripHtml(CharBuffer.wrap(html)).length();
    }

    public static void main(String[] args) throws Exception {
        ThreadMXBean memory = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!memory.isThreadAllocatedMemorySupported()) {
            throw new AssertionError("Thread allocation measurement is unavailable");
        }
        memory.setThreadAllocatedMemoryEnabled(true);
        String source;
        try (var input = ParserWorkload.class.getResourceAsStream("/performance/w3.html")) {
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        // W3C home page snapshot, 2026-11-01: 101 individual links and 339 tags in the nested body.
        String[] tags = Pattern.compile("<a\\b[^>]*>").matcher(source).results()
                .map(MatchResult::group).toArray(String[]::new);
        if (tags.length != 101) throw new AssertionError("Standalone tag corpus changed");
        HtmlElement body = new Html(source).tag("body").findFirst().orElseThrow();
        HtmlElement main = body.tag("main").findFirst().orElseThrow();
        Selector.Compiled compiled = Selector.compile("a.nav-link[href]");
        HtmlElement copyright = body.css("p.copyright").findFirst().orElseThrow();
        String plainText = copyright.getTextContent();
        String taggedText = copyright.html().getHtml();
        Runnable[] workloads = {
            () -> queryTags(tags, null),
            () -> queryTags(tags, "a[hreflang='ja']"),
            () -> queryTags(tags, "a.nav-link[href]"),
            () -> queryTags(tags, "a[data-parser-missing]"),
            () -> sink = body.css("nav a").count(),
            () -> queryFragment(main),
            () -> {
                compiledSink = Selector.compile("a.nav-link[href]");
                sink = compiledSink == null ? 0 : 1;
            },
            () -> queryCompiledTags(tags, compiled),
            () -> text(plainText),
            () -> text(taggedText)
        };
        int[] expected = {101, 1, 7, 0, 64, 24, 1, 7, 114, 114};
        for (int scenario = 0; scenario < workloads.length; scenario++) {
            Runnable workload = workloads[scenario];
            for (int warmup = 0; warmup < 10; warmup++) workload.run();
            for (int repetition = 0; repetition < 2; repetition++) {
                begin(SELECTORS[scenario], repetition);
                workload.run();
                end();
                // Separate invocation keeps allocation accounting outside instruction stepping.
                long before = memory.getCurrentThreadAllocatedBytes();
                workload.run();
                long bytes = memory.getCurrentThreadAllocatedBytes() - before;
                allocation(SELECTORS[scenario], bytes);
                if (sink != expected[scenario]) {
                    throw new AssertionError("Incorrect result: " + SELECTORS[scenario] + " = " + sink);
                }
            }
        }
    }
}
