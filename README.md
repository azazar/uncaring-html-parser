# Uncaring HTML Parser

HTML parser that intend to be fast, but I didn't benchmark it yet. And it's yet
to be documented. And it's not a parser actually. It searches given strings for
matching tags without parsing whole HTML or building DOM.

# Example usage

```java
String html = "<a href='http://example.org/' class='link'>";

new Html(html).elements("a").stream().filter(e -> e.hasClass("link")).forEach(e -> {
    System.out.println(e.getTextContent() + " : " + e.attr("href"));
});
```
