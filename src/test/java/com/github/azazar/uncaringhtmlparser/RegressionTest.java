package com.github.azazar.uncaringhtmlparser;
import com.github.azazar.uncaringhtmlparser.util.HtmlUtil;
import java.nio.CharBuffer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class RegressionTest {
 static void check(String name, Object expected, Supplier<?> actual) { assertEquals(expected, actual.get(), name); }

 @Test void truncatedOpeningTag() {
  check("truncated opening tag", 0L, () -> new Html("<div").tag("div").count());
 }
 @Test void selfClosingTag() {
  check("self-closing tag", 1L, () -> new Html("<br/>").tag("br").count());
 }
 @Test void multipleClasses() {
  check("two CSS classes", 1L, () -> new Html("<div class='a b'></div>").css("div.a.b").count());
 }
 @Test void exactAttributeName() {
  check("attribute boundary", "right", () -> new Html("<a data-id='wrong' id='right'></a>").tag("a").findFirst().get().id());
 }
 @Test void exactAttributePresence() {
  check("attribute presence boundary", 0L, () -> new Html("<a hrefx='x'></a>").css("a[href]").count());
 }
 @Test void quotedAngleBracket() {
  check("quoted angle bracket", "x>y", () -> new Html("<a title='x>y'>text</a>").tag("a").findFirst().get().attr("title"));
 }
 @Test void nestedTagPrefix() {
  check("nested tag name prefix", "<divx>x</divx>tail", () -> new Html("<div><divx>x</divx>tail</div>").tag("div").findFirst().get().html().getHtml());
 }
 @Test void unfinishedTextTag() {
  check("strip unfinished tag", "x<", () -> HtmlUtil.stripHtml("x<").toString());
 }
 @Test void nextWithoutHasNext() {
  check("iterator next without hasNext", true, () -> HtmlParser.byTagName(CharBuffer.wrap("<a></a>"), "a").next() != null);
 }
 @Test void repeatedHasNextAtEnd() {
  check("iterator repeated hasNext at EOF", true, () -> { var it = HtmlParser.byTagName(CharBuffer.wrap("<a>"),"a"); return it.hasNext() && it.hasNext(); });
 }
 @Test void classWhitespace() {
  check("class whitespace", 1L, () -> new Html("<div class='a\tb'></div>").css("div.b").count());
 }

 @Test void attributeNameInsideQuotedValueIsIgnored() {
  var e = new Html("<a title=\" id='wrong'\" id='right'></a>").tag("a").findFirst().get();
  assertEquals("right", e.id());
  assertFalse(new Html("<a title=' href=wrong'></a>").tag("a").findFirst().get().hasAttr("href"));
 }
 @Test void entitiesStillDecode() {
  assertEquals("a&b", new Html("<a href='a&amp;b'></a>").tag("a").findFirst().get().attr("href"));
 }
 @Test void bufferPositionAndCaseArePreserved() {
  var buffer = CharBuffer.wrap("prefix<A ID='x'>ok</A>suffix");
  buffer.position(6); buffer.limit(buffer.limit() - 6);
  var e = new Html(buffer).tag("a").findFirst().get();
  assertEquals("x", e.id());
  assertEquals("ok", e.getTextContent());
  assertEquals(6, buffer.position());
 }
 @Test void exhaustedIteratorThrows() {
  var iterator = HtmlParser.byTagName(CharBuffer.wrap("<a>"), "a");
  assertNotNull(iterator.next());
  assertFalse(iterator.hasNext());
  assertThrows(java.util.NoSuchElementException.class, iterator::next);
 }
}
