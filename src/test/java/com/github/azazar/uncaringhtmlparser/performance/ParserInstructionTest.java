package com.github.azazar.uncaringhtmlparser.performance;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

class ParserInstructionTest {
    private static final int UPPERCASE_TAG = 41_023;
    private static final int TAG = 40_734;
    private static final int CLASS = 79_096;
    private static final int ATTRIBUTE_VALUE = 77_958;
    private static final int ATTRIBUTE = 50_331;
    private static final int MISSING_ATTRIBUTE = 69_559;
    private static final int DESCENDANT = 105_955;
    private static final int MISSING_TAG = 37_687;
    private static final int PLAIN_TEXT = 1_409;
    private static final int TAGGED_TEXT = 3_906;

    private static final int UPPERCASE_TAG_BYTES = 1_040;
    private static final int TAG_BYTES = 1_040;
    private static final int CLASS_BYTES = 2_320;
    private static final int ATTRIBUTE_VALUE_BYTES = 1_968;
    private static final int ATTRIBUTE_BYTES = 1_280;
    private static final int MISSING_ATTRIBUTE_BYTES = 1_288;
    private static final int DESCENDANT_BYTES = 2_648;
    private static final int MISSING_TAG_BYTES = 360;
    private static final int PLAIN_TEXT_BYTES = 56;
    private static final int TAGGED_TEXT_BYTES = 120;

    @Test @Timeout(180) void instructionBudget() throws Exception {
        // Fail rather than silently skip the performance gate in a different environment.
        assertEquals("25.0.4.1+1-LTS/Eclipse Adoptium",
                System.getProperty("java.runtime.version") + "/" + System.getProperty("java.vendor"), "JDK changed");
        assertEquals("-Xint -Xms64m -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:ActiveProcessorCount=1", JdiInstructionCounter.OPTIONS);
        assertEquals("ec88f6e6634ca92b9ba489f48a06e83e559d8f7bbc820f834c42ce2da060cfec",
                digest(JdiInstructionCounter.class.getResourceAsStream("JdiInstructionCounter.class")), "Counter changed");
        assertEquals("4f1fb299e6e140908767c538b9fece44a2f1cfafc5af41c43a695d7f1acbba51",
                digest(ParserWorkload.class.getResourceAsStream("ParserWorkload.class")), "Workload changed");
        assertEquals("4eeeae8d20c078abb64b015ec158add383ac581571cddc45c68f0c9ae0230720",
                jarDigest(org.apache.commons.lang3.StringUtils.class), "commons-lang3 changed");
        assertEquals("770cd903fa7b604d1f7ef7ba17f84108667294b2b478be8ed1af3bffb4ae0018",
                jarDigest(org.apache.commons.text.StringEscapeUtils.class), "commons-text changed");

        var first = JdiInstructionCounter.measure();
        var second = JdiInstructionCounter.measure();
        assertEquals(first, second, "Instruction counts differ between fresh JVMs");
        Map<String, Integer> expected = Map.of(
                "A", UPPERCASE_TAG, "a", TAG, "a.link", CLASS,
                "a[class='link']", ATTRIBUTE_VALUE, "a[href]", ATTRIBUTE,
                "a[missing]", MISSING_ATTRIBUTE, "div a", DESCENDANT, "missing", MISSING_TAG,
                "plain-text", PLAIN_TEXT, "tagged-text", TAGGED_TEXT);
        Map<String, Integer> expectedBytes = Map.of(
                "A", UPPERCASE_TAG_BYTES, "a", TAG_BYTES, "a.link", CLASS_BYTES,
                "a[class='link']", ATTRIBUTE_VALUE_BYTES, "a[href]", ATTRIBUTE_BYTES,
                "a[missing]", MISSING_ATTRIBUTE_BYTES, "div a", DESCENDANT_BYTES, "missing", MISSING_TAG_BYTES,
                "plain-text", PLAIN_TEXT_BYTES, "tagged-text", TAGGED_TEXT_BYTES);
        assertAll(
                () -> assertBudget(expected, first.instructions(), "instructions"),
                () -> assertBudget(expectedBytes, first.allocatedBytes(), "allocated bytes"));
    }

    private static void assertBudget(Map<String, Integer> expected, Map<String, Long> measured, String metric) {
        assertEquals(expected.keySet(), measured.keySet(), "Scenarios changed");
        assertAll(expected.entrySet().stream().map(entry -> () -> {
            long actual = measured.get(entry.getKey());
            assertEquals(entry.getValue().longValue(), actual, () -> entry.getKey() + " " + metric + ": "
                    + (actual < entry.getValue()
                    ? "UPDATE_REQUIRED: lower the corresponding static final int to " + actual
                    : "REGRESSION: " + metric + " increased"));
        }));
    }

    private static String jarDigest(Class<?> type) throws Exception {
        return digest(Files.newInputStream(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())));
    }

    private static String digest(InputStream stream) throws Exception {
        try (InputStream in = stream) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }
    }
}
