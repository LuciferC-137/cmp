package com.luciferc137.cmp.database.importer;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.MusicEntity;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importer for AIMP playlist files (.xspf and .aimppl4 formats).
 * Converts Windows paths to Linux paths and matches tracks in the library.
 */
public class AimpPlaylistImporter {

    private static final Pattern WINDOWS_PATH_PATTERN = Pattern.compile("^file:///([A-Za-z]):/(.*)$");
    private static final Pattern WINDOWS_DRIVE_PATTERN = Pattern.compile("^([A-Za-z]):/(.*)$");
    private static final Pattern WINDOWS_BACKSLASH_PATTERN = Pattern.compile("^([A-Za-z]):\\\\(.*)$");

    private final LibraryService libraryService;
    private final String linuxMusicBasePath;

    /**
     * Creates a new AIMP playlist importer.
     * Automatically detects the current user for path conversion.
     */
    public AimpPlaylistImporter() {
        this.libraryService = LibraryService.getInstance();
        this.linuxMusicBasePath = detectLinuxMusicBasePath();
    }

    /**
     * Creates a new AIMP playlist importer with a custom Linux base path.
     *
     * @param linuxMusicBasePath The base path for music on Linux (e.g., "/home/user/Musique")
     */
    public AimpPlaylistImporter(String linuxMusicBasePath) {
        this.libraryService = LibraryService.getInstance();
        this.linuxMusicBasePath = linuxMusicBasePath;
    }

    /**
     * Detects the Linux music base path using the current user.
     *
     * @return The detected music base path
     */
    private String detectLinuxMusicBasePath() {
        String user = System.getProperty("user.name");
        return "/home/" + user + "/Musique";
    }

    /**
     * Gets the detected Linux music base path.
     *
     * @return The Linux music base path
     */
    public String getLinuxMusicBasePath() {
        return linuxMusicBasePath;
    }

    /**
     * Imports an AIMP playlist file (auto-detects format from extension).
     *
     * @param playlistFile The playlist file to import (.xspf or .aimppl4)
     * @return The import result
     */
    public ImportResult importPlaylist(File playlistFile) {
        String fileName = playlistFile.getName().toLowerCase();
        if (fileName.endsWith(".xspf")) {
            return importXspfPlaylist(playlistFile);
        } else if (fileName.endsWith(".aimppl4")) {
            return importAimppl4Playlist(playlistFile);
        } else {
            ImportResult result = new ImportResult();
            result.setError("Unsupported file format: " + fileName);
            result.setSuccess(false);
            return result;
        }
    }

    /**
     * Imports an AIMP .xspf playlist file.
     *
     * @param xspfFile The .xspf file to import
     * @return The import result
     */
    public ImportResult importXspfPlaylist(File xspfFile) {
        ImportResult result = new ImportResult();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xspfFile);

            // Extract playlist name from <title> element
            String playlistName = extractPlaylistName(document, xspfFile.getName());
            result.setPlaylistName(playlistName);

            // Extract all track locations
            List<String> trackLocations = extractTrackLocations(document);
            result.setTotalTracks(trackLocations.size());

            // Create the playlist
            Optional<PlaylistEntity> playlistOpt = libraryService.createPlaylist(playlistName);
            if (playlistOpt.isEmpty()) {
                result.setError("Failed to create playlist: " + playlistName);
                return result;
            }

            PlaylistEntity playlist = playlistOpt.get();
            result.setPlaylistId(playlist.getId());

            // Convert paths and add matching tracks
            for (String windowsLocation : trackLocations) {
                String linuxPath = convertWindowsPathToLinux(windowsLocation);
                result.addConvertedPath(windowsLocation, linuxPath);

                Optional<MusicEntity> musicOpt = findMusicByPath(linuxPath);
                if (musicOpt.isPresent()) {
                    libraryService.addMusicToPlaylist(playlist.getId(), musicOpt.get().getId());
                    result.incrementImportedTracks();
                } else {
                    result.addMissingTrack(linuxPath);
                }
            }

            result.setSuccess(true);

        } catch (Exception e) {
            result.setError("Error parsing XSPF file: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    /**
     * Imports an AIMP .aimppl4 playlist file.
     * Format: pipe-separated values with sections marked by #-----SECTION-----#
     *
     * @param aimppl4File The .aimppl4 file to import
     * @return The import result
     */
    public ImportResult importAimppl4Playlist(File aimppl4File) {
        ImportResult result = new ImportResult();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(aimppl4File), StandardCharsets.UTF_16LE))) {

            String playlistName = null;
            List<String> trackPaths = new ArrayList<>();
            boolean inContentSection = false;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                // Detect section headers
                if (line.startsWith("#-----SUMMARY-----#")) {
                    // Parse summary section for Name
                    String summaryContent = line.substring("#-----SUMMARY-----#".length()).trim();
                    playlistName = extractAimppl4Name(summaryContent);
                    inContentSection = false;
                    continue;
                }

                if (line.startsWith("#-----SETTINGS-----#")) {
                    inContentSection = false;
                    continue;
                }

                if (line.startsWith("#-----CONTENT-----#")) {
                    inContentSection = true;
                    continue;
                }

                // Parse content lines
                if (inContentSection) {
                    // Skip group header lines (start with -)
                    if (line.startsWith("-")) {
                        continue;
                    }

                    // Extract path from pipe-separated format
                    // Format: path|title|artist|album|...
                    String path = extractPathFromAimppl4Line(line);
                    if (path != null && !path.isEmpty()) {
                        trackPaths.add(path);
                    }
                }
            }

            // Fallback name from filename
            if (playlistName == null || playlistName.isEmpty()) {
                playlistName = aimppl4File.getName()
                        .replaceFirst("\\.aimppl4$", "")
                        .replaceFirst("^~", "");
            }

            result.setPlaylistName(playlistName);
            result.setTotalTracks(trackPaths.size());

            // Create the playlist
            Optional<PlaylistEntity> playlistOpt = libraryService.createPlaylist(playlistName);
            if (playlistOpt.isEmpty()) {
                result.setError("Failed to create playlist: " + playlistName);
                return result;
            }

            PlaylistEntity playlist = playlistOpt.get();
            result.setPlaylistId(playlist.getId());

            // Convert paths and add matching tracks
            for (String windowsPath : trackPaths) {
                String linuxPath = convertWindowsPathToLinux(windowsPath);
                result.addConvertedPath(windowsPath, linuxPath);

                Optional<MusicEntity> musicOpt = findMusicByPath(linuxPath);
                if (musicOpt.isPresent()) {
                    libraryService.addMusicToPlaylist(playlist.getId(), musicOpt.get().getId());
                    result.incrementImportedTracks();
                } else {
                    result.addMissingTrack(linuxPath);
                }
            }

            result.setSuccess(true);

        } catch (Exception e) {
            result.setError("Error parsing AIMPPL4 file: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    /**
     * Extracts the playlist name from the SUMMARY section of an .aimppl4 file.
     */
    private String extractAimppl4Name(String summaryContent) {
        // Look for Name= in the summary content
        Pattern namePattern = Pattern.compile("Name=([^\\s]+(?:\\s+[^=]+(?=[^\\s]*=|$))*)");
        Matcher matcher = namePattern.matcher(summaryContent);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            // Stop at the next key (e.g., NameIsAutoSet)
            int nextKey = name.indexOf("IsAutoSet");
            if (nextKey > 0) {
                // Find where Name value ends (before "NameIsAutoSet" or any other key)
                name = name.substring(0, nextKey).trim();
                if (name.endsWith("Name")) {
                    name = name.substring(0, name.length() - 4).trim();
                }
            }
            return name;
        }
        return null;
    }

    /**
     * Extracts the file path from an .aimppl4 content line.
     * Format: path|title|artist|album|...
     */
    private String extractPathFromAimppl4Line(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        // Split by pipe and get the first element (path)
        int pipeIndex = line.indexOf('|');
        if (pipeIndex > 0) {
            return line.substring(0, pipeIndex).trim();
        }

        // If no pipe, the whole line might be a path
        if (line.matches("^[A-Za-z]:[/\\\\].*")) {
            return line;
        }

        return null;
    }

    /**
     * Extracts the playlist name from the XSPF document.
     */
    private String extractPlaylistName(Document document, String defaultName) {
        NodeList titleNodes = document.getElementsByTagName("title");
        if (titleNodes.getLength() > 0) {
            String title = titleNodes.item(0).getTextContent().trim();
            if (!title.isEmpty()) {
                return title;
            }
        }
        // Fallback: use filename without extension
        return defaultName.replaceFirst("\\.xspf$", "").replaceFirst("^~", "");
    }

    /**
     * Extracts all track locations from the XSPF document.
     */
    private List<String> extractTrackLocations(Document document) {
        List<String> locations = new ArrayList<>();
        NodeList trackNodes = document.getElementsByTagName("track");

        for (int i = 0; i < trackNodes.getLength(); i++) {
            Element track = (Element) trackNodes.item(i);
            NodeList locationNodes = track.getElementsByTagName("location");
            if (locationNodes.getLength() > 0) {
                String location = locationNodes.item(0).getTextContent().trim();
                locations.add(location);
            }
        }

        return locations;
    }

    /**
     * Converts a Windows path (from AIMP) to a Linux path.
     * Example: "file:///D:/Musique/track.m4a" -> "/home/user/Musique/track.m4a"
     * Example: "D:\Musique\track.mp3" -> "/home/user/Musique/track.mp3"
     *
     * @param windowsPath The Windows path from the playlist file
     * @return The converted Linux path
     */
    public String convertWindowsPathToLinux(String windowsPath) {
        // URL decode the path
        String decoded;
        try {
            decoded = URLDecoder.decode(windowsPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            decoded = windowsPath;
        }

        // Handle file:///D:/path format
        Matcher matcher = WINDOWS_PATH_PATTERN.matcher(decoded);
        if (matcher.matches()) {
            String remainingPath = matcher.group(2);
            return convertRelativePath(remainingPath);
        }

        // Handle D:/path format (forward slashes)
        Matcher driveMatcher = WINDOWS_DRIVE_PATTERN.matcher(decoded);
        if (driveMatcher.matches()) {
            String remainingPath = driveMatcher.group(2);
            return convertRelativePath(remainingPath);
        }

        // Handle D:\path format (backslashes - common in .aimppl4 files)
        Matcher backslashMatcher = WINDOWS_BACKSLASH_PATTERN.matcher(decoded);
        if (backslashMatcher.matches()) {
            String remainingPath = backslashMatcher.group(2);
            // Convert backslashes to forward slashes
            remainingPath = remainingPath.replace("\\", "/");
            return convertRelativePath(remainingPath);
        }

        // If no Windows pattern found, return as-is
        return decoded;
    }

    /**
     * Converts the relative path portion.
     * Example: "Musique/track.m4a" -> "/home/user/Musique/track.m4a"
     */
    private String convertRelativePath(String relativePath) {
        // Remove "Musique/" prefix if present (as it's in the base path)
        if (relativePath.startsWith("Musique/")) {
            relativePath = relativePath.substring("Musique/".length());
            return linuxMusicBasePath + "/" + relativePath;
        }

        // If path doesn't start with Musique, just append to base path
        return linuxMusicBasePath + "/" + relativePath;
    }

    /**
     * Finds music in the library by its path.
     */
    private Optional<MusicEntity> findMusicByPath(String path) {
        return libraryService.getMusicByPath(path);
    }

    /**
     * Result of an import operation.
     */
    public static class ImportResult {
        private boolean success;
        private String error;
        private String playlistName;
        private Long playlistId;
        private int totalTracks;
        private int importedTracks;
        private final List<String> missingTracks = new ArrayList<>();
        private final List<PathConversion> convertedPaths = new ArrayList<>();

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getPlaylistName() {
            return playlistName;
        }

        public void setPlaylistName(String playlistName) {
            this.playlistName = playlistName;
        }

        public Long getPlaylistId() {
            return playlistId;
        }

        public void setPlaylistId(Long playlistId) {
            this.playlistId = playlistId;
        }

        public int getTotalTracks() {
            return totalTracks;
        }

        public void setTotalTracks(int totalTracks) {
            this.totalTracks = totalTracks;
        }

        public int getImportedTracks() {
            return importedTracks;
        }

        public void incrementImportedTracks() {
            this.importedTracks++;
        }

        public List<String> getMissingTracks() {
            return missingTracks;
        }

        public void addMissingTrack(String path) {
            this.missingTracks.add(path);
        }

        public List<PathConversion> getConvertedPaths() {
            return convertedPaths;
        }

        public void addConvertedPath(String original, String converted) {
            this.convertedPaths.add(new PathConversion(original, converted));
        }

        public int getMissingCount() {
            return totalTracks - importedTracks;
        }

        @Override
        public String toString() {
            if (success) {
                return String.format("Imported playlist '%s': %d/%d tracks added",
                        playlistName, importedTracks, totalTracks);
            } else {
                return "Import failed: " + error;
            }
        }
    }

    /**
     * Represents a path conversion from Windows to Linux.
     */
    public record PathConversion(String original, String converted) {
    }
}

