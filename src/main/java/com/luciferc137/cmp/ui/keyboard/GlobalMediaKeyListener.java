package com.luciferc137.cmp.ui.keyboard;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.luciferc137.cmp.MainApp;
import com.luciferc137.cmp.ui.controllers.MainController;
import javafx.application.Platform;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalMediaKeyListener implements NativeKeyListener {
    private final MainController controller;

    public GlobalMediaKeyListener(MainController controller) {
        this.controller = controller;
    }

    public void register() {
        try {
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException e) {
            MainApp.logger.log(Level.SEVERE, "Error registering global keyboard hook", e);
        }
    }

    public void unregister() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            MainApp.logger.log(Level.SEVERE,
                    "Error during global keyboard hook unregistration", e);
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
        switch (e.getKeyCode()) {
            case NativeKeyEvent.VC_MEDIA_NEXT -> Platform.runLater(controller::onNextFromShortcut);
            case NativeKeyEvent.VC_MEDIA_PREVIOUS -> Platform.runLater(controller::onPreviousFromShortcut);
            case NativeKeyEvent.VC_MEDIA_PLAY -> Platform.runLater(controller::onPauseFromShortcut);
            default -> { /* nothing */ }
        }
    }
}
