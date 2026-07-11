package com.collegefest.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderPoller drives both dashboards' live refresh. These tests exercise the
 * real javax.swing.Timer + SwingWorker plumbing (no mocking of Swing itself),
 * using CountDownLatch to wait for async results instead of Thread.sleep
 * guesses. Runs fine headless - Timer/SwingWorker don't need a real display.
 */
public class OrderPollerTest {

    @Test
    @Timeout(5)
    public void startFetchesImmediatelyWithoutWaitingAFullInterval() throws InterruptedException {
        // initialDelay is set to 0 in the constructor specifically so the
        // first fetch happens right away, not after the configured interval.
        CountDownLatch firstResult = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);

        OrderPoller<String> poller = new OrderPoller<>(10_000, () -> {
            callCount.incrementAndGet();
            return "ok";
        }, result -> firstResult.countDown());

        poller.start();
        try {
            assertTrue(firstResult.await(2, TimeUnit.SECONDS),
                    "first fetch should fire almost immediately, not after the 10s interval");
            assertEquals(1, callCount.get());
        } finally {
            poller.stop();
        }
    }

    @Test
    @Timeout(5)
    public void pollsRepeatedlyOnTheGivenInterval() throws InterruptedException {
        CountDownLatch threeResults = new CountDownLatch(3);

        OrderPoller<Integer> poller = new OrderPoller<>(100, () -> 42,
                result -> threeResults.countDown());

        poller.start();
        try {
            assertTrue(threeResults.await(3, TimeUnit.SECONDS),
                    "poller must keep firing on the interval, not just once");
        } finally {
            poller.stop();
        }
    }

    @Test
    @Timeout(5)
    public void stopPreventsAnyFurtherResults() throws InterruptedException {
        AtomicInteger resultCount = new AtomicInteger(0);
        CountDownLatch firstResult = new CountDownLatch(1);

        OrderPoller<Integer> poller = new OrderPoller<>(80, () -> 1, result -> {
            resultCount.incrementAndGet();
            firstResult.countDown();
        });

        poller.start();
        assertTrue(firstResult.await(2, TimeUnit.SECONDS));
        poller.stop();

        int countAtStop = resultCount.get();
        // Wait a few more intervals' worth of time; the count must not grow.
        Thread.sleep(400);
        assertEquals(countAtStop, resultCount.get(),
                "stop() must prevent any further scheduled fetches from landing");
    }

    @Test
    @Timeout(5)
    public void slowFetchesAreNotOverlappedByTheNextTick() throws InterruptedException {
        // A fetch that takes longer than the interval must not have two
        // copies running at once - the poller should skip the tick that
        // would otherwise pile another worker on top of the in-flight one.
        AtomicInteger concurrentFetches = new AtomicInteger(0);
        AtomicInteger maxObservedConcurrency = new AtomicInteger(0);
        CountDownLatch twoResults = new CountDownLatch(2);

        OrderPoller<Integer> poller = new OrderPoller<>(50, () -> {
            int inFlight = concurrentFetches.incrementAndGet();
            maxObservedConcurrency.updateAndGet(prev -> Math.max(prev, inFlight));
            try {
                Thread.sleep(200); // much slower than the 50ms interval
            } finally {
                concurrentFetches.decrementAndGet();
            }
            return 1;
        }, result -> twoResults.countDown());

        poller.start();
        try {
            assertTrue(twoResults.await(4, TimeUnit.SECONDS));
            assertEquals(1, maxObservedConcurrency.get(),
                    "overlapping ticks must be skipped, never run two fetches at once");
        } finally {
            poller.stop();
        }
    }

    @Test
    @Timeout(5)
    public void onPollStartAndOnPollCompleteHooksFireAroundEachFetch() throws InterruptedException {
        CountDownLatch startedThenFinished = new CountDownLatch(1);
        StringBuilder order = new StringBuilder();

        OrderPoller<Integer> poller = new OrderPoller<>(10_000, () -> 5, result -> {
            order.append("R"); // result handler runs after onPollComplete
            startedThenFinished.countDown();
        });
        poller.setOnPollStart(() -> order.append("S"));
        poller.setOnPollComplete(() -> order.append("C"));

        poller.start();
        try {
            assertTrue(startedThenFinished.await(2, TimeUnit.SECONDS));
            assertEquals("SCR", order.toString(),
                    "start hook, then complete hook, then the result handler - in that order");
        } finally {
            poller.stop();
        }
    }

    @Test
    @Timeout(5)
    public void anExceptionInTheFetchTaskDoesNotCrashLaterPolls() throws InterruptedException {
        AtomicInteger attempt = new AtomicInteger(0);
        CountDownLatch secondGoodResult = new CountDownLatch(1);

        OrderPoller<String> poller = new OrderPoller<>(100, () -> {
            if (attempt.incrementAndGet() == 1) {
                throw new RuntimeException("simulated network blip");
            }
            return "recovered";
        }, result -> {
            if ("recovered".equals(result)) secondGoodResult.countDown();
        });

        poller.start();
        try {
            assertTrue(secondGoodResult.await(3, TimeUnit.SECONDS),
                    "one failed fetch must not stop later polls from succeeding");
        } finally {
            poller.stop();
        }
    }
}
