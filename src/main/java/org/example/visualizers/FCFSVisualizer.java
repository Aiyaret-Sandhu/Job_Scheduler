package org.example.visualizers;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.schedulers.FCFS;

import java.util.List;

public class FCFSVisualizer extends Application {

    private static List<FCFS.Job> completedJobs;

    public static void launchVisualizer(List<FCFS.Job> jobs) {
        completedJobs = jobs;
        // Disable JavaFX logging for PropertyValueFactory warnings
        System.setProperty("javafx.sg.warn", "false");
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("FCFS Scheduler Visualization");

        // TabPane to hold Gantt Chart, Table, and Metrics
        TabPane tabPane = new TabPane();

        // Gantt Chart Tab
        Tab ganttTab = new Tab("Gantt Chart");
        ganttTab.setContent(createGanttChart());
        ganttTab.setClosable(false);

        // Table Tab
        Tab tableTab = new Tab("Job Table");
        tableTab.setContent(createJobTable());
        tableTab.setClosable(false);

        // Metrics Tab
        Tab metricsTab = new Tab("Metrics");
        metricsTab.setContent(createMetricsView());
        metricsTab.setClosable(false);

        tabPane.getTabs().addAll(ganttTab, tableTab, metricsTab);

        // Set up the scene
        BorderPane root = new BorderPane(tabPane);
        Scene scene = new Scene(root, 900, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private BarChart<String, Number> createGanttChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jobs");
        yAxis.setLabel("Time");

        BarChart<String, Number> ganttChart = new BarChart<>(xAxis, yAxis);
        ganttChart.setTitle("FCFS Gantt Chart");
        ganttChart.setLegendVisible(false);
        ganttChart.setCategoryGap(50);
        ganttChart.setBarGap(0);

        XYChart.Series<String, Number> executionSeries = new XYChart.Series<>();
        executionSeries.setName("Execution");

        XYChart.Series<String, Number> waitingSeries = new XYChart.Series<>();
        waitingSeries.setName("Waiting");

        for (FCFS.Job job : completedJobs) {
            // Execution time (from start to completion)
            XYChart.Data<String, Number> execData = new XYChart.Data<>(
                    job.jobId,
                    job.completionTime - job.startTime
            );
            execData.setNode(new CustomBar(job.jobId + "\n" + (job.completionTime - job.startTime), "execution"));
            executionSeries.getData().add(execData);

            // Waiting time (from arrival to start)
            if (job.startTime > job.arrivalTime) {
                XYChart.Data<String, Number> waitData = new XYChart.Data<>(
                        job.jobId,
                        job.startTime - job.arrivalTime
                );
                waitData.setNode(new CustomBar(job.jobId + "\n" + (job.startTime - job.arrivalTime), "waiting"));
                waitingSeries.getData().add(waitData);
            }
        }

        ganttChart.getData().addAll(waitingSeries, executionSeries);
        return ganttChart;
    }

    private TableView<FCFS.Job> createJobTable() {
        TableView<FCFS.Job> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Helper method to create columns with proper type safety
        createColumn(table, "Job ID", "jobId", String.class);
        createColumn(table, "Arrival Time", "arrivalTime", Integer.class);
        createColumn(table, "Burst Time", "burstTime", Integer.class);
        createColumn(table, "Start Time", "startTime", Integer.class);
        createColumn(table, "Completion Time", "completionTime", Integer.class);
        createColumn(table, "Turnaround Time", "turnaroundTime", Integer.class);
        createColumn(table, "Waiting Time", "waitingTime", Integer.class);
        createColumn(table, "Response Time", "responseTime", Integer.class);

        table.getItems().addAll(completedJobs);
        return table;
    }

    private <T> void createColumn(TableView<T> table, String title, String property, Class<?> propertyType) {
        TableColumn<T, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> {
            try {
                // Use reflection to get the property value
                java.lang.reflect.Field field = data.getValue().getClass().getDeclaredField(property);
                field.setAccessible(true);
                Object value = field.get(data.getValue());
                return new javafx.beans.property.SimpleObjectProperty<>(value);
            } catch (Exception e) {
                return new javafx.beans.property.SimpleObjectProperty<>("N/A");
            }
        });
        table.getColumns().add(column);
    }


    private Text createMetricsView() {
        double cpuUtilization = calculateCpuUtilization();
        double throughput = calculateThroughput();
        double avgTurnaround = calculateAverageTurnaroundTime();
        double avgWaiting = calculateAverageWaitingTime();
        double avgResponse = completedJobs.stream().mapToInt(job -> job.responseTime).average().orElse(0);

        int minWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).min().orElse(0);
        int maxWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).max().orElse(0);
        int minTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).min().orElse(0);
        int maxTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).max().orElse(0);

        String metrics = String.format("""
                CPU Utilization: %.2f%%
                Throughput: %.2f jobs/unit time
                
                Turnaround Time:
                  Average: %.2f
                  Minimum: %d
                  Maximum: %d
                
                Waiting Time:
                  Average: %.2f
                  Minimum: %d
                  Maximum: %d
                
                Response Time:
                  Average: %.2f
                """,
                cpuUtilization, throughput,
                avgTurnaround, minTurnaround, maxTurnaround,
                avgWaiting, minWaiting, maxWaiting,
                avgResponse);

        Text metricsText = new Text(metrics);
        metricsText.setStyle("-fx-font-family: monospace; -fx-font-size: 14;");
        return metricsText;
    }

    private double calculateCpuUtilization() {
        int totalBurstTime = completedJobs.stream().mapToInt(job -> job.burstTime).sum();
        int totalTime = completedJobs.get(completedJobs.size() - 1).completionTime - completedJobs.get(0).arrivalTime;
        return (double) totalBurstTime / totalTime * 100;
    }

    private double calculateThroughput() {
        int totalTime = completedJobs.get(completedJobs.size() - 1).completionTime - completedJobs.get(0).arrivalTime;
        return (double) completedJobs.size() / totalTime;
    }

    private double calculateAverageTurnaroundTime() {
        return completedJobs.stream().mapToInt(job -> job.turnaroundTime).average().orElse(0);
    }

    private double calculateAverageWaitingTime() {
        return completedJobs.stream().mapToInt(job -> job.waitingTime).average().orElse(0);
    }

    // Custom bar class for Gantt chart with better visualization
    private static class CustomBar extends javafx.scene.layout.StackPane {
        CustomBar(String text, String styleClass) {
            javafx.scene.control.Label label = new javafx.scene.control.Label(text);
            label.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();
            rectangle.setHeight(20);
            rectangle.widthProperty().bind(this.widthProperty());

            this.getChildren().addAll(rectangle, label);
            this.getStyleClass().add(styleClass);
        }
    }
}