package com.luciferc137.cmp.library;

import java.util.HashMap;
import java.util.Map;

/**
 * Advanced filter configuration that combines tag filters, rating filters, and sorting.
 * Supports multiple simultaneous filters with include/exclude logic.
 */
public class AdvancedFilter {

    private final Map<Long, TagFilterState> tagFilters; // tagId -> state
    private final java.util.Set<Integer> selectedRatings; // rating values (0-5) that are selected
    private SortableColumn sortColumn;
    private ColumnSortState sortState;
    private String searchQuery;

    public AdvancedFilter() {
        this.tagFilters = new HashMap<>();
        this.selectedRatings = new java.util.HashSet<>();
        this.sortColumn = null;
        this.sortState = ColumnSortState.NONE;
        this.searchQuery = null;
    }

    // ==================== Tag Filters ====================

    /**
     * Gets the filter state for a tag.
     */
    public TagFilterState getTagFilterState(Long tagId) {
        return tagFilters.getOrDefault(tagId, TagFilterState.IRRELEVANT);
    }

    /**
     * Sets the filter state for a tag.
     */
    public void setTagFilterState(Long tagId, TagFilterState state) {
        if (state == TagFilterState.IRRELEVANT) {
            tagFilters.remove(tagId);
        } else {
            tagFilters.put(tagId, state);
        }
    }

    /**
     * Cycles the filter state for a tag to the next state.
     */
    public TagFilterState cycleTagFilter(Long tagId) {
        TagFilterState current = getTagFilterState(tagId);
        TagFilterState next = current.next();
        setTagFilterState(tagId, next);
        return next;
    }

    /**
     * Returns all active tag filters (non-IRRELEVANT).
     */
    public Map<Long, TagFilterState> getActiveTagFilters() {
        return new HashMap<>(tagFilters);
    }

    /**
     * Checks if any tag filters are active.
     */
    public boolean hasActiveTagFilters() {
        return !tagFilters.isEmpty();
    }

    // ==================== Rating Filters ====================

    /**
     * Checks if a rating is selected in the filter.
     */
    public boolean isRatingSelected(int rating) {
        return selectedRatings.contains(rating);
    }

    /**
     * Sets whether a rating is selected in the filter.
     */
    public void setRatingSelected(int rating, boolean selected) {
        if (selected) {
            selectedRatings.add(rating);
        } else {
            selectedRatings.remove(rating);
        }
    }

    /**
     * Toggles the selection of a rating.
     * @return the new selection state
     */
    public boolean toggleRatingFilter(int rating) {
        boolean newState = !isRatingSelected(rating);
        setRatingSelected(rating, newState);
        return newState;
    }

    /**
     * Returns all selected ratings.
     */
    public java.util.Set<Integer> getSelectedRatings() {
        return new java.util.HashSet<>(selectedRatings);
    }

    /**
     * Checks if any rating filters are active.
     */
    public boolean hasActiveRatingFilters() {
        return !selectedRatings.isEmpty();
    }

    /**
     * Clears all rating filters.
     */
    public void clearRatingFilters() {
        selectedRatings.clear();
    }

    // ==================== Sorting ====================

    /**
     * Gets the current sort column.
     */
    public SortableColumn getSortColumn() {
        return sortColumn;
    }

    /**
     * Gets the current sort state.
     */
    public ColumnSortState getSortState() {
        return sortState;
    }

    /**
     * Sets the sort column and state.
     */
    public void setSort(SortableColumn column, ColumnSortState state) {
        this.sortColumn = column;
        this.sortState = state;
    }

    /**
     * Cycles the sort state for a column.
     * If clicking a different column, starts with ASCENDING.
     */
    public void cycleSort(SortableColumn column) {
        if (column == sortColumn) {
            sortState = sortState.next();
            if (sortState == ColumnSortState.NONE) {
                sortColumn = null;
            }
        } else {
            sortColumn = column;
            sortState = ColumnSortState.ASCENDING;
        }
    }

    /**
     * Clears all sorting.
     */
    public void clearSort() {
        sortColumn = null;
        sortState = ColumnSortState.NONE;
    }

    /**
     * Checks if sorting is active.
     */
    public boolean hasSorting() {
        return sortColumn != null && sortState != ColumnSortState.NONE;
    }

    // ==================== Search ====================

    /**
     * Gets the current search query.
     */
    public String getSearchQuery() {
        return searchQuery;
    }

    /**
     * Sets the search query.
     */
    public void setSearchQuery(String query) {
        this.searchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
    }

    /**
     * Clears the search query.
     */
    public void clearSearch() {
        this.searchQuery = null;
    }

    /**
     * Checks if a search is active.
     */
    public boolean hasSearch() {
        return searchQuery != null && !searchQuery.isEmpty();
    }

    // ==================== Filter Application ====================

    /**
     * Tests if a music item passes all active filters.
     *
     * @param music The music to test
     * @param musicTagIds The tag IDs associated with the music
     * @return true if the music passes all filters
     */
    public boolean matches(Music music, java.util.Set<Long> musicTagIds) {
        // Check search query
        if (hasSearch()) {
            String query = searchQuery.toLowerCase();
            boolean matchesSearch =
                (music.title != null && music.title.toLowerCase().contains(query)) ||
                (music.artist != null && music.artist.toLowerCase().contains(query)) ||
                (music.album != null && music.album.toLowerCase().contains(query));
            if (!matchesSearch) {
                return false;
            }
        }

        // Check tag filters
        for (Map.Entry<Long, TagFilterState> entry : tagFilters.entrySet()) {
            Long tagId = entry.getKey();
            TagFilterState state = entry.getValue();
            boolean hasTag = musicTagIds.contains(tagId);

            if (state == TagFilterState.INCLUDE && !hasTag) {
                return false; // Must have this tag but doesn't
            }
            if (state == TagFilterState.EXCLUDE && hasTag) {
                return false; // Must not have this tag but does
            }
        }

        // Check rating filters (OR logic - music matches if its rating is among selected ratings)
        if (hasActiveRatingFilters()) {
            if (!selectedRatings.contains(music.getRating())) {
                return false; // Music's rating is not among the selected ratings
            }
        }

        return true;
    }

    /**
     * Clears all filters and sorting.
     */
    public void clearAll() {
        tagFilters.clear();
        clearRatingFilters();
        clearSort();
        clearSearch();
    }

    /**
     * Checks if any filter is active.
     */
    public boolean hasAnyFilter() {
        return hasActiveTagFilters() || hasActiveRatingFilters() || hasSearch();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AdvancedFilter{");
        if (hasSearch()) sb.append("search='").append(searchQuery).append("', ");
        if (hasActiveTagFilters()) sb.append("tags=").append(tagFilters.size()).append(", ");
        if (hasActiveRatingFilters()) sb.append("ratings=").append(selectedRatings.size()).append(", ");
        if (hasSorting()) sb.append("sort=").append(sortColumn).append(" ").append(sortState);
        sb.append("}");
        return sb.toString();
    }
}

