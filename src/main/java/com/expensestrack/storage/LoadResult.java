package com.expensestrack.storage;

import java.util.Objects;

public record LoadResult(TrackerData data, boolean recoveredFromError, String noticeMessage) {

    public LoadResult {
        Objects.requireNonNull(data, "Data must not be null");
    }

    public static LoadResult ok(TrackerData data) {
        return new LoadResult(data, false, null);
    }

    public static LoadResult recovered(TrackerData fallbackData, String noticeMessage) {
        return new LoadResult(fallbackData, true, noticeMessage);
    }
}
