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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.schedulers.FCFS;

import java.util.List;

public class FCFSVisualizer extends Application {

    private static List<FCFS.Job> completedJobs;

    public static void launchVisualizer(List<FCFS.Job> jobs) {
        completedJobs = jobs;
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
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private BarChart<String, Number> createGanttChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jobs");
        yAxis.setLabel("Time");

        BarChart<String, Number> ganttChart = new BarChart<>(xAxis, yAxis);
        ganttChart.setTitle("Gantt Chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Execution Timeline");

        for (FCFS.Job job : completedJobs) {
            series.getData().add(new XYChart.Data<>(job.jobId, job.startTime));
            series.getData().add(new XYChart.Data<>(job.jobId, job.completionTime));
        }

        ganttChart.getData().add(series);
        return ganttChart;
    }

    private TableView<FCFS.Job> createJobTable() {
        TableView<FCFS.Job> table = new TableView<>();

        TableColumn<FCFS.Job, String> jobIdCol = new TableColumn<>("Job ID");
        jobIdCol.setCellValueFactory(new PropertyValueFactory<>("jobId"));

        TableColumn<FCFS.Job, Integer> arrivalTimeCol = new TableColumn<>("Arrival Time");
        arrivalTimeCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));

        TableColumn<FCFS.Job, Integer> burstTimeCol = new TableColumn<>("Burst Time");
        burstTimeCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));

        TableColumn<FCFS.Job, Integer> startTimeCol = new TableColumn<>("Start Time");
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        TableColumn<FCFS.Job, Integer> completionTimeCol = new TableColumn<>("Completion Time");
        completionTimeCol.setCellValueFactory(new PropertyValueFactory<>("completionTime"));

        TableColumn<FCFS.Job, Integer> turnaroundTimeCol = new TableColumn<>("Turnaround Time");
        turnaroundTimeCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));

        TableColumn<FCFS.Job, Integer> waitingTimeCol = new TableColumn<>("Waiting Time");
        waitingTimeCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));

        TableColumn<FCFS.Job, Integer> responseTimeCol = new TableColumn<>("Response Time");
        responseTimeCol.setCellValueFactory(new PropertyValueFactory<>("responseTime"));

        table.getColumns().addAll(jobIdCol, arrivalTimeCol, burstTimeCol, startTimeCol, completionTimeCol, turnaroundTimeCol, waitingTimeCol, responseTimeCol);
        table.getItems().addAll(completedJobs);

        return table;
    }

    private Text createMetricsView() {
        double cpuUtilization = calculateCpuUtilization();
        double throughput = calculateThroughput();
        double avgTurnaround = calculateAverageTurnaroundTime();
        double avgWaiting = calculateAverageWaitingTime();

        String metrics = String.format("""
                CPU Utilization: %.2f%%
                Throughput: %.2f jobs/unit time
                Average Turnaround Time: %.2f
                Average Waiting Time: %.2f
                """, cpuUtilization, throughput, avgTurnaround, avgWaiting);

        return new Text(metrics);
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
}