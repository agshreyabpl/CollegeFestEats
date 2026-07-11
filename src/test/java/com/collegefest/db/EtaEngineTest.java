package com.collegefest.db;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The pure ETA formula: eta = base × (queue + 1) + 2.
 * Tested through the computeEta seam, so no MongoDB connection is needed.
 */
public class EtaEngineTest {

    @Test
    public void workedExampleFromTheDocs() {
        // base 3 min, 2 orders already queued -> 3 × (2+1) + 2 = 11
        assertEquals(11, EtaEngine.computeEta(3, 2));
    }

    @Test
    public void emptyQueueMeansBasePlusBuffer() {
        assertEquals(7, EtaEngine.computeEta(5, 0));   // 5 × 1 + 2
    }

    @Test
    public void nonPositiveBaseFallsBackToFiveMinutes() {
        assertEquals(EtaEngine.computeEta(5, 3), EtaEngine.computeEta(0, 3));
        assertEquals(EtaEngine.computeEta(5, 3), EtaEngine.computeEta(-4, 3));
    }

    @Test
    public void rushHourScalesLinearlyWithQueueLength() {
        int quiet = EtaEngine.computeEta(4, 1);    // 4×2+2 = 10
        int busy  = EtaEngine.computeEta(4, 9);    // 4×10+2 = 42
        assertEquals(10, quiet);
        assertEquals(42, busy);
        assertTrue(busy > quiet, "more queue must never shorten the ETA");
    }

    // ---------------- extra edge cases ----------------

    @Test
    public void baseMinutesOfExactlyOneStillObeysTheFormula() {
        // boundary just above the "non-positive" fallback branch
        assertEquals(3, EtaEngine.computeEta(1, 0));  // 1×1+2 = 3
        assertEquals(6, EtaEngine.computeEta(1, 3));  // 1×4+2 = 6
    }

    @Test
    public void zeroBaseMinutesIsTreatedAsTheFiveMinuteFallback() {
        // baseMinutes == 0 is the exact boundary of the "<= 0" guard
        assertEquals(EtaEngine.computeEta(5, 0), EtaEngine.computeEta(0, 0));
    }

    @Test
    public void veryLargeQueueLengthDoesNotOverflowOrGoNegative() {
        // queueLength arrives as a long from countDocuments(); make sure the
        // int cast in computeEta doesn't wrap around for a big backlog.
        int eta = EtaEngine.computeEta(5, 1000);
        assertEquals(5 * 1001 + 2, eta);
        assertTrue(eta > 0, "eta must never come back negative");
    }

    @Test
    public void bufferMinutesAreAlwaysAddedEvenWithAnEmptyQueue() {
        // isolates the "+2" buffer: with queueLength 0, eta must be
        // strictly greater than baseMinutes alone.
        int base = 6;
        int eta = EtaEngine.computeEta(base, 0);
        assertEquals(base + 2, eta);
    }
}
