import com.github.azazar.uncaringhtmlparser.Html;

/** Counts the same operation after initialization, without measuring time. */
public class InstructionBenchmark {
    private static native void start();
    private static native long stop();
    private static volatile long sink;

    private static long query(Html html, String selector) {
        return html.css(selector).count();
    }

    private static long sample(Html html, String selector) {
        start();
        sink = query(html, selector);
        return stop();
    }

    public static void main(String[] args) {
        System.load(System.getProperty("counter.library"));
        Html html = new Html("<div><a href='https://example.org' class='link'>text</a></div>".repeat(50));
        String[] selectors = {"a", "a.link", "a[href]", "div a", "missing", "A", "a[missing]", "a[class='link']"};
        for (String selector : selectors) {
            for (int i = 0; i < 20; i++) query(html, selector);
            // Initialize paths used when stepping, too; exclude first-use work.
            sample(html, selector);
            long expected = selector.contains("missing") ? 0 : 50;
            long first = sample(html, selector);
            for (int i = 0; i < 3; i++) {
                long next = sample(html, selector);
                if (next != first) throw new AssertionError("Unstable instruction count for " + selector + ": " + first + " != " + next);
                if (sink != expected) throw new AssertionError("Wrong query result for " + selector);
            }
            System.out.println(selector + "\t" + first);
        }
    }
}
