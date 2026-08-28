package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.VlcAudioPlayer;
import com.luciferc137.cmp.audio.WaveformExtractor;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.ui.handlers.*;

/**
 * The MainCoordinator class serves as a central point for coordinating
 * various UI handlers and core services in the application.
 */
public class Coordinator {

    private static Coordinator instance;

    // ==================== Handlers ====================

    public PlaybackHandler playbackHandler;
    public TableHandler tableHandler;
    public ContextMenuHandler contextMenuHandler;
    public FilterPopupHandler filterPopupHandler;
    public SessionHandler sessionHandler;
    public ShuffleLoopHandler shuffleLoopHandler;
    public PlaylistPanelHandler playlistPanelHandler;
    public QueuePanelHandler queuePanelHandler;

    // ==================== Core Services ====================

    private final VlcAudioPlayer audioPlayer = new VlcAudioPlayer();
    private final WaveformExtractor waveformExtractor = new WaveformExtractor();
    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private final PlaybackQueue playbackQueue = PlaybackQueue.getInstance();

    // ==================== State Flags ====================
    private boolean mainReady = false;
    private boolean playlistReady = false;
    private boolean crossWired = false;

    // ============ Cross Controller Communication ===============
    private Runnable mainTableRefreshAction;
    private Runnable playlistTableRefreshAction;

    public Coordinator() {
        createHandlers();
        configureHandlerListeners();
    }

    private void createHandlers() {
        playbackHandler = new PlaybackHandler(audioPlayer, waveformExtractor);
        tableHandler = new TableHandler();
        contextMenuHandler = new ContextMenuHandler();
        filterPopupHandler = new FilterPopupHandler();
        sessionHandler = new SessionHandler();
        shuffleLoopHandler = new ShuffleLoopHandler();
        playlistPanelHandler = new PlaylistPanelHandler();
        queuePanelHandler = new QueuePanelHandler();
    }

    public synchronized void onMainControllerReady() {
        mainReady = true;
        wireCrossControllerIfReady();
    }

    public synchronized void onPlaylistControllerReady() {
        playlistReady = true;
        wireCrossControllerIfReady();
    }

    private void wireCrossControllerIfReady() {
        if (crossWired || !mainReady || !playlistReady) return;
        crossWired = true;
        // Future cross-wiring here
    }

    private void configureHandlerListeners() {

    }

    // ==================== Synchronization Methods ====================

    public static void setMainTableRefreshAction(Runnable action) {
        getInstance().mainTableRefreshAction = action;
    }

    public static void refreshMainTable() {
        if (getInstance().mainTableRefreshAction != null) {
            getInstance().mainTableRefreshAction.run();
        }
    }

    public static void setPlaylistTableRefreshAction(Runnable action) {
        getInstance().playlistTableRefreshAction = action;
    }

    public static void refreshPlaylistTable() {
        if (getInstance().playlistTableRefreshAction != null) {
            getInstance().playlistTableRefreshAction.run();
        }
    }

    // ==================== Singleton Accessors ====================

    public static synchronized Coordinator getInstance() {
        if (instance == null) {
            instance = new Coordinator();
        }
        return instance;
    }

    public static VlcAudioPlayer audioPlayer() {
        return getInstance().audioPlayer;
    }

    public static WaveformExtractor waveformExtractor() {
        return getInstance().waveformExtractor;
    }

    public static MusicLibrary musicLibrary() {
        return getInstance().musicLibrary;
    }

    public static PlaybackQueue playbackQueue() {
        return getInstance().playbackQueue;
    }

    public static PlaybackHandler playbackHandler() {
        return getInstance().playbackHandler;
    }

    public static TableHandler tableHandler() {
        return getInstance().tableHandler;
    }

    public static ContextMenuHandler contextMenuHandler() {
        return getInstance().contextMenuHandler;
    }

    public static FilterPopupHandler filterPopupHandler() {
        return getInstance().filterPopupHandler;
    }

    public static SessionHandler sessionHandler() {
        return getInstance().sessionHandler;
    }

    public static ShuffleLoopHandler shuffleLoopHandler() {
        return getInstance().shuffleLoopHandler;
    }

    public static PlaylistPanelHandler playlistPanelHandler() {
        return getInstance().playlistPanelHandler;
    }

    public static QueuePanelHandler queuePanelHandler() {
        return getInstance().queuePanelHandler;
    }
}
