package com.github.azazar.uncaringhtmlparser.performance;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.github.azazar.uncaringhtmlparser.performance.InstructionBudget.Status.*;

class InstructionBudgetTest {
    @Test void exactMatchPasses() {
        assertEquals(Map.of("a", PASS), InstructionBudget.compare(Map.of("a", 100L), Map.of("a", 100L)));
    }
    @Test void singleSavedInstructionRequiresUpdate() {
        assertEquals(Map.of("a", UPDATE_REQUIRED), InstructionBudget.compare(Map.of("a", 99L), Map.of("a", 100L)));
    }
    @Test void singleExtraInstructionFails() {
        assertEquals(Map.of("a", REGRESSION), InstructionBudget.compare(Map.of("a", 101L), Map.of("a", 100L)));
    }
    @Test void improvementDoesNotHideRegression() {
        assertEquals(Map.of("a", UPDATE_REQUIRED, "b", REGRESSION), InstructionBudget.compare(
                Map.of("a", 99L, "b", 101L), Map.of("a", 100L, "b", 100L)));
    }
    @Test void scenarioChangesFail() {
        assertThrows(IllegalArgumentException.class, () -> InstructionBudget.compare(Map.of("b", 1L), Map.of("a", 1L)));
    }
    @Test void emptyOrInvalidCounterFails() {
        assertThrows(IllegalArgumentException.class, () -> InstructionBudget.compare(Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> InstructionBudget.compare(Map.of("a", 0L), Map.of("a", 1L)));
    }
    @Test void updateCannotAcceptRegressionEvenWithOtherImprovements() {
        assertThrows(AssertionError.class, () -> InstructionBudget.requireAcceptable(
                Map.of("a", UPDATE_REQUIRED, "b", REGRESSION), true));
    }
    @Test void improvementRequiresExplicitUpdate() {
        assertThrows(AssertionError.class, () -> InstructionBudget.requireAcceptable(Map.of("a", UPDATE_REQUIRED), false));
        assertTrue(InstructionBudget.requireAcceptable(Map.of("a", UPDATE_REQUIRED), true));
    }
    @Test void passingRunDoesNotRewriteBaseline() {
        assertFalse(InstructionBudget.requireAcceptable(Map.of("a", PASS), true));
    }
}
