package com.github.azazar.uncaringhtmlparser.performance;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Uses the JDK's debugger backend, with no custom agent or bytecode rewriting. */
final class JdiInstructionCounter {
    static final String OPTIONS = "-Xint -Xms64m -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:ActiveProcessorCount=1";

    record Measurement(Map<String, Long> instructions, Map<String, Long> jdkInstructions,
                       Map<String, Long> allocatedBytes) { }

    static Measurement measure(String selectedScenario) throws Exception {
        var connector = Bootstrap.virtualMachineManager().defaultConnector();
        var arguments = connector.defaultArguments();
        String target = ParserWorkload.class.getName();
        arguments.get("main").setValue(target + " " + selectedScenario);
        arguments.get("home").setValue(System.getProperty("java.home"));
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        arguments.get("options").setValue(OPTIONS + " -cp \"" + classpath + "\"");
        VirtualMachine vm = connector.launch(arguments);
        Process process = vm.process();
        var requests = vm.eventRequestManager();
        var prepare = requests.createClassPrepareRequest();
        prepare.addClassFilter(target); // Only marker installation is filtered, never stepping.
        prepare.enable();
        Map<String, Long> totals = new TreeMap<>();
        Map<String, Long> jdkTotals = new TreeMap<>();
        Map<String, Long> allocations = new TreeMap<>();
        long count = 0, jdkCount = 0;
        String scenario = null;
        StepRequest step = null;
        boolean active = false, finished = false;
        try {
            while (!finished) {
                EventSet events = vm.eventQueue().remove();
                for (Event event : events) {
                    if (event instanceof ClassPrepareEvent prepared) {
                        for (String name : List.of("begin", "end", "allocation")) {
                            var marker = requests.createBreakpointRequest(prepared.referenceType().methodsByName(name).getFirst().location());
                            marker.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                            marker.putProperty("marker", name);
                            marker.enable();
                        }
                        prepare.disable();
                    } else if (event instanceof BreakpointEvent marker) {
                        if ("allocation".equals(marker.request().getProperty("marker"))) {
                            var values = marker.thread().frame(0).getArgumentValues();
                            String selector = ((StringReference) values.getFirst()).value();
                            long bytes = ((LongValue) values.get(1)).value();
                            assertTrue(bytes > 0, "Empty allocation counter: " + selector);
                            assertNull(allocations.putIfAbsent(selector, bytes), "Duplicate allocations: " + selector);
                        } else if ("begin".equals(marker.request().getProperty("marker"))) {
                            assertFalse(active, "Nested measurement");
                            scenario = ((StringReference) marker.thread().frame(0).getArgumentValues().getFirst()).value();
                            count = 0;
                            jdkCount = 0;
                            active = true;
                            step = requests.createStepRequest(marker.thread(), StepRequest.STEP_MIN, StepRequest.STEP_INTO);
                            // Async delivery avoids a debugger round-trip for every instruction.
                            // No class exclusion filters: java.*, jdk.*, libraries and lambda adapters all count.
                            step.setSuspendPolicy(EventRequest.SUSPEND_NONE);
                            step.enable();
                        } else {
                            assertTrue(active, "End marker without beginning");
                            active = false;
                            requests.deleteEventRequest(step);
                            assertTrue(count > 0 && jdkCount > 0, "Empty counter or missing JDK coverage");
                            assertNull(totals.putIfAbsent(scenario, count), "Duplicate measurement: " + scenario);
                            jdkTotals.put(scenario, jdkCount);
                        }
                    } else if (event instanceof StepEvent instruction && active) {
                        var location = instruction.location();
                        String owner = location.declaringType().name();
                        // A step and breakpoint can share the end-marker location in either order.
                        if (owner.equals(target) && location.method().name().equals("end")) continue;
                        count++;
                        if (owner.startsWith("java.") || owner.startsWith("javax.") || owner.startsWith("jdk.") || owner.startsWith("sun.")) jdkCount++;
                    } else if (event instanceof VMDeathEvent) {
                        finished = true;
                    } else if (event instanceof VMDisconnectEvent) {
                        throw new AssertionError("Target disconnected before normal VM death");
                    }
                }
                events.resume();
            }
            assertFalse(active, "Target exited during measurement");
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Target did not exit");
            assertEquals(0, process.exitValue(), new String(process.getErrorStream().readAllBytes()));
            assertEquals(Set.of(selectedScenario), totals.keySet());
            assertEquals(totals.keySet(), allocations.keySet(), "Missing allocation measurements");
            return new Measurement(totals, jdkTotals, allocations);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            try { vm.dispose(); } catch (VMDisconnectedException ignored) { }
        }
    }
}
