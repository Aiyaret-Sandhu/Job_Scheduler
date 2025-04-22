package org.example.schedulers;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Shortest Job First (SJF) CPU Scheduling Algorithm Implementation
 *
 * Data structures used:
 * - PriorityQueue: For ordering jobs by burst/remaining time
 * - ArrayList: For storing completed jobs and results
 * - HashMap: For O(1) job lookups
 * - Stream API: For efficient statistical calculations
 *
 * This implementation supports:
 * 1. Non-preemptive SJF - scheduled based on original burst time
 * 2. Preemptive SJF (SRTF) - can be preempted by jobs with shorter remaining time
 */
public class SJF {

    /**
     * Job class to encapsulate all process-related data
     * Provides a clean representation of each process and its metrics
     */
    public static class Job {
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
        
        @Override
        public String toString() {
            return jobId + " (Arrival: " + arrivalTime + ", Burst: " + burstTime + 
                   ", Remaining: " + remainingTime + ")";
        }
    }
    
    /**
     * Class to represent each execution time slice of a job
     * Useful for visualizing execution history and preemption points
     */
    public static class TimeSlice {
        public int startTime;
        public int endTime;
        public String jobId;  // Added for efficient job identification
        
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
    
    // Mode setting: preemptive or non-preemptive
    private boolean preemptive = false;  // Default is non-preemptive SJF
    
    /**
     * Adds a new job to be scheduled
     * 
     * @param jobId Unique identifier for the job
     * @param arrivalTime Time at which the job arrives in the system
     * @param burstTime Time required for job execution
     */
    public void addJob(String jobId, int arrivalTime, int burstTime) {
        if (arrivalTime < 0 || burstTime <= 0) {
            throw new IllegalArgumentException("Invalid job parameters");
        }
        Job job = new Job(jobId, arrivalTime, burstTime);
        jobs.add(job);
        jobMap.put(jobId, job);  // Store in map for O(1) lookup
    }
    
    /**
     * Set scheduling mode to preemptive or non-preemptive
     * In preemptive mode (SRTF), jobs can be interrupted when a shorter job arrives
     * 
     * @param preemptive true for Shortest Remaining Time First (SRTF), false for SJF
     */
    public void setPreemptive(boolean preemptive) {
        this.preemptive = preemptive;
    }
    
    public String getModeDescription() {
        return preemptive ? 
                "Preemptive Shortest Job First (SRTF)" : 
                "Non-Preemptive Shortest Job First (SJF)";
    }
    
    /**
     * Execute the SJF scheduling algorithm
     *
     * Algorithm for Non-preemptive SJF:
     * 1. Sort jobs by arrival time
     * 2. At each completion point, select the shortest job that has arrived
     * 3. Execute the selected job to completion
     *
     * Algorithm for Preemptive SJF (SRTF):
     * 1. Sort jobs by arrival time
     * 2. At each decision point (job arrival/completion), select job with shortest remaining time
     * 3. Execute the selected job until next decision point
     *
     * Time Complexity:
     * O(n²) in worst case due to job selection at each decision point
     * Space Complexity: O(n) for storing jobs and execution history
     */
    public void execute() {
        if (jobs.isEmpty()) {
            throw new IllegalStateException("No jobs to schedule");
        }
        
        // Clear previous execution data
        completedJobs.clear();
        
        // Reset all jobs to their original state
        for (Job job : jobs) {
            job.remainingTime = job.burstTime;
            job.startTime = -1;
            job.completionTime = 0;
            job.turnaroundTime = 0;
            job.waitingTime = 0;
            job.responseTime = 0;
            job.executionHistory.clear();
        }
        
        // Create a copy of jobs sorted by arrival time
        List<Job> remainingJobs = new ArrayList<>(jobs);
        remainingJobs.sort(Comparator.comparingInt(job -> job.arrivalTime));
        
        int currentTime = 0;
        Job currentJob = null;
        
        // Continue until all jobs are processed
        while (!remainingJobs.isEmpty() || currentJob != null) {
            // Find available jobs at current time
            List<Job> availableJobs = new ArrayList<>();
            
            // Identify jobs that have arrived by the current time
            Iterator<Job> iterator = remainingJobs.iterator();
            while (iterator.hasNext()) {
                Job job = iterator.next();
                if (job.arrivalTime <= currentTime) {
                    availableJobs.add(job);
                    iterator.remove();
                } else {
                    // Jobs are sorted by arrival time, so we can break early
                    break;
                }
            }
            
            // Decision point: Select next job to execute
            if (currentJob == null) {
                if (availableJobs.isEmpty()) {
                    // No jobs available, advance time to next arrival
                    if (remainingJobs.isEmpty()) {
                        break; // No more jobs to process
                    }
                    currentTime = remainingJobs.get(0).arrivalTime;
                    continue;
                }
                
                // Select job with shortest burst/remaining time
                if (preemptive) {
                    // In preemptive mode (SRTF), select based on remaining time
                    currentJob = availableJobs.stream()
                            .min(Comparator.comparingInt(job -> job.remainingTime))
                            .orElse(null);
                } else {
                    // In non-preemptive mode (SJF), select based on burst time
                    currentJob = availableJobs.stream()
                            .min(Comparator.comparingInt(job -> job.burstTime))
                            .orElse(null);
                }
                
                // Remove selected job from available jobs
                availableJobs.remove(currentJob);
                
                // Record the job's start time if this is its first execution
                if (currentJob.startTime == -1) {
                    currentJob.startTime = currentTime;
                    currentJob.responseTime = currentJob.startTime - currentJob.arrivalTime;
                }
            }
            
            // Determine how long this job will run
            int executionTime;
            
            if (preemptive) {
                // In preemptive mode, determine next decision point
                int nextArrivalTime = remainingJobs.isEmpty() ? 
                    Integer.MAX_VALUE : remainingJobs.get(0).arrivalTime;
                
                // Run until completion or next arrival, whichever comes first
                executionTime = Math.min(currentJob.remainingTime, nextArrivalTime - currentTime);
            } else {
                // In non-preemptive mode, run until completion
                executionTime = currentJob.remainingTime;
            }
            
            // Record time slice
            TimeSlice timeSlice = new TimeSlice(currentTime, currentTime + executionTime, currentJob.jobId);
            currentJob.executionHistory.add(timeSlice);
            
            // Update time and job's remaining time
            currentTime += executionTime;
            currentJob.remainingTime -= executionTime;
            
            // Check if job completed
            if (currentJob.remainingTime == 0) {
                currentJob.completionTime = currentTime;
                currentJob.turnaroundTime = currentJob.completionTime - currentJob.arrivalTime;
                currentJob.waitingTime = currentJob.turnaroundTime - currentJob.burstTime;
                
                completedJobs.add(currentJob);
                currentJob = null;
            } else if (preemptive) {
                // In preemptive mode, check if a new job with shorter remaining time has arrived
                boolean preempt = false;
                
                // Add newly arrived jobs to available jobs
                iterator = remainingJobs.iterator();
                while (iterator.hasNext()) {
                    Job job = iterator.next();
                    if (job.arrivalTime <= currentTime) {
                        availableJobs.add(job);
                        iterator.remove();
                        
                        // Check if this job should preempt current one
                        if (job.remainingTime < currentJob.remainingTime) {
                            preempt = true;
                        }
                    } else {
                        // Jobs are sorted by arrival time, so we can break early
                        break;
                    }
                }
                
                if (preempt) {
                    // Find job with shortest remaining time
                    availableJobs.add(currentJob); // Include current job in selection
                    Job nextJob = availableJobs.stream()
                            .min(Comparator.comparingInt(job -> job.remainingTime))
                            .orElse(null);
                    
                    if (nextJob != currentJob) {
                        // Preempt current job
                        availableJobs.remove(nextJob);
                        Job tempJob = currentJob;
                        currentJob = nextJob;
                        availableJobs.add(tempJob);
                        
                        // Record start time if this is first execution of new job
                        if (currentJob.startTime == -1) {
                            currentJob.startTime = currentTime;
                            currentJob.responseTime = currentJob.startTime - currentJob.arrivalTime;
                        }
                    } else {
                        // Keep current job
                        availableJobs.remove(currentJob);
                    }
                }
            }
            
            // Add remaining available jobs back to pending queue
            if (!availableJobs.isEmpty()) {
                remainingJobs.addAll(0, availableJobs); // Add at beginning to maintain order
                // Re-sort by arrival time
                remainingJobs.sort(Comparator.comparingInt(job -> job.arrivalTime));
            }
        }
    }
    
    /**
     * Reset all jobs to their original state
     * This allows re-running the algorithm with different parameters
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
    
    /**
     * Get a copy of the job list
     * @return List of all jobs
     */
    public List<Job> getJobs() {
        return new ArrayList<>(jobs);
    }
    
    /**
     * Get completed jobs with all calculated metrics
     * @return List of completed jobs
     */
    public List<Job> getCompletedJobs() {
        return new ArrayList<>(completedJobs);
    }
    
    /**
     * Get a specific job by ID
     * @param jobId ID of the job to retrieve
     * @return The job with the specified ID, or null if not found
     */
    public Job getJob(String jobId) {
        return jobMap.get(jobId);
    }
    
    /**
     * Check if algorithm is running in preemptive mode
     * @return true if preemptive, false if non-preemptive
     */
    public boolean isPreemptive() {
        return preemptive;
    }
    
    /**
     * Calculate makespan (total schedule length)
     * @return The makespan value
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
    
    /**
     * Calculate CPU utilization
     * @return CPU utilization as a percentage
     */
    public double calculateCpuUtilization() {
        if (completedJobs.isEmpty()) return 0;

        int totalBurstTime = completedJobs.stream().mapToInt(job -> job.burstTime).sum();
        int makespan = calculateMakespan();

        return makespan > 0 ? (double) totalBurstTime / makespan * 100 : 0;
    }
    
    /**
     * Calculate throughput
     * @return Throughput (jobs per time unit)
     */
    public double calculateThroughput() {
        if (completedJobs.isEmpty()) return 0;

        int makespan = calculateMakespan();
        return makespan > 0 ? (double) completedJobs.size() / makespan : 0;
    }
    
    /**
     * Calculate average waiting time
     * @return Average waiting time
     */
    public double calculateAverageWaitingTime() {
        return completedJobs.stream().mapToInt(job -> job.waitingTime).average().orElse(0);
    }
    
    /**
     * Calculate average turnaround time
     * @return Average turnaround time
     */
    public double calculateAverageTurnaroundTime() {
        return completedJobs.stream().mapToInt(job -> job.turnaroundTime).average().orElse(0);
    }
    
    /**
     * Calculate average response time
     * @return Average response time
     */
    public double calculateAverageResponseTime() {
        return completedJobs.stream().mapToInt(job -> job.responseTime).average().orElse(0);
    }
    
    /**
     * Print a Gantt chart of the job execution
     */
    public void printGanttChart() {
        if (completedJobs.isEmpty()) {
            System.out.println("No jobs completed yet.");
            return;
        }

        System.out.println("\nGANTT CHART }-:");

        // Collect all time slices and sort them by start time
        List<TimeSlice> allSlices = completedJobs.stream()
                .flatMap(job -> job.executionHistory.stream())
                .sorted(Comparator.comparingInt(slice -> slice.startTime))
                .collect(Collectors.toList());

        // Map of job IDs to colors for visualization
        Map<String, String> jobColors = new HashMap<>();
        String[] colors = {"\u001B[42m", "\u001B[43m", "\u001B[44m", "\u001B[45m", "\u001B[46m", "\u001B[41m"};
        String resetColor = "\u001B[0m";

        // Assign colors to jobs
        int colorIndex = 0;
        for (Job job : completedJobs) {
            jobColors.put(job.jobId, colors[colorIndex % colors.length]);
            colorIndex++;
        }

        // Build Gantt chart using StringBuilder
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
    }
    
    /**
     * Print comprehensive performance metrics
     */
    public void printMetrics() {
        System.out.println("\nPERFORMANCE METRICS }-:");

        // Calculate metrics using stream operations
        double cpuUtilization = calculateCpuUtilization();
        double throughput = calculateThroughput();
        double avgTurnaround = calculateAverageTurnaroundTime();
        double avgWaiting = calculateAverageWaitingTime();
        double avgResponse = calculateAverageResponseTime();

        // Min/max metrics calculations
        int minWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).min().orElse(0);
        int maxWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).max().orElse(0);
        int minTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).min().orElse(0);
        int maxTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).max().orElse(0);
        int minResponse = completedJobs.stream().mapToInt(job -> job.responseTime).min().orElse(0);
        int maxResponse = completedJobs.stream().mapToInt(job -> job.responseTime).max().orElse(0);

        // Calculate scheduling length (makespan)
        int makespan = calculateMakespan();

        // Calculate total preemptions (specific to preemptive mode)
        int totalPreemptions = 0;
        if (preemptive) {
            totalPreemptions = completedJobs.stream()
                    .mapToInt(job -> Math.max(0, job.executionHistory.size() - 1))
                    .sum();
        }

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
        if (preemptive) {
            System.out.printf(intFormat, "Total Preemptions", totalPreemptions);
        }
        System.out.printf(intFormat, "Scheduling Length", makespan);
        System.out.printf(intFormat, "Total Jobs Completed", completedJobs.size());
        System.out.printf(border);

        // Performance indicators with qualitative assessment
        System.out.println("\nPERFORMANCE INDICATORS:");
        System.out.println("CPU Utilization: " + getPerformanceIndicator(cpuUtilization, 70, 90));
        System.out.println("Waiting Time: " + 
                (maxWaiting - minWaiting <= avgWaiting * 0.5 ? "Good" : "Could be improved"));
        System.out.println("Fairness: " +
                (maxTurnaround - minTurnaround <= avgTurnaround * 0.3 ? "Good" : "Varies significantly"));
        if (preemptive) {
            System.out.println("Preemption Overhead: " +
                    (totalPreemptions <= completedJobs.size() * 0.5 ? "Low" :
                     totalPreemptions <= completedJobs.size() ? "Moderate" : "High"));
        }
        System.out.println("Algorithm Efficiency: " + 
                (avgTurnaround <= 1.2 * avgBurstTime() ? "Excellent" : 
                 avgTurnaround <= 1.5 * avgBurstTime() ? "Good" : "Fair"));
    }
    
    /**
     * Helper method to calculate average burst time
     * @return Average burst time
     */
    private double avgBurstTime() {
        return completedJobs.stream().mapToDouble(job -> job.burstTime).average().orElse(0);
    }

    /**
     * Generate qualitative performance indicators
     * @param value The value to evaluate
     * @param goodThreshold Threshold for good performance
     * @param excellentThreshold Threshold for excellent performance
     * @return A string with the performance indicator
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
     * Prints educational information about the SJF algorithm
     */
    public void printAlgorithmInfo() {
        System.out.println("\nSHORTEST JOB FIRST (SJF) SCHEDULING ALGORITHM\n");

        System.out.println("DESCRIPTION:");
        System.out.println("Shortest Job First (SJF) is a CPU scheduling algorithm that selects the job");
        System.out.println("with the smallest execution time first. It aims to minimize the average");
        System.out.println("waiting time across all processes.");

        System.out.println("\nVARIATIONS:");
        System.out.println("1. Non-preemptive SJF: Once a process starts executing, it runs to completion");
        System.out.println("2. Preemptive SJF (SRTF): If a new process arrives with a shorter remaining");
        System.out.println("   time than the currently running process, the CPU is preempted");

        System.out.println("\nCHARACTERISTICS:");
        System.out.println("- Optimal for minimizing average waiting time");
        System.out.println("- " + (preemptive ? "Preemptive" : "Non-preemptive") + " in current implementation");
        System.out.println("- Requires prior knowledge of job execution time");
        System.out.println("- Can lead to starvation of longer jobs in busy systems");

        System.out.println("\nMETRICS CALCULATED:");
        System.out.println("1. Turnaround Time: Time from arrival to completion");
        System.out.println("2. Waiting Time: Time spent waiting in ready queue");
        System.out.println("3. Response Time: Time from arrival to first execution");
        System.out.println("4. CPU Utilization: Percentage of time CPU is busy");
        System.out.println("5. Throughput: Number of processes completed per unit time");
        if (preemptive) {
            System.out.println("6. Preemptions: Number of times processes are interrupted");
        }

        System.out.println("\nADVANTAGES:");
        System.out.println("- Optimal average waiting time among all scheduling algorithms");
        System.out.println("- Good for systems where job lengths are known in advance");
        System.out.println("- " + (preemptive ? "SRTF provides better response time for short processes" : 
                                "Simpler implementation with no context switching overhead"));

        System.out.println("\nDISADVANTAGES:");
        System.out.println("- Requires prior knowledge of job execution times");
        System.out.println("- Potential starvation for longer jobs");
        System.out.println("- " + (preemptive ? "Overhead from context switching during preemption" : 
                                "Long jobs can block shorter jobs that arrive later"));

        System.out.println("\nUSE CASES:");
        System.out.println("- Batch processing systems where job times are known");
        System.out.println("- Systems optimized for minimizing average waiting time");
        System.out.println("- " + (preemptive ? "Interactive systems requiring good response time" : 
                                "Systems where context switching overhead is significant"));

        System.out.println("\nCURRENT CONFIGURATION:");
        System.out.println("- Mode: " + getModeDescription());
        System.out.println("- Total Jobs: " + jobs.size());
    }

    /**
     * Export job results in CSV format
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
}