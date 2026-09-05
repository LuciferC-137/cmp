package com.luciferc137.cmp;

import com.luciferc137.cmp.ui.Coordinator;
import com.luciferc137.cmp.ui.controllers.MainController;
import com.luciferc137.cmp.ui.keyboard.MprisMediaKeyService;
import com.luciferc137.cmp.ui.utils.ThemeManager;
import com.luciferc137.cmp.ui.keyboard.GlobalMediaKeyListener;
import com.luciferc137.cmp.ui.keyboard.KeyBoardShortCut;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;


public class MainApp extends Application {
    public static KeyBoardShortCut keyBoardShortCut;
    public static GlobalMediaKeyListener globalMediaKeyListener;
    public static MprisMediaKeyService mprisMediaKeyService;
    public static Logger logger = Logger.getLogger(MainApp.class.getName());

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ui/main.fxml")
        );

        Scene scene = new Scene(loader.load());
        MainController controller = loader.getController();
        keyBoardShortCut = new KeyBoardShortCut(controller);
        keyBoardShortCut.mainSceneShortcuts(scene);

        globalMediaKeyListener = new GlobalMediaKeyListener(controller);
        globalMediaKeyListener.register();
        mprisMediaKeyService = new MprisMediaKeyService(controller, "CMP", "CMP");
        mprisMediaKeyService.register();
        MainApp.mprisMediaKeyService.setPositionSupplier(Coordinator.playbackHandler()::getCurrentPosition);

        // Apply dark theme stylesheet
        scene.getStylesheets().add(ThemeManager.getDarkThemeUrl());

        stage.setTitle("Custom Music Player");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        globalMediaKeyListener.unregister();
        mprisMediaKeyService.unregister();
    }
}