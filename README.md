# Expenses Track

A minimalist, keyboard-first personal expense calculator built with **Java 21** and **JavaFX**.

Instead of tables, charts, or sidebars, Expenses Track looks and feels like a lightweight dark text editor: enter your income once, write expenses line by line, and see what percentage of your original income each line represents in real time.

```text
Expenses Track

Income: 800 $

Expenses:
80 church                         // - 10%
80 savings                        // - 10%
125 hang out with friends         // - 15.6%

TOTAL: 285$ = 35.6%
Rest: 515$ = 64.4%
```

---

## Features

- **Text-editor aesthetic**: Minimalist `#1E1E1E` dark UI built with AtlantaFX (`PrimerDark`) and custom JavaFX CSS.
- **Instant BigDecimal math**: Exact monetary totals with `BigDecimal` (zero `double`/`float` rounding drift) and `HALF_UP` rounding for display percentages (`10%` instead of `10.0%`, `15.6%` when needed).
- **Keyboard-first workflow**:
  - `Space` or `Right` in an expense amount moves straight to its description.
  - `Enter` inserts a new expense line immediately below.
  - `Up` / `Down` arrows navigate smoothly between income and expense lines.
  - `Left` or `Backspace` on an empty description returns to the amount field.
  - `Backspace` on a completely empty line deletes it and moves focus back up.
  - `Cmd+Backspace` (macOS) / `Ctrl+Backspace` (Windows/Linux) or `Cmd/Ctrl+D` deletes the active row without confirmation popups.
- **Automatic background persistence**: Debounced JSON auto-save (`400ms`) off the JavaFX Application Thread, atomic temp-file replacement, and automatic backup + recovery if a save file is corrupted.
- **Cross-platform OS storage**: Data is stored in your operating system's standard application-data folder, never inside the project directory:
  - **macOS**: `~/Library/Application Support/ExpensesTrack/expenses.json`
  - **Windows**: `%APPDATA%\ExpensesTrack\expenses.json`
  - **Linux**: `$XDG_DATA_HOME/ExpensesTrack/expenses.json` (or `~/.local/share/ExpensesTrack/expenses.json`)

---

## Tech Stack & Dependency Choices

| Dependency | Version | Why Chosen |
|---|---|---|
| **Java** | `21` (LTS) | Modern LTS baseline with `record`s, switch expressions, and `java.nio.file` APIs. |
| **JavaFX** (`javafx-controls`) | `21.0.13` | Matches Java 21 LTS and provides cross-platform desktop UI + native Maven classifiers. |
| **AtlantaFX** (`atlantafx-base`) | `2.1.0` | Clean modern `PrimerDark` base theme; `2.1.0` targets Java 21 (whereas `3.0.0` requires Java 25). |
| **Jackson** (`jackson-databind`) | `2.18.3` | Standard JSON serialization with exact `BigDecimal` preservation (`WRITE_BIGDECIMAL_AS_PLAIN`). |
| **JUnit 5** (`junit-jupiter`) | `5.11.4` | Modern unit testing with `@TempDir` and clean assertions. |
| **Maven Wrapper** | `3.3.4` / Maven `3.9.16` | Reproducible builds without requiring a global Maven installation. |

---

## Prerequisites

- **JDK 21** (including `jpackage` for packaging). Verify with:
  ```bash
  java -version
  ```

---

## Running the Application & Tests

Use the included Maven Wrapper (`./mvnw` on macOS/Linux, `mvnw.cmd` on Windows):

### Run the Desktop Application

```bash
# macOS / Linux
./mvnw javafx:run

# Windows
mvnw.cmd javafx:run
```

### Run Unit Tests

```bash
# macOS / Linux
./mvnw test

# Windows
mvnw.cmd test
```

### Build Standard JAR

```bash
./mvnw clean package
```

---

## Packaging with `jpackage`

`pom.xml` includes a `jpackage` Maven profile (`-Pjpackage`) that collects runtime dependencies into `target/jpackage-input` and invokes `${java.home}/bin/jpackage` to output a standalone bundle in `dist/`.

> Note: `jpackage` must run on the target operating system (macOS builds `.app`/`.dmg`, Windows builds `.exe`/`.msi`, Linux builds `.deb`/`.rpm`/`app-image`).

### macOS

```bash
# Standalone .app bundle (dist/Expenses Track.app)
./mvnw clean package -Pjpackage

# macOS disk image (.dmg)
./mvnw clean package -Pjpackage -Djpackage.type=dmg
```

### Windows

```cmd
:: Standalone portable application folder (dist\Expenses Track)
mvnw.cmd clean package -Pjpackage

:: Windows installer (.msi or .exe — requires WiX Toolset v3+)
mvnw.cmd clean package -Pjpackage -Djpackage.type=msi
```

### Linux

```bash
# Standalone portable app-image (dist/Expenses Track)
./mvnw clean package -Pjpackage

# Debian package (.deb) or RPM (.rpm)
./mvnw clean package -Pjpackage -Djpackage.type=deb
```

### Equivalent Raw `jpackage` Command

If you want to inspect or customize the underlying tool call directly:

```bash
jpackage \
  --type app-image \
  --name "Expenses Track" \
  --app-version 1.0.0 \
  --vendor "Expenses Track" \
  --input target/jpackage-input \
  --main-jar expenses-track-1.0.0.jar \
  --main-class com.expensestrack.Launcher \
  --dest dist
```

---

## Project Architecture

The codebase separates domain logic, persistence, and JavaFX presentation into four clean packages:

```text
src/main/java/com/expensestrack/
├── Launcher.java                # Non-Application entry point for packaged JAR / jpackage
├── ExpensesTrackApp.java        # JavaFX Application lifecycle, wiring, and clean shutdown
├── model/
│   ├── Income.java              # Immutable Income record (amount >= 0, non-blank currency)
│   ├── Expense.java             # Immutable Expense record (id, amount >= 0, description)
│   └── ExpenseSummary.java      # Calculated snapshot: line percentages, total, rest
├── service/
│   ├── CalculationService.java  # Pure BigDecimal calculation & HALF_UP percentage rounding
│   ├── AmountFormatter.java     # Input parsing and monetary / percentage text formatting
│   └── ExpenseService.java      # Thread-safe state holder + summary listener notifications
├── storage/
│   ├── ExpenseStorage.java      # Persistence abstraction (load / save)
│   ├── JsonExpenseStorage.java  # Jackson JSON persistence, OS paths, atomic writes, backup
│   ├── AutoSaveCoordinator.java # Debounced background ScheduledExecutorService writer
│   ├── TrackerData.java         # Immutable snapshot passed from service to storage
│   ├── LoadResult.java          # Load outcome carrying optional corruption-recovery notice
│   └── StorageException.java    # Unchecked wrapper for storage I/O failures
└── ui/
    ├── MainView.java            # Top-level layout, JavaFX ObjectProperty binding, scroll & status
    ├── IncomeView.java          # Editable income amount and currency ComboBox
    ├── ExpenseListView.java     # Dynamic row list, keyboard navigation, trailing blank row
    ├── ExpenseRow.java          # Single editable line (amount, description, // - %, delete)
    ├── SummaryView.java         # TOTAL and Rest display bound to ExpenseSummary
    └── UiFormatting.java        # Numeric TextFormatter filter, auto-sizing fields, font lookup
```

### Data Flow

```text
User types in IncomeView / ExpenseRow
   │
   ▼
UI parses draft input via AmountFormatter (empty lines are omitted from active expenses)
   │
   ▼
ExpenseService updates Income / Expense list & calls CalculationService.summarize(...)
   │
   ├──► MainView updates ObjectProperty<ExpenseSummary> on FX Application Thread
   │       ├──► ExpenseListView refreshes "// - X%" on each row
   │       └──► SummaryView refreshes "TOTAL" and "Rest" (muted coral when negative)
   │
   └──► AutoSaveCoordinator schedules a debounced write (400ms) on a daemon thread
           └──► JsonExpenseStorage writes to *.tmp and atomically replaces expenses.json
```

---

## Important Architectural Decisions

1. **Zero JavaFX imports in `model`, `service`, and `storage`**:
   All calculations, validations, and persistence live in plain Java classes. This makes unit tests fast and independent of any graphics toolkit or display server.
2. **`BigDecimal` with single-step division for percentages**:
   Monetary totals (`totalExpenses` and `rest`) are never rounded. Percentages compute `(amount * 100).divide(income, 1, RoundingMode.HALF_UP)` directly in one division step to prevent double-rounding errors, and whole percentages omit `.0` (`10%`). Zero income safely returns `0%`.
3. **Separate `Launcher` class**:
   Putting `main` in `Launcher` (which does not extend `javafx.application.Application`) ensures JavaFX loads reliably from non-modular classpath bundles created by `jpackage`.
4. **Immutable snapshots + debounced atomic saves**:
   `ExpenseService` notifies `AutoSaveCoordinator` with an immutable `TrackerData` snapshot. Rapid typing collapses into a single background write after `400ms`, written first to `expenses.json.tmp` and moved atomically over `expenses.json`. Closing the window immediately flushes any pending write.
5. **Graceful corruption recovery**:
   If `expenses.json` is malformed or contains invalid negative numbers, `JsonExpenseStorage` copies the unreadable file to `expenses.corrupted-<timestamp>.json` before resetting so user data is never silently lost.

---

## Key Classes to Study & Java Concepts

If you are studying this project to prepare for a Java internship, focus on these classes in order:

1. **`Income` & `Expense` (`model/`)**:
   - *Concepts*: Java `record`s, compact constructors for domain invariant validation, defensive defaults, immutability (`withAmount`, `withCurrency`).
2. **`CalculationService` & `AmountFormatter` (`service/`)**:
   - *Concepts*: `BigDecimal` scale vs precision, `RoundingMode.HALF_UP`, avoiding `ArithmeticException` on repeating decimals and zero divisors, `Optional<BigDecimal>` for safe parsing.
3. **`ExpenseService` (`service/`)**:
   - *Concepts*: Encapsulation, defensive copying (`List.copyOf`), thread safety (`synchronized`, `CopyOnWriteArrayList`), Observer pattern via `Consumer<ExpenseSummary>`.
4. **`JsonExpenseStorage` & `AutoSaveCoordinator` (`storage/`)**:
   - *Concepts*: Interfaces for testability (`ExpenseStorage`), `java.nio.file.Path` & `Files`, atomic file moves, private DTO records decoupling JSON layout from domain models, `ScheduledExecutorService` debouncing, `AutoCloseable` shutdown cleanup.
5. **`ExpenseRow`, `ExpenseListView`, & `MainView` (`ui/`)**:
   - *Concepts*: Component composition in JavaFX, `TextFormatter` input filtering, JavaFX `ObjectProperty` & `Bindings`, avoiding infinite update loops with `suppressEvents` flags, `Platform.runLater` thread dispatch.
