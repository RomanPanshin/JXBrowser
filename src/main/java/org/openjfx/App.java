package org.openjfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.image.Image;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/MainView.fxml"));

        Scene scene = new Scene(root, 800, 600);

        stage.setTitle("JXBrowser");
        stage.setScene(scene);

        // Add this line to set the icon
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/Images/BrowserLogo.png")));


        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
