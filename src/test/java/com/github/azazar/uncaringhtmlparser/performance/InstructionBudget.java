package com.github.azazar.uncaringhtmlparser.performance;

import java.util.Map;
import java.util.TreeMap;

/** Exact ratchet policy, independent of instrumentation and files. */
final class InstructionBudget {
    enum Status { PASS, REGRESSION, UPDATE_REQUIRED }

    static Map<String, Status> compare(Map<String, Long> actual, Map<String, Long> reference) {
        if (actual.isEmpty() || !actual.keySet().equals(reference.keySet())) {
            throw new IllegalArgumentException("Scenarios changed; explicitly migrate the baseline");
        }
        Map<String, Status> result = new TreeMap<>();
        actual.forEach((name, count) -> {
            long expected = reference.get(name);
            if (count <= 0 || expected <= 0) throw new IllegalArgumentException("Invalid instruction count");
            result.put(name, count > expected ? Status.REGRESSION :
                    count < expected ? Status.UPDATE_REQUIRED : Status.PASS);
        });
        return result;
    }
    static boolean requireAcceptable(Map<String, Status> statuses, boolean update) {
        if (statuses.containsValue(Status.REGRESSION)) {
            throw new AssertionError("REGRESSION: baseline update refused");
        }
        boolean improved = statuses.containsValue(Status.UPDATE_REQUIRED);
        if (improved && !update) {
            throw new AssertionError("UPDATE_REQUIRED: rerun mvn test -Dperformance.update=true");
        }
        return improved;
    }
}
