package com.expensestrack.storage;

import com.expensestrack.model.Expense;
import com.expensestrack.model.Income;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class JsonExpenseStorage implements ExpenseStorage {

    private static final System.Logger LOGGER = System.getLogger(JsonExpenseStorage.class.getName());
    private static final String APP_DIR_NAME = "ExpensesTrack";
    private static final String DATA_FILE_NAME = "expenses.json";
    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path filePath;
    private final ObjectMapper objectMapper;

    public JsonExpenseStorage(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "File path must not be null");
        this.objectMapper = createObjectMapper();
    }

    public static JsonExpenseStorage createDefault() {
        return new JsonExpenseStorage(defaultDataFile());
    }

    public static Path defaultDataFile() {
        return resolveDataFile(
                System.getProperty("os.name", ""),
                System.getProperty("user.home", "."),
                System.getenv()
        );
    }

    static Path resolveDataFile(String osName, String userHome, Map<String, String> env) {
        String normalizedOs = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        Path baseDirectory;

        if (normalizedOs.contains("mac")) {
            baseDirectory = Path.of(userHome, "Library", "Application Support");
        } else if (normalizedOs.contains("win")) {
            String appData = env.get("APPDATA");
            if (appData != null && !appData.isBlank()) {
                baseDirectory = Path.of(appData);
            } else {
                baseDirectory = Path.of(userHome, "AppData", "Roaming");
            }
        } else {
            String xdgDataHome = env.get("XDG_DATA_HOME");
            if (xdgDataHome != null && !xdgDataHome.isBlank()) {
                baseDirectory = Path.of(xdgDataHome);
            } else {
                baseDirectory = Path.of(userHome, ".local", "share");
            }
        }

        return baseDirectory.resolve(APP_DIR_NAME).resolve(DATA_FILE_NAME);
    }

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public LoadResult load() {
        if (Files.notExists(filePath)) {
            return LoadResult.ok(TrackerData.empty());
        }

        try {
            String content = Files.readString(filePath);
            if (content.isBlank()) {
                return LoadResult.ok(TrackerData.empty());
            }

            StoredData stored = objectMapper.readValue(content, StoredData.class);
            TrackerData data = toDomain(stored);
            return LoadResult.ok(data);
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Failed to read expenses data file {0}: {1}", filePath, ex.getMessage());
            LOGGER.log(System.Logger.Level.DEBUG, "Corrupted data file details", ex);
            Path backupPath = backupCorruptedFile();
            String notice = backupPath != null
                    ? "Previous data file was unreadable and backed up to " + backupPath.getFileName() + "."
                    : "Previous data file was unreadable and has been reset.";
            return LoadResult.recovered(TrackerData.empty(), notice);
        }
    }

    @Override
    public void save(TrackerData data) {
        Objects.requireNonNull(data, "Tracker data must not be null");
        Path parent = filePath.getParent();
        Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StoredData stored = fromDomain(data);
            objectMapper.writeValue(tempFile.toFile(), stored);
            moveAtomically(tempFile, filePath);
        } catch (IOException ex) {
            throw new StorageException("Failed to save expenses to " + filePath, ex);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException cleanupEx) {
                LOGGER.log(System.Logger.Level.DEBUG, "Could not remove temp file: " + tempFile, cleanupEx);
            }
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path backupCorruptedFile() {
        String timestamp = LocalDateTime.now().format(BACKUP_TIME_FORMAT);
        Path backupPath = filePath.resolveSibling("expenses.corrupted-" + timestamp + ".json");
        try {
            Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            return backupPath;
        } catch (IOException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Could not create backup for corrupted file: " + filePath, ex);
            return null;
        }
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
    }

    private static TrackerData toDomain(StoredData stored) {
        if (stored == null) {
            return TrackerData.empty();
        }

        Income income;
        if (stored.income() == null) {
            income = Income.empty();
        } else {
            BigDecimal amount = stored.income().amount() == null ? BigDecimal.ZERO : stored.income().amount();
            String currency = (stored.income().currency() == null || stored.income().currency().isBlank())
                    ? Income.DEFAULT_CURRENCY
                    : stored.income().currency();
            income = Income.of(amount, currency);
        }

        List<Expense> expenses = new ArrayList<>();
        if (stored.expenses() != null) {
            for (StoredExpense item : stored.expenses()) {
                if (item == null || item.amount() == null) {
                    continue;
                }
                String id = (item.id() == null || item.id().isBlank())
                        ? UUID.randomUUID().toString()
                        : item.id();
                expenses.add(new Expense(id, item.amount(), item.description()));
            }
        }

        return new TrackerData(income, expenses);
    }

    private static StoredData fromDomain(TrackerData data) {
        StoredIncome storedIncome = new StoredIncome(data.income().amount(), data.income().currency());
        List<StoredExpense> storedExpenses = data.expenses().stream()
                .map(expense -> new StoredExpense(expense.id(), expense.amount(), expense.description()))
                .toList();
        return new StoredData(storedIncome, storedExpenses);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StoredData(StoredIncome income, List<StoredExpense> expenses) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StoredIncome(BigDecimal amount, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StoredExpense(String id, BigDecimal amount, String description) {
    }
}
