package com.luciferc137.cmp.ui.keyboard;

import com.luciferc137.cmp.ui.controllers.MainController;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;


public class KeyBoardShortCut {
    private final MainController controller;

    public KeyBoardShortCut(MainController controller) {
        this.controller = controller;
    }

    public void mainSceneShortcuts(Scene scene) {
        // Close App on Ctrl + W
        put(scene, ctrl(KeyCode.W), () -> {
            if (scene.getWindow() != null) {
                scene.getWindow().hide();
            }
            Platform.exit();
        });
        put(scene, key(KeyCode.O), controller::openSettingsFromShortcut);
        put(scene, key(KeyCode.SPACE), controller::onPauseFromShortcut);
        arrowHandling(scene);
        bindCommonShortcuts(scene);
    }

    public void bindCommonShortcuts(Scene scene) {
        // Removing default focus traversal for arrow keys to allow custom handling
        disableFocusTraversal(scene.getRoot());
        // Close current scene on Ctrl + W
        put(scene, ctrl(KeyCode.W), () -> {
            if (scene.getWindow() != null) {
                scene.getWindow().hide();
            }
        });
    }

    private void arrowHandling(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (scene.getFocusOwner() instanceof TextInputControl) {
                return;
            }
            switch (event.getCode()) {
                case LEFT -> {
                    if (event.isControlDown()) {
                        controller.onPreviousFromShortcut();
                    } else {
                        controller.fiveSecondBackwardFromShortcut();
                    }
                    event.consume();
                }
                case RIGHT -> {
                    if (event.isControlDown()) {
                        controller.onNextFromShortcut();
                    } else {
                        controller.fiveSecondForwardFromShortcut();
                    }
                    event.consume();
                }
                case SPACE -> {
                    controller.onPauseFromShortcut();
                    event.consume();
                }
                default -> { }
            }
        });
    }

    public static void disableFocusTraversal(Parent root) {
        for (Node node : root.getChildrenUnmodifiable()) {
            if (!(node instanceof TextInputControl)) {
                node.setFocusTraversable(false);
            }
            if (node instanceof Parent p) {
                disableFocusTraversal(p);
            }
        }
    }

    private static void put(Scene scene, KeyCodeCombination keyCombination, Runnable action) {
        scene.getAccelerators().put(keyCombination, action);
    }

    public static KeyCodeCombination ctrl(KeyCode key) {
        return new KeyCodeCombination(key, KeyCombination.CONTROL_DOWN);
    }

    private static KeyCodeCombination key(KeyCode key) {
        return new KeyCodeCombination(key);
    }
}
