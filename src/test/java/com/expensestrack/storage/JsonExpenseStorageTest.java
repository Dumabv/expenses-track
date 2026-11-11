package com.expensestrack.storage;

import com.expensestrack.model.Expense;
import com.expensestrack.model.Income;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonExpenseStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsStateWithExactBigDecimals() {
        Path file = tempDir.resolve("nested").resolve("expenses.json");
        JsonExpenseStorage storage = new JsonExpenseStorage(file);

        TrackerData original = new TrackerData(
                Income.of(new BigDecimal("800.50"), "€"),
                List.of(
                        new Expense("id-1", new BigDecimal("80"), "church"),
                        new Expense("id-2", new BigDecimal("33.335"), "")
                )
        );

        storage.save(original);
        LoadResult result = storage.load();

        assertFalse(result.recoveredFromError());
        assertEquals(0, new BigDecimal("800.50").compareTo(result.data().income().amount()));
        assertEquals("€", result.data().income().currency());
        assertEquals(2, result.data().expenses().size());
        assertEquals("id-1", result.data().expenses().get(0).id());
        assertEquals("church", result.data().expenses().get(0).description());
        assertEquals(0, new BigDecimal("33.335").compareTo(result.data().expenses().get(1).amount()));
        assertEquals("", result.data().expenses().get(1).description());
    }

    @Test
    void returnsEmptyStateWhenFileDoesNotExistOrIsBlank() throws IOException {
        Path missingFile = tempDir.resolve("missing.json");
        LoadResult missingResult = new JsonExpenseStorage(missingFile).load();
        assertFalse(missingResult.recoveredFromError());
        assertEquals(TrackerData.empty(), missingResult.data());

        Path blankFile = tempDir.resolve("blank.json");
        Files.writeString(blankFile, "   \n  ");
        LoadResult blankResult = new JsonExpenseStorage(blankFile).load();
        assertFalse(blankResult.recoveredFromError());
        assertEquals(TrackerData.empty(), blankResult.data());
    }

    @Test
    void recoversGracefullyAndBacksUpCorruptedJson() throws IOException {
        Path corruptedFile = tempDir.resolve("expenses.json");
        Files.writeString(corruptedFile, "{ invalid json !!!");

        LoadResult result = new JsonExpenseStorage(corruptedFile).load();

        assertTrue(result.recoveredFromError());
        assertEquals(TrackerData.empty(), result.data());
        try (var files = Files.list(tempDir)) {
            boolean hasBackup = files.anyMatch(p -> p.getFileName().toString().startsWith("expenses.corrupted-"));
            assertTrue(hasBackup, "Corrupted file should be backed up before reset");
        }
    }

    @Test
    void recoversWhenSavedJsonContainsNegativeAmount() throws IOException {
        Path invalidFile = tempDir.resolve("expenses.json");
        Files.writeString(invalidFile, """
                {
                  "income": { "amount": 800, "currency": "$" },
                  "expenses": [ { "amount": -50, "description": "bad" } ]
                }
                """);

        LoadResult result = new JsonExpenseStorage(invalidFile).load();

        assertTrue(result.recoveredFromError());
        assertEquals(TrackerData.empty(), result.data());
    }

    @Test
    void generatesMissingExpenseIdWhenReadingManuallyEditedJson() throws IOException {
        Path editedFile = tempDir.resolve("expenses.json");
        Files.writeString(editedFile, """
                {
                  "income": { "amount": 800, "currency": "$" },
                  "expenses": [ { "amount": 80, "description": "church" } ]
                }
                """);

        LoadResult result = new JsonExpenseStorage(editedFile).load();

        assertFalse(result.recoveredFromError());
        assertEquals(1, result.data().expenses().size());
        assertFalse(result.data().expenses().get(0).id().isBlank());
        assertEquals("church", result.data().expenses().get(0).description());
    }

    @Test
    void resolvesStandardOsAppDataDirectories() {
        Path macPath = JsonExpenseStorage.resolveDataFile("Mac OS X", "/Users/alice", Map.of());
        assertEquals(Path.of("/Users/alice/Library/Application Support/ExpensesTrack/expenses.json"), macPath);

        Path winPath = JsonExpenseStorage.resolveDataFile(
                "Windows 11", "C:\\Users\\alice", Map.of("APPDATA", "C:\\Users\\alice\\AppData\\Roaming"));
        assertEquals(Path.of("C:\\Users\\alice\\AppData\\Roaming", "ExpensesTrack", "expenses.json"), winPath);

        Path linuxDefault = JsonExpenseStorage.resolveDataFile("Linux", "/home/alice", Map.of());
        assertEquals(Path.of("/home/alice/.local/share/ExpensesTrack/expenses.json"), linuxDefault);

        Path linuxXdg = JsonExpenseStorage.resolveDataFile(
                "Linux", "/home/alice", Map.of("XDG_DATA_HOME", "/home/alice/.data"));
        assertEquals(Path.of("/home/alice/.data/ExpensesTrack/expenses.json"), linuxXdg);
    }
}
