package com.github.azazar.uncaringhtmlparser.performance;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

class ParserInstructionTest {
    private static final int STANDALONE_TAGS = 689_484;
    private static final int STANDALONE_VALUE = 1_602_962;
    private static final int STANDALONE_CLASS = 1_637_986;
    private static final int STANDALONE_MISSING = 1_602_529;
    private static final int NESTED_DESCENDANT = 7_600_586;
    private static final int REPEATED_FRAGMENT = 2_968_931;
    private static final int COMPILE_SELECTOR = 2_923;
    private static final int COMPILED_TAGS = 1_342_765;
    private static final int PLAIN_TEXT = 12_128;
    private static final int TAGGED_TEXT = 64_704;

    private static final int STANDALONE_TAGS_BYTES = 37_976;
    private static final int STANDALONE_VALUE_BYTES = 81_608;
    private static final int STANDALONE_CLASS_BYTES = 90_216;
    private static final int STANDALONE_MISSING_BYTES = 67_872;
    private static final int NESTED_DESCENDANT_BYTES = 9_784;
    private static final int REPEATED_FRAGMENT_BYTES = 4_072;
    private static final int COMPILE_SELECTOR_BYTES = 384;
    private static final int COMPILED_TAGS_BYTES = 51_432;
    private static final int PLAIN_TEXT_BYTES = 56;
    private static final int TAGGED_TEXT_BYTES = 552;

    @TestFactory @Timeout(60) Stream<DynamicTest> instructionBudget() throws Exception {
        // Fail rather than silently skip the performance gate in a different environment.
        assertEquals("25.0.4.1+1-LTS/Eclipse Adoptium",
                System.getProperty("java.runtime.version") + "/" + System.getProperty("java.vendor"), "JDK changed");
        assertEquals("-Xint -Xms64m -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:ActiveProcessorCount=1", JdiInstructionCounter.OPTIONS);
        assertEquals("4437feae00d72ab9bb7ded41045a752415707efe4073a381abb648b0cdb7a864",
                digest(JdiInstructionCounter.class.getResourceAsStream("JdiInstructionCounter.class")), "Counter changed");
        assertEquals("7132cf7a826d236ab1821146b4eb96814ae6f2c1b4d8a71e9698e08678bdd46f",
                digest(ParserWorkload.class.getResourceAsStream("ParserWorkload.class")), "Workload changed");
        assertEquals("65de35778b07ed3384e8bd84e81caa4e69eb6d71e5d0b049ba87313ccacbee3d",
                digest(ParserWorkload.class.getResourceAsStream("/performance/w3.html")), "Fixture changed");
        assertEquals("4eeeae8d20c078abb64b015ec158add383ac581571cddc45c68f0c9ae0230720",
                jarDigest(org.apache.commons.lang3.StringUtils.class), "commons-lang3 changed");
        assertEquals("770cd903fa7b604d1f7ef7ba17f84108667294b2b478be8ed1af3bffb4ae0018",
                jarDigest(org.apache.commons.text.StringEscapeUtils.class), "commons-text changed");

        Map<String, Integer> expected = Map.of(
                "standalone-tags", STANDALONE_TAGS,
                "standalone-value", STANDALONE_VALUE,
                "standalone-class", STANDALONE_CLASS,
                "standalone-missing", STANDALONE_MISSING,
                "nested-descendant", NESTED_DESCENDANT,
                "repeated-fragment", REPEATED_FRAGMENT,
                "compile-selector", COMPILE_SELECTOR,
                "compiled-tags", COMPILED_TAGS,
                "plain-text", PLAIN_TEXT,
                "tagged-text", TAGGED_TEXT);
        Map<String, Integer> expectedBytes = Map.of(
                "standalone-tags", STANDALONE_TAGS_BYTES,
                "standalone-value", STANDALONE_VALUE_BYTES,
                "standalone-class", STANDALONE_CLASS_BYTES,
                "standalone-missing", STANDALONE_MISSING_BYTES,
                "nested-descendant", NESTED_DESCENDANT_BYTES,
                "repeated-fragment", REPEATED_FRAGMENT_BYTES,
                "compile-selector", COMPILE_SELECTOR_BYTES,
                "compiled-tags", COMPILED_TAGS_BYTES,
                "plain-text", PLAIN_TEXT_BYTES,
                "tagged-text", TAGGED_TEXT_BYTES);
        return Arrays.stream(ParserWorkload.SELECTORS).map(scenario -> DynamicTest.dynamicTest(scenario,
                () -> assertTimeoutPreemptively(Duration.ofMinutes(5), () -> {
                    var measured = JdiInstructionCounter.measure(scenario);
                    assertAll(
                            () -> assertBudget(Map.of(scenario, expected.get(scenario)), measured.instructions(), "instructions"),
                            () -> assertBudget(Map.of(scenario, expectedBytes.get(scenario)), measured.allocatedBytes(), "allocated bytes"));
                }, scenario)));
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
