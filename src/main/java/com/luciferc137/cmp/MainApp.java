package com.luciferc137.cmp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ui/main.fxml")
        );

        Scene scene = new Scene(loader.load());
        stage.setTitle("Custom Music Player");
        stage.setScene(scene);
        stage.show();
    }
}