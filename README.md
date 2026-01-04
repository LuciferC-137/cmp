# CMP - Custom Music Player

> ⚠️ **This project is currently in active development.** Features may be incomplete, unstable, or subject to change.

A modern, feature-rich music player built with JavaFX, designed to manage and play your local music library with advanced filtering and organization capabilities.
This project has been highly inspired by AIMP music player. It aims to be as portable as possible, requiring only Java, VLC and SQLite installation.

## Features

### 🎵 Audio Playback
- **Multi-format support**: MP3, M4A, FLAC, OGG, WAV, AAC, WMA, AIFF
- **VLC-powered playback**: Uses VLCJ for reliable audio playback
- **Waveform visualization**: Visual representation of the audio track with progress indicator
- **All basic audio player functionnalities**: Play, Pause, Stop, Seek, Volume control

### 📚 Library Management
- **SQLite database**: Local database to store your music library metadata
- **Folder synchronization**: Scan a folder to import music and extract metadata automatically
- **Metadata extraction & edition**: Uses JAudioTagger to read and edit all common metadata

### 🏷️ Organization
- **Tags**: Create custom tags and assign them to any track
- **Ratings**: Rate your music from 1 to 5 stars (click stars to set rating)
- **Playlists**: Organize your music into playlists

### 🔍 Advanced Filtering & Sorting
- **Column sorting**: Click on Title, Artist, Album, or Duration column headers to sort
  - Cycles through: Unsorted → Ascending (▲) → Descending (▼)
  - Only one sort column active at a time
- **Tag filtering**: Click the Tags column header to open filter popup
  - Tri-state filter: Irrelevant (○) → Include (✓) → Exclude (✗)
  - Include: Only show tracks with this tag
  - Exclude: Hide tracks with this tag
  - Multiple tag filters work together (AND logic)
- **Rating filtering**: Same tri-state filtering for ratings 0-5
- **Search**: Text search across title, artist, and album fields

### ⚙️ Settings
- **Music folder selection**: Choose which folder to scan for music
- **Persistent settings**: Preferences saved between sessions
- **Session restoration**: Resume playback and queue from last session
- **Settings stored in**: `~/.cmp/settings.json`
- **Database stored in**: `~/.cmp/library.db`

## Requirements

- **Java 21** or higher
- **VLC Media Player** installed on your system (required for audio playback)
- **Gradle** (wrapper included)

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/LuciferC-137/cmp.git
   cd cmp
   ```

2. Make sure VLC is installed:
   - **Linux**: `sudo apt install vlc` (Debian/Ubuntu) or equivalent
   - **macOS**: `brew install vlc` or download from [videolan.org](https://www.videolan.org/vlc/)
   - **Windows**: Download from [videolan.org](https://www.videolan.org/vlc/)

3. Build and run:
   ```bash
   ./gradlew run
   ```

## Usage

### First Launch

1. Click the **⚙ Settings** button in the toolbar
2. In the Library section, click **Browse...** to select your music folder
3. Click **Resync** to scan the folder and import your music
4. Close settings - your music library is now populated!

### Playing Music

1. Select a track in the table
2. Click **Play** or double-click the track
3. Use the waveform bar to see progress and seek
4. Use **Pause** to pause/resume and **Stop** to stop playback

### Queue & Playlists

When a song si double-clicked or played, it is added to the default playlist named `Local`.
This playlist is always overwritten when a new song is played from the left table.
It acts otherwise as any other playlist, except that it cannot be deleted.
To create custom playlists:
1. Click the **⚙** icon above the playlist panel (or go in Settings → Playlists)
2. Create a new playlist and give it a name with `Create Playlist` button
3. You can add song using right click → `Add to Playlist` → `My Playlist` (multiple selection supported)
4. Delete a playlist from the same settings menu

### Organizing with Tags

1. **Create a tag**: Click the Tags column header → "+ New Tag"
2. **Assign tags**: Right-click a track → "Add Tag" → select tags
3. **Filter by tag**: Click Tags header → click a tag to cycle through:
   - ○ Irrelevant (not used in filter)
   - ✓ Include (must have this tag)
   - ✗ Exclude (must NOT have this tag)

### Rating Music

- Click the stars (☆☆☆☆☆) directly in the Rating column to set a rating
- Click the same star again to remove the rating
- Filter by rating using the Rating column header dropdown
- Ratings can also be added from the right-click context menu (to support multiple selection)

### Sorting

- Click any sortable column header (Title, Artist, Album, Duration)
- First click: Sort ascending (▲)
- Second click: Sort descending (▼)
- Third click: Remove sort

## Project Structure

```
src/main/java/com/luciferc137/cmp/
├── MainApp.java               # Application entry point
├── audio/                     # Audio playback components
│   ├── AudioFormat.java       # Enum of supported audio formats
│   ├── AudioMetadata.java     # Unified audio metadata representation
│   ├── AudioPlayer.java       # Audio player interface
│   ├── VlcAudioPlayer.java    # VLC-based audio player
│   ├── VolumeControl.java     # Volume management interface
│   └── WaveformExtractor.java # Audio waveform extraction
├── database/                  # Database layer
│   ├── DatabaseManager.java   # SQLite connection & schema
│   ├── LibraryService.java    # High-level database operations
│   ├── dao/                   # Data Access Objects
│   │   ├── MusicDao.java
│   │   ├── PlaylistDao.java
│   │   ├── TagDao.java
│   │   └── SyncLogDao.java
│   ├── importer/                     # Import tools
│   │   └── AimpPlaylistImporter.java # Import AIMP windows playlist
│   ├── model/                # Database entities
│   │   ├── MusicEntity.java
│   │   ├── PlaylistEntity.java
│   │   ├── TagEntity.java
│   │   └── SyncLogEntity.java
│   └── sync/                 # Folder synchronization
│       ├── AudioMetadataExtractor.java
│       ├── LibrarySyncService.java
│       ├── SyncProgressListener.java
│       └── SyncResult.java
├── library/                  # Library management & filtering
│   ├── AdvancedFilter.java   # Filter configuration
│   ├── ColumnSortState.java  # Sort state enum
│   ├── FilterType.java       # Filter types enum
│   ├── LibraryFilter.java    # Simple filter class
│   ├── Music.java            # Object representing a playable track
│   ├── MusicLibrary.java     # Main library interface
│   ├── PlaybackQueue.java    # Queue management
│   ├── TagFilterState.java   # Tri-state filter enum
│   ├── SortableColumn.java   # Sortable columns enum
│   └── TagFilterState.java   # Tri-state filter enum
├── model/                    # UI models
│   └── Music.java            # Music track model
├── settings/                 # Application settings
│   ├── PlayBackSession.java  # Used to restore user sessions
│   ├── Settings.java         # Settings data model
│   └── SettingsManager.java  # Load/save settings
└── ui/
    ├── BatchCoverArtDialog.java
    ├── ConverArtLoader.java
    ├── MainController.java            # Main UI controller
    ├── MetadataEditorDialog.java
    ├── PlaylistManagerDialog.java
    ├── ThemeManager.java
    ├── WaveformProgressBar.java
    ├── handlers/                      # UI event handlers
    │   ├── PlaybackHandler.java
    │   ├── PlaylistPanelHandler.java
    │   ├── TableHandler.java
    │   ├── ContextMenuHandler.java
    │   ├── FilterPopupHandler.java
    │   ├── SessionHandler.java
    │   └── ShuffleLoopHandler.java
    └── settings/
        ├── SettingsController.java
        └── SettingsWindow.java
    
 
```

## Database Schema

```sql
music (id, path, title, artist, album, duration, hash, rating, created_at, updated_at)
playlist (id, name, created_at, updated_at)
playlist_music (playlist_id, music_id, position, added_at)
tag (id, name, color, created_at)
music_tag (music_id, tag_id, added_at)
sync_log (id, sync_date, folder_path, files_added, files_updated, files_removed, status)
```

## Technologies Used

- **JavaFX 21** - UI framework
- **VLCJ 4.8** - VLC bindings for Java (audio playback)
- **SQLite** - Local database
- **JAudioTagger 3.0** - Audio metadata extraction
- **Gson** - JSON serialization for settings
- **Gradle** - Build system

## Known Limitations

- VLC must be installed on the system for audio playback
- Waveform extraction may not work for all audio formats
- Large libraries may take time to sync initially

## Roadmap

- [ ] Equalizer
- [ ] Keyboard shortcuts
- [ ] Lyrics auto-fetching and better display
- [ ] YouTube Music auto-downloader

## License

This project is under GPL-3.0 License

by LuciferC137

