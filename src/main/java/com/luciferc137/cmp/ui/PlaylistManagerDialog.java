package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller for the playlist management dialog.
 */
public class PlaylistManagerDialog {

    private final LibraryService libraryService = LibraryService.getInstance();
    private Consumer<PlaylistEntity> onPlaylistSelected;
    private Runnable onPlaylistCreated;

    /**
     * Shows a dialog to select or create a playlist.
     *
     * @param onSelect Callback when a playlist is selected
     * @param onCreate Callback when a new playlist is created
     */
    public static void show(Consumer<PlaylistEntity> onSelect, Runnable onCreate) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Playlist Manager");
        dialog.setHeaderText("Select or create a playlist");

        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        ButtonType createButtonType = new ButtonType("New Playlist", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, createButtonType, ButtonType.CANCEL);

        LibraryService libraryService = LibraryService.getInstance();
        List<PlaylistEntity> playlists = libraryService.getAllPlaylists();

        ListView<PlaylistEntity> listView = new ListView<>();
        listView.getItems().addAll(playlists);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PlaylistEntity item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        listView.setPrefHeight(200);
        listView.setPrefWidth(300);

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("Available playlists:"), listView);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);

        // Enable/disable select button based on selection
        dialog.getDialogPane().lookupButton(selectButtonType).setDisable(true);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            dialog.getDialogPane().lookupButton(selectButtonType).setDisable(newVal == null);
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent()) {
            if (result.get() == selectButtonType) {
                PlaylistEntity selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && onSelect != null) {
                    onSelect.accept(selected);
                }
            } else if (result.get() == createButtonType) {
                showCreatePlaylistDialog(onCreate);
            }
        }
    }

    /**
     * Shows a dialog to create a new playlist.
     */
    public static Optional<PlaylistEntity> showCreatePlaylistDialog(Runnable onCreate) {
        Dialog<PlaylistEntity> dialog = new Dialog<>();
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("Enter playlist details");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Playlist name");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Disable create button if name is empty
        dialog.getDialogPane().lookupButton(createButtonType).setDisable(true);
        nameField.textProperty().addListener((obs, old, newVal) -> {
            dialog.getDialogPane().lookupButton(createButtonType).setDisable(
                    newVal == null || newVal.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String name = nameField.getText().trim();
                if (!name.isEmpty()) {
                    LibraryService libraryService = LibraryService.getInstance();
                    Optional<PlaylistEntity> created = libraryService.createPlaylist(name);
                    if (created.isPresent()) {
                        if (onCreate != null) {
                            onCreate.run();
                        }
                        return created.get();
                    }
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Shows a simple dialog to select a playlist for adding a song.
     *
     * @param onSelect Callback with the selected playlist
     */
    public static void showPlaylistSelector(Consumer<PlaylistEntity> onSelect) {
        LibraryService libraryService = LibraryService.getInstance();
        List<PlaylistEntity> playlists = libraryService.getAllPlaylists();

        if (playlists.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Playlists");
            alert.setHeaderText(null);
            alert.setContentText("No playlists available. Create one first.");
            alert.showAndWait();
            return;
        }

        ChoiceDialog<PlaylistEntity> dialog = new ChoiceDialog<>(playlists.get(0), playlists);
        dialog.setTitle("Add to Playlist");
        dialog.setHeaderText("Select a playlist");
        dialog.setContentText("Playlist:");

        dialog.showAndWait().ifPresent(onSelect);
    }
}

