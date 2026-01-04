package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.ui.ThemeManager;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.util.List;

/**
 * Handles context menu creation and display for music items including:
 * - Tag management menu items
 * - Rating menu items
 * - Play and queue options
 * - Add to playlist options
 */
public class ContextMenuHandler {

    private final MusicLibrary musicLibrary;
    private final LibraryService libraryService;
    private final PlaybackQueue playbackQueue;

    private ContextMenu activeContextMenu = null;

    // Event listener
    private ContextMenuEventListener eventListener;

    /**
     * Listener interface for context menu events.
     */
    public interface ContextMenuEventListener {
        void onPlayRequested(Music music);
        void onShowCreateTagDialog();
        void onPlaylistRefreshNeeded();
        void onDisplayedPlaylistRefreshNeeded(Long playlistId);
        void onEditMetadataRequested(Music music);
    }

    public ContextMenuHandler() {
        this.musicLibrary = MusicLibrary.getInstance();
        this.libraryService = LibraryService.getInstance();
        this.playbackQueue = PlaybackQueue.getInstance();
    }

    public void setEventListener(ContextMenuEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Hides any active context menu.
     */
    public void hideActiveMenu() {
        if (activeContextMenu != null && activeContextMenu.isShowing()) {
            activeContextMenu.hide();
        }
    }

    /**
     * Shows the context menu for one or more music items.
     */
    public void showMusicContextMenu(
            List<Music> selectedMusic,
            double screenX,
            double screenY,
            TableView<Music> musicTable,
            Long displayedPlaylistId
    ) {
        if (selectedMusic.isEmpty()) return;

        hideActiveMenu();

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);
        activeContextMenu = contextMenu;

        boolean isMultiple = selectedMusic.size() > 1;

        // Add tags submenu (only for single selection)
        if (!isMultiple) {
            Music music = selectedMusic.getFirst();
            contextMenu.getItems().add(createTagMenu(music, musicTable));
            contextMenu.getItems().add(createRatingMenu(music, musicTable));
        } else {
            contextMenu.getItems().add(createBatchTagMenu(selectedMusic, musicTable));
        }

        // Play option
        contextMenu.getItems().add(new SeparatorMenuItem());
        MenuItem playItem = new MenuItem(isMultiple ? "Play First" : "Play");
        playItem.setOnAction(e -> {
            if (eventListener != null) {
                eventListener.onPlayRequested(selectedMusic.getFirst());
            }
        });
        contextMenu.getItems().add(playItem);

        // Add to queue
        MenuItem addToQueueItem = new MenuItem(isMultiple ? "Add All to Current Queue" : "Add to Current Queue");
        addToQueueItem.setOnAction(e -> {
            for (Music music : selectedMusic) {
                playbackQueue.addToQueue(music);
            }
        });
        contextMenu.getItems().add(addToQueueItem);

        // Add to playlist submenu
        contextMenu.getItems().add(createAddToPlaylistMenu(selectedMusic, displayedPlaylistId));

        // Edit Metadata option (only for single selection)
        if (!isMultiple) {
            contextMenu.getItems().add(new SeparatorMenuItem());
            MenuItem editMetadataItem = new MenuItem("Edit Metadata...");
            editMetadataItem.setOnAction(e -> {
                if (eventListener != null) {
                    eventListener.onEditMetadataRequested(selectedMusic.getFirst());
                }
            });
            contextMenu.getItems().add(editMetadataItem);
        }

        contextMenu.show(musicTable, screenX, screenY);
    }

    private Menu createTagMenu(Music music, TableView<Music> musicTable) {
        Menu addTagMenu = new Menu("Add Tag");
        ObservableList<TagEntity> availableTags = musicLibrary.getAvailableTags();

        if (availableTags.isEmpty()) {
            MenuItem noTags = new MenuItem("No tags available");
            noTags.setDisable(true);
            addTagMenu.getItems().add(noTags);
        } else {
            for (TagEntity tag : availableTags) {
                CheckMenuItem tagItem = new CheckMenuItem(tag.getName());
                tagItem.setSelected(music.getTags().contains(tag.getName()));
                tagItem.setOnAction(e -> {
                    if (tagItem.isSelected()) {
                        musicLibrary.addTagToMusic(music, tag);
                    } else {
                        musicLibrary.removeTagFromMusic(music, tag);
                    }
                    musicTable.refresh();
                });
                addTagMenu.getItems().add(tagItem);
            }
        }

        // Create new tag option
        addTagMenu.getItems().add(new SeparatorMenuItem());
        MenuItem createTag = new MenuItem("+ Create New Tag...");
        createTag.setOnAction(e -> {
            if (eventListener != null) {
                eventListener.onShowCreateTagDialog();
            }
        });
        addTagMenu.getItems().add(createTag);

        return addTagMenu;
    }

    private Menu createBatchTagMenu(List<Music> selectedMusic, TableView<Music> musicTable) {
        Menu addTagMenu = new Menu("Add Tag to All");
        ObservableList<TagEntity> availableTags = musicLibrary.getAvailableTags();

        if (availableTags.isEmpty()) {
            MenuItem noTags = new MenuItem("No tags available");
            noTags.setDisable(true);
            addTagMenu.getItems().add(noTags);
        } else {
            for (TagEntity tag : availableTags) {
                MenuItem tagItem = new MenuItem(tag.getName());
                tagItem.setOnAction(e -> {
                    for (Music music : selectedMusic) {
                        musicLibrary.addTagToMusic(music, tag);
                    }
                    musicTable.refresh();
                });
                addTagMenu.getItems().add(tagItem);
            }
        }

        return addTagMenu;
    }

    private Menu createRatingMenu(Music music, TableView<Music> musicTable) {
        Menu ratingMenu = new Menu("Set Rating");
        for (int i = 0; i <= 5; i++) {
            final int rating = i;
            String label = i == 0 ? "No rating" : "★".repeat(i) + "☆".repeat(5 - rating);
            CheckMenuItem ratingItem = new CheckMenuItem(label);
            ratingItem.setSelected(music.getRating() == i);
            ratingItem.setOnAction(e -> {
                musicLibrary.updateRating(music, rating);
                musicTable.refresh();
            });
            ratingMenu.getItems().add(ratingItem);
        }
        return ratingMenu;
    }

    private Menu createAddToPlaylistMenu(List<Music> selectedMusic, Long displayedPlaylistId) {
        boolean isMultiple = selectedMusic.size() > 1;
        Menu addToPlaylistMenu = new Menu(isMultiple ? "Add All to Playlist" : "Add to Playlist");
        List<PlaylistEntity> playlists = libraryService.getAllPlaylists();

        if (playlists.isEmpty()) {
            MenuItem noPlaylists = new MenuItem("No playlists available");
            noPlaylists.setDisable(true);
            addToPlaylistMenu.getItems().add(noPlaylists);
        } else {
            for (PlaylistEntity playlist : playlists) {
                MenuItem playlistItem = new MenuItem(playlist.getName());
                playlistItem.setOnAction(e -> addMusicsToPlaylist(selectedMusic, playlist, displayedPlaylistId));
                addToPlaylistMenu.getItems().add(playlistItem);
            }
        }

        addToPlaylistMenu.getItems().add(new SeparatorMenuItem());
        MenuItem createPlaylistItem = new MenuItem("+ New Playlist...");
        createPlaylistItem.setOnAction(e -> {
            com.luciferc137.cmp.ui.PlaylistManagerDialog.showCreatePlaylistDialog(() -> {
                if (eventListener != null) {
                    eventListener.onPlaylistRefreshNeeded();
                }
            }).ifPresent(newPlaylist -> {
                addMusicsToPlaylist(selectedMusic, newPlaylist, displayedPlaylistId);
                if (eventListener != null) {
                    eventListener.onPlaylistRefreshNeeded();
                }
            });
        });
        addToPlaylistMenu.getItems().add(createPlaylistItem);

        return addToPlaylistMenu;
    }

    private void addMusicsToPlaylist(List<Music> musics, PlaylistEntity playlist, Long displayedPlaylistId) {
        if (playlist.getId() == null) return;

        int addedCount = 0;
        for (Music music : musics) {
            if (music.getId() != null) {
                libraryService.addMusicToPlaylist(playlist.getId(), music.getId());
                addedCount++;
            }
        }

        // Refresh the view if we added to the currently displayed playlist
        if (displayedPlaylistId != null && displayedPlaylistId.equals(playlist.getId())) {
            if (eventListener != null) {
                eventListener.onDisplayedPlaylistRefreshNeeded(displayedPlaylistId);
            }
        }

        // Show confirmation
        final int count = addedCount;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Added to Playlist");
        alert.setHeaderText(null);
        alert.setContentText(count + " track(s) added to \"" + playlist.getName() + "\"");
        ThemeManager.applyDarkTheme(alert);
        alert.show();

        // Auto-hide after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(alert::close);
            } catch (InterruptedException ignored) {
            }
        }).start();
    }
}

