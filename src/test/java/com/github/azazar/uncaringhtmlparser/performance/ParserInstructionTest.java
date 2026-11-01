package com.github.azazar.uncaringhtmlparser.performance;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

class ParserInstructionTest {
    private static final Path ROOT = Path.of(System.getProperty("project.root"));
    private static final Path BASELINE = ROOT.resolve("src/test/resources/performance/instructions.properties");

    @Test @Timeout(180) void instructionBudget() throws Exception {
        boolean initialize = Boolean.getBoolean("performance.initialize");
        boolean update = Boolean.getBoolean("performance.update");
        assertFalse(initialize && Files.exists(BASELINE), "Initialization cannot overwrite an existing baseline");
        assertTrue(initialize || Files.exists(BASELINE), "Missing baseline: explicitly use -Dperformance.initialize=true");
        Properties metadata = new Properties();
        metadata.setProperty("metadata.engine", "JDK JDI STEP_MIN/STEP_INTO; no class exclusions");
        metadata.setProperty("metadata.options", JdiInstructionCounter.OPTIONS);
        metadata.setProperty("metadata.jdk", System.getProperty("java.runtime.version") + "/" + System.getProperty("java.vendor"));
        for (Class<?> type : List.of(ParserWorkload.class, JdiInstructionCounter.class)) {
            metadata.setProperty("metadata.class." + type.getSimpleName(), digest(type.getResourceAsStream(type.getSimpleName() + ".class")));
        }
        for (Class<?> type : List.of(org.apache.commons.lang3.StringUtils.class, org.apache.commons.text.StringEscapeUtils.class)) {
            Path jar = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
            metadata.setProperty("metadata.jar." + type.getName(), digest(Files.newInputStream(jar)));
        }
        Properties previous = new Properties();
        if (Files.exists(BASELINE)) {
            try (Reader reader = Files.newBufferedReader(BASELINE)) { previous.load(reader); }
            for (String key : metadata.stringPropertyNames()) {
                assertEquals(previous.getProperty(key), metadata.getProperty(key), "Measurement environment changed: " + key);
            }
        }

        var first = JdiInstructionCounter.measure();
        var second = JdiInstructionCounter.measure();
        assertEquals(first, second, "Instruction counts differ between fresh JVMs");
        Properties measured = new Properties();
        measured.putAll(metadata);
        first.instructions().forEach((name, count) -> measured.setProperty("count." + name, count.toString()));
        first.jdkInstructions().forEach((name, count) -> measured.setProperty("jdk." + name, count.toString()));
        Path report = ROOT.resolve("target/instruction-results.properties");
        try (Writer writer = Files.newBufferedWriter(report)) { measured.store(writer, "Java bytecodes including JDK"); }
        Map<String, Long> expected = new TreeMap<>();
        previous.stringPropertyNames().stream().filter(k -> k.startsWith("count.")).forEach(k ->
                expected.put(k.substring(6), Long.parseLong(previous.getProperty(k))));
        Map<String, InstructionBudget.Status> statuses = InstructionBudget.compare(first.instructions(), initialize ? first.instructions() : expected);
        statuses.forEach((name, status) -> System.out.println(name + ": " + first.instructions().get(name)
                + " instructions (JDK: " + first.jdkInstructions().get(name) + "); " + status));
        boolean improved = InstructionBudget.requireAcceptable(statuses, update);
        if (initialize || (improved && update)) {
            Path temporary = Files.createTempFile(BASELINE.getParent(), "instructions-", ".tmp");
            try {
                try (Writer writer = Files.newBufferedWriter(temporary)) { measured.store(writer, "Instruction baseline; lower is better"); }
                Files.move(temporary, BASELINE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporary); }
        }
    }

    private static String digest(InputStream stream) throws Exception {
        try (InputStream in = stream) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }
    }
}
