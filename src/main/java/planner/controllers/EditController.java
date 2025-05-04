package planner.controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import planner.models.Category;
import planner.models.Database;
import planner.models.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.sql.ResultSet;

public class EditController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<Integer> priorityCombo;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    
    private Task task;
    private ObservableList<Category> categories;
    
    public void setTask(Task task) {
        this.task = task;
        
        if (task != null) {
            // Редактирование существующей задачи
            titleField.setText(task.getTitle());
            descriptionArea.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
            
            for (Category category : categories) {
                if (category.getId() == task.getCategoryId()) {
                    categoryCombo.getSelectionModel().select(category);
                    break;
                }
            }
        } else {
            // Новая задача
            dueDatePicker.setValue(LocalDate.now());
            categoryCombo.getSelectionModel().selectFirst();
        }
    }
    
    public void setCategories(ObservableList<Category> categories) {
        this.categories = categories;
        categoryCombo.setItems(categories);
    }
    
    @FXML
    public void initialize() {
        // Обработчики событий
        saveButton.setOnAction(e -> saveTask());
        cancelButton.setOnAction(e -> closeWindow());
    }
    
    private void saveTask() {
        if (titleField.getText().isEmpty()) {
            showAlert("Ошибка", "Поле 'Название' не может быть пустым", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            if (task == null) {
                // Создание новой задачи
                task = new Task();
                task.setTitle(titleField.getText());
                task.setDescription(descriptionArea.getText());
                task.setDueDate(dueDatePicker.getValue());
                task.setCategoryId(categoryCombo.getValue().getId());
                
                try (Connection conn = Database.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                             "INSERT INTO tasks (title, description, due_date, category_id) " +
                             "VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    
                    stmt.setString(1, task.getTitle());
                    stmt.setString(2, task.getDescription());
                    stmt.setString(3, task.getDueDate().toString());
                    stmt.setInt(4, task.getCategoryId());
                    stmt.executeUpdate();
                    
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            task.setId(rs.getInt(1));
                        }
                    }
                    closeWindow();
                }
            } else {
                // Обновление существующей задачи
                task.setTitle(titleField.getText());
                task.setDescription(descriptionArea.getText());
                task.setDueDate(dueDatePicker.getValue());
                task.setCategoryId(categoryCombo.getValue().getId());
                
                try (Connection conn = Database.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE tasks SET title = ?, description = ?, due_date = ?, category_id = ? " +
                             "WHERE id = ?")) {
                    
                    stmt.setString(1, task.getTitle());
                    stmt.setString(2, task.getDescription());
                    stmt.setString(3, task.getDueDate().toString());
                    stmt.setInt(4, task.getCategoryId());
                    stmt.setInt(5, task.getId());
                    stmt.executeUpdate();
                }
            }
            // Получаем ссылку на MainController
            MainController mainController = (MainController) saveButton.getScene()
                .getWindow()
                .getUserData();
            
            // Добавляем задачу и обновляем фильтрацию
            mainController.addAndFilterTask(task);

            closeWindow();
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось сохранить задачу", Alert.AlertType.ERROR);
        }
    }
    
    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
