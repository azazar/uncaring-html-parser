import com.github.azazar.uncaringhtmlparser.Html;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
/** Run in separate JVMs with identical heap/JDK; report medians after warmup. */
public class ParserBenchmark {
 static volatile long sink;
 public static void main(String[] args) {
  Html html = new Html("<div><a href='https://example.org' class='link'>text</a></div>".repeat(500));
  String[] selectors = {"a", "a.link", "a[href]", "div a", "missing", "A", "a[missing]", "a[class='link']"};
  var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
  for (String selector : selectors) {
   long[] times = new long[9]; long bytes = 0;
   for (int round = -10; round < 9; round++) {
    long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
    long start = System.nanoTime();
    for (int i = 0; i < 200; i++) sink = html.css(selector).count();
    long elapsed = System.nanoTime() - start;
    if (round >= 0) { times[round] = elapsed / 200; bytes += (bean.getThreadAllocatedBytes(Thread.currentThread().getId()) - allocated) / 200; }
   }
   Arrays.sort(times);
   System.out.println(selector + ": ns/op=" + times[4] + " bytes/op=" + bytes / 9);
  }
 }
}
