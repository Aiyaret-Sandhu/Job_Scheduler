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

import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

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

        // Create a pane to hold our custom Gantt chart
        Pane ganttPane = new Pane();
        ganttPane.setStyle("-fx-background-color: white;");

        // Create a ScrollPane to allow scrolling for many jobs
        ScrollPane scrollPane = new ScrollPane(ganttPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);

        // Calculate metrics for scaling
        int maxTime = completedJobs.stream()
                .mapToInt(job -> job.completionTime)
                .max()
                .orElse(0);

        double timeUnitWidth = 30;  // Width per time unit in pixels
        double jobHeight = 40;      // Height per job in pixels
        double horizontalOffset = 150; // Space for job labels

        // Set the canvas size
        ganttPane.setPrefWidth(horizontalOffset + (maxTime * timeUnitWidth) + 50);
        ganttPane.setPrefHeight((completedJobs.size() * (jobHeight + 10)) + 150);

        // Draw timeline axis
        Line timeAxis = new Line(horizontalOffset, 70, horizontalOffset + (maxTime * timeUnitWidth), 70);
        timeAxis.setStroke(Color.BLACK);
        timeAxis.setStrokeWidth(2);
        ganttPane.getChildren().add(timeAxis);

        // Draw time markers
        int timeStep = Math.max(1, maxTime / 20); // Adapt step size to the chart width
        for (int t = 0; t <= maxTime; t += timeStep) {
            double xPos = horizontalOffset + (t * timeUnitWidth);

            // Tick mark
            Line tick = new Line(xPos, 65, xPos, 75);
            tick.setStroke(Color.BLACK);
            ganttPane.getChildren().add(tick);

            // Time label
            Text timeLabel = new Text(String.valueOf(t));
            timeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            timeLabel.setX(xPos - 3);
            timeLabel.setY(60);
            ganttPane.getChildren().add(timeLabel);
        }

        // Title
        Text title = new Text("Job Execution Timeline");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setX(horizontalOffset);
        title.setY(25);
        ganttPane.getChildren().add(title);

        // Add legend
        double legendX = horizontalOffset + 400;
        double legendY = 25;

        Text legendTitle = new Text("Legend:");
        legendTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        legendTitle.setX(legendX);
        legendTitle.setY(legendY);
        ganttPane.getChildren().add(legendTitle);

        // Waiting time legend
        Rectangle waitingRect = new Rectangle(legendX + 60, legendY - 10, 20, 10);
        waitingRect.setFill(Color.LIGHTGRAY);
        waitingRect.setStroke(Color.BLACK);
        ganttPane.getChildren().add(waitingRect);

        Text waitingText = new Text("Waiting");
        waitingText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        waitingText.setX(legendX + 85);
        waitingText.setY(legendY);
        ganttPane.getChildren().add(waitingText);

        // Execution time legend
        Rectangle execRect = new Rectangle(legendX + 150, legendY - 10, 20, 10);
        execRect.setFill(Color.DODGERBLUE);
        execRect.setStroke(Color.BLACK);
        ganttPane.getChildren().add(execRect);

        Text execText = new Text("Execution");
        execText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        execText.setX(legendX + 175);
        execText.setY(legendY);
        ganttPane.getChildren().add(execText);

        // Draw job bars
        double currentY = 100;

        for (FCFS.Job job : completedJobs) {
            // Job label
            Text jobLabel = new Text(job.jobId);
            jobLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            jobLabel.setX(horizontalOffset - 100);
            jobLabel.setY(currentY + 15);
            ganttPane.getChildren().add(jobLabel);

            double barStartX = horizontalOffset + (job.arrivalTime * timeUnitWidth);
            double waitEndX = horizontalOffset + (job.startTime * timeUnitWidth);
            double execEndX = horizontalOffset + (job.completionTime * timeUnitWidth);

            // Waiting time block (if any)
            if (job.startTime > job.arrivalTime) {
                Rectangle waitingBar = new Rectangle(
                        barStartX,
                        currentY,
                        (job.startTime - job.arrivalTime) * timeUnitWidth,
                        jobHeight
                );
                waitingBar.setFill(Color.LIGHTGRAY);
                waitingBar.setStroke(Color.BLACK);
                waitingBar.setStrokeWidth(1);

                // Add tooltip with details
                Tooltip waitTooltip = new Tooltip(
                        "Job: " + job.jobId +
                                "\nArrival Time: " + job.arrivalTime +
                                "\nWaiting Time: " + (job.startTime - job.arrivalTime)
                );
                Tooltip.install(waitingBar, waitTooltip);

                ganttPane.getChildren().add(waitingBar);

                // Add label if there's enough space
                if ((job.startTime - job.arrivalTime) * timeUnitWidth > 40) {
                    Text waitLabel = new Text("Wait: " + (job.startTime - job.arrivalTime));
                    waitLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
                    waitLabel.setX(barStartX + 5);
                    waitLabel.setY(currentY + (jobHeight / 2) + 5);
                    ganttPane.getChildren().add(waitLabel);
                }
            }

            // Execution time block
            Rectangle execBar = new Rectangle(
                    waitEndX,
                    currentY,
                    job.burstTime * timeUnitWidth,
                    jobHeight
            );
            execBar.setFill(Color.DODGERBLUE);
            execBar.setStroke(Color.BLACK);
            execBar.setStrokeWidth(1);

            // Add tooltip with details
            Tooltip execTooltip = new Tooltip(
                    "Job: " + job.jobId +
                            "\nStart Time: " + job.startTime +
                            "\nBurst Time: " + job.burstTime +
                            "\nCompletion Time: " + job.completionTime
            );
            Tooltip.install(execBar, execTooltip);

            ganttPane.getChildren().add(execBar);

            // Add execution time label if there's enough space
            if (job.burstTime * timeUnitWidth > 40) {
                Text execLabel = new Text("Exec: " + job.burstTime);
                execLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
                execLabel.setFill(Color.WHITE);
                execLabel.setX(waitEndX + 5);
                execLabel.setY(currentY + (jobHeight / 2) + 5);
                ganttPane.getChildren().add(execLabel);
            }

            // Add completion time marker
            Text completeLabel = new Text("T=" + job.completionTime);
            completeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            completeLabel.setX(execEndX + 5);
            completeLabel.setY(currentY + (jobHeight / 2) + 5);
            ganttPane.getChildren().add(completeLabel);

            currentY += jobHeight + 10;  // Move to next job
        }

        tab.setContent(scrollPane);
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
        // Suppress PDFBox logging messages
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.OFF);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report as PDF");
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
                contentStream.showText("FCFS Scheduler Report");
                contentStream.endText();

                // Add date and time
                contentStream.beginText();
                contentStream.setFont(textFont, 12);
                contentStream.newLineAtOffset(50, 730);
                contentStream.showText("Generated: " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                contentStream.endText();

                // Add summary metrics
                contentStream.beginText();
                contentStream.setFont(headerFont, 14);
                contentStream.newLineAtOffset(50, 690);
                contentStream.showText("Performance Metrics");
                contentStream.endText();

                String[] metrics = {
                        "CPU Utilization: " + df.format(calculateCpuUtilization()) + "%",
                        "Throughput: " + df.format(calculateThroughput()) + " jobs/unit",
                        "Avg Turnaround Time: " + df.format(calculateAverageTurnaroundTime()),
                        "Avg Waiting Time: " + df.format(calculateAverageWaitingTime()),
                        "Avg Response Time: " + df.format(calculateAverageResponseTime())
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

                for (FCFS.Job job : completedJobs) {
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

                // Add a simple representation of the Gantt chart
                if (jobCount < jobsPerPage - 5) {
                    yPosition -= 40;

                    contentStream.beginText();
                    contentStream.setFont(headerFont, 14);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText("Gantt Chart");
                    contentStream.endText();

                    // Add generation timestamp
                    String timestamp = "Generated on: " + java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    contentStream.beginText();
                    contentStream.setFont(textFont, 10);
                    contentStream.newLineAtOffset(250, yPosition);
                    contentStream.showText(timestamp);
                    contentStream.endText();

                    yPosition -= 40;

                    // Draw time scale
                    int maxTime = completedJobs.stream()
                            .mapToInt(j -> j.completionTime)
                            .max()
                            .orElse(0);

                    float timeUnitWidth = 400f / maxTime;  // Scale to fit on page
                    int scaleStep = Math.max(1, maxTime / 20);  // Determine appropriate scale step

                    // Draw time scale markers
                    contentStream.setLineWidth(0.5f);
                    for (int t = 0; t <= maxTime; t += scaleStep) {
                        float xPos = 100 + (t * timeUnitWidth);

                        // Draw tick mark
                        contentStream.moveTo(xPos, yPosition + 5);
                        contentStream.lineTo(xPos, yPosition);
                        contentStream.stroke();

                        // Draw time label
                        contentStream.beginText();
                        contentStream.setFont(textFont, 8);
                        contentStream.newLineAtOffset(xPos - 3, yPosition - 15);
                        contentStream.showText(String.valueOf(t));
                        contentStream.endText();
                    }

                    yPosition -= 25;
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

                    // Draw job rows with clear labels and borders
                    for (FCFS.Job job : completedJobs) {
                        contentStream.beginText();
                        contentStream.setFont(textFont, 10);
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText(job.jobId);
                        contentStream.endText();

                        // Add text labels for start/end times inside the bars
                        float barStartX = 100 + (job.arrivalTime * timeUnitWidth);
                        float waitEndX = 100 + (job.startTime * timeUnitWidth);
                        float execEndX = 100 + (job.completionTime * timeUnitWidth);

                        // Draw waiting time (if any) with border
                        if (job.startTime > job.arrivalTime) {
                            contentStream.setNonStrokingColor(0.8f, 0.8f, 0.8f); // Light gray
                            contentStream.addRect(barStartX, yPosition - 5,
                                    (job.startTime - job.arrivalTime) * timeUnitWidth,
                                    timelineHeight);
                            contentStream.fill();

                            // Add border
                            contentStream.setStrokingColor(0, 0, 0);
                            contentStream.setLineWidth(0.5f);
                            contentStream.addRect(barStartX, yPosition - 5,
                                    (job.startTime - job.arrivalTime) * timeUnitWidth,
                                    timelineHeight);
                            contentStream.stroke();

                            // Add label if there's enough space
                            if ((job.startTime - job.arrivalTime) * timeUnitWidth > 25) {
                                contentStream.beginText();
                                contentStream.setNonStrokingColor(0, 0, 0);
                                contentStream.setFont(textFont, 8);
                                contentStream.newLineAtOffset(barStartX + 2, yPosition + 5);
                                contentStream.showText("Wait: " + (job.startTime - job.arrivalTime));
                                contentStream.endText();
                            }
                        }

                        // Draw execution time with border
                        contentStream.setNonStrokingColor(0.2f, 0.6f, 0.9f);  // Blue color
                        contentStream.addRect(waitEndX, yPosition - 5,
                                job.burstTime * timeUnitWidth,
                                timelineHeight);
                        contentStream.fill();

                        // Add border
                        contentStream.setStrokingColor(0, 0, 0);
                        contentStream.setLineWidth(0.5f);
                        contentStream.addRect(waitEndX, yPosition - 5,
                                job.burstTime * timeUnitWidth,
                                timelineHeight);
                        contentStream.stroke();

                        // Add time labels
                        if (job.burstTime * timeUnitWidth > 25) {
                            contentStream.beginText();
                            contentStream.setNonStrokingColor(1, 1, 1);  // White text
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
                }

                // Close the content stream
                contentStream.close();

                // Save the document
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