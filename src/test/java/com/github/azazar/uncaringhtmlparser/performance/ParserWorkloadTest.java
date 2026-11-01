package com.github.azazar.uncaringhtmlparser.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ParserWorkloadTest {
    @Test @Timeout(5) void completesRepeatedlyWithoutDebugger() throws Exception {
        ParserWorkload.main(new String[0]);
        ParserWorkload.main(new String[0]);
    }
}
