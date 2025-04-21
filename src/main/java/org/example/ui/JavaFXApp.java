package org.example.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class JavaFXApp extends Application {

    public static void launchApp(String[] args) {
//        System.setProperty("javafx.sg.warn", "false");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Welcome to Job Scheduler!");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 200);
        primaryStage.setTitle("Job Scheduler - Made by Arshdeep Singh");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
