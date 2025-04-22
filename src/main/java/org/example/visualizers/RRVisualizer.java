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
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.example.schedulers.RR;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JavaFX visualization for Round Robin (RR) scheduling algorithm
 * Supports both preemptive and non-preemptive modes
 * Shows execution timeline, process metrics, and performance indicators
 */
public class RRVisualizer extends Application {

    // Static properties for launching from Main class
    private static List<RR.Job> jobs;
    private static int timeQuantum = 2;
    private static boolean preemptive = true;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    /**
     * Wrapper class for JavaFX TableView binding
     * Provides property accessors for job attributes
     */
    public static class JobWrapper {
        private final SimpleStringProperty jobId;
        private final SimpleIntegerProperty arrivalTime;
        private final SimpleIntegerProperty burstTime;
        private final SimpleIntegerProperty startTime;
        private final SimpleIntegerProperty completionTime;
        private final SimpleIntegerProperty turnaroundTime;
        private final SimpleIntegerProperty waitingTime;
        private final SimpleIntegerProperty responseTime;
        private final SimpleIntegerProperty executionSegments;
        private final RR.Job originalJob;

        public JobWrapper(RR.Job job) {
            this.jobId = new SimpleStringProperty(job.jobId);
            this.arrivalTime = new SimpleIntegerProperty(job.arrivalTime);
            this.burstTime = new SimpleIntegerProperty(job.burstTime);
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
        public int getStartTime() { return startTime.get(); }
        public int getCompletionTime() { return completionTime.get(); }
        public int getTurnaroundTime() { return turnaroundTime.get(); }
        public int getWaitingTime() { return waitingTime.get(); }
        public int getResponseTime() { return responseTime.get(); }
        public int getExecutionSegments() { return executionSegments.get(); }
        public RR.Job getOriginalJob() { return originalJob; }
    }

    // Setters for static properties
    public static void setJobs(List<RR.Job> jobList) {
        jobs = jobList;
    }

    public static void setTimeQuantum(int quantum) {
        timeQuantum = quantum;
    }

    public static void setPreemptive(boolean isPreemptive) {
        preemptive = isPreemptive;
    }

    public static void launchVisualizer(List<RR.Job> jobList, int quantum, boolean isPreemptive) {
        jobs = jobList;
        timeQuantum = quantum;
        preemptive = isPreemptive;
        System.setProperty("javafx.sg.warn", "false");
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        // Set up the primary stage
        primaryStage.setTitle("Round Robin Scheduler Visualization" + 
                              (preemptive ? " (Preemptive)" : " (Non-Preemptive)"));

        // Create tabs for different views
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-font-size: 14px;");
        tabPane.getTabs().addAll(
            createTimeline(),
            createGanttTab(),
            createTableTab(),
            createMetricsTab(),
            createContextSwitchTab()
        );

        // Create action buttons
        Button exportCsvButton = createStyledButton("Export to CSV", this::exportToCsv);
        Button exportPdfButton = createStyledButton("Export to PDF", this::exportToPdf);
        Button exportJsonButton = createStyledButton("Export to JSON", this::exportToJson);

        HBox buttonBox = new HBox(10, exportCsvButton, exportPdfButton, exportJsonButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setStyle("-fx-background-color: #f5f5f5;");

        // Create info label showing the mode and quantum
        Label infoLabel = new Label("Mode: " + (preemptive ? "Preemptive" : "Non-Preemptive") + 
                                   " | Time Quantum: " + timeQuantum);
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
        ganttChart.setTitle("Round Robin Gantt Chart (Time Quantum: " + timeQuantum + ")");
        ganttChart.setLegendVisible(true);
        ganttChart.setCategoryGap(10);
        ganttChart.setAnimated(false);

        // Create series for different states
        XYChart.Series<String, Number> waitingSeries = new XYChart.Series<>();
        waitingSeries.setName("Waiting Time");

        XYChart.Series<String, Number> executionSeries = new XYChart.Series<>();
        executionSeries.setName("Execution Time");

        for (RR.Job job : jobs) {
            // Calculate initial waiting (from arrival to first execution)
            int initialWaiting = job.startTime - job.arrivalTime;
            if (initialWaiting > 0) {
                XYChart.Data<String, Number> waitData = new XYChart.Data<>(job.jobId, initialWaiting);
                waitingSeries.getData().add(waitData);
            } else {
                waitingSeries.getData().add(new XYChart.Data<>(job.jobId, 0));
            }

            // Total execution time equals the burst time
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

    /**
     * Creates a table view displaying job metrics
     */
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
        
        TableColumn<JobWrapper, Number> segmentsCol = new TableColumn<>("Exec Segments");
        segmentsCol.setCellValueFactory(new PropertyValueFactory<>("executionSegments"));

        table.getColumns().addAll(jobIdCol, arrivalCol, burstCol, startCol,
                completionCol, turnaroundCol, waitingCol, responseCol, segmentsCol);

        // Convert the original jobs to wrapper objects and add to table
        List<JobWrapper> wrappedJobs = jobs.stream()
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

        // Add table to tab
        tab.setContent(table);
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
                "Algorithm: Round Robin" + (preemptive ? " (Preemptive)" : " (Non-Preemptive)"),
                "Time Quantum: " + timeQuantum,
                "Total Jobs: " + jobs.size());

        // CPU Metrics
        TitledPane cpuPane = createMetricTitledPane("CPU Utilization",
                "Utilization: " + df.format(calculateCpuUtilization()) + "%",
                "Throughput: " + df.format(calculateThroughput()) + " jobs/unit");

        // Time Metrics
        TitledPane timePane = createMetricTitledPane("Time Metrics",
                "Avg Turnaround: " + df.format(calculateAverageTurnaroundTime()),
                "Avg Waiting: " + df.format(calculateAverageWaitingTime()),
                "Avg Response: " + df.format(calculateAverageResponseTime()));

        // Context Switch metrics
        int totalContextSwitches = jobs.stream()
                .mapToInt(job -> Math.max(0, job.executionHistory.size() - 1))
                .sum();
                
        TitledPane switchPane = createMetricTitledPane("Context Switches",
                "Total Switches: " + totalContextSwitches,
                "Switches per Job: " + df.format((double)totalContextSwitches / jobs.size()),
                preemptive ? "Mode: Preemptive (jobs can be interrupted)" : 
                            "Mode: Non-Preemptive (jobs run to completion)");

        metricsBox.getChildren().addAll(algoPane, cpuPane, timePane, switchPane);

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

        // Find all time slices from all jobs and sort them
        List<RR.TimeSlice> allSlices = jobs.stream()
                .flatMap(job -> job.executionHistory.stream())
                .sorted(Comparator.comparingInt(slice -> slice.startTime))
                .collect(Collectors.toList());

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

        // Draw algorithm info at the top
        Text algorithmInfo = new Text("Round Robin " + 
                                    (preemptive ? "(Preemptive)" : "(Non-Preemptive)") + 
                                    " - Time Quantum: " + timeQuantum);
        algorithmInfo.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        algorithmInfo.setX(horizontalOffset);
        algorithmInfo.setY(25);
        timelinePane.getChildren().add(algorithmInfo);

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
            Color.DEEPPINK, Color.TEAL
        };

        int colorIndex = 0;
        for (RR.Job job : jobs) {
            jobColors.put(job.jobId, colorPalette[colorIndex % colorPalette.length]);
            colorIndex++;
        }

        // Add legend
        Text legendTitle = new Text("Legend:");
        legendTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        legendTitle.setX(horizontalOffset);
        legendTitle.setY(40);
        timelinePane.getChildren().add(legendTitle);

        // Add color legend for each job
        double legendItemX = horizontalOffset + 60;
        for (Map.Entry<String, Color> entry : jobColors.entrySet()) {
            Rectangle colorRect = new Rectangle(legendItemX, 32, 15, 10);
            colorRect.setFill(entry.getValue());
            colorRect.setStroke(Color.BLACK);
            timelinePane.getChildren().add(colorRect);

            Text jobText = new Text(entry.getKey());
            jobText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            jobText.setX(legendItemX + 20);
            jobText.setY(40);
            timelinePane.getChildren().add(jobText);

            legendItemX += 60; // Move to the next legend item
        }

        // Draw job rows
        double currentY = 100;
        for (RR.Job job : jobs) {
            // Job label
            Text jobLabel = new Text(job.jobId);
            jobLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            jobLabel.setX(horizontalOffset - 100);
            jobLabel.setY(currentY + 25);
            timelinePane.getChildren().add(jobLabel);

            // Arrival marker - vertical dotted line
            double arrivalX = horizontalOffset + (job.arrivalTime * timeUnitWidth);
            Line arrivalLine = new Line(arrivalX, currentY - 10, arrivalX, currentY + jobHeight + 10);
            arrivalLine.setStroke(Color.GRAY);
            arrivalLine.setStrokeWidth(1);
            arrivalLine.getStrokeDashArray().addAll(2d, 4d);
            timelinePane.getChildren().add(arrivalLine);

            // Arrival text - positioned directly above the arrival line
            Text arrivalText = new Text("Arrival");
            arrivalText.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            arrivalText.setFill(Color.GRAY);
            // Center the text on the arrival line
            double textWidth = arrivalText.getBoundsInLocal().getWidth();
            arrivalText.setX(arrivalX - (textWidth / 2));  
            arrivalText.setY(currentY - 15);
            timelinePane.getChildren().add(arrivalText);

            // Draw each execution slice
            Color jobColor = jobColors.get(job.jobId);
            for (RR.TimeSlice slice : job.executionHistory) {
                double sliceStartX = horizontalOffset + (slice.startTime * timeUnitWidth);
                double sliceWidth = (slice.endTime - slice.startTime) * timeUnitWidth;

                // Draw execution rectangle
                Rectangle execRect = new Rectangle(sliceStartX, currentY, sliceWidth, jobHeight);
                execRect.setFill(jobColor);
                execRect.setStroke(Color.BLACK);
                execRect.setStrokeWidth(1);
                timelinePane.getChildren().add(execRect);

                // Add tooltip with details
                Tooltip tooltip = new Tooltip(
                    "Job: " + job.jobId +
                    "\nStart: " + slice.startTime +
                    "\nEnd: " + slice.endTime +
                    "\nDuration: " + slice.getDuration() +
                    (slice.getDuration() == timeQuantum ? " (Full Quantum)" : "")
                );
                Tooltip.install(execRect, tooltip);

                // Add text label if there's enough space
                if (sliceWidth > 30) {
                    Text execLabel = new Text(String.valueOf(slice.getDuration()));
                    execLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
                    execLabel.setFill(Color.WHITE);
                    execLabel.setX(sliceStartX + (sliceWidth / 2) - 5);
                    execLabel.setY(currentY + (jobHeight / 2) + 5);
                    timelinePane.getChildren().add(execLabel);
                }

                // If it's a full quantum (preemption), mark it
                if (preemptive && slice.getDuration() == timeQuantum && job.remainingTime > 0) {
                    Line preemptLine = new Line(
                        sliceStartX + sliceWidth, currentY,
                        sliceStartX + sliceWidth, currentY + jobHeight
                    );
                    preemptLine.setStroke(Color.RED);
                    preemptLine.setStrokeWidth(2);
                    timelinePane.getChildren().add(preemptLine);
                }
            }

            // Mark completion - vertical green line
            double completionX = horizontalOffset + (job.completionTime * timeUnitWidth);
            Line completionLine = new Line(completionX, currentY, completionX, currentY + jobHeight);
            completionLine.setStroke(Color.GREEN);
            completionLine.setStrokeWidth(2);
            timelinePane.getChildren().add(completionLine);

            // Completion text - positioned directly below the completion line
            Text completeLabel = new Text("Complete");
            completeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            completeLabel.setFill(Color.GREEN);
            // Center the text on the completion line
//            double textWidth = completeLabel.getBoundsInLocal().getWidth();
            completeLabel.setX(completionX - (textWidth / 2));
            completeLabel.setY(currentY + jobHeight + 15);
            timelinePane.getChildren().add(completeLabel);

            currentY += jobHeight + 30;  // Move to next job with more spacing
        }

        tab.setContent(scrollPane);
        return tab;
    }
    /**
     * Creates a tab displaying context switch information
     */
    private Tab createContextSwitchTab() {
        Tab tab = new Tab("Context Switches");
        tab.setClosable(false);

        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white;");

        // Add title and description
        Text title = new Text("Context Switch Analysis");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        Text description = new Text(
            "A context switch occurs when the CPU changes from one process to another. " +
            "In Round Robin scheduling with preemption, this happens when a process uses its " +
            "entire time quantum or completes execution."
        );
        description.setWrappingWidth(800);
        
        container.getChildren().addAll(title, description);

        // Add mode-specific information
        String modeInfo = preemptive ?
            "In preemptive mode, a process will be interrupted after using its time quantum." :
            "In non-preemptive mode, processes run to completion once selected.";
        
        Text modeText = new Text(modeInfo);
        modeText.setWrappingWidth(800);
        container.getChildren().add(modeText);

        // Create table of context switches
        TableView<ContextSwitchData> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<ContextSwitchData, String> jobIdCol = new TableColumn<>("Job ID");
        jobIdCol.setCellValueFactory(data -> data.getValue().jobIdProperty());
        
        TableColumn<ContextSwitchData, Number> switchesCol = new TableColumn<>("Context Switches");
        switchesCol.setCellValueFactory(data -> data.getValue().contextSwitchesProperty());
        
        TableColumn<ContextSwitchData, Number> segmentsCol = new TableColumn<>("Execution Segments");
        segmentsCol.setCellValueFactory(data -> data.getValue().segmentsProperty());
        
        table.getColumns().addAll(jobIdCol, switchesCol, segmentsCol);
        
        // Populate the table with context switch data
        List<ContextSwitchData> switchData = jobs.stream()
            .map(job -> new ContextSwitchData(
                job.jobId, 
                Math.max(0, job.executionHistory.size() - 1),
                job.executionHistory.size()
            ))
            .collect(Collectors.toList());
        
        table.setItems(FXCollections.observableArrayList(switchData));
        
        container.getChildren().add(table);
        
        // Add context switch chart
        container.getChildren().add(createContextSwitchChart(switchData));
        
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    /**
     * Creates a chart displaying context switches by job
     */
    private BarChart<String, Number> createContextSwitchChart(List<ContextSwitchData> switchData) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jobs");
        yAxis.setLabel("Context Switches");
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Context Switches by Job");
        chart.setAnimated(false);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Number of Context Switches");
        
        for (ContextSwitchData data : switchData) {
            series.getData().add(new XYChart.Data<>(data.getJobId(), data.getContextSwitches()));
        }
        
        chart.getData().add(series);
        chart.setLegendVisible(false);
        
        return chart;
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

    /**
     * Creates a styled button with the given action
     */
    private Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setStyle("-fx-font-size: 14px; -fx-padding: 8 15; -fx-background-radius: 5;");
        button.setOnAction(e -> action.run());
        return button;
    }

    /**
     * Calculate CPU utilization
     */
    private double calculateCpuUtilization() {
        int totalBurstTime = jobs.stream().mapToInt(job -> job.burstTime).sum();
        int lastCompletion = jobs.stream().mapToInt(job -> job.completionTime).max().orElse(0);
        int firstArrival = jobs.stream().mapToInt(job -> job.arrivalTime).min().orElse(0);
        int totalTime = lastCompletion - firstArrival;
        return totalTime > 0 ? (double) totalBurstTime / totalTime * 100 : 0;
    }

    /**
     * Calculate throughput
     */
    private double calculateThroughput() {
        int lastCompletion = jobs.stream().mapToInt(job -> job.completionTime).max().orElse(0);
        int firstArrival = jobs.stream().mapToInt(job -> job.arrivalTime).min().orElse(0);
        int totalTime = lastCompletion - firstArrival;
        return totalTime > 0 ? (double) jobs.size() / totalTime : 0;
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
     * Export job data to CSV
     */
    private void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Round Robin Scheduling Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write header
                writer.write("Job ID,Arrival Time,Burst Time,Start Time,Completion Time," +
                        "Turnaround Time,Waiting Time,Response Time,Execution Segments\n");

                // Write data
                for (RR.Job job : jobs) {
                    writer.write(String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d\n",
                            job.jobId, job.arrivalTime, job.burstTime, job.startTime,
                            job.completionTime, job.turnaroundTime, job.waitingTime, 
                            job.responseTime, job.executionHistory.size()));
                }

                // Add execution history
                writer.write("\nExecution History\n");
                writer.write("Job ID,Start Time,End Time,Duration\n");
                
                for (RR.Job job : jobs) {
                    for (RR.TimeSlice slice : job.executionHistory) {
                        writer.write(String.format("%s,%d,%d,%d\n",
                                job.jobId, slice.startTime, slice.endTime, slice.getDuration()));
                    }
                }

                // Add algorithm info
                writer.write("\nAlgorithm Information\n");
                writer.write(String.format("Algorithm,Round Robin %s\n", 
                                          preemptive ? "(Preemptive)" : "(Non-Preemptive)"));
                writer.write(String.format("Time Quantum,%d\n", timeQuantum));
                
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
        // Suppress PDFBox logging messages
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.OFF);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Round Robin Report as PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                // Create a new PDF document
                PDDocument document = new PDDocument();
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                // Create a content stream for adding content to the page
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                // Set up fonts
                PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDFont headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDFont textFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Add title
                contentStream.beginText();
                contentStream.setFont(titleFont, 18);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Round Robin Scheduler Report");
                contentStream.endText();

                // Add algorithm information
                contentStream.beginText();
                contentStream.setFont(textFont, 12);
                contentStream.newLineAtOffset(50, 730);
                contentStream.showText("Mode: " + (preemptive ? "Preemptive" : "Non-Preemptive") + 
                                      " | Time Quantum: " + timeQuantum);
                contentStream.endText();

                // Add date and time
                contentStream.beginText();
                contentStream.setFont(textFont, 12);
                contentStream.newLineAtOffset(50, 710);
                contentStream.showText("Generated: " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                contentStream.endText();

                // Add summary metrics
                contentStream.beginText();
                contentStream.setFont(headerFont, 14);
                contentStream.newLineAtOffset(50, 670);
                contentStream.showText("Performance Metrics");
                contentStream.endText();

                String[] metrics = {
                        "CPU Utilization: " + df.format(calculateCpuUtilization()) + "%",
                        "Throughput: " + df.format(calculateThroughput()) + " jobs/unit",
                        "Avg Turnaround Time: " + df.format(calculateAverageTurnaroundTime()),
                        "Avg Waiting Time: " + df.format(calculateAverageWaitingTime()),
                        "Avg Response Time: " + df.format(calculateAverageResponseTime())
                };

                int yPosition = 650;
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
                String[] headers = {"Job ID", "Arrival", "Burst", "Start", "Complete", "Turnaround", "Waiting", "Response"};
                int[] colWidths = {50, 50, 50, 50, 60, 70, 50, 60};

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

                for (RR.Job job : jobs) {
                    // If we've reached the limit for this page, create a new page
                    if (jobCount >= jobsPerPage) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 750;

                        // Add table headers again on new page
                        contentStream.beginText();
                        contentStream.setFont(headerFont, 14);
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText("Job Execution Details (continued)");
                        contentStream.endText();

                        yPosition -= 30;
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

                    // Row data
                    int cellX = 50;
                    
                    // Job ID
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(job.jobId);
                    contentStream.endText();
                    cellX += colWidths[0];

                    // Arrival Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.arrivalTime));
                    contentStream.endText();
                    cellX += colWidths[1];

                    // Burst Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.burstTime));
                    contentStream.endText();
                    cellX += colWidths[2];

                    // Start Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.startTime));
                    contentStream.endText();
                    cellX += colWidths[3];

                    // Completion Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.completionTime));
                    contentStream.endText();
                    cellX += colWidths[4];

                    // Turnaround Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.turnaroundTime));
                    contentStream.endText();
                    cellX += colWidths[5];

                    // Waiting Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(job.waitingTime));
                    contentStream.endText();
                    cellX += colWidths[6];

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
                String[] historyHeaders = {"Job ID", "Start Time", "End Time", "Duration", "Notes"};
                int[] historyColWidths = {60, 70, 70, 60, 200};

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

                // Flat list of all slices with job IDs for sorting
                List<Object[]> allExecutionSlices = new ArrayList<>();
                for (RR.Job job : jobs) {
                    for (RR.TimeSlice slice : job.executionHistory) {
                        allExecutionSlices.add(new Object[]{job, slice});
                    }
                }

                // Sort by start time
                allExecutionSlices.sort((a, b) -> {
                    RR.TimeSlice sliceA = (RR.TimeSlice) a[1];
                    RR.TimeSlice sliceB = (RR.TimeSlice) b[1];
                    return Integer.compare(sliceA.startTime, sliceB.startTime);
                });

                // Print all slices
                for (Object[] pair : allExecutionSlices) {
                    RR.Job job = (RR.Job) pair[0];
                    RR.TimeSlice slice = (RR.TimeSlice) pair[1];

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
                    
                    // Start Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(slice.startTime));
                    contentStream.endText();
                    cellX += historyColWidths[1];
                    
                    // End Time
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(slice.endTime));
                    contentStream.endText();
                    cellX += historyColWidths[2];
                    
                    // Duration
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(String.valueOf(slice.getDuration()));
                    contentStream.endText();
                    cellX += historyColWidths[3];
                    
                    // Notes
                    String notes = "";
                    if (preemptive && slice.getDuration() == timeQuantum) {
                        notes = "Used full quantum (preempted)";
                    } else if (slice.endTime == job.completionTime) {
                        notes = "Job completed";
                    }
                    
                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(cellX, yPosition);
                    contentStream.showText(notes);
                    contentStream.endText();
                    
                    yPosition -= 20;
                    sliceCount++;
                }

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
     * Export job data to JSON format
     */
    private void exportToJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Round Robin Scheduling Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                StringBuilder json = new StringBuilder();
                json.append("{\n");
                json.append("  \"algorithm\": \"Round Robin");
                json.append(preemptive ? " (Preemptive)" : " (Non-Preemptive)");
                json.append("\",\n");
                json.append("  \"timeQuantum\": ").append(timeQuantum).append(",\n");
                
                // Add metrics
                json.append("  \"metrics\": {\n");
                json.append("    \"cpuUtilization\": ").append(df.format(calculateCpuUtilization())).append(",\n");
                json.append("    \"throughput\": ").append(df.format(calculateThroughput())).append(",\n");
                json.append("    \"averageTurnaroundTime\": ").append(df.format(calculateAverageTurnaroundTime())).append(",\n");
                json.append("    \"averageWaitingTime\": ").append(df.format(calculateAverageWaitingTime())).append(",\n");
                json.append("    \"averageResponseTime\": ").append(df.format(calculateAverageResponseTime())).append("\n");
                json.append("  },\n");
                
                // Add jobs
                json.append("  \"jobs\": [\n");
                
                for (int i = 0; i < jobs.size(); i++) {
                    RR.Job job = jobs.get(i);
                    json.append("    {\n");
                    json.append("      \"id\": \"").append(job.jobId).append("\",\n");
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
                        RR.TimeSlice slice = job.executionHistory.get(j);
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
     * Export selected rows from table to CSV
     */
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
                        "Turnaround Time,Waiting Time,Response Time,Execution Segments\n");

                for (JobWrapper wrapper : table.getSelectionModel().getSelectedItems()) {
                    RR.Job job = wrapper.getOriginalJob();
                    writer.write(String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d\n",
                            job.jobId, job.arrivalTime, job.burstTime, job.startTime,
                            job.completionTime, job.turnaroundTime, job.waitingTime, 
                            job.responseTime, job.executionHistory.size()));
                }

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Selected rows exported successfully!");
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
    
    /**
     * Data class for context switch information
     */
    public static class ContextSwitchData {
        private final SimpleStringProperty jobId;
        private final SimpleIntegerProperty contextSwitches;
        private final SimpleIntegerProperty segments;
        
        public ContextSwitchData(String jobId, int contextSwitches, int segments) {
            this.jobId = new SimpleStringProperty(jobId);
            this.contextSwitches = new SimpleIntegerProperty(contextSwitches);
            this.segments = new SimpleIntegerProperty(segments);
        }
        
        public String getJobId() {
            return jobId.get();
        }
        
        public SimpleStringProperty jobIdProperty() {
            return jobId;
        }
        
        public int getContextSwitches() {
            return contextSwitches.get();
        }
        
        public SimpleIntegerProperty contextSwitchesProperty() {
            return contextSwitches;
        }
        
        public int getSegments() {
            return segments.get();
        }
        
        public SimpleIntegerProperty segmentsProperty() {
            return segments;
        }
    }
}