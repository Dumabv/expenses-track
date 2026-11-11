package com.expensestrack.storage;

import com.expensestrack.model.Expense;
import com.expensestrack.model.Income;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoSaveCoordinatorTest {

    @TempDir
    Path tempDir;

    @Test
    void debouncesRapidSavesAndFlushesLatestSnapshotOnClose() throws InterruptedException {
        Path file = tempDir.resolve("expenses.json");
        JsonExpenseStorage delegate = new JsonExpenseStorage(file);
        AtomicInteger saveCount = new AtomicInteger();

        ExpenseStorage countingStorage = new ExpenseStorage() {
            @Override
            public LoadResult load() {
                return delegate.load();
            }

            @Override
            public void save(TrackerData data) {
                saveCount.incrementAndGet();
                delegate.save(data);
            }
        };

        try (AutoSaveCoordinator coordinator = new AutoSaveCoordinator(
                countingStorage, Duration.ofMillis(120), Throwable::printStackTrace)) {
            coordinator.scheduleSave(new TrackerData(Income.of(new BigDecimal("100"), "$"), List.of()));
            coordinator.scheduleSave(new TrackerData(Income.of(new BigDecimal("200"), "$"), List.of()));
            coordinator.scheduleSave(new TrackerData(
                    Income.of(new BigDecimal("800"), "$"),
                    List.of(Expense.of(new BigDecimal("80"), "church"))
            ));

            Thread.sleep(250);
            assertEquals(1, saveCount.get());
        }

        LoadResult reloaded = delegate.load();
        assertEquals(0, new BigDecimal("800").compareTo(reloaded.data().income().amount()));
        assertEquals(1, reloaded.data().expenses().size());
    }

    @Test
    void flushesPendingSaveImmediatelyWhenClosedBeforeDebounceExpires() {
        Path file = tempDir.resolve("expenses.json");
        JsonExpenseStorage storage = new JsonExpenseStorage(file);

        AutoSaveCoordinator coordinator = new AutoSaveCoordinator(
                storage, Duration.ofSeconds(10), Throwable::printStackTrace);
        coordinator.scheduleSave(new TrackerData(
                Income.of(new BigDecimal("950"), "€"),
                List.of(Expense.of(new BigDecimal("45"), "books"))
        ));
        coordinator.close();

        LoadResult reloaded = storage.load();
        assertEquals(0, new BigDecimal("950").compareTo(reloaded.data().income().amount()));
        assertEquals("€", reloaded.data().income().currency());
        assertEquals(1, reloaded.data().expenses().size());
        assertEquals("books", reloaded.data().expenses().get(0).description());
    }
}
