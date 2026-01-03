package com.luciferc137.cmp.library;

import com.luciferc137.cmp.model.Music;

import java.util.HashMap;
import java.util.Map;

/**
 * Advanced filter configuration that combines tag filters, rating filters, and sorting.
 * Supports multiple simultaneous filters with include/exclude logic.
 */
public class AdvancedFilter {

    private final Map<Long, TagFilterState> tagFilters; // tagId -> state
    private final Map<Integer, TagFilterState> ratingFilters; // rating value (0-5) -> state
    private SortableColumn sortColumn;
    private ColumnSortState sortState;
    private String searchQuery;

    public AdvancedFilter() {
        this.tagFilters = new HashMap<>();
        this.ratingFilters = new HashMap<>();
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
     * Gets the filter state for a rating value.
     */
    public TagFilterState getRatingFilterState(int rating) {
        return ratingFilters.getOrDefault(rating, TagFilterState.IRRELEVANT);
    }

    /**
     * Sets the filter state for a rating value.
     */
    public void setRatingFilterState(int rating, TagFilterState state) {
        if (state == TagFilterState.IRRELEVANT) {
            ratingFilters.remove(rating);
        } else {
            ratingFilters.put(rating, state);
        }
    }

    /**
     * Cycles the filter state for a rating to the next state.
     */
    public TagFilterState cycleRatingFilter(int rating) {
        TagFilterState current = getRatingFilterState(rating);
        TagFilterState next = current.next();
        setRatingFilterState(rating, next);
        return next;
    }

    /**
     * Returns all active rating filters (non-IRRELEVANT).
     */
    public Map<Integer, TagFilterState> getActiveRatingFilters() {
        return new HashMap<>(ratingFilters);
    }

    /**
     * Checks if any rating filters are active.
     */
    public boolean hasActiveRatingFilters() {
        return !ratingFilters.isEmpty();
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

        // Check rating filters
        for (Map.Entry<Integer, TagFilterState> entry : ratingFilters.entrySet()) {
            int ratingValue = entry.getKey();
            TagFilterState state = entry.getValue();
            boolean hasRating = music.getRating() == ratingValue;

            if (state == TagFilterState.INCLUDE && !hasRating) {
                return false; // Must have this rating but doesn't
            }
            if (state == TagFilterState.EXCLUDE && hasRating) {
                return false; // Must not have this rating but does
            }
        }

        return true;
    }

    /**
     * Clears all filters and sorting.
     */
    public void clearAll() {
        tagFilters.clear();
        ratingFilters.clear();
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
        if (hasActiveRatingFilters()) sb.append("ratings=").append(ratingFilters.size()).append(", ");
        if (hasSorting()) sb.append("sort=").append(sortColumn).append(" ").append(sortState);
        sb.append("}");
        return sb.toString();
    }
}

