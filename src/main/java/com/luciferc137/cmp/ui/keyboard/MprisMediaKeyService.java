package com.luciferc137.cmp.ui.keyboard;

import com.luciferc137.cmp.MainApp;
import com.luciferc137.cmp.ui.controllers.MainController;
import javafx.application.Platform;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Expose the app as an MPRIS media player (org.mpris.MediaPlayer2.*)
 * on the D-Bus session bus.
 */
public class MprisMediaKeyService implements DBusInterface, Properties {

    private static final Logger LOGGER = Logger.getLogger(MprisMediaKeyService.class.getName());

    private static final String OBJECT_PATH = "/org/mpris/MediaPlayer2";
    private static final String IFACE_ROOT = "org.mpris.MediaPlayer2";
    private static final String IFACE_PLAYER = "org.mpris.MediaPlayer2.Player";

    private final MainController controller;
    private final String appName;
    private final String identity;

    private DBusConnection connection;
    private volatile String playbackStatus = "Stopped"; // "Playing" | "Paused" | "Stopped"

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
     * To call from the controller when playback status changes, to notify Desktop Environments
     */
    public void setPlaybackStatus(boolean playing) {
        String newStatus = playing ? "Playing" : "Paused";
        if (!newStatus.equals(playbackStatus)) {
            playbackStatus = newStatus;
            emitPropertiesChanged(IFACE_PLAYER, "PlaybackStatus", newStatus);
        }
    }

    public void Raise() {
        // TODO: bring the app window to front if minimized or behind other windows.
    }

    public void Quit() {
        // Not implemented
    }

    public void Next() {
        Platform.runLater(controller::onNextFromShortcut);
    }

    public void Previous() {
        Platform.runLater(controller::onPreviousFromShortcut);
    }

    public void Pause() {
        Platform.runLater(controller::onPauseFromShortcut);
    }

    public void PlayPause() {
        Platform.runLater(controller::onPauseFromShortcut);
    }

    public void Stop() {
        Platform.runLater(controller::onStopFromShortcut);
    }

    public void Play() {
        Platform.runLater(controller::onPlayFromShortcut);
    }

    public void Seek(long offsetMicroseconds) {
        // TODO
    }

    public void SetPosition(DBusPath trackId, long positionMicroseconds) {
        // Not Implemented
    }

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

    @Override
    public String getObjectPath() {
        return OBJECT_PATH;
    }
}