package main.java.com.collegefest.gui;
import javax.swing.*;

/**
 * Generic polling helper: fetches data off the Event Dispatch Thread on a
 * fixed interval (javax.swing.Timer drives it, javax.swing.SwingWorker does
 * the actual fetch), then delivers the result back on the EDT so it's safe
 * to update Swing components directly in the result handler.
 *
 * If a fetch is still running when the next tick fires, that tick is
 * skipped — this stops slow calls from piling up into overlapping workers.
 *
 * Used by both dashboards for their live-refreshing order tables (Step 3/4
 * of the plan: "SwingWorker polling every 5 seconds").
 */
public class OrderPoller<T> {

    @FunctionalInterface
    public interface FetchTask<T> {
        T fetch() throws Exception;
    }

    @FunctionalInterface
    public interface ResultHandler<T> {
        void onResult(T result);
    }

    private final Timer timer;
    private final FetchTask<T> fetchTask;
    private final ResultHandler<T> resultHandler;
    private volatile boolean fetchInFlight = false;

    /** Optional — called on the EDT right before a fetch begins. Lets the UI show "Refreshing…". */
    private Runnable onPollStart;
    /** Optional — called on the EDT right after a fetch finishes, before results are applied. */
    private Runnable onPollComplete;

    public OrderPoller(int intervalMillis, FetchTask<T> fetchTask, ResultHandler<T> resultHandler) {
        this.fetchTask = fetchTask;
        this.resultHandler = resultHandler;
        this.timer = new Timer(intervalMillis, e -> poll());
        this.timer.setInitialDelay(0); // fetch right away on start(), not after the first full interval
    }

    public void setOnPollStart(Runnable onPollStart) {
        this.onPollStart = onPollStart;
    }

    public void setOnPollComplete(Runnable onPollComplete) {
        this.onPollComplete = onPollComplete;
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    /** Forces an immediate poll — e.g. right after the user places an order — without waiting for the next tick. */
    public void pollNow() {
        timer.restart();
    }

    private void poll() {
        if (fetchInFlight) return;
        fetchInFlight = true;
        if (onPollStart != null) onPollStart.run();

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return fetchTask.fetch();
            }

            @Override
            protected void done() {
                fetchInFlight = false;
                if (onPollComplete != null) onPollComplete.run();
                try {
                    resultHandler.onResult(get());
                } catch (Exception ex) {
                    // TODO(Step 7): replace with proper custom exceptions and a
                    // non-intrusive in-UI error indicator instead of a stack trace.
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }
}
