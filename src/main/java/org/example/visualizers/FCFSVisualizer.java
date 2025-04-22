package org.example.visualizers;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import org.example.schedulers.FCFS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

public class FCFSVisualizer extends Application {

    private static List<FCFS.Job> completedJobs;
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static final String CSS_STYLE = """
            -fx-font-family: 'Segoe UI', Arial;
            -fx-base: #3498db;
            -fx-accent: #2980b9;
            -fx-default-button: #2c3e50;
            -fx-focus-color: #2c3e50;
            -fx-faint-focus-color: #2c3e5022;
            """;

    // Wrapper class for job display in TableView
    public static class JobWrapper {
        private final SimpleStringProperty jobId;
        private final SimpleIntegerProperty arrivalTime;
        private final SimpleIntegerProperty burstTime;
        private final SimpleIntegerProperty startTime;
        private final SimpleIntegerProperty completionTime;
        private final SimpleIntegerProperty turnaroundTime;
        private final SimpleIntegerProperty waitingTime;
        private final SimpleIntegerProperty responseTime;
        private final FCFS.Job originalJob;

        public JobWrapper(FCFS.Job job) {
            this.jobId = new SimpleStringProperty(job.jobId);
            this.arrivalTime = new SimpleIntegerProperty(job.arrivalTime);
            this.burstTime = new SimpleIntegerProperty(job.burstTime);
            this.startTime = new SimpleIntegerProperty(job.startTime);
            this.completionTime = new SimpleIntegerProperty(job.completionTime);
            this.turnaroundTime = new SimpleIntegerProperty(job.turnaroundTime);
            this.waitingTime = new SimpleIntegerProperty(job.waitingTime);
            this.responseTime = new SimpleIntegerProperty(job.responseTime);
            this.originalJob = job;
        }

        public String getJobId() { return jobId.get(); }
        public int getArrivalTime() { return arrivalTime.get(); }
        public int getBurstTime() { return burstTime.get(); }
        public int getStartTime() { return startTime.get(); }
        public int getCompletionTime() { return completionTime.get(); }
        public int getTurnaroundTime() { return turnaroundTime.get(); }
        public int getWaitingTime() { return waitingTime.get(); }
        public int getResponseTime() { return responseTime.get(); }
        public FCFS.Job getOriginalJob() { return originalJob; }
    }

    public static void launchVisualizer(List<FCFS.Job> jobs) {
        completedJobs = jobs;
        System.setProperty("javafx.sg.warn", "false");
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("FCFS Scheduler Visualization");

        // Create tabs
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-font-size: 14px;");
        tabPane.getTabs().addAll(
                createGanttTab(),
                createTableTab(),
                createMetricsTab(),
                createTimelineTab()
        );

        // Create export buttons with better styling
        Button exportCsvButton = createStyledButton("Export to CSV", this::exportToCsv);
        Button exportPdfButton = createStyledButton("Export to PDF", this::exportToPdf);
        Button exportJsonButton = createStyledButton("Export to JSON", this::exportToJson);

        HBox buttonBox = new HBox(10, exportCsvButton, exportPdfButton, exportJsonButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setStyle("-fx-background-color: #f5f5f5;");

        // Main layout
        BorderPane root = new BorderPane(tabPane);
        root.setBottom(buttonBox);
        root.setStyle("-fx-background-color: #ecf0f1;");

        Scene scene = new Scene(root, 1100, 750);
        scene.getStylesheets().add("https://fonts.googleapis.com/css2?family=Roboto");
        scene.setFill(Color.web("#ecf0f1"));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createGanttTab() {
        Tab tab = new Tab("Gantt Chart");
        tab.setClosable(false);

        StackedBarChart<String, Number> ganttChart = createGanttChart();
        ganttChart.setStyle("-fx-font-size: 12px;");

        ScrollPane scrollPane = new ScrollPane(ganttChart);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #ffffff;");

        tab.setContent(scrollPane);
        return tab;
    }

    private StackedBarChart<String, Number> createGanttChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jobs");
        yAxis.setLabel("Time Units");

        StackedBarChart<String, Number> ganttChart = new StackedBarChart<>(xAxis, yAxis);
        ganttChart.setTitle("FCFS Gantt Chart");
        ganttChart.setLegendVisible(true);
        ganttChart.setCategoryGap(10);
        ganttChart.setAnimated(false);

        // Create series for different states
        XYChart.Series<String, Number> waitingSeries = new XYChart.Series<>();
        waitingSeries.setName("Waiting Time");

        XYChart.Series<String, Number> executionSeries = new XYChart.Series<>();
        executionSeries.setName("Execution Time");

        for (FCFS.Job job : completedJobs) {
            // Waiting time (from arrival to start)
            int waitingTime = job.startTime - job.arrivalTime;
            if (waitingTime > 0) {
                XYChart.Data<String, Number> waitData = new XYChart.Data<>(job.jobId, waitingTime);
                waitingSeries.getData().add(waitData);
            } else {
                waitingSeries.getData().add(new XYChart.Data<>(job.jobId, 0));
            }

            // Execution time
            XYChart.Data<String, Number> execData = new XYChart.Data<>(job.jobId, job.burstTime);
            executionSeries.getData().add(execData);
        }

        ganttChart.getData().addAll(waitingSeries, executionSeries);

        // Customize colors using CSS
        ganttChart.setStyle("""
        .default-color0.chart-bar { -fx-bar-fill: #e74c3c; }
        .default-color1.chart-bar { -fx-bar-fill: #2ecc71; }
        """);

        return ganttChart;
    }

    private Tab createTableTab() {
        Tab tab = new Tab("Job Table");
        tab.setClosable(false);

        // Create a TableView with our wrapper class
        TableView<JobWrapper> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size: 13px;");

        // Create columns with proper typing
        TableColumn<JobWrapper, String> jobIdCol = new TableColumn<>("Job ID");
        jobIdCol.setCellValueFactory(new PropertyValueFactory<>("jobId"));

        TableColumn<JobWrapper, Number> arrivalCol = new TableColumn<>("Arrival");
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));

        TableColumn<JobWrapper, Number> burstCol = new TableColumn<>("Burst");
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));

        TableColumn<JobWrapper, Number> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        TableColumn<JobWrapper, Number> completionCol = new TableColumn<>("Completion");
        completionCol.setCellValueFactory(new PropertyValueFactory<>("completionTime"));

        TableColumn<JobWrapper, Number> turnaroundCol = new TableColumn<>("Turnaround");
        turnaroundCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));

        TableColumn<JobWrapper, Number> waitingCol = new TableColumn<>("Waiting");
        waitingCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));

        TableColumn<JobWrapper, Number> responseCol = new TableColumn<>("Response");
        responseCol.setCellValueFactory(new PropertyValueFactory<>("responseTime"));

        table.getColumns().addAll(jobIdCol, arrivalCol, burstCol, startCol,
                completionCol, turnaroundCol, waitingCol, responseCol);

        // Convert the original jobs to wrapper objects and add to table
        List<JobWrapper> wrappedJobs = completedJobs.stream()
                .map(JobWrapper::new)
                .collect(Collectors.toList());
        table.setItems(FXCollections.observableArrayList(wrappedJobs));

        // Add sorting capability
        table.getSortOrder().add(arrivalCol);

        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem exportItem = new MenuItem("Export Selected Rows");
        exportItem.setOnAction(e -> exportSelectedRows(table));
        contextMenu.getItems().add(exportItem);
        table.setContextMenu(contextMenu);

        tab.setContent(table);
        return tab;
    }

    private Tab createMetricsTab() {
        Tab tab = new Tab("Performance Metrics");
        tab.setClosable(false);

        VBox metricsBox = new VBox(15);
        metricsBox.setPadding(new Insets(20));
        metricsBox.setStyle("-fx-background-color: #ffffff;");

        // CPU Metrics
        TitledPane cpuPane = createMetricTitledPane("CPU Utilization",
                "Utilization: " + df.format(calculateCpuUtilization()) + "%",
                "Throughput: " + df.format(calculateThroughput()) + " jobs/unit");

        // Time Metrics
        TitledPane timePane = createMetricTitledPane("Time Metrics",
                "Avg Turnaround: " + df.format(calculateAverageTurnaroundTime()),
                "Avg Waiting: " + df.format(calculateAverageWaitingTime()),
                "Avg Response: " + df.format(calculateAverageResponseTime()));

        // Job Counts
        TitledPane countPane = createMetricTitledPane("Job Counts",
                "Total Jobs: " + completedJobs.size(),
                "Max Burst Time: " + completedJobs.stream().mapToInt(j -> j.burstTime).max().orElse(0),
                "Min Burst Time: " + completedJobs.stream().mapToInt(j -> j.burstTime).min().orElse(0));

        metricsBox.getChildren().addAll(cpuPane, timePane, countPane);

        // Add a summary chart
        metricsBox.getChildren().add(createMetricsChart());

        tab.setContent(new ScrollPane(metricsBox));
        return tab;
    }

    private Tab createTimelineTab() {
        Tab tab = new Tab("Timeline View");
        tab.setClosable(false);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time Units");

        CategoryAxis yAxis = new CategoryAxis();
        yAxis.setLabel("Jobs");

        StackedBarChart<Number, String> timelineChart = new StackedBarChart<>(xAxis, yAxis);
        timelineChart.setTitle("Job Execution Timeline");
        timelineChart.setCategoryGap(1);
        timelineChart.setAnimated(false);

        // We would need multiple series for stacking
        XYChart.Series<Number, String> executionSeries = new XYChart.Series<>();
        executionSeries.setName("Execution");

        for (FCFS.Job job : completedJobs) {
            XYChart.Data<Number, String> data = new XYChart.Data<>(job.burstTime, job.jobId);
            data.setNode(new HoveredThresholdNode(job.startTime, job.completionTime));
            executionSeries.getData().add(data);
        }

        timelineChart.getData().add(executionSeries);

        // CSS styling
        timelineChart.setStyle("""
        .default-color0.chart-bar { 
            -fx-bar-fill: #3498db;
        }
        """);

        tab.setContent(timelineChart);
        return tab;
    }

    private BarChart<String, Number> createMetricsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Performance Metrics Comparison");
        chart.setLegendVisible(true);
        chart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Time Units");

        series.getData().add(new XYChart.Data<>("Avg Turnaround", calculateAverageTurnaroundTime()));
        series.getData().add(new XYChart.Data<>("Avg Waiting", calculateAverageWaitingTime()));
        series.getData().add(new XYChart.Data<>("Avg Response", calculateAverageResponseTime()));

        chart.getData().add(series);

        // Customize colors
        chart.setStyle(".default-color0.chart-bar { -fx-bar-fill: #9b59b6; }");

        return chart;
    }

    private TitledPane createMetricTitledPane(String title, String... metrics) {
        VBox content = new VBox(5);
        for (String metric : metrics) {
            Text text = new Text(metric);
            text.setFont(Font.font("Roboto", 14));
            content.getChildren().add(text);
        }

        TitledPane pane = new TitledPane(title, content);
        pane.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        pane.setExpanded(true);
        return pane;
    }

    private Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setStyle("-fx-font-size: 14px; -fx-padding: 8 15; -fx-background-radius: 5;");
        button.setOnAction(e -> action.run());
        return button;
    }

    private double calculateCpuUtilization() {
        int totalBurstTime = completedJobs.stream().mapToInt(job -> job.burstTime).sum();
        int firstArrival = completedJobs.get(0).arrivalTime;
        int lastCompletion = completedJobs.get(completedJobs.size() - 1).completionTime;
        int totalTime = lastCompletion - firstArrival;
        return totalTime > 0 ? (double) totalBurstTime / totalTime * 100 : 0;
    }

    private double calculateThroughput() {
        int firstArrival = completedJobs.get(0).arrivalTime;
        int lastCompletion = completedJobs.get(completedJobs.size() - 1).completionTime;
        int totalTime = lastCompletion - firstArrival;
        return totalTime > 0 ? (double) completedJobs.size() / totalTime : 0;
    }

    private double calculateAverageTurnaroundTime() {
        return completedJobs.stream().mapToInt(job -> job.turnaroundTime).average().orElse(0);
    }

    private double calculateAverageWaitingTime() {
        return completedJobs.stream().mapToInt(job -> job.waitingTime).average().orElse(0);
    }

    private double calculateAverageResponseTime() {
        return completedJobs.stream().mapToInt(job -> job.responseTime).average().orElse(0);
    }

    private void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Scheduling Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write header
                writer.write("Job ID,Arrival Time,Burst Time,Start Time,Completion Time," +
                        "Turnaround Time,Waiting Time,Response Time\n");

                // Write data
                for (FCFS.Job job : completedJobs) {
                    writer.write(String.format("%s,%d,%d,%d,%d,%d,%d,%d\n",
                            job.jobId, job.arrivalTime, job.burstTime, job.startTime,
                            job.completionTime, job.turnaroundTime, job.waitingTime, job.responseTime));
                }

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Data exported to CSV successfully!");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export data: " + e.getMessage());
            }
        }
    }

    private void exportToPdf() {
        // This would typically require a PDF library like Apache PDFBox or iText
        showAlert(Alert.AlertType.INFORMATION, "PDF Export",
                "PDF export would be implemented with a PDF library in a real application.");
    }

    private void exportToJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Scheduling Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("{\"jobs\": [\n");
                for (int i = 0; i < completedJobs.size(); i++) {
                    FCFS.Job job = completedJobs.get(i);
                    writer.write(String.format(
                            "  {\"id\": \"%s\", \"arrival\": %d, \"burst\": %d, \"start\": %d, \"completion\": %d, " +
                                    "\"turnaround\": %d, \"waiting\": %d, \"response\": %d}%s\n",
                            job.jobId, job.arrivalTime, job.burstTime, job.startTime, job.completionTime,
                            job.turnaroundTime, job.waitingTime, job.responseTime,
                            i < completedJobs.size() - 1 ? "," : ""
                    ));
                }
                writer.write("]}");

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Data exported to JSON successfully!");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export data: " + e.getMessage());
            }
        }
    }

    private void exportSelectedRows(TableView<JobWrapper> table) {
        if (table.getSelectionModel().getSelectedItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select rows to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Selected Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Job ID,Arrival Time,Burst Time,Start Time,Completion Time," +
                        "Turnaround Time,Waiting Time,Response Time\n");

                for (JobWrapper wrapper : table.getSelectionModel().getSelectedItems()) {
                    FCFS.Job job = wrapper.getOriginalJob();
                    writer.write(String.format("%s,%d,%d,%d,%d,%d,%d,%d\n",
                            job.jobId, job.arrivalTime, job.burstTime, job.startTime,
                            job.completionTime, job.turnaroundTime, job.waitingTime, job.responseTime));
                }

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Selected rows exported successfully!");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export data: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Custom node for timeline chart tooltips
    private static class HoveredThresholdNode extends StackPane {
        HoveredThresholdNode(int start, int end) {
            setPrefSize(15, 15);

            final Label label = createDataLabel(start, end);

            setOnMouseEntered(event -> {
                getChildren().setAll(label);
                setCursor(javafx.scene.Cursor.HAND);
                toFront();
            });
            setOnMouseExited(event -> {
                getChildren().clear();
                setCursor(javafx.scene.Cursor.DEFAULT);
            });
        }

        private Label createDataLabel(int start, int end) {
            final Label label = new Label(start + " - " + end);
            label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
            label.setStyle("-fx-font-size: 10; -fx-font-weight: bold;");
            label.setTextFill(Color.BLACK);
            label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
            return label;
        }
    }
}