package org.example.visualizers;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.property.*;
import javafx.util.Callback;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Tooltip;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.schedulers.PS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JavaFX visualization for Priority Scheduling (PS) algorithm
 * Shows execution timeline, process metrics, and performance indicators
 */
public class PSVisualizer extends Application {

    // Static properties for launching from Main
    private static List<PS.Job> jobs;
    private static List<PS.TimeSlice> allSlices;
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static final String CSS_STYLE = """
            -fx-font-family: 'Segoe UI', Arial;
            -fx-base: #3498db;
            -fx-accent: #2980b9;
            -fx-default-button: #2c3e50;
            -fx-focus-color: #2c3e50;
            -fx-faint-focus-color: #2c3e5022;
            """;

    /**
     * Wrapper class for job display in TableView
     * Provides property accessors for job attributes
     */
    public static class JobWrapper {
        private final SimpleStringProperty jobId;
        private final SimpleIntegerProperty arrivalTime;
        private final SimpleIntegerProperty burstTime;
        private final SimpleIntegerProperty priority;
        private final SimpleIntegerProperty startTime;
        private final SimpleIntegerProperty completionTime;
        private final SimpleIntegerProperty turnaroundTime;
        private final SimpleIntegerProperty waitingTime;
        private final SimpleIntegerProperty responseTime;
        private final SimpleIntegerProperty executionSegments;
        private final PS.Job originalJob;

        public JobWrapper(PS.Job job) {
            this.jobId = new SimpleStringProperty(job.jobId);
            this.arrivalTime = new SimpleIntegerProperty(job.arrivalTime);
            this.burstTime = new SimpleIntegerProperty(job.burstTime);
            this.priority = new SimpleIntegerProperty(job.priority);
            this.startTime = new SimpleIntegerProperty(job.startTime);
            this.completionTime = new SimpleIntegerProperty(job.completionTime);
            this.turnaroundTime = new SimpleIntegerProperty(job.turnaroundTime);
            this.waitingTime = new SimpleIntegerProperty(job.waitingTime);
            this.responseTime = new SimpleIntegerProperty(job.responseTime);
            this.executionSegments = new SimpleIntegerProperty(job.executionHistory.size());
            this.originalJob = job;
        }

        // Property getters for TableView
        public String getJobId() { return jobId.get(); }
        public int getArrivalTime() { return arrivalTime.get(); }
        public int getBurstTime() { return burstTime.get(); }
        public int getPriority() { return priority.get(); }
        public int getStartTime() { return startTime.get(); }
        public int getCompletionTime() { return completionTime.get(); }
        public int getTurnaroundTime() { return turnaroundTime.get(); }
        public int getWaitingTime() { return waitingTime.get(); }
        public int getResponseTime() { return responseTime.get(); }
        public int getExecutionSegments() { return executionSegments.get(); }
        public PS.Job getOriginalJob() { return originalJob; }
    }

    /**
     * Launch the visualizer with the provided jobs and time slices
     */
    public static void launchVisualizer(List<PS.Job> completedJobs, List<PS.TimeSlice> timeSlices) {
        jobs = completedJobs;
        allSlices = timeSlices;
        System.setProperty("javafx.sg.warn", "false");
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Priority Scheduling Visualization");

        // Create tabs
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-font-size: 14px;");
        tabPane.getTabs().addAll(
            createTimeline(),
            createGanttTab(),
            createTableTab(),
            createMetricsTab()
        );

        // Create export buttons with better styling
        Button exportCsvButton = createStyledButton("Export to CSV", this::exportToCsv);
        Button exportPdfButton = createStyledButton("Export to PDF", this::exportToPdf);
        Button exportJsonButton = createStyledButton("Export to JSON", this::exportToJson);

        HBox buttonBox = new HBox(10, exportCsvButton, exportPdfButton, exportJsonButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setStyle("-fx-background-color: #f5f5f5;");

        // Create info label showing algorithm info
        Label infoLabel = new Label("Priority Scheduling (PS) Algorithm");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 5 10;");
        
        // Top panel with info
        HBox topBox = new HBox(infoLabel);
        topBox.setStyle("-fx-background-color: #e8e8e8; -fx-padding: 5; -fx-alignment: center;");

        // Main layout
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(tabPane);
        root.setBottom(buttonBox);
        root.setStyle("-fx-background-color: #ecf0f1;");

        Scene scene = new Scene(root, 1100, 750);
        scene.getStylesheets().add("https://fonts.googleapis.com/css2?family=Roboto");
        scene.setFill(Color.web("#ecf0f1"));
        primaryStage.setScene(scene);
        // Set minimum size to prevent resizing too small
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        
        // Center on screen
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Creates the gantt chart tab showing job execution blocks
     */
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

    /**
     * Creates a Gantt chart showing job execution times as stacked bars
     */
    private StackedBarChart<String, Number> createGanttChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jobs");
        yAxis.setLabel("Time Units");

        StackedBarChart<String, Number> ganttChart = new StackedBarChart<>(xAxis, yAxis);
        ganttChart.setTitle("Priority Scheduling Gantt Chart");
        ganttChart.setLegendVisible(true);
        ganttChart.setCategoryGap(10);
        ganttChart.setAnimated(false);

        // Create series for different states
        XYChart.Series<String, Number> waitingSeries = new XYChart.Series<>();
        waitingSeries.setName("Waiting Time");

        XYChart.Series<String, Number> executionSeries = new XYChart.Series<>();
        executionSeries.setName("Execution Time");

        // Keep track of job order based on priority
        List<PS.Job> sortedJobs = new ArrayList<>(jobs);
        Collections.sort(sortedJobs, Comparator.comparingInt(job -> job.priority));

        for (PS.Job job : sortedJobs) {
            // Calculate initial waiting (from arrival to first execution)
            int initialWaiting = job.startTime - job.arrivalTime;
            if (initialWaiting > 0) {
                XYChart.Data<String, Number> waitData = new XYChart.Data<>(job.jobId + " (P:" + job.priority + ")", initialWaiting);
                waitingSeries.getData().add(waitData);
            } else {
                waitingSeries.getData().add(new XYChart.Data<>(job.jobId + " (P:" + job.priority + ")", 0));
            }

            // Total execution time equals the burst time
            XYChart.Data<String, Number> execData = new XYChart.Data<>(job.jobId + " (P:" + job.priority + ")", job.burstTime);
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

    /**
     * Creates a table view displaying job metrics
     */
    private Tab createTableTab() {
        Tab tab = new Tab("Job Details");
        tab.setClosable(false);

        TableView<JobWrapper> table = new TableView<>();
        table.setStyle("-fx-font-size: 14px;");

        // Define columns
        TableColumn<JobWrapper, String> idCol = new TableColumn<>("Job ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("jobId"));

        TableColumn<JobWrapper, Number> arrivalCol = new TableColumn<>("Arrival");
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));

        TableColumn<JobWrapper, Number> burstCol = new TableColumn<>("Burst");
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        
        TableColumn<JobWrapper, Number> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));

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

        TableColumn<JobWrapper, Number> execSegmentsCol = new TableColumn<>("Execution Segments");
        execSegmentsCol.setCellValueFactory(new PropertyValueFactory<>("executionSegments"));

        table.getColumns().addAll(
            idCol, priorityCol, arrivalCol, burstCol, startCol, 
            completionCol, turnaroundCol, waitingCol, responseCol, execSegmentsCol
        );

        // Add data
        ObservableList<JobWrapper> tableData = FXCollections.observableArrayList();
        for (PS.Job job : jobs) {
            tableData.add(new JobWrapper(job));
        }
        table.setItems(tableData);

        // Set column widths
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white;");
        
        // Add statistics header
        Text statsHeader = new Text("Job Execution Statistics");
        statsHeader.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        container.getChildren().addAll(statsHeader, table);
        
        tab.setContent(container);
        return tab;
    }

    /**
     * Creates the metrics tab with performance indicators
     */
    private Tab createMetricsTab() {
        Tab tab = new Tab("Performance Metrics");
        tab.setClosable(false);

        VBox metricsBox = new VBox(15);
        metricsBox.setPadding(new Insets(20));
        metricsBox.setStyle("-fx-background-color: #ffffff;");

        // Algorithm information
        TitledPane algoPane = createMetricTitledPane("Algorithm Information",
                "Algorithm: Priority Scheduling (PS)",
                "Total Jobs: " + jobs.size(),
                "Note: Lower priority value means higher priority");

        // CPU Metrics
        TitledPane cpuPane = createMetricTitledPane("CPU Utilization",
                "Utilization: " + df.format(calculateCpuUtilization()) + "%",
                "Throughput: " + df.format(calculateThroughput()) + " jobs/unit");

        // Time Metrics
        TitledPane timePane = createMetricTitledPane("Time Metrics",
                "Avg Turnaround: " + df.format(calculateAverageTurnaroundTime()),
                "Avg Waiting: " + df.format(calculateAverageWaitingTime()),
                "Avg Response: " + df.format(calculateAverageResponseTime()));

        // Priority Metrics
        TitledPane priorityPane = createMetricTitledPane("Priority Analysis",
                "Average Priority: " + df.format(calculateAveragePriority()),
                "Priority Range: " + calculateMinPriority() + " - " + calculateMaxPriority());

        metricsBox.getChildren().addAll(algoPane, cpuPane, timePane, priorityPane);

        // Add a summary chart
        metricsBox.getChildren().add(createMetricsChart());

        tab.setContent(new ScrollPane(metricsBox));
        return tab;
    }

    /**
     * Creates a tab showing detailed execution timeline
     */
    private Tab createTimeline() {
        Tab tab = new Tab("Execution Timeline");
        tab.setClosable(false);

        // Create a pane to hold our custom timeline
        Pane timelinePane = new Pane();
        timelinePane.setStyle("-fx-background-color: white;");

        // Create a ScrollPane to allow scrolling for many jobs
        ScrollPane scrollPane = new ScrollPane(timelinePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);

        // Calculate metrics for scaling
        int maxTime = allSlices.stream()
                .mapToInt(slice -> slice.endTime)
                .max()
                .orElse(0);

        double timeUnitWidth = 30;  // Width per time unit in pixels
        double jobHeight = 40;      // Height per job in pixels
        double horizontalOffset = 150; // Space for job labels

        // Set the canvas size
        timelinePane.setPrefWidth(horizontalOffset + (maxTime * timeUnitWidth) + 50);
        timelinePane.setPrefHeight((jobs.size() * (jobHeight + 10)) + 150);

        // Draw timeline axis
        Line timeAxis = new Line(horizontalOffset, 70, horizontalOffset + (maxTime * timeUnitWidth), 70);
        timeAxis.setStroke(Color.BLACK);
        timeAxis.setStrokeWidth(2);
        timelinePane.getChildren().add(timeAxis);

        // Draw time markers
        int timeStep = Math.max(1, maxTime / 20); // Adapt step size to the chart width
        for (int t = 0; t <= maxTime; t += timeStep) {
            double xPos = horizontalOffset + (t * timeUnitWidth);

            // Tick mark
            Line tick = new Line(xPos, 65, xPos, 75);
            tick.setStroke(Color.BLACK);
            timelinePane.getChildren().add(tick);

            // Time label
            Text timeLabel = new Text(String.valueOf(t));
            timeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            timeLabel.setX(xPos - 3);
            timeLabel.setY(60);
            timelinePane.getChildren().add(timeLabel);
        }

        // Create color map for jobs
        Map<String, Color> jobColors = new HashMap<>();
        Color[] colorPalette = {
            Color.LIMEGREEN, Color.DODGERBLUE, Color.MEDIUMPURPLE, 
            Color.TOMATO, Color.GOLD, Color.DARKORANGE, 
            Color.DEEPPINK, Color.TEAL, Color.DARKVIOLET, Color.CADETBLUE
        };

        // Add legend
        Text legendTitle = new Text("Legend:");
        legendTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        legendTitle.setX(horizontalOffset);
        legendTitle.setY(40);
        timelinePane.getChildren().add(legendTitle);

        // Add color legend for each job
        double legendItemX = horizontalOffset + 60;
        
        int colorIndex = 0;
        for (PS.Job job : jobs) {
            jobColors.put(job.jobId, colorPalette[colorIndex % colorPalette.length]);
            colorIndex++;
        }

        for (Map.Entry<String, Color> entry : jobColors.entrySet()) {
            Rectangle colorRect = new Rectangle(legendItemX, 32, 15, 10);
            colorRect.setFill(entry.getValue());
            colorRect.setStroke(Color.BLACK);
            timelinePane.getChildren().add(colorRect);

            Text jobText = new Text(entry.getKey() + " (P:" + 
                jobs.stream()
                    .filter(job -> job.jobId.equals(entry.getKey()))
                    .findFirst()
                    .map(job -> job.priority)
                    .orElse(0) + 
                ")");
            jobText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            jobText.setX(legendItemX + 20);
            jobText.setY(40);
            timelinePane.getChildren().add(jobText);

            legendItemX += 90; // More space for priority information
        }

        // Draw job rows
        Map<String, Double> jobYPositions = new HashMap<>();
        double currentY = 100;

        // First pass: create job rows and arrival markers
        for (PS.Job job : jobs) {
            // Store the Y position of this job
            jobYPositions.put(job.jobId, currentY);
            
            // Job label - now with priority
            Text jobLabel = new Text(job.jobId + " (Priority: " + job.priority + ")");
            jobLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            jobLabel.setX(horizontalOffset - 145);  // Move left to accommodate priority
            jobLabel.setY(currentY + 25);
            timelinePane.getChildren().add(jobLabel);

            // Arrival marker - vertical dotted line
            double arrivalX = horizontalOffset + (job.arrivalTime * timeUnitWidth);
            Line arrivalLine = new Line(arrivalX, currentY - 10, arrivalX, currentY + jobHeight + 10);
            arrivalLine.setStroke(Color.GRAY);
            arrivalLine.setStrokeWidth(1);
            arrivalLine.getStrokeDashArray().addAll(2d, 4d);
            timelinePane.getChildren().add(arrivalLine);

            // Arrival text
            Text arrivalText = new Text("Arrival");
            arrivalText.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            arrivalText.setFill(Color.GRAY);
            double textWidth = arrivalText.getBoundsInLocal().getWidth();
            arrivalText.setX(arrivalX - (textWidth / 2));
            arrivalText.setY(currentY - 15);
            timelinePane.getChildren().add(arrivalText);

            currentY += jobHeight + 30;  // Move to next job
        }

        // Second pass: Add execution slices
        for (PS.TimeSlice slice : allSlices) {
            // Find the job for this slice
            Optional<PS.Job> jobOpt = jobs.stream()
                    .filter(j -> j.executionHistory.contains(slice))
                    .findFirst();
            
            if (jobOpt.isPresent()) {
                PS.Job job = jobOpt.get();
                double jobY = jobYPositions.get(job.jobId);
                Color jobColor = jobColors.get(job.jobId);
                
                // Draw execution block
                double sliceStartX = horizontalOffset + (slice.startTime * timeUnitWidth);
                double sliceWidth = (slice.endTime - slice.startTime) * timeUnitWidth;
                
                // Draw execution rectangle
                Rectangle execRect = new Rectangle(sliceStartX, jobY, sliceWidth, jobHeight);
                execRect.setFill(jobColor);
                execRect.setStroke(Color.BLACK);
                execRect.setStrokeWidth(1);
                timelinePane.getChildren().add(execRect);

                // Add tooltip with details
                Tooltip tooltip = new Tooltip(
                    "Job: " + job.jobId +
                    "\nPriority: " + job.priority +
                    "\nStart: " + slice.startTime +
                    "\nEnd: " + slice.endTime +
                    "\nDuration: " + slice.getDuration()
                );
                Tooltip.install(execRect, tooltip);

                // Add text label if there's enough space
                if (sliceWidth > 30) {
                    Text execLabel = new Text(String.valueOf(slice.getDuration()));
                    execLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
                    execLabel.setFill(Color.WHITE);
                    execLabel.setX(sliceStartX + (sliceWidth / 2) - 5);
                    execLabel.setY(jobY + (jobHeight / 2) + 5);
                    timelinePane.getChildren().add(execLabel);
                }
            }

            // Mark completion - vertical green line
            for (PS.Job job : jobs) {
                double jobY = jobYPositions.get(job.jobId);
                double completionX = horizontalOffset + (job.completionTime * timeUnitWidth);
                Line completionLine = new Line(completionX, jobY, completionX, jobY + jobHeight);
                completionLine.setStroke(Color.GREEN);
                completionLine.setStrokeWidth(2);
                timelinePane.getChildren().add(completionLine);
                
                // Completion text
                Text completeLabel = new Text("Complete");
                completeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
                completeLabel.setFill(Color.GREEN);
                double textWidth = completeLabel.getBoundsInLocal().getWidth();
                completeLabel.setX(completionX - (textWidth / 2));
                completeLabel.setY(jobY + jobHeight + 15);
                timelinePane.getChildren().add(completeLabel);
            }
        }

        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Creates a chart displaying performance metrics
     */
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

    /**
     * Creates a titled pane for displaying metrics groups
     */
    private TitledPane createMetricTitledPane(String title, String... metrics) {
        VBox content = new VBox(5);
        content.setPadding(new Insets(10, 5, 10, 5));
        content.setStyle("-fx-background-color: #f8f9fa;");

        for (String metric : metrics) {
            Label label = new Label(metric);
            label.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            content.getChildren().add(label);
        }

        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(true);
        return pane;
    }

    /**
     * Creates a styled button with the given action
     */
    private Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(e -> action.run());
        button.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(35);
        button.setPrefWidth(150);
        
        button.setOnMouseEntered(e -> 
            button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;")
        );
        button.setOnMouseExited(e -> 
            button.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;")
        );
        
        return button;
    }

    /**
     * Calculate CPU utilization
     */
    private double calculateCpuUtilization() {
        int totalBurstTime = jobs.stream().mapToInt(job -> job.burstTime).sum();
        int makespan = jobs.stream().mapToInt(job -> job.completionTime).max().orElse(0);
        return makespan > 0 ? (double) totalBurstTime / makespan * 100 : 0;
    }

    /**
     * Calculate throughput
     */
    private double calculateThroughput() {
        int makespan = jobs.stream().mapToInt(job -> job.completionTime).max().orElse(0);
        return makespan > 0 ? (double) jobs.size() / makespan : 0;
    }

    /**
     * Calculate average turnaround time
     */
    private double calculateAverageTurnaroundTime() {
        return jobs.stream().mapToInt(job -> job.turnaroundTime).average().orElse(0);
    }

    /**
     * Calculate average waiting time
     */
    private double calculateAverageWaitingTime() {
        return jobs.stream().mapToInt(job -> job.waitingTime).average().orElse(0);
    }

    /**
     * Calculate average response time
     */
    private double calculateAverageResponseTime() {
        return jobs.stream().mapToInt(job -> job.responseTime).average().orElse(0);
    }

    /**
     * Calculate average priority
     */
    private double calculateAveragePriority() {
        return jobs.stream().mapToInt(job -> job.priority).average().orElse(0);
    }

    /**
     * Calculate minimum priority value
     */
    private int calculateMinPriority() {
        return jobs.stream().mapToInt(job -> job.priority).min().orElse(0);
    }

    /**
     * Calculate maximum priority value
     */
    private int calculateMaxPriority() {
        return jobs.stream().mapToInt(job -> job.priority).max().orElse(0);
    }

    /**
     * Export job data to CSV
     */
    private void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Headers
                writer.write("Job ID,Priority,Arrival Time,Burst Time,Start Time,Completion Time,Turnaround Time,Waiting Time,Response Time,Execution Segments\n");
                
                // Job data
                for (PS.Job job : jobs) {
                    writer.write(String.format(
                        "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
                        job.jobId, job.priority, job.arrivalTime, job.burstTime, job.startTime,
                        job.completionTime, job.turnaroundTime, job.waitingTime, job.responseTime,
                        job.executionHistory.size()
                    ));
                }
                
                // Add detailed execution history
                writer.write("\nExecution History\n");
                writer.write("Job ID,Start Time,End Time,Duration\n");
                
                for (PS.Job job : jobs) {
                    for (PS.TimeSlice slice : job.executionHistory) {
                        writer.write(String.format(
                            "%s,%d,%d,%d\n",
                            job.jobId, slice.startTime, slice.endTime, slice.getDuration()
                        ));
                    }
                }
                
                // Add algorithm metrics
                writer.write("\nAlgorithm Information\n");
                writer.write("Algorithm,Process Scheduling (PS)\n");
                writer.write(String.format("CPU Utilization,%.2f%%\n", calculateCpuUtilization()));
                writer.write(String.format("Throughput,%.2f jobs/unit\n", calculateThroughput()));
                writer.write(String.format("Average Turnaround Time,%.2f\n", calculateAverageTurnaroundTime()));
                writer.write(String.format("Average Waiting Time,%.2f\n", calculateAverageWaitingTime()));
                writer.write(String.format("Average Response Time,%.2f\n", calculateAverageResponseTime()));
                
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Data exported to CSV successfully!");
                
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export data: " + e.getMessage());
            }
        }
    }

    /**
     * Export job data to PDF
     */
    private void exportToPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);
        
        if (file != null) {
            try {
                PDDocument document = new PDDocument();
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                
                // Add title
                contentStream.beginText();
                PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                contentStream.setFont(headerFont, 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Priority Scheduling Algorithm - Job Report");
                contentStream.endText();

                // Add date and time
                contentStream.beginText();
                PDType1Font textFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                contentStream.setFont(textFont, 12);
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("Generated: " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                contentStream.endText();

                // Add summary metrics
                contentStream.beginText();
                contentStream.setFont(headerFont, 14);
                contentStream.newLineAtOffset(50, 680);
                contentStream.showText("Performance Metrics");
                contentStream.endText();

                String[] metrics = {
                        "CPU Utilization: " + df.format(calculateCpuUtilization()) + "%",
                        "Throughput: " + df.format(calculateThroughput()) + " jobs/unit",
                        "Avg Turnaround Time: " + df.format(calculateAverageTurnaroundTime()),
                        "Avg Waiting Time: " + df.format(calculateAverageWaitingTime()),
                        "Avg Response Time: " + df.format(calculateAverageResponseTime()),
                        "Avg Priority: " + df.format(calculateAveragePriority()),
                        "Priority Range: " + calculateMinPriority() + "-" + calculateMaxPriority()
                };

                int yPosition = 670;
                for (String metric : metrics) {
                    contentStream.beginText();
                    contentStream.setFont(textFont, 12);
                    contentStream.newLineAtOffset(70, yPosition);
                    contentStream.showText(metric);
                    contentStream.endText();
                    yPosition -= 20;
                }

                // Add job table header
                yPosition = 550;
                contentStream.beginText();
                contentStream.setFont(headerFont, 14);
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Job Execution Details");
                contentStream.endText();

                yPosition -= 30;

                // Table headers
                String[] headers = {"Job ID", "Priority", "Arrival", "Burst", "Start", "Complete", "Turnaround", "Waiting", "Response"};
                int[] colWidths = {50, 50, 50, 50, 50, 60, 70, 50, 60};

                // Draw table header
                int headerX = 50;
                for (int i = 0; i < headers.length; i++) {
                    contentStream.beginText();
                    contentStream.setFont(headerFont, 10);
                    contentStream.newLineAtOffset(headerX, yPosition);
                    contentStream.showText(headers[i]);
                    contentStream.endText();
                    headerX += colWidths[i];
                }

                // Table rows
                int jobsPerPage = 20;
                int jobCount = 0;

                // Track the row positions
                yPosition -= 20;

                // Sort jobs by priority for display
                List<PS.Job> sortedJobs = new ArrayList<>(jobs);
                Collections.sort(sortedJobs, Comparator.comparingInt(job -> job.priority));

                for (PS.Job job : sortedJobs) {
                    // Check if we need a new page
                    if (jobCount >= jobsPerPage) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        
                        // Reset position for new page
                        yPosition = 750;
                        
                        // Add header on new page
                        contentStream.beginText();
                        contentStream.setFont(headerFont, 14);
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText("Job Execution Details (continued)");
                        contentStream.endText();
                        
                        yPosition -= 30;
                        
                        // Add table header again
                        headerX = 50;
                        for (int i = 0; i < headers.length; i++) {
                            contentStream.beginText();
                            contentStream.setFont(headerFont, 10);
                            contentStream.newLineAtOffset(headerX, yPosition);
                            contentStream.showText(headers[i]);
                            contentStream.endText();
                            headerX += colWidths[i];
                        }
                        
                        yPosition -= 20;
                        jobCount = 0;
                    }
                    
                    // Cell data
                    int cellX = 50;
                    
                    // Job ID
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(job.jobId);
                    contentStream.endText();
                    cellX += colWidths[0];
                    
                    // Priority
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.priority));
                    contentStream.endText();
                    cellX += colWidths[1];

                    // Arrival Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.arrivalTime));
                    contentStream.endText();
                    cellX += colWidths[2];

                    // Burst Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.burstTime));
                    contentStream.endText();
                    cellX += colWidths[3];

                    // Start Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.startTime));
                    contentStream.endText();
                    cellX += colWidths[4];

                    // Completion Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.completionTime));
                    contentStream.endText();
                    cellX += colWidths[5];

                    // Turnaround Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.turnaroundTime));
                    contentStream.endText();
                    cellX += colWidths[6];

                    // Waiting Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.waitingTime));
                    contentStream.endText();
                    cellX += colWidths[7];

                    // Response Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.responseTime));
                    contentStream.endText();

                    jobCount++;
                    yPosition -= 20;
                }

                // Add execution history on a new page
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);

                // Title for execution history
                contentStream.beginText();
                contentStream.setFont(headerFont, 14);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Job Execution History");
                contentStream.endText();

                // Add description
                contentStream.beginText();
                contentStream.setFont(textFont, 12);
                contentStream.newLineAtOffset(50, 730);
                contentStream.showText("This section shows all execution time slices for each job.");
                contentStream.endText();

                // Table headers for execution history
                String[] historyHeaders = {"Job ID", "Priority", "Start Time", "End Time", "Duration", "Notes"};
                int[] historyColWidths = {50, 50, 70, 70, 60, 180};

                yPosition = 700;
                headerX = 50;

                for (int i = 0; i < historyHeaders.length; i++) {
                    contentStream.beginText();
                    contentStream.setFont(headerFont, 10);
                    contentStream.newLineAtOffset(headerX, yPosition);
                    contentStream.showText(historyHeaders[i]);
                    contentStream.endText();
                    headerX += historyColWidths[i];
                }

                yPosition -= 20;
                int sliceCount = 0;
                int slicesPerPage = 30;

                // Sort all slices by start time
                List<Object[]> allExecutionSlices = new ArrayList<>();
                for (PS.Job job : jobs) {
                    for (PS.TimeSlice slice : job.executionHistory) {
                        allExecutionSlices.add(new Object[]{job, slice});
                    }
                }

                // Sort by start time
                allExecutionSlices.sort((a, b) -> {
                    PS.TimeSlice sliceA = (PS.TimeSlice) a[1];
                    PS.TimeSlice sliceB = (PS.TimeSlice) b[1];
                    return Integer.compare(sliceA.startTime, sliceB.startTime);
                });

                // Print all slices
                for (Object[] pair : allExecutionSlices) {
                    PS.Job job = (PS.Job) pair[0];
                    PS.TimeSlice slice = (PS.TimeSlice) pair[1];

                    // Check if we need a new page
                    if (sliceCount >= slicesPerPage) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        
                        // Add headers on the new page
                        yPosition = 750;
                        contentStream.beginText();
                        contentStream.setFont(headerFont, 14);
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText("Job Execution History (continued)");
                        contentStream.endText();
                        
                        yPosition = 730;
                        headerX = 50;
                        
                        for (int i = 0; i < historyHeaders.length; i++) {
                            contentStream.beginText();
                            contentStream.setFont(headerFont, 10);
                            contentStream.newLineAtOffset(headerX, yPosition);
                            contentStream.showText(historyHeaders[i]);
                            contentStream.endText();
                            headerX += historyColWidths[i];
                        }
                        
                        yPosition -= 20;
                        sliceCount = 0;
                    }
                    
                    // Cell data
                    int cellX = 50;
                    
                    // Job ID
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(job.jobId);
                    contentStream.endText();
                    cellX += historyColWidths[0];
                    
                    // Priority
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.priority));
                    contentStream.endText();
                    cellX += historyColWidths[1];
                    
                    // Start Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(slice.startTime));
                    contentStream.endText();
                    cellX += historyColWidths[2];
                    
                    // End Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(slice.endTime));
                    contentStream.endText();
                    cellX += historyColWidths[3];
                    
                    // Duration
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(slice.getDuration()));
                    contentStream.endText();
                    cellX += historyColWidths[4];
                    
                    // Notes - check if this is the completion slice
                    String notes = slice.endTime == job.completionTime ? "Job completed" : "";
                    
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(notes);
                    contentStream.endText();
                    
                    yPosition -= 20;
                    sliceCount++;
                }

                // Draw a basic visual representation of the Gantt chart
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);

                contentStream.beginText();
                contentStream.setFont(headerFont, 14);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Gantt Chart");
                contentStream.endText();

                // Add generation timestamp
                String timestamp = "Generated on: " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                contentStream.beginText();
                contentStream.setFont(textFont, 10);
                contentStream.newLineAtOffset(250, 750);
                contentStream.showText(timestamp);
                contentStream.endText();

                yPosition = 720;

                // Draw time scale
                int maxTime = jobs.stream()
                        .mapToInt(j -> j.completionTime)
                        .max()
                        .orElse(0);

                float timeUnitWidth = 500f / maxTime;  // Scale to fit on page
                int scaleStep = Math.max(1, maxTime / 20);  // Determine appropriate scale step

                // Draw time scale markers
                contentStream.setLineWidth(0.5f);
                contentStream.moveTo(100, yPosition);
                contentStream.lineTo(100 + (maxTime * timeUnitWidth), yPosition);
                contentStream.stroke();

                for (int t = 0; t <= maxTime; t += scaleStep) {
                    float xPos = 100 + (t * timeUnitWidth);
                    
                    // Tick mark
                    contentStream.moveTo(xPos, yPosition - 5);
                    contentStream.lineTo(xPos, yPosition + 5);
                    contentStream.stroke();
                    
                    // Draw time label
                    contentStream.beginText();
                    contentStream.setFont(textFont, 8);
                    contentStream.newLineAtOffset(xPos - 3, yPosition - 15);
                    contentStream.showText(String.valueOf(t));
                    contentStream.endText();
                }

                yPosition -= 30;
                int timelineHeight = 25;  // Increase height for better visibility

                // Add legend
                float legendX = 400;
                contentStream.beginText();
                contentStream.setFont(textFont, 8);
                contentStream.newLineAtOffset(legendX, yPosition);
                contentStream.showText("Legend:");
                contentStream.endText();

                // Waiting time legend
                contentStream.setNonStrokingColor(0.8f, 0.8f, 0.8f); // Light gray
                contentStream.addRect(legendX + 50, yPosition - 5, 15, 10);
                contentStream.fill();

                contentStream.beginText();
                contentStream.setNonStrokingColor(0, 0, 0);
                contentStream.setFont(textFont, 8);
                contentStream.newLineAtOffset(legendX + 70, yPosition);
                contentStream.showText("Waiting");
                contentStream.endText();

                // Execution time legend
                contentStream.setNonStrokingColor(0.2f, 0.6f, 0.9f);
                contentStream.addRect(legendX + 120, yPosition - 5, 15, 10);
                contentStream.fill();

                contentStream.beginText();
                contentStream.setNonStrokingColor(0, 0, 0);
                contentStream.setFont(textFont, 8);
                contentStream.newLineAtOffset(legendX + 140, yPosition);
                contentStream.showText("Execution");
                contentStream.endText();

                yPosition -= 25;

                // Sort jobs by priority before drawing
                sortedJobs = new ArrayList<>(jobs);
                Collections.sort(sortedJobs, Comparator.comparingInt(job -> job.priority));

                // Draw job rows with clear labels and borders
                for (PS.Job job : sortedJobs) {
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(50, yPosition);
                    // Show job ID with priority
                    contentStream.showText(job.jobId + " (P:" + job.priority + ")");
                    contentStream.endText();

                    // Add text labels for start/end times inside the bars
                    float barStartX = 100 + (job.arrivalTime * timeUnitWidth);
                    float waitEndX = 100 + (job.startTime * timeUnitWidth);
                    float execEndX = 100 + (job.completionTime * timeUnitWidth);

                    // Draw waiting time (if any) with border
                    if (job.startTime > job.arrivalTime) {
                        contentStream.setNonStrokingColor(0.8f, 0.8f, 0.8f); // Light gray
                        contentStream.addRect(barStartX, yPosition - 5, waitEndX - barStartX, timelineHeight);
                        contentStream.fill();
                        
                        contentStream.setStrokingColor(0, 0, 0);
                        contentStream.addRect(barStartX, yPosition - 5, waitEndX - barStartX, timelineHeight);
                        contentStream.stroke();
                        
                        // Add waiting time label if there's enough space
                        if (waitEndX - barStartX > 40) {
                            contentStream.beginText();
                            contentStream.setNonStrokingColor(0, 0, 0);
                            contentStream.setFont(textFont, 8);
                            contentStream.newLineAtOffset(barStartX + 2, yPosition + 5);
                            contentStream.showText("Wait: " + (job.startTime - job.arrivalTime));
                            contentStream.endText();
                        }
                    }
                    
                    // Draw execution time with border
                    contentStream.setNonStrokingColor(0.2f, 0.6f, 0.9f); // Blue
                    contentStream.addRect(waitEndX, yPosition - 5, execEndX - waitEndX, timelineHeight);
                    contentStream.fill();
                    
                    contentStream.setStrokingColor(0, 0, 0);
                    contentStream.addRect(waitEndX, yPosition - 5, execEndX - waitEndX, timelineHeight);
                    contentStream.stroke();
                    
                    // Add execution time label if there's enough space
                    if (execEndX - waitEndX > 40) {
                        contentStream.beginText();
                        contentStream.setNonStrokingColor(1, 1, 1); // White text
                        contentStream.setFont(textFont, 8);
                        contentStream.newLineAtOffset(waitEndX + 2, yPosition + 5);
                        contentStream.showText("Exec: " + job.burstTime);
                        contentStream.endText();
                    }

                    // Add completion time
                    contentStream.beginText();
                    contentStream.setNonStrokingColor(0, 0, 0);
                    contentStream.setFont(textFont, 8);
                    contentStream.newLineAtOffset(execEndX + 5, yPosition + 5);
                    contentStream.showText("T=" + job.completionTime);
                    contentStream.endText();

                    yPosition -= timelineHeight + 15;  // More space between jobs

                    // Check if we need to add a new page for the gantt chart
                    if (yPosition < 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 750;

                        contentStream.beginText();
                        contentStream.setFont(headerFont, 14);
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText("Gantt Chart (continued)");
                        contentStream.endText();
                        yPosition -= 40;
                    }
                }

                // Reset color to black for text
                contentStream.setNonStrokingColor(0, 0, 0);

                // Close the content stream and save the document
                contentStream.close();
                document.save(file);
                document.close();

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "PDF report exported successfully!");

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Export data to JSON format
     */
    private void exportToJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Scheduling Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                StringBuilder json = new StringBuilder();
                json.append("{\n");
                json.append("  \"algorithm\": \"Priority Scheduling\",\n");
                
                // Add metrics
                json.append("  \"metrics\": {\n");
                json.append("    \"cpuUtilization\": ").append(df.format(calculateCpuUtilization())).append(",\n");
                json.append("    \"throughput\": ").append(df.format(calculateThroughput())).append(",\n");
                json.append("    \"averageTurnaroundTime\": ").append(df.format(calculateAverageTurnaroundTime())).append(",\n");
                json.append("    \"averageWaitingTime\": ").append(df.format(calculateAverageWaitingTime())).append(",\n");
                json.append("    \"averageResponseTime\": ").append(df.format(calculateAverageResponseTime())).append(",\n");
                json.append("    \"averagePriority\": ").append(df.format(calculateAveragePriority())).append("\n");
                json.append("  },\n");
                
                // Add jobs
                json.append("  \"jobs\": [\n");
                
                for (int i = 0; i < jobs.size(); i++) {
                    PS.Job job = jobs.get(i);
                    json.append("    {\n");
                    json.append("      \"id\": \"").append(job.jobId).append("\",\n");
                    json.append("      \"priority\": ").append(job.priority).append(",\n");
                    json.append("      \"arrival\": ").append(job.arrivalTime).append(",\n");
                    json.append("      \"burst\": ").append(job.burstTime).append(",\n");
                    json.append("      \"start\": ").append(job.startTime).append(",\n");
                    json.append("      \"completion\": ").append(job.completionTime).append(",\n");
                    json.append("      \"turnaround\": ").append(job.turnaroundTime).append(",\n");
                    json.append("      \"waiting\": ").append(job.waitingTime).append(",\n");
                    json.append("      \"response\": ").append(job.responseTime).append(",\n");
                    
                    // Add execution history
                    json.append("      \"executionHistory\": [\n");
                    for (int j = 0; j < job.executionHistory.size(); j++) {
                        PS.TimeSlice slice = job.executionHistory.get(j);
                        json.append("        {\n");
                        json.append("          \"start\": ").append(slice.startTime).append(",\n");
                        json.append("          \"end\": ").append(slice.endTime).append(",\n");
                        json.append("          \"duration\": ").append(slice.getDuration()).append("\n");
                        json.append("        }").append(j < job.executionHistory.size() - 1 ? "," : "").append("\n");
                    }
                    json.append("      ]\n");
                    json.append("    }").append(i < jobs.size() - 1 ? "," : "").append("\n");
                }
                
                json.append("  ]\n");
                json.append("}\n");
                
                writer.write(json.toString());
                
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Data exported to JSON successfully!");
                
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export data: " + e.getMessage());
            }
        }
    }

    /**
     * Helper method to show alerts
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}