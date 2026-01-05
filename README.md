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
- **Rating filtering**: Filter by rating using the Rating column header dropdown
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
5. Remove elements from a playlist using right click → `Remove from Playlist` in the playlist view

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

## Building & Packaging

This section explains how to compile, package, and install CMP on Linux.

### Prerequisites

Before building, ensure you have:

```bash
# Check Java version (21+ required)
java --version

# Check jpackage is available (included in JDK 14+)
which jpackage

# Install VLC (required for audio playback)
sudo apt install vlc  # Debian/Ubuntu
```

### Available Gradle Tasks

| Task | Description |
|------|-------------|
| `./gradlew run` | Run the application directly (development mode) |
| `./gradlew build` | Compile and run tests |
| `./gradlew fatJar` | Create a fat JAR with all dependencies (`build/libs/cmp-1.0.0-all.jar`) |
| `./gradlew jpackageImage` | Create a standalone Linux application image |
| `./gradlew jpackage` | Create a `.deb` package (use `-PinstallerType=rpm` for RPM) |
| `./gradlew installDesktop` | Create app image + install `.desktop` file for launcher integration |
| `./gradlew clean` | Delete the build directory |

### Quick Start (Development)

```bash
# Clone and run immediately
git clone https://github.com/LuciferC-137/cmp.git
cd cmp
./gradlew run
```

### Creating a Standalone Application

#### Option 1: Application Image (Recommended)

Creates a self-contained application folder with embedded JRE:

```bash
./gradlew jpackageImage
```

Output: `build/jpackage/CMP/`

To run the application:
```bash
./build/jpackage/CMP/bin/CMP
```

#### Option 2: With Desktop Integration

Creates the application image and registers it in your Linux desktop:

```bash
./gradlew installDesktop
```

This creates:
- Application at `build/jpackage/CMP/`
- Desktop entry at `~/.local/share/applications/cmp.desktop`

The application will appear in your application menu.

#### Option 3: DEB/RPM Package (System-wide Installation)

Use the provided packaging script:

```bash
# Create a .deb package (Debian/Ubuntu)
./packaging/linux/build-linux-package.sh deb

# Create a .rpm package (Fedora/RHEL)
./packaging/linux/build-linux-package.sh rpm

# Create both
./packaging/linux/build-linux-package.sh all
```

Install the generated package:
```bash
# For DEB
sudo dpkg -i build/jpackage/cmp_1.0.0_amd64.deb
# If there are missing dependencies:
sudo apt-get install -f

# For RPM
sudo rpm -i build/jpackage/cmp-1.0.0-1.x86_64.rpm
```

### Reinstalling After Code Changes

```bash
# Quick reinstall (development)
./gradlew clean run

# Rebuild application image
./gradlew clean jpackageImage

# Full reinstall with desktop integration
./gradlew clean installDesktop
```

### Adding New Dependencies

1. **Edit `build.gradle.kts`** and add your dependency in the `dependencies` block:

```kotlin
dependencies {
    // ...existing dependencies...
    
    // Add your new dependency
    implementation("group.id:artifact-id:version")
}
```

2. **Sync Gradle** (in IDE) or run:
```bash
./gradlew --refresh-dependencies
```

3. **Rebuild** the project:
```bash
./gradlew build
```

#### Example: Adding a new library

```kotlin
// Example: Adding Apache Commons IO
implementation("commons-io:commons-io:2.15.1")

// Example: Adding a test dependency
testImplementation("org.mockito:mockito-core:5.8.0")
```

### Project Build Structure

After building, key outputs are:

```
build/
├── classes/java/main/          # Compiled .class files
├── libs/
│   └── cmp-1.0.0-all.jar       # Fat JAR (after ./gradlew fatJar)
├── jpackage/
│   ├── cmp_1.0.0_amd64.deb     # DEB package (after ./gradlew jpackage)
│   └── CMP/                    # Standalone application (after jpackageImage)
│       ├── bin/CMP             # Executable launcher
│       └── lib/
│           ├── app/cmp-1.0.0-all.jar
│           └── runtime/        # Embedded JRE
└── resources/main/             # Copied resources (FXML, CSS, icons)
```

### Troubleshooting

| Problem | Solution |
|---------|----------|
| `jpackage not found` | Ensure JDK 21+ is installed and `JAVA_HOME` is set |
| `VLC not found` at runtime | Install VLC: `sudo apt install vlc` |
| Icon missing in jpackage | Ensure `packaging/linux/cmp.png` exists |
| Desktop entry not showing | Run `update-desktop-database ~/.local/share/applications` |
| Permission denied on scripts | Run `chmod +x packaging/linux/*.sh gradlew` |

### Useful Commands Reference

```bash
# List all available Gradle tasks
./gradlew tasks --all

# Check dependencies
./gradlew dependencies

# Run with debug output
./gradlew run --debug

# Build without tests
./gradlew build -x test

# Force refresh dependencies
./gradlew build --refresh-dependencies
```

---

## Complete Build & Packaging Guide

This section provides a comprehensive guide for building, packaging, and maintaining the CMP application.

### Build Process Overview

```
Source Code → Compile → Fat JAR → jpackage → .deb/.rpm
     ↓           ↓          ↓          ↓
  src/main   build/classes  build/libs  build/jpackage
```

### Step-by-Step: Creating a .deb Package

```bash
# 1. Clean previous builds
./gradlew clean

# 2. Create the fat JAR (all dependencies bundled)
./gradlew fatJar

# 3. Verify the JAR was created
ls -la build/libs/cmp-1.0.0-all.jar

# 4. Create the .deb package
./gradlew jpackage

# 5. Verify the .deb was created
ls -la build/jpackage/cmp_1.0.0_amd64.deb

# 6. Install the package
sudo dpkg -i build/jpackage/cmp_1.0.0_amd64.deb

# 7. If missing dependencies, fix with:
sudo apt-get install -f
```

### Uninstalling

```bash
# Remove the installed package
sudo dpkg -r cmp

# Or with apt
sudo apt remove cmp
```

### Verify Package Contents

Before installing, you can inspect the .deb contents:

```bash
# List all files in the package
dpkg-deb --contents build/jpackage/cmp_1.0.0_amd64.deb

# Check for the executable
dpkg-deb --contents build/jpackage/cmp_1.0.0_amd64.deb | grep -E "bin/|\.jar"

# Extract package info
dpkg-deb --info build/jpackage/cmp_1.0.0_amd64.deb
```

A valid package should contain:
- `/opt/cmp/bin/CMP` - The executable launcher
- `/opt/cmp/lib/app/cmp-1.0.0-all.jar` - The application JAR
- `/opt/cmp/lib/runtime/` - Embedded Java runtime
- `/opt/cmp/lib/CMP.png` - Application icon
- `/usr/share/applications/cmp-CMP.desktop` - Desktop entry

### Debugging Launch Issues

If the application doesn't start after installation:

```bash
# 1. Try launching from terminal to see errors
/opt/cmp/bin/CMP

# 2. Check if the executable exists and is runnable
ls -la /opt/cmp/bin/
file /opt/cmp/bin/CMP

# 3. Check for missing libraries
ldd /opt/cmp/bin/CMP 2>&1 | grep "not found"

# 4. Verify VLC is installed (required dependency)
which vlc
vlc --version

# 5. Check application logs (if any)
cat ~/.cmp/logs/*.log 2>/dev/null
```

### Common Build Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Empty/broken .deb | Fat JAR not created | Run `./gradlew fatJar` before `jpackage` |
| "Fat JAR not found" error | Clean removed the JAR | Run `./gradlew fatJar jpackage` together |
| App icon not showing | Missing cmp.png | Ensure `packaging/linux/cmp.png` exists |
| App won't start (silent fail) | Missing VLC | Install VLC: `sudo apt install vlc` |
| Desktop entry missing | jpackage issue | Check `/usr/share/applications/` |
| Java errors at runtime | Wrong Java version | Ensure JDK 21+ is used for building |

### Development Workflow

```bash
# Quick development cycle
./gradlew run                    # Run directly without packaging

# Test changes in packaged form
./gradlew clean fatJar jpackageImage
./build/jpackage/CMP/bin/CMP     # Run the packaged app

# Full release build
./gradlew clean fatJar jpackage
```

### Version Update Checklist

When updating the version number:

1. Edit `build.gradle.kts`:
   ```kotlin
   version = "1.1.0"  // Update version here
   ```

2. Rebuild everything:
   ```bash
   ./gradlew clean fatJar jpackage
   ```

3. The new package will be: `build/jpackage/cmp_1.1.0_amd64.deb`

### Files & Directories Reference

| Path | Description |
|------|-------------|
| `build.gradle.kts` | Build configuration, dependencies, tasks |
| `src/main/java/` | Java source code |
| `src/main/resources/` | FXML, CSS, icons |
| `packaging/linux/` | Linux packaging files (icon, scripts) |
| `build/libs/` | Output: Fat JAR |
| `build/jpackage/` | Output: .deb, .rpm, app image |
| `~/.cmp/` | User data: settings.json, library.db |

### Gradle Task Dependencies

```
jpackage ──depends──► fatJar ──depends──► compileJava
    │
    └──creates──► build/jpackage/cmp_*.deb

jpackageImage ──depends──► fatJar
    │
    └──creates──► build/jpackage/CMP/

installDesktop ──depends──► jpackageImage
    │
    └──creates──► ~/.local/share/applications/cmp.desktop
```

## License

This project is under GPL-3.0 License

by LuciferC137

