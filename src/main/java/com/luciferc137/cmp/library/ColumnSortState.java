package com.luciferc137.cmp.library;

/**
 * Represents the sort state for a column.
 * Cycles through: NONE → ASCENDING → DESCENDING → NONE
 */
public enum ColumnSortState {
    /**
     * Column is not used for sorting.
     */
    NONE,

    /**
     * Sort ascending (A-Z, 0-9, shortest first).
     */
    ASCENDING,

    /**
     * Sort descending (Z-A, 9-0, longest first).
     */
    DESCENDING;

    /**
     * Returns the next state in the cycle.
     */
    public ColumnSortState next() {
        return switch (this) {
            case NONE -> ASCENDING;
            case ASCENDING -> DESCENDING;
            case DESCENDING -> NONE;
        };
    }

    /**
     * Returns a symbol representing the state.
     */
    public String getSymbol() {
        return switch (this) {
            case NONE -> "";
            case ASCENDING -> " ▲";
            case DESCENDING -> " ▼";
        };
    }
}

