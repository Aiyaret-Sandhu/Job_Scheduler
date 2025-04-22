package org.example.schedulers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * First-Come, First-Served (FCFS) CPU Scheduling Algorithm Implementation
 *
 * Data structures used:
 * - ArrayDeque: Efficient queue implementation for job processing
 * - ArrayList: For storing completed jobs and results
 * - Stream API: For efficient statistical calculations
 */
public class FCFS {

    /**
     * Job class to encapsulate all process-related data
     * Provides a clean representation of each process and its metrics
     */
    public static class Job {
        public String jobId;
        public int arrivalTime;
        public int burstTime;
        public int startTime;
        public int completionTime;
        public int turnaroundTime;
        public int waitingTime;
        public int responseTime;

        public Job(String jobId, int arrivalTime, int burstTime) {
            this.jobId = jobId;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
        }
    }

    // ArrayDeque provides more efficient queue operations than LinkedList
    private final Queue<Job> jobQueue = new ArrayDeque<>();

    // ArrayList for storing completed jobs to enable direct access and iteration
    private final List<Job> completedJobs = new ArrayList<>();

    private boolean dummyMode = false;

    /**
     * Adds a new job to the scheduling queue
     *
     * Input validation ensures job parameters are valid
     * Time Complexity: O(1) for adding to queue
     */
    public void addJob(String jobId, int arrivalTime, int burstTime) {
        if (arrivalTime < 0 || burstTime <= 0) {
            throw new IllegalArgumentException("Invalid job parameters");
        }
        jobQueue.add(new Job(jobId, arrivalTime, burstTime));
    }

    /**
     * Sets the dummy mode for testing and demonstration purposes
     */
    public void setDummyMode(boolean enabled) {
        this.dummyMode = enabled;
    }

    /**
     * Execute the FCFS scheduling algorithm
     *
     * Algorithm:
     * 1. Process jobs in arrival order (FIFO)
     * 2. Non-preemptive execution (jobs run to completion)
     * 3. Calculate performance metrics for each job
     *
     * Time Complexity: O(n) where n is the number of jobs
     * Space Complexity: O(n) for storing job information
     */
    public void execute() {
        if (jobQueue.isEmpty()) {
            throw new IllegalStateException("No jobs to schedule");
        }

        if (dummyMode) {
            dummyExecute();
            return;
        }

        int currentTime = 0;

        // Process each job in FIFO order - O(n) time complexity
        while (!jobQueue.isEmpty()) {
            Job job = jobQueue.poll();

            // CPU idle time handling - advance time if needed
            if (currentTime < job.arrivalTime) {
                currentTime = job.arrivalTime;
            }

            // Calculate job metrics
            job.startTime = currentTime;
            job.completionTime = currentTime + job.burstTime;
            job.turnaroundTime = job.completionTime - job.arrivalTime;
            job.waitingTime = job.turnaroundTime - job.burstTime;
            job.responseTime = job.startTime - job.arrivalTime;

            // Update current time and store the completed job
            currentTime = job.completionTime;
            completedJobs.add(job);
        }
    }

    /**
     * Demo mode for educational purposes
     * Generates fixed values to demonstrate the algorithm behavior
     */
    private void dummyExecute() {
        System.out.println("Running in dummy mode - using preset values for demonstration");

        int currentTime = 0;
        for (int i = 0; i < completedJobs.size(); i++) {
            Job job = completedJobs.get(i);
            job.startTime = currentTime;
            job.completionTime = currentTime + job.burstTime;
            job.turnaroundTime = job.completionTime - job.arrivalTime;
            job.waitingTime = job.turnaroundTime - job.burstTime;
            job.responseTime = job.startTime - job.arrivalTime;
            currentTime = job.completionTime;

            // Add some variation for demonstration purposes
            if (i % 2 == 0) {
                job.waitingTime += 1;
                job.responseTime += 1;
            }
        }
    }

    /**
     * Calculate CPU utilization using Stream API
     *
     * CPU Utilization = (Total Burst Time / Total Schedule Time) * 100%
     * Uses efficient stream operations for calculations
     */
    public double calculateCpuUtilization() {
        if (completedJobs.isEmpty()) return 0;

        int totalBurstTime = completedJobs.stream().mapToInt(job -> job.burstTime).sum();
        int totalTime = completedJobs.get(completedJobs.size() - 1).completionTime -
                completedJobs.get(0).arrivalTime;

        return totalTime > 0 ? (double) totalBurstTime / totalTime * 100 : 0;
    }

    /**
     * Calculate throughput using makespan
     *
     * Throughput = Number of Jobs / Total Schedule Time
     */
    public double calculateThroughput() {
        if (completedJobs.isEmpty()) return 0;

        int totalTime = completedJobs.get(completedJobs.size() - 1).completionTime -
                completedJobs.get(0).arrivalTime;

        return totalTime > 0 ? (double) completedJobs.size() / totalTime : 0;
    }

    /**
     * Calculate average waiting time using Stream API aggregation
     * Efficient statistical operation using functional programming
     */
    public double calculateAverageWaitingTime() {
        return completedJobs.stream().mapToInt(job -> job.waitingTime).average().orElse(0);
    }

    /**
     * Calculate average turnaround time using Stream API aggregation
     * Efficient statistical operation using functional programming
     */
    public double calculateAverageTurnaroundTime() {
        return completedJobs.stream().mapToInt(job -> job.turnaroundTime).average().orElse(0);
    }

    /**
     * Print a Gantt chart visualization of the schedule
     * Visual representation aids in understanding the algorithm behavior
     */
    public void printGanttChart() {
        System.out.println("\nGANTT CHART }-:");

        // Top border
        System.out.print("+");
        for (Job job : completedJobs) {
            System.out.print("--------+");
        }
        System.out.println();

        // Job IDs
        System.out.print("|");
        for (Job job : completedJobs) {
            System.out.printf("  %-5s |", job.jobId);
        }
        System.out.println();

        // Bottom border
        System.out.print("+");
        for (Job job : completedJobs) {
            System.out.print("--------+");
        }
        System.out.println();

        // Timeline - uses string manipulation to properly align times
        System.out.print("0");

        int position = 1;
        for (Job job : completedJobs) {
            int targetPosition = position + 8;
            int timeValue = job.completionTime;
            String timeStr = String.valueOf(timeValue);

            int spacesNeeded = targetPosition - position - timeStr.length() + 1;

            System.out.print(" ".repeat(spacesNeeded) + timeValue);

            position = targetPosition + timeStr.length();
        }
        System.out.println("\n");
    }

    /**
     * Print detailed job metrics in tabular format
     * Using formatted string output for aligned table presentation
     */
    public void printTableData() {
        System.out.println("\nPROCESS SCHEDULE TABLE }-:");

        // Header formatting using printf for alignment
        String headerFormat = "+-----------+---------------+---------------+---------------+---------------+---------------+---------------+---------------+%n";
        System.out.printf(headerFormat);
        System.out.printf("| %-9s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s |%n",
                "Job ID", "Arrival Time", "Burst Time", "Start Time", "Completion", "Turnaround", "Waiting", "Response");
        System.out.printf("| %-9s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s |%n",
                "", "", "", "", "Time", "Time", "Time", "Time");
        System.out.printf(headerFormat);

        // Data rows - formatted for consistent column alignment
        String rowFormat = "| %-9s | %-13d | %-13d | %-13d | %-13d | %-13d | %-13d | %-13d |%n";
        for (Job job : completedJobs) {
            System.out.printf(rowFormat,
                    job.jobId, job.arrivalTime, job.burstTime, job.startTime, job.completionTime,
                    job.turnaroundTime, job.waitingTime, job.responseTime);
        }
        System.out.printf(headerFormat);
    }

    /**
     * Print comprehensive performance metrics with visual indicators
     * Uses stream operations for statistical calculations
     */
    public void printMetrics() {
        System.out.println("\nPERFORMANCE METRICS }-:");

        // Calculate metrics using stream operations for efficient data aggregation
        double cpuUtilization = calculateCpuUtilization();
        double throughput = calculateThroughput();
        double avgTurnaround = calculateAverageTurnaroundTime();
        double avgWaiting = calculateAverageWaitingTime();
        double avgResponse = completedJobs.stream().mapToInt(job -> job.responseTime).average().orElse(0);

        // Min/max metrics calculations using stream operations
        int minWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).min().orElse(0);
        int maxWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).max().orElse(0);
        int minTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).min().orElse(0);
        int maxTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).max().orElse(0);

        // Calculate scheduling length (makespan)
        int schedulingLength = completedJobs.isEmpty() ? 0 :
                completedJobs.get(completedJobs.size() - 1).completionTime - completedJobs.get(0).arrivalTime;

        // Format and print metrics table
        String border = "+------------------------------+---------------------+%n";
        String format = "| %-28s | %-19.2f |%n";
        String intFormat = "| %-28s | %-19d |%n";

        System.out.printf(border);
        System.out.printf(format, "CPU Utilization (%)", cpuUtilization);
        System.out.printf(format, "Throughput (jobs/unit time)", throughput);
        System.out.printf(border);
        System.out.printf(format, "Average Turnaround Time", avgTurnaround);
        System.out.printf(intFormat, "Minimum Turnaround Time", minTurnaround);
        System.out.printf(intFormat, "Maximum Turnaround Time", maxTurnaround);
        System.out.printf(border);
        System.out.printf(format, "Average Waiting Time", avgWaiting);
        System.out.printf(intFormat, "Minimum Waiting Time", minWaiting);
        System.out.printf(intFormat, "Maximum Waiting Time", maxWaiting);
        System.out.printf(border);
        System.out.printf(format, "Average Response Time", avgResponse);
        System.out.printf(intFormat, "Scheduling Length", schedulingLength);
        System.out.printf(intFormat, "Total Jobs Completed", completedJobs.size());
        System.out.printf(border);

        // Performance indicators with qualitative assessment
        System.out.println("\nPERFORMANCE INDICATORS:");
        System.out.println("CPU Utilization: " + getPerformanceIndicator(cpuUtilization, 70, 90));
        System.out.println("Waiting Time Balance: " +
                (maxWaiting - minWaiting <= 2 ? "Good" : "Could be improved"));
        System.out.println("Fairness: " +
                (maxTurnaround - minTurnaround <= avgTurnaround * 0.3 ? "Good" : "Varies significantly"));
    }

    /**
     * Generate qualitative performance indicators using threshold comparison
     * Provides a human-readable interpretation of numerical metrics
     */
    private String getPerformanceIndicator(double value, double goodThreshold, double excellentThreshold) {
        if (value >= excellentThreshold) return "★★★★★ Excellent";
        if (value >= goodThreshold) return "★★★★ Good";
        if (value >= goodThreshold * 0.7) return "★★★ Fair";
        if (value >= goodThreshold * 0.5) return "★★ Poor";
        return "★ Very Poor";
    }

    /**
     * Convenience method to print all results at once
     */
    public void printResults() {
        printGanttChart();
        printTableData();
        printMetrics();
    }

    /**
     * Prints educational information about the FCFS algorithm
     * Used for teaching and documentation purposes
     */
    public void printAlgorithmInfo() {
        System.out.println("\nFIRST-COME, FIRST-SERVED (FCFS) SCHEDULING ALGORITHM\n");

        System.out.println("DESCRIPTION:");
        System.out.println("FCFS is the simplest CPU scheduling algorithm that schedules processes");
        System.out.println("in the order they arrive in the ready queue. It's non-preemptive, meaning");
        System.out.println("once a process starts executing, it runs to completion.");

        System.out.println("\nCHARACTERISTICS:");
        System.out.println("- Non-preemptive scheduling");
        System.out.println("- Simple to understand and implement");
        System.out.println("- No starvation (every process gets chance to execute)");
        System.out.println("- Poor performance for time-sharing systems");

        System.out.println("\nMETRICS CALCULATED:");
        System.out.println("1. Turnaround Time: Time from arrival to completion");
        System.out.println("2. Waiting Time: Time spent waiting in ready queue");
        System.out.println("3. Response Time: Time from arrival to first response");
        System.out.println("4. CPU Utilization: Percentage of time CPU is busy");
        System.out.println("5. Throughput: Number of processes completed per unit time");

        System.out.println("\nADVANTAGES:");
        System.out.println("- Simple to understand and implement");
        System.out.println("- No starvation - first come first served");
        System.out.println("- Minimal overhead in scheduling");

        System.out.println("\nDISADVANTAGES:");
        System.out.println("- Poor performance (average waiting time is often quite long)");
        System.out.println("- Not suitable for time-sharing systems");
        System.out.println("- Convoy effect: Short processes wait for long ones to finish");

        System.out.println("\nUSE CASES:");
        System.out.println("- Batch systems where simplicity is prioritized");
        System.out.println("- Situations where fairness is more important than performance");
        System.out.println("- Educational purposes to teach basic scheduling concepts");
    }

    /**
     * Export job results in CSV format for data analysis
     * Uses StringBuilder for efficient string concatenation
     *
     * @return CSV formatted string with job metrics
     */
    public String getResultsAsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Job ID,Arrival Time,Burst Time,Start Time,Completion Time,Turnaround Time,Waiting Time,Response Time\n");

        for (Job job : completedJobs) {
            sb.append(String.format("%s,%d,%d,%d,%d,%d,%d,%d%n",
                    job.jobId, job.arrivalTime, job.burstTime, job.startTime, job.completionTime,
                    job.turnaroundTime, job.waitingTime, job.responseTime));
        }

        return sb.toString();
    }

    /**
     * Export metrics summary in CSV format for data analysis
     *
     * @return CSV formatted string with key performance metrics
     */
    public String getMetricsAsCsv() {
        return String.format("Metric,Value%n" +
                        "CPU Utilization (%%),%.2f%n" +
                        "Throughput (jobs/unit),%.2f%n" +
                        "Average Turnaround Time,%.2f%n" +
                        "Average Waiting Time,%.2f%n",
                calculateCpuUtilization(),
                calculateThroughput(),
                calculateAverageTurnaroundTime(),
                calculateAverageWaitingTime());
    }

    /**
     * Get list of completed jobs for visualization or further analysis
     *
     * @return List of completed jobs with all calculated metrics
     */
    public List<Job> getCompletedJobs() {
        return completedJobs;
    }
}