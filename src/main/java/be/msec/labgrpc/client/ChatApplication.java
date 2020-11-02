package be.msec.labgrpc.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class ChatApplication extends Application {

    private static Stage primaryStageObj;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStageObj = primaryStage;
        Parent root = FXMLLoader.load(new URL("file:src/main/java/be/msec/labgrpc/gui/ChatView_v2.fxml"));

        Scene scene = new Scene(root, 300, 275);

        String title = "gRPC based Chat service";
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setFullScreen(false);
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStageObj;
    }

}