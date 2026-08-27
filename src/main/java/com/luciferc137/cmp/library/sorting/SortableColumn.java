package com.luciferc137.cmp.library.sorting;

/**
 * Enum representing sortable columns in the music table.
 */
public enum SortableColumn {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("⏲");

    private final String displayName;

    SortableColumn(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayName(ColumnSortState state) {
        return displayName + state.getSymbol();
    }

    @Override
    public String toString() {
        return displayName;
    }
}

