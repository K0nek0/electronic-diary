package planner.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import planner.models.Category;
import planner.models.Database;
import planner.models.Task;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class MainController {
    @FXML private TableView<Task> tasksTable;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML private TableColumn<Task, String> categoryColumn;
    @FXML private TableColumn<Task, String> descriptionColumn;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private DatePicker dateFilter;
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button markCompletedButton;
    
    private ObservableList<Task> tasks = FXCollections.observableArrayList();
    private ObservableList<Category> categories = FXCollections.observableArrayList();
    private ObservableList<Category> filterCategories = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        // Настройка таблицы
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        categoryColumn.setCellValueFactory(cellData -> {
            int categoryId = cellData.getValue().getCategoryId();
            Category category = categories.stream()
                .filter(c -> c.getId() == categoryId)
                .findFirst()
                .orElse(null);
            return javafx.beans.binding.Bindings.createStringBinding(() -> 
                category != null ? category.getName() : "");
        });
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        tasksTable.setItems(tasks);
        
        // Загрузка данных
        loadCategoriesForFilter();
        loadCategories();
        loadTasks();

        // Настройка фильтров
        categoryFilter.setItems(filterCategories);
        categoryFilter.getSelectionModel().selectFirst();
        
        // Обработчики событий
        addButton.setOnAction(e -> showEditDialog(null));
        editButton.setOnAction(e -> {
            Task selected = tasksTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditDialog(selected);
            }
        });
        deleteButton.setOnAction(e -> deleteSelectedTask());
        markCompletedButton.setOnAction(e -> markTaskCompleted());
        tasksTable.setRowFactory(tv -> new TableRow<Task>() {
            private final PseudoClass completed = PseudoClass.getPseudoClass("completed");
            
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                
                pseudoClassStateChanged(completed, !empty && !isSelected() && task != null && task.isCompleted());
                
                if (empty || task == null) {
                    setStyle("");
                } else if (isSelected()) {
                    setStyle("-fx-text-fill: -fx-selection-bar-text;");
                } else if (task.isCompleted()) {
                    setStyle("-fx-background-color: #4CAF50;");
                } else {
                    setStyle("");
                }
            }
        });
        
        // Фильтрация при изменении параметров
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterTasks());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterTasks());
        dateFilter.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {filterTasks();});
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTasks());
    }

    private void loadTasks() {
        tasks.clear();
        try (Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tasks");
            ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Task task = new Task();
                task.setId(rs.getInt("id"));
                task.setTitle(rs.getString("title"));
                task.setDescription(rs.getString("description"));
                task.setDueDate(LocalDate.parse(rs.getString("due_date")));
                task.setCompleted(rs.getBoolean("completed"));
                task.setCategoryId(rs.getInt("category_id"));
                
                tasks.add(task);
            }
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось загрузить задачи", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadCategoriesForFilter() {
        filterCategories.clear();
        Category allCategories = new Category("Все категории", "");
        filterCategories.add(0, allCategories);
        
        try (Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM categories");
            ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Category filterCategory = new Category();
                filterCategory.setId(rs.getInt("id"));
                filterCategory.setName(rs.getString("name"));

                filterCategories.add(filterCategory);
            }
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось загрузить категории для фильтров", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadCategories() {
        categories.clear();
        try (Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM categories");
            ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Category category = new Category();
                category.setId(rs.getInt("id"));
                category.setName(rs.getString("name"));
                
                categories.add(category);
            }
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось загрузить категории", e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void filterTasks() {
        Category selectedCategory = categoryFilter.getValue();
        String searchText = searchField.getText().toLowerCase().trim();
        
        // Получаем текст из редактора DatePicker
        String dateText = dateFilter.getEditor().getText();
        
        // Парсим дату только если поле не пустое
        LocalDate selectedDate = null;
        if (!dateText.isEmpty()) {
            try {
                selectedDate = dateFilter.getConverter().fromString(dateText);
            } catch (Exception e) {
                // Если дата введена некорректно, игнорируем ее
                selectedDate = null;
            }
        }
    
        ObservableList<Task> filteredTasks = FXCollections.observableArrayList();
    
        for (Task task : tasks) {
            boolean matchesAll = true;
    
            // Фильтр по категории
            if (selectedCategory != null && !selectedCategory.getName().equals("Все категории")) {
                matchesAll = matchesAll && (task.getCategoryId() == selectedCategory.getId());
            }
    
            // Фильтр по дате (работает только если дата успешно распаршена)
            if (selectedDate != null) {
                matchesAll = matchesAll && task.getDueDate().equals(selectedDate);
            }
    
            // Фильтр по тексту
            if (!searchText.isEmpty()) {
                matchesAll = matchesAll && 
                    (task.getTitle().toLowerCase().contains(searchText) ||
                    (task.getDescription() != null && 
                     task.getDescription().toLowerCase().contains(searchText)));
            }
    
            if (matchesAll) {
                filteredTasks.add(task);
            }
        }
    
        tasksTable.setItems(filteredTasks);
    }

    public void addAndFilterTask(Task newTask) {
        tasks.add(newTask);
        filterTasks();
    }

    private void showEditDialog(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/edit.fxml"));
            Parent root = loader.load();
            
            EditController controller = loader.getController();
            controller.setCategories(categories);
            controller.setTask(task);
            
            Stage stage = new Stage();
            stage.setTitle(task == null ? "Добавить задачу" : "Редактировать задачу");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setUserData(this);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            loadTasks();
        } catch (IOException e) {
            showAlert("Ошибка", "Не удалось открыть окно редактирования", e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void deleteSelectedTask() {
        Task selected = tasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удалить задачу?");
        alert.setContentText("Вы уверены, что хотите удалить выбранную задачу?");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
                
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
                
                tasks.remove(selected);
                filterTasks();
            } catch (SQLException e) {
                showAlert("Ошибка", "Не удалось удалить задачу", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    private void markTaskCompleted() {
        Task selected = tasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        try (Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement("UPDATE tasks SET completed = ? WHERE id = ?")) {
            
            stmt.setBoolean(1, !selected.isCompleted());
            stmt.setInt(2, selected.getId());
            stmt.executeUpdate();
            
            selected.setCompleted(!selected.isCompleted());
            tasksTable.refresh();
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось обновить статус задачи", e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void showAlert(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
