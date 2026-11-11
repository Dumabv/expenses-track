package com.expensestrack.storage;

public interface ExpenseStorage {

    LoadResult load();

    void save(TrackerData data);
}
