package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.library.AdvancedFilter;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.TagFilterState;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

/**
 * Handles filter popup dialogs including:
 * - Tag filter popup with tri-state checkboxes
 * - Rating filter popup with tri-state toggles
 * - Create tag dialog
 */
public class FilterPopupHandler {

    private final MusicLibrary musicLibrary;

    public FilterPopupHandler() {
        this.musicLibrary = MusicLibrary.getInstance();
    }

    /**
     * Shows the tag filter popup near the specified window coordinates.
     */
    public void showTagFilterPopup(Window window, double windowX, double windowY) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(5);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5;");

        Label title = new Label("Filter by Tags");
        title.setStyle("-fx-font-weight: bold;");
        content.getChildren().add(title);

        ObservableList<TagEntity> tags = musicLibrary.getAvailableTags();
        AdvancedFilter filter = musicLibrary.getAdvancedFilter();

        if (tags.isEmpty()) {
            content.getChildren().add(new Label("No tags available"));
        } else {
            for (TagEntity tag : tags) {
                HBox row = createTagFilterRow(tag, filter);
                content.getChildren().add(row);
            }
        }

        // Add new tag button
        Button addTagBtn = new Button("+ New Tag");
        addTagBtn.setOnAction(e -> {
            popup.hide();
            showCreateTagDialog();
        });
        content.getChildren().add(new Separator());
        content.getChildren().add(addTagBtn);

        // Clear filters button
        if (filter.hasActiveTagFilters()) {
            Button clearBtn = new Button("Clear Tag Filters");
            clearBtn.setOnAction(e -> {
                filter.getActiveTagFilters().keySet().forEach(id ->
                        filter.setTagFilterState(id, TagFilterState.IRRELEVANT));
                musicLibrary.applyFilterAndSort();
                popup.hide();
            });
            content.getChildren().add(clearBtn);
        }

        popup.getContent().add(content);
        popup.show(window, windowX + 400, windowY + 100);
    }

    private HBox createTagFilterRow(TagEntity tag, AdvancedFilter filter) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TagFilterState state = filter.getTagFilterState(tag.getId());
        Label stateLabel = new Label(state.getSymbol());
        stateLabel.setStyle("-fx-font-size: 14px; -fx-min-width: 20px;");

        Label nameLabel = new Label(tag.getName());

        row.getChildren().addAll(stateLabel, nameLabel);
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(e -> {
            TagFilterState newState = musicLibrary.cycleTagFilter(tag.getId());
            stateLabel.setText(newState.getSymbol());
        });

        return row;
    }

    /**
     * Shows the rating filter popup near the specified window coordinates.
     */
    public void showRatingFilterPopup(Window window, double windowX, double windowY) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(5);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5;");

        Label title = new Label("Filter by Rating");
        title.setStyle("-fx-font-weight: bold;");
        content.getChildren().add(title);

        AdvancedFilter filter = musicLibrary.getAdvancedFilter();

        for (int rating = 5; rating >= 0; rating--) {
            HBox row = createRatingFilterRow(rating, filter);
            content.getChildren().add(row);
        }

        // Clear filters button
        if (filter.hasActiveRatingFilters()) {
            content.getChildren().add(new Separator());
            Button clearBtn = new Button("Clear Rating Filters");
            clearBtn.setOnAction(e -> {
                filter.getActiveRatingFilters().keySet().forEach(r ->
                        filter.setRatingFilterState(r, TagFilterState.IRRELEVANT));
                musicLibrary.applyFilterAndSort();
                popup.hide();
            });
            content.getChildren().add(clearBtn);
        }

        popup.getContent().add(content);
        popup.show(window, windowX + 600, windowY + 100);
    }

    private HBox createRatingFilterRow(int rating, AdvancedFilter filter) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TagFilterState state = filter.getRatingFilterState(rating);
        Label stateLabel = new Label(state.getSymbol());
        stateLabel.setStyle("-fx-font-size: 14px; -fx-min-width: 20px;");

        String stars = rating == 0 ? "No rating" : "★".repeat(rating) + "☆".repeat(5 - rating);
        Label ratingLabel = new Label(stars);

        row.getChildren().addAll(stateLabel, ratingLabel);
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(e -> {
            TagFilterState newState = musicLibrary.cycleRatingFilter(rating);
            stateLabel.setText(newState.getSymbol());
        });

        return row;
    }

    /**
     * Shows a dialog to create a new tag.
     */
    public void showCreateTagDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Tag");
        dialog.setHeaderText("Create a new tag");
        dialog.setContentText("Tag name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                musicLibrary.createTag(name.trim(), "#808080");
            }
        });
    }
}

