# CMP - Custom Music Player

> ⚠️ **This project is currently in active development.** Features may be incomplete, unstable, or subject to change.

A modern, feature-rich music player built with JavaFX, designed to manage and play your local music library with advanced filtering and organization capabilities.
This project has been highly inspired by AIMP music player. It aims to be as portable as possible, requiring only Java, VLC and SQLite installation.

## Features

### 🎵 Audio Playback
- **Multi-format support**: MP3, M4A, FLAC, OGG, WAV, AAC, WMA, AIFF
- **VLC-powered playback**: Uses VLCJ for reliable audio playback
- **Waveform visualization**: Visual representation of the audio track with progress indicator
- **Seek functionality**: Click anywhere on the waveform to jump to that position
- **Volume control**: Adjustable volume with persistent settings

### 📚 Library Management
- **SQLite database**: Local database to store your music library metadata
- **Folder synchronization**: Scan a folder to import music and extract metadata automatically
- **Metadata extraction**: Uses JAudioTagger to read title, artist, album, and duration from audio files
- **On-demand sync**: Synchronization only happens when you request it, not automatically

### 🏷️ Organization
- **Tags**: Create custom tags and assign them to any track
- **Ratings**: Rate your music from 1 to 5 stars (click stars to set rating)
- **Playlists**: Organize your music into playlists (database structure ready, not yet implemented in UI)

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
- **Persistent settings**: Volume and preferences saved between sessions
- **Settings stored in**: `~/.cmp/settings.json`
- **Database stored in**: `~/.cmp/library.db`

## Requirements

- **Java 21** or higher
- **VLC Media Player** installed on your system (required for audio playback)
- **Gradle** (wrapper included)

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/cmp.git
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

### Sorting

- Click any sortable column header (Title, Artist, Album, Duration)
- First click: Sort ascending (▲)
- Second click: Sort descending (▼)
- Third click: Remove sort

## Project Structure

```
src/main/java/com/luciferc137/cmp/
├── MainApp.java              # Application entry point
├── audio/                    # Audio playback components
│   ├── VlcAudioPlayer.java   # VLC-based audio player
│   ├── VolumeControl.java    # Volume management
│   └── WaveformExtractor.java # Audio waveform extraction
├── database/                 # Database layer
│   ├── DatabaseManager.java  # SQLite connection & schema
│   ├── LibraryService.java   # High-level database operations
│   ├── dao/                  # Data Access Objects
│   │   ├── MusicDao.java
│   │   ├── PlaylistDao.java
│   │   ├── TagDao.java
│   │   └── SyncLogDao.java
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
│   ├── MusicLibrary.java     # Main library interface
│   ├── AdvancedFilter.java   # Filter configuration
│   ├── TagFilterState.java   # Tri-state filter enum
│   ├── ColumnSortState.java  # Sort state enum
│   ├── SortableColumn.java   # Sortable columns enum
│   ├── FilterType.java       # Filter types enum
│   ├── LibraryFilter.java    # Simple filter class
│   └── SortOrder.java        # Sort order enum
├── model/                    # UI models
│   └── Music.java            # Music track model
├── settings/                 # Application settings
│   ├── Settings.java
│   └── SettingsManager.java
└── ui/                       # User interface
    ├── MainController.java   # Main window controller
    ├── WaveformProgressBar.java # Custom waveform component
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

- [ ] Playlist management UI
- [ ] Album art display
- [ ] Equalizer
- [ ] Keyboard shortcuts
- [ ] Dark/Light theme toggle
- [ ] Import/Export playlists
- [ ] Scrobbling support (Last.fm)
- [ ] CSS styling for UI customization

## License

This project is currently unlicensed. All rights reserved.

by LuciferC137

