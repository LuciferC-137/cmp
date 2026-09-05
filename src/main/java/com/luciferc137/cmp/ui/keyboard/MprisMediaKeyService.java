package com.luciferc137.cmp.ui.keyboard;

import com.luciferc137.cmp.MainApp;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.ui.controllers.MainController;
import com.luciferc137.cmp.ui.utils.CoverArtLoader;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Expose the app as an MPRIS media player (org.mpris.MediaPlayer2.*)
 * on the D-Bus session bus.
 *
 * <p>Implémente deux interfaces dédiées ({@link MediaPlayer2Root} et
 * {@link MediaPlayer2Player}) en plus de {@link Properties} : c'est
 * nécessaire car dbus-java ne route les appels D-Bus entrants vers une
 * méthode Java que si celle-ci est déclarée sur une interface qui étend
 * {@link DBusInterface}. Sans ça, Next()/Play()/Pause() etc. existeraient
 * bien côté Java mais seraient invisibles pour KWin/Plasma, qui recevrait
 * une erreur "UnknownMethod" en tentant de les appeler.</p>
 */
public class MprisMediaKeyService implements MediaPlayer2Root, MediaPlayer2Player, Properties {

    private static final Logger LOGGER = Logger.getLogger(MprisMediaKeyService.class.getName());

    private static final String OBJECT_PATH = "/org/mpris/MediaPlayer2";
    private static final String IFACE_ROOT = "org.mpris.MediaPlayer2";
    private static final String IFACE_PLAYER = "org.mpris.MediaPlayer2.Player";

    private final MainController controller;
    private final String appName;
    private final String identity;

    private DBusConnection connection;
    private volatile String playbackStatus = "Stopped"; // "Playing" | "Paused" | "Stopped"
    private volatile Map<String, Variant<?>> metadata = new HashMap<>();
    private volatile LongSupplier positionMicrosSupplier = () -> 0L;

    private final File artCacheDir = new File(System.getProperty("java.io.tmpdir"), "cmp-mpris-art");

    public MprisMediaKeyService(MainController controller, String appName, String identity) {
        this.controller = controller;
        this.appName = appName;
        this.identity = identity;
    }

    public void register() {
        try {
            connection = DBusConnectionBuilder.forSessionBus().build();

            String busName = "org.mpris.MediaPlayer2." + appName;
            connection.requestBusName(busName);
            connection.exportObject(OBJECT_PATH, this);

            LOGGER.info(() -> "MPRIS service registered as " + busName + " at " + OBJECT_PATH);
        } catch (DBusException e) {
            LOGGER.log(Level.SEVERE, "Failed to register MPRIS service on session bus", e);
        }
    }

    public void unregister() {
        if (connection == null) {
            return;
        }
        try {
            connection.unExportObject(OBJECT_PATH);
            connection.releaseBusName("org.mpris.MediaPlayer2." + appName);
        } catch (DBusException e) {
            LOGGER.log(Level.WARNING, "Error while unexporting MPRIS object", e);
        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    /**
     * Called by the controller when the playback state changes,
     * to notify desktop environments (play/pause icon, etc.).
     */
    public void setPlaybackStatus(boolean playing) {
        String newStatus = playing ? "Playing" : "Paused";
        if (!newStatus.equals(playbackStatus)) {
            playbackStatus = newStatus;
            emitPropertiesChanged(IFACE_PLAYER, "PlaybackStatus", newStatus);
        }
    }

    /**
     * Supply a function that provides the current playback position in microseconds.
     */
    public void setPositionSupplier(LongSupplier positionMillisSupplier) {
        this.positionMicrosSupplier = () -> positionMillisSupplier.getAsLong() * 1000L;
    }

    /**
     * Update the metadata displayed by the desktop media widget
     * (title, artist, album, cover art, duration) and notify the change.
     */
    public void setNowPlaying(String trackId, String title, String artist, String album,
                              long durationMs, File artFile) {
        Map<String, Variant<?>> m = new HashMap<>();

        String safeId = trackId == null ? "unknown" : trackId.replaceAll("[^a-zA-Z0-9_]", "_");
        m.put("mpris:trackid", new Variant<>(new DBusPath("/org/mpris/MediaPlayer2/Track/" + safeId)));
        m.put("mpris:length", new Variant<>(durationMs * 1000L));

        m.put("xesam:title", new Variant<>(title != null ? title : ""));
        if (artist != null && !artist.isEmpty()) {
            m.put("xesam:artist", new Variant<>(new String[]{artist}));
        }
        if (album != null && !album.isEmpty()) {
            m.put("xesam:album", new Variant<>(album));
        }
        if (artFile != null && artFile.exists()) {
            m.put("mpris:artUrl", new Variant<>(artFile.toURI().toString()));
        }

        this.metadata = m;
        emitMetadataChanged(m);
    }

    /** Utility to set the now playing information with a cover art image. */
    public void setNowPlaying(Music music) {
        String trackId = music.title + "_" + music.getId();
        String title = music.title;
        String artist = music.artist;
        String album = music.album;
        long durationMs = music.duration;
        Image coverArt = CoverArtLoader.loadCoverArt(music.absPath(), 300);
        File artFile = exportCoverArt(coverArt, trackId);
        setNowPlaying(trackId, title, artist, album, durationMs, artFile);
    }

    /**
     * Export a JavaFX Image to a PNG file in cache: required because
     * MPRIS only transports URLs (file://), never binary data in a property.
     * The file is reused if it already exists for the same key.
     */
    public File exportCoverArt(Image image, String cacheKey) {
        if (image == null || cacheKey == null) {
            return null;
        }
        try {
            if (!artCacheDir.exists()) {
                artCacheDir.mkdirs();
            }
            String safeKey = cacheKey.replaceAll("[^a-zA-Z0-9_-]", "_");
            File file = new File(artCacheDir, safeKey + ".png");
            if (!file.exists()) {
                BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bImage, "png", file);
            }
            return file;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to export cover art for MPRIS", e);
            return null;
        }
    }

    @Override
    public void Raise() {
        // TODO: bring the app window to front if minimized or behind other windows.
    }

    @Override
    public void Quit() {
        // Not implemented
    }

    @Override
    public void Next() {
        Platform.runLater(controller::onNextFromShortcut);
    }

    @Override
    public void Previous() {
        Platform.runLater(controller::onPreviousFromShortcut);
    }

    @Override
    public void Pause() {
        Platform.runLater(controller::onPauseFromShortcut);
    }

    @Override
    public void PlayPause() {
        Platform.runLater(controller::onPauseFromShortcut);
    }

    @Override
    public void Stop() {
        Platform.runLater(controller::onStopFromShortcut);
    }

    @Override
    public void Play() {
        Platform.runLater(controller::onPlayFromShortcut);
    }

    @Override
    public void Seek(long offsetMicroseconds) {
        // TODO
    }

    @Override
    public void SetPosition(DBusPath trackId, long positionMicroseconds) {
        // Not Implemented
    }

    @Override
    public void OpenUri(String uri) {
        MainApp.logger.log(Level.WARNING, "Opening URI via MPRIS is not supported: " + uri);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A Get(String interfaceName, String propertyName) {
        Object value = switch (interfaceName) {
            case IFACE_ROOT -> getRootProperty(propertyName);
            case IFACE_PLAYER -> getPlayerProperty(propertyName);
            default -> null;
        };
        return (A) value;
    }

    @Override
    public <A> void Set(String interfaceName, String propertyName, A value) {
        // Ignore set requests, as this service is read-only for properties.
    }

    @Override
    public Map<String, Variant<?>> GetAll(String interfaceName) {
        Map<String, Variant<?>> props = new HashMap<>();
        switch (interfaceName) {
            case IFACE_ROOT -> {
                props.put("CanQuit", new Variant<>(true));
                props.put("CanRaise", new Variant<>(true));
                props.put("HasTrackList", new Variant<>(false));
                props.put("Identity", new Variant<>(identity));
                props.put("SupportedUriSchemes", new Variant<>(new String[0]));
                props.put("SupportedMimeTypes", new Variant<>(new String[0]));
            }
            case IFACE_PLAYER -> {
                props.put("PlaybackStatus", new Variant<>(playbackStatus));
                props.put("Metadata", new Variant<>(metadata, "a{sv}"));
                props.put("Position", new Variant<>(positionMicrosSupplier.getAsLong()));
                props.put("CanGoNext", new Variant<>(true));
                props.put("CanGoPrevious", new Variant<>(true));
                props.put("CanPlay", new Variant<>(true));
                props.put("CanPause", new Variant<>(true));
                props.put("CanSeek", new Variant<>(false));
                props.put("CanControl", new Variant<>(true));
            }
            default -> { /* nothing */ }
        }
        return props;
    }

    private Object getRootProperty(String name) {
        return switch (name) {
            case "CanQuit", "CanRaise" -> true;
            case "HasTrackList" -> false;
            case "Identity" -> identity;
            case "SupportedUriSchemes", "SupportedMimeTypes" -> new String[0];
            default -> null;
        };
    }

    private Object getPlayerProperty(String name) {
        return switch (name) {
            case "PlaybackStatus" -> playbackStatus;
            case "Metadata" -> metadata;
            case "Position" -> positionMicrosSupplier.getAsLong();
            case "CanGoNext", "CanGoPrevious", "CanPlay", "CanPause", "CanControl" -> true;
            case "CanSeek" -> false;
            default -> null;
        };
    }

    private void emitPropertiesChanged(String interfaceName, String propertyName, Object value) {
        if (connection == null) {
            return;
        }
        try {
            Map<String, Variant<?>> changed = new HashMap<>();
            changed.put(propertyName, new Variant<>(value));
            connection.sendMessage(new Properties.PropertiesChanged(
                    OBJECT_PATH, interfaceName, changed, List.of(new String[0])));
        } catch (DBusException e) {
            LOGGER.log(Level.WARNING, "Failed to emit PropertiesChanged", e);
        }
    }

    /**
     * Dedicated emission for Metadata: requires an explicit D-Bus signature
     * "a{sv}", because dbus-java does not correctly infer by reflection the type
     * of Map<String, Variant<?>> nested in a generic Variant.
     */
    private void emitMetadataChanged(Map<String, Variant<?>> metadataValue) {
        if (connection == null) {
            return;
        }
        try {
            Map<String, Variant<?>> changed = new HashMap<>();
            changed.put("Metadata", new Variant<>(metadataValue, "a{sv}"));
            connection.sendMessage(new Properties.PropertiesChanged(
                    OBJECT_PATH, IFACE_PLAYER, changed, List.of(new String[0])));
        } catch (DBusException e) {
            LOGGER.log(Level.WARNING, "Failed to emit Metadata PropertiesChanged", e);
        }
    }

    @Override
    public String getObjectPath() {
        return OBJECT_PATH;
    }
}