package com.luciferc137.cmp.library;

/**
 * Represents the filter state for a tag or rating.
 * Cycles through: IRRELEVANT → INCLUDE → EXCLUDE → IRRELEVANT
 */
public enum TagFilterState {
    /**
     * Tag/rating is ignored in filtering.
     */
    IRRELEVANT,

    /**
     * Music must have this tag/rating to be shown.
     */
    INCLUDE,

    /**
     * Music must NOT have this tag/rating to be shown.
     */
    EXCLUDE;

    /**
     * Returns the next state in the cycle.
     */
    public TagFilterState next() {
        return switch (this) {
            case IRRELEVANT -> INCLUDE;
            case INCLUDE -> EXCLUDE;
            case EXCLUDE -> IRRELEVANT;
        };
    }

    /**
     * Returns a symbol representing the state.
     */
    public String getSymbol() {
        return switch (this) {
            case IRRELEVANT -> "○";
            case INCLUDE -> "✓";
            case EXCLUDE -> "✗";
        };
    }
}

