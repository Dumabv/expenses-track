package com.expensestrack.storage;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Debounces frequent state updates so rapid typing does not write to disk on every keystroke
 * or block the JavaFX Application Thread.
 */
public class AutoSaveCoordinator implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(AutoSaveCoordinator.class.getName());
    private static final Duration DEFAULT_DEBOUNCE = Duration.ofMillis(400);

    private final ExpenseStorage storage;
    private final long debounceMillis;
    private final Consumer<Throwable> errorHandler;
    private final ScheduledExecutorService scheduler;

    private final Object stateLock = new Object();
    private final Object ioLock = new Object();

    private TrackerData pendingSnapshot;
    private ScheduledFuture<?> scheduledTask;
    private boolean closed;

    public AutoSaveCoordinator(ExpenseStorage storage) {
        this(storage, DEFAULT_DEBOUNCE, ex -> LOGGER.log(System.Logger.Level.ERROR, "Auto-save failed", ex));
    }

    public AutoSaveCoordinator(ExpenseStorage storage, Duration debounceDelay, Consumer<Throwable> errorHandler) {
        this.storage = Objects.requireNonNull(storage, "ExpenseStorage must not be null");
        Objects.requireNonNull(debounceDelay, "Debounce delay must not be null");
        if (debounceDelay.isNegative()) {
            throw new IllegalArgumentException("Debounce delay cannot be negative");
        }
        this.debounceMillis = debounceDelay.toMillis();
        this.errorHandler = Objects.requireNonNull(errorHandler, "Error handler must not be null");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "expenses-track-autosave");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void scheduleSave(TrackerData snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot must not be null");
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            pendingSnapshot = snapshot;
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            scheduledTask = scheduler.schedule(this::flushPending, debounceMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void flushPending() {
        TrackerData snapshotToWrite;
        synchronized (stateLock) {
            snapshotToWrite = pendingSnapshot;
            pendingSnapshot = null;
            scheduledTask = null;
        }
        if (snapshotToWrite == null) {
            return;
        }
        synchronized (ioLock) {
            try {
                storage.save(snapshotToWrite);
            } catch (RuntimeException ex) {
                errorHandler.accept(ex);
            }
        }
    }

    @Override
    public void close() {
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            closed = true;
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
                scheduledTask = null;
            }
        }
        // Wait for any in-flight scheduler write to finish before flushing the latest snapshot.
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
        flushPending();
    }
}
