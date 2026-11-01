# Uncaring HTML Parser

An HTML parser designed for speed, though I haven't benchmarked it yet.
Documentation is still pending. It searches HTML for matching tags without
parsing the entire document or building a DOM.

## Example usage

```java
String html = "<a href='http://example.org/' class='link'>";

new Html(html).css("a.link").filter(a -> a.attr("href").contains("http:")).forEach(e -> {
    System.out.println(e.getTextContent() + " : " + e.attr("href"));
});
```

## Repositories

I don't plan to publish the library to public Maven repositories, but you can
add it as a dependency via [JitPack](https://jitpack.io/#azazar/uncaring-html-parser).
