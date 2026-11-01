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
    private static final int UPPERCASE_TAG = 45_946;
    private static final int TAG = 45_657;
    private static final int CLASS = 99_714;
    private static final int ATTRIBUTE_VALUE = 98_924;
    private static final int ATTRIBUTE = 61_794;
    private static final int MISSING_ATTRIBUTE = 90_348;
    private static final int DESCENDANT = 113_680;
    private static final int MISSING_TAG = 38_479;

    @Test @Timeout(180) void instructionBudget() throws Exception {
        // Fail rather than silently skip the performance gate in a different environment.
        assertEquals("25.0.4.1+1-LTS/Eclipse Adoptium",
                System.getProperty("java.runtime.version") + "/" + System.getProperty("java.vendor"), "JDK changed");
        assertEquals("-Xint -Xms64m -Xmx128m -XX:+UseSerialGC -XX:ActiveProcessorCount=1", JdiInstructionCounter.OPTIONS);
        assertEquals("8881c85c011297c5b5e5946937ed814fcceedc723884fb0884c0cfe09f27bf4f",
                digest(JdiInstructionCounter.class.getResourceAsStream("JdiInstructionCounter.class")), "Counter changed");
        assertEquals("38f3303174da668a92aa46d7d97b24a4d1272b5f7b568ca85e23f5a2ae716e14",
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
                "a[missing]", MISSING_ATTRIBUTE, "div a", DESCENDANT, "missing", MISSING_TAG);
        assertEquals(expected.keySet(), first.instructions().keySet(), "Scenarios changed");
        assertAll(expected.entrySet().stream().map(entry -> () -> {
            long actual = first.instructions().get(entry.getKey());
            assertEquals(entry.getValue().longValue(), actual, () -> entry.getKey() + ": "
                    + (actual < entry.getValue()
                    ? "UPDATE_REQUIRED: lower the corresponding static final int to " + actual
                    : "REGRESSION: instruction count increased"));
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
