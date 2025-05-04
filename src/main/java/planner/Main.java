package planner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import planner.models.Database;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Инициализация базы данных
        Database.initialize();
        
        // Загрузка главного окна
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("Электронный ежедневник");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
