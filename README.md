# Uncaring HTML Parser

HTML parser that intend to be fast, but I didn't benchmark it yet. And it's yet
to be documented. And it doesn't parse all HTML. It searches given HTML for
matching tags without parsing whole HTML or building it's DOM.

# Example usage

```java
String html = "<a href='http://example.org/' class='link'>";

new Html(html).byTagName("a").stream().filter(e -> e.hasClass("link")).forEach(e -> {
    System.out.println(e.getTextContent() + " : " + e.attr("href"));
});
```
