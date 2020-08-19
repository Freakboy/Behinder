package vip.youwe.sheller.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import vip.youwe.sheller.core.Constants;
import vip.youwe.sheller.utils.Utils;

import java.io.ByteArrayInputStream;

/**
 * @author Allen
 * @Description:
 * @Date: 2020/08/18 11:27:01
 * @Version 1.0
 **/
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("Main.fxml"));
        primaryStage.setTitle(String.format("webshell manager %s", Constants.VERSION));
        primaryStage.getIcons().add(new Image(new ByteArrayInputStream(Utils.getResourceData("logo.jpg"))));
        primaryStage.setScene(new Scene(root, 1100.0D, 600.0D));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
