# Uncaring HTML Parser

HTML parser that intend to be fast, but I didn't benchmark it yet. And it's yet
to be documented. And it doesn't parse all HTML. It searches given HTML for
matching tags without parsing whole HTML or building it's DOM.

# Example usage

```java
String html = "<a href='http://example.org/' class='link'>";

new Html(html).bySelector("a.link").filter(a -> a.attr("href").contains("http:")).forEach(e -> {
    System.out.println(e.getTextContent() + " : " + e.attr("href"));
});
```

## Repositories

I'm not planning to upload it to any public Maven repositories, but it can be
linked using [JitPack repository](https://jitpack.io/#azazar/uncaring-html-parser/0.9.2 "JitPack repository").