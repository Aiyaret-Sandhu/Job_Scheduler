package org.example.schedulers;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Round Robin (RR) CPU Scheduling Algorithm implementation
 *
 * Data structures used:
 * - PriorityQueue: For managing future jobs based on arrival time
 * - ArrayDeque: For efficient ready queue operations
 * - HashMap: For O(1) job lookup by ID and tracking statistics
 * - ArrayList: For maintaining job execution history and result collection
 */
public class RR {

    public static class Job implements Comparable<Job> {
        public String jobId;
        public int arrivalTime;
        public int burstTime;
        public int remainingTime;
        public int startTime = -1;      // First time the job starts executing
        public int completionTime;
        public int turnaroundTime;
        public int waitingTime;
        public int responseTime;
        public List<TimeSlice> executionHistory = new ArrayList<>();

        public Job(String jobId, int arrivalTime, int burstTime) {
            this.jobId = jobId;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
        }

        // For priority queue ordering based on arrival time
        @Override
        public int compareTo(Job other) {
            return Integer.compare(this.arrivalTime, other.arrivalTime);
        }

        @Override
        public String toString() {
            return jobId + " (Arrival: " + arrivalTime + ", Burst: " + burstTime +
                    ", Remaining: " + remainingTime + ")";
        }
    }

    // Class to represent each execution time slice of a job
    public static class TimeSlice {
        public int startTime;
        public int endTime;
        public String jobId;  // Added to make lookups more efficient

        public TimeSlice(int startTime, int endTime, String jobId) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.jobId = jobId;
        }

        public int getDuration() {
            return endTime - startTime;
        }
    }

    // Using ArrayList for input storage and completed jobs
    private final List<Job> jobs = new ArrayList<>();
    private final List<Job> completedJobs = new ArrayList<>();

    // Using HashMap for O(1) job lookups by ID
    private final Map<String, Job> jobMap = new HashMap<>();

    private int timeQuantum = 2;  // Default time quantum
    private boolean preemptive = true;  // Default mode is preemptive

    public void addJob(String jobId, int arrivalTime, int burstTime) {
        if (arrivalTime < 0 || burstTime <= 0) {
            throw new IllegalArgumentException("Invalid job parameters");
        }
        Job job = new Job(jobId, arrivalTime, burstTime);
        jobs.add(job);
        jobMap.put(jobId, job);  // Store in map for O(1) lookup
    }

    public void setTimeQuantum(int timeQuantum) {
        if (timeQuantum <= 0) {
            throw new IllegalArgumentException("Time quantum must be positive");
        }
        this.timeQuantum = timeQuantum;
    }

    public void setPreemptive(boolean preemptive) {
        this.preemptive = preemptive;
    }

    public String getModeDescription() {
        return preemptive ?
                "Preemptive Round Robin (Time Quantum: " + timeQuantum + ")" :
                "Non-Preemptive Round Robin (Time Quantum: " + timeQuantum + ")";
    }

    /**
     * Execute the Round Robin scheduling algorithm
     *
     * Algorithm:
     * 1. Sort jobs by arrival time using a PriorityQueue (min-heap)
     * 2. Use an ArrayDeque for the ready queue (more efficient than LinkedList)
     * 3. Process jobs in time slices determined by the time quantum
     * 4. Track execution using TimeSlice objects in execution history
     *
     * Time Complexity: O(n * (n/q + log n)) where:
     *   - n is the number of jobs
     *   - q is the time quantum
     *   - log n comes from priority queue operations
     * Space Complexity: O(n) for storing jobs and execution history
     */
    public void execute() {
        if (jobs.isEmpty()) {
            throw new IllegalStateException("No jobs to schedule");
        }

        // Clear previous execution data
        completedJobs.clear();

        // Use a priority queue for efficient job sorting by arrival time
        PriorityQueue<Job> futureJobs = new PriorityQueue<>(jobs);

        // Use ArrayDeque instead of LinkedList for better performance as a queue
        ArrayDeque<Job> readyQueue = new ArrayDeque<>();

        int currentTime = 0;
        Job currentJob = null;

        // Store processes in their original state for reset
        Map<String, Integer> originalBurstTimes = jobs.stream()
                .collect(Collectors.toMap(job -> job.jobId, job -> job.burstTime));

        // Main scheduling loop
        while (!futureJobs.isEmpty() || !readyQueue.isEmpty() || currentJob != null) {
            // Add any newly arrived jobs to the ready queue
            while (!futureJobs.isEmpty() && futureJobs.peek().arrivalTime <= currentTime) {
                readyQueue.add(futureJobs.poll());
            }

            // If no current job is running, get the next job from the ready queue
            if (currentJob == null) {
                if (readyQueue.isEmpty()) {
                    // Time-jumping optimization: advance time directly to next job arrival
                    currentTime = futureJobs.isEmpty() ? currentTime : futureJobs.peek().arrivalTime;
                    continue;
                }

                currentJob = readyQueue.poll();

                // Record the job's start time if this is its first execution
                if (currentJob.startTime == -1) {
                    currentJob.startTime = currentTime;
                    currentJob.responseTime = currentJob.startTime - currentJob.arrivalTime;
                }
            }

            // Determine how long this job will run
            int executionTime;

            if (preemptive) {
                executionTime = Math.min(timeQuantum, currentJob.remainingTime);
            } else {
                // In non-preemptive mode, run until completion
                executionTime = currentJob.remainingTime;
            }

            // Record the time slice for this execution
            TimeSlice timeSlice = new TimeSlice(currentTime, currentTime + executionTime, currentJob.jobId);
            currentJob.executionHistory.add(timeSlice);

            // Update time and job's remaining time
            currentTime += executionTime;
            currentJob.remainingTime -= executionTime;

            // Check if more jobs arrived during execution - efficient batch processing
            while (!futureJobs.isEmpty() && futureJobs.peek().arrivalTime <= currentTime) {
                readyQueue.add(futureJobs.poll());
            }

            // Check if the job is completed
            if (currentJob.remainingTime == 0) {
                currentJob.completionTime = currentTime;
                currentJob.turnaroundTime = currentJob.completionTime - currentJob.arrivalTime;
                currentJob.waitingTime = currentJob.turnaroundTime - currentJob.burstTime;

                completedJobs.add(currentJob);
                currentJob = null;
            } else if (preemptive) {
                // If preemptive and job not done, put it back in the ready queue
                readyQueue.add(currentJob);
                currentJob = null;
            }
        }
    }

    /**
     * Reset all jobs to their original state
     * This allows re-running the algorithm with different parameters
     *
     * Time Complexity: O(n)
     */
    public void reset() {
        for (Job job : jobs) {
            job.remainingTime = job.burstTime;
            job.startTime = -1;
            job.completionTime = 0;
            job.turnaroundTime = 0;
            job.waitingTime = 0;
            job.responseTime = 0;
            job.executionHistory.clear();
        }
        completedJobs.clear();
    }

    public List<Job> getJobs() {
        return new ArrayList<>(jobs);
    }

    public List<Job> getCompletedJobs() {
        return new ArrayList<>(completedJobs);
    }

    public Job getJob(String jobId) {
        // O(1) lookup using HashMap
        return jobMap.get(jobId);
    }

    public boolean isPreemptive() {
        return preemptive;
    }

    /**
     * Calculate CPU utilization using Stream API
     *
     * Algorithm uses:
     * - Stream aggregation for efficient min/max/sum operations
     * - Functional programming for cleaner code
     */
    public double calculateCpuUtilization() {
        if (completedJobs.isEmpty()) return 0;

        int totalBurstTime = completedJobs.stream().mapToInt(job -> job.burstTime).sum();
        int makespan = calculateMakespan();

        return makespan > 0 ? (double) totalBurstTime / makespan * 100 : 0;
    }

    /**
     * Calculate makespan (total schedule length)
     *
     * Time Complexity: O(n) using stream operations
     */
    public int calculateMakespan() {
        if (completedJobs.isEmpty()) return 0;

        int lastCompletion = completedJobs.stream()
                .mapToInt(job -> job.completionTime)
                .max()
                .orElse(0);

        int firstArrival = completedJobs.stream()
                .mapToInt(job -> job.arrivalTime)
                .min()
                .orElse(0);

        return lastCompletion - firstArrival;
    }

    public double calculateThroughput() {
        if (completedJobs.isEmpty()) return 0;

        int makespan = calculateMakespan();
        return makespan > 0 ? (double) completedJobs.size() / makespan : 0;
    }

    // Efficient statistical calculations using Stream API
    public double calculateAverageWaitingTime() {
        return completedJobs.stream().mapToInt(job -> job.waitingTime).average().orElse(0);
    }

    public double calculateAverageTurnaroundTime() {
        return completedJobs.stream().mapToInt(job -> job.turnaroundTime).average().orElse(0);
    }

    public double calculateAverageResponseTime() {
        return completedJobs.stream().mapToInt(job -> job.responseTime).average().orElse(0);
    }

    /**
     * Print a Gantt chart of the job execution
     * Uses efficient string building with StringBuilder
     */
    public void printGanttChart() {
        if (completedJobs.isEmpty()) {
            System.out.println("No jobs completed yet.");
            return;
        }

        System.out.println("\nGANTT CHART }-:");

        // Collect all time slices and sort them using a merge sort based algorithm
        List<TimeSlice> allSlices = completedJobs.stream()
                .flatMap(job -> job.executionHistory.stream())
                .sorted(Comparator.comparingInt(slice -> slice.startTime))
                .collect(Collectors.toList());

        // Use HashMap for O(1) color lookup by job ID
        Map<String, String> jobColors = new HashMap<>();
        String[] colors = {"\u001B[42m", "\u001B[43m", "\u001B[44m", "\u001B[45m", "\u001B[46m", "\u001B[41m"};
        String resetColor = "\u001B[0m";

        // Assign colors to jobs - O(n) operation
        int colorIndex = 0;
        for (Job job : completedJobs) {
            jobColors.put(job.jobId, colors[colorIndex % colors.length]);
            colorIndex++;
        }

        // Use StringBuilder for efficient string concatenation
        StringBuilder chart = new StringBuilder();

        // Build top border
        chart.append("+");
        for (int i = 0; i < allSlices.size(); i++) {
            chart.append("--------+");
        }
        chart.append("\n|");

        // Build job labels with colors
        for (TimeSlice slice : allSlices) {
            String color = jobColors.getOrDefault(slice.jobId, colors[0]);
            chart.append(String.format(" %s%5s%s |", color, slice.jobId, resetColor));
        }
        chart.append("\n+");

        // Build bottom border
        for (int i = 0; i < allSlices.size(); i++) {
            chart.append("--------+");
        }
        chart.append("\n");

        // Build timeline
        chart.append("0");
        for (TimeSlice slice : allSlices) {
            int spaces = 8 - String.valueOf(slice.endTime).length();
            chart.append(" ".repeat(spaces)).append(slice.endTime);
        }

        System.out.println(chart.toString());
    }

    /**
     * Print job scheduling data in tabular format
     * Uses StringBuilder for efficient string concatenation
     */
    public void printTableData() {
        if (completedJobs.isEmpty()) {
            System.out.println("No jobs completed yet.");
            return;
        }

        System.out.println("\nPROCESS SCHEDULE TABLE }-:");

        String headerFormat = "+-----------+---------------+---------------+---------------+---------------+---------------+---------------+---------------+%n";
        String rowFormat = "| %-9s | %-13d | %-13d | %-13d | %-13d | %-13d | %-13d | %-13d |%n";

        StringBuilder table = new StringBuilder();

        table.append(String.format(headerFormat));
        table.append(String.format("| %-9s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s |%n",
                "Process", "Arrival Time", "Burst Time", "Start Time", "Completion", "Turnaround", "Waiting", "Response"));
        table.append(String.format(headerFormat));

        // Sort jobs by completion time for better readability
        completedJobs.stream()
                .sorted(Comparator.comparingInt(job -> job.completionTime))
                .forEach(job -> {
                    table.append(String.format(rowFormat,
                            job.jobId,
                            job.arrivalTime,
                            job.burstTime,
                            job.startTime,
                            job.completionTime,
                            job.turnaroundTime,
                            job.waitingTime,
                            job.responseTime
                    ));
                });

        table.append(String.format(headerFormat));

        // Add averages row
        table.append(String.format("| %-9s | %-13s | %-13.2f | %-13s | %-13s | %-13.2f | %-13.2f | %-13.2f |%n",
                "Average", "-",
                completedJobs.stream().mapToDouble(job -> job.burstTime).average().orElse(0),
                "-", "-",
                calculateAverageTurnaroundTime(),
                calculateAverageWaitingTime(),
                calculateAverageResponseTime()));

        table.append(String.format(headerFormat));
        System.out.println(table.toString());

        // Print additional metrics
        // System.out.printf("CPU Utilization: %.2f%%\n", calculateCpuUtilization());
        // System.out.printf("Throughput: %.2f jobs per time unit\n", calculateThroughput());
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
        double avgResponse = calculateAverageResponseTime();

        // Min/max metrics calculations using stream operations
        int minWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).min().orElse(0);
        int maxWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).max().orElse(0);
        int minTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).min().orElse(0);
        int maxTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).max().orElse(0);
        int minResponse = completedJobs.stream().mapToInt(job -> job.responseTime).min().orElse(0);
        int maxResponse = completedJobs.stream().mapToInt(job -> job.responseTime).max().orElse(0);

        // Calculate scheduling length (makespan)
        int makespan = calculateMakespan();

        // Calculate total context switches - specific to Round Robin
        int contextSwitches = completedJobs.stream()
                .mapToInt(job -> Math.max(0, job.executionHistory.size() - 1))
                .sum();

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
        System.out.printf(intFormat, "Minimum Response Time", minResponse);
        System.out.printf(intFormat, "Maximum Response Time", maxResponse);
        System.out.printf(border);
        System.out.printf(intFormat, "Time Quantum", timeQuantum);
        System.out.printf(intFormat, "Total Context Switches", contextSwitches);
        System.out.printf(intFormat, "Scheduling Length", makespan);
        System.out.printf(intFormat, "Total Jobs Completed", completedJobs.size());
        System.out.printf(border);

        // Performance indicators with qualitative assessment
        System.out.println("\nPERFORMANCE INDICATORS:");
        System.out.println("CPU Utilization: " + getPerformanceIndicator(cpuUtilization, 70, 90));
        System.out.println("Waiting Time Balance: " +
                (maxWaiting - minWaiting <= avgWaiting * 0.5 ? "Good" : "Could be improved"));
        System.out.println("Fairness: " +
                (maxTurnaround - minTurnaround <= avgTurnaround * 0.3 ? "Good" : "Varies significantly"));
        System.out.println("Context Switching Overhead: " +
                (contextSwitches <= completedJobs.size() * 2 ? "Low" : 
                contextSwitches <= completedJobs.size() * 4 ? "Moderate" : "High"));
        System.out.println("Time Quantum Efficiency: " + 
                (timeQuantum >= avgBurstTime() * 0.3 && timeQuantum <= avgBurstTime() * 0.7 ? 
                "Optimal" : (timeQuantum < avgBurstTime() * 0.3 ? "Too small" : "Too large")));
    }

    /**
     * Helper method to calculate average burst time
     */
    private double avgBurstTime() {
        return completedJobs.stream().mapToDouble(job -> job.burstTime).average().orElse(0);
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
     * Prints educational information about the Round Robin algorithm
     * Used for teaching and documentation purposes
     */
    public void printAlgorithmInfo() {
        System.out.println("\nROUND ROBIN (RR) SCHEDULING ALGORITHM\n");

        System.out.println("DESCRIPTION:");
        System.out.println("Round Robin is a CPU scheduling algorithm that allocates a small unit of time");
        System.out.println("(called a time quantum or time slice) to each process in a circular order.");
        System.out.println("Each process gets an equal share of CPU time and runs in a circular queue.");
        System.out.println("It's designed especially for time-sharing systems.");

        System.out.println("\nCHARACTERISTICS:");
        System.out.println("- Preemptive scheduling (in standard implementation)");
        System.out.println("- Time slicing approach with fixed quantum");
        System.out.println("- Fair CPU allocation (equal priority to all processes)");
        System.out.println("- Good for interactive systems requiring quick response times");

        System.out.println("\nVARIATIONS:");
        System.out.println("1. Preemptive Round Robin (standard): Processes are interrupted after their time quantum");
        System.out.println("2. Non-Preemptive Round Robin: Processes run to completion once selected (similar to FCFS)");
        System.out.println("3. Weighted Round Robin: Processes get different time quanta based on priority");
        System.out.println("4. Selfish Round Robin: Dynamically adjusts time quantum based on process behavior");

        System.out.println("\nMETRICS CALCULATED:");
        System.out.println("1. Turnaround Time: Time from arrival to completion");
        System.out.println("2. Waiting Time: Time spent waiting in ready queue");
        System.out.println("3. Response Time: Time from arrival to first execution");
        System.out.println("4. CPU Utilization: Percentage of time CPU is busy");
        System.out.println("5. Throughput: Number of processes completed per unit time");
        System.out.println("6. Context Switches: Number of times CPU switches between processes");

        System.out.println("\nTIME QUANTUM CONSIDERATIONS:");
        System.out.println("- Too large: Acts like FCFS, poor response time, potential convoy effect");
        System.out.println("- Too small: High context switching overhead, poor throughput");
        System.out.println("- Optimal: Typically 80% of processes should complete within one time quantum");

        System.out.println("\nADVANTAGES:");
        System.out.println("- Fair allocation of CPU time");
        System.out.println("- Good response times for short processes");
        System.out.println("- No starvation - every process gets CPU time");
        System.out.println("- Better for interactive systems than FCFS");

        System.out.println("\nDISADVANTAGES:");
        System.out.println("- Context switching overhead");
        System.out.println("- Higher average turnaround time than SJF");
        System.out.println("- Time quantum selection critically affects performance");
        System.out.println("- Treats all processes equally (no priority)");

        System.out.println("\nUSE CASES:");
        System.out.println("- Time-sharing systems");
        System.out.println("- Interactive environments requiring good response times");
        System.out.println("- Systems where fairness is more important than overall throughput");
        System.out.println("- Operating systems like Unix, Linux, and Windows use RR variants");

        System.out.println("\nCURRENT CONFIGURATION:");
        System.out.println("- Mode: " + (preemptive ? "Preemptive" : "Non-preemptive"));
        System.out.println("- Time Quantum: " + timeQuantum + " time units");
    }
}