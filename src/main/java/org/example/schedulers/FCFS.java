package org.example.schedulers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class FCFS {

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

    private final Queue<Job> jobQueue = new ArrayDeque<>();
    private final List<Job> completedJobs = new ArrayList<>();
    private boolean dummyMode = false;


    public void addJob(String jobId, int arrivalTime, int burstTime) {
        if (arrivalTime < 0 || burstTime <= 0) {
            throw new IllegalArgumentException("Invalid job parameters");
        }
        jobQueue.add(new Job(jobId, arrivalTime, burstTime));
    }

    public void setDummyMode(boolean enabled) {
        this.dummyMode = enabled;
    }

    public void execute() {
        if (jobQueue.isEmpty()) {
            throw new IllegalStateException("No jobs to schedule");
        }

        if (dummyMode) {
            dummyExecute();
            return;
        }

        int currentTime = 0;

        while (!jobQueue.isEmpty()) {
            Job job = jobQueue.poll();

            if (currentTime < job.arrivalTime) {
                currentTime = job.arrivalTime;
            }

            job.startTime = currentTime;
            job.completionTime = currentTime + job.burstTime;
            job.turnaroundTime = job.completionTime - job.arrivalTime;
            job.waitingTime = job.turnaroundTime - job.burstTime;
            job.responseTime = job.startTime - job.arrivalTime;

            currentTime = job.completionTime;
            completedJobs.add(job);
        }
    }


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


            if (i % 2 == 0) {
                job.waitingTime += 1;
                job.responseTime += 1;
            }
        }
    }


    public double calculateCpuUtilization() {
        int totalBurstTime = completedJobs.stream().mapToInt(job -> job.burstTime).sum();
        int totalTime = completedJobs.get(completedJobs.size() - 1).completionTime - completedJobs.get(0).arrivalTime;
        return (double) totalBurstTime / totalTime * 100;
    }


    public double calculateThroughput() {
        int totalTime = completedJobs.get(completedJobs.size() - 1).completionTime - completedJobs.get(0).arrivalTime;
        return (double) completedJobs.size() / totalTime;
    }


    public double calculateAverageWaitingTime() {
        return completedJobs.stream().mapToInt(job -> job.waitingTime).average().orElse(0);
    }


    public double calculateAverageTurnaroundTime() {
        return completedJobs.stream().mapToInt(job -> job.turnaroundTime).average().orElse(0);
    }


    public void printGanttChart() {
        System.out.println("\nGANTT CHART }-:");


        System.out.print("+");
        for (Job job : completedJobs) {
            System.out.print("--------+");
        }
        System.out.println();


        System.out.print("|");
        for (Job job : completedJobs) {
            System.out.printf("  %-5s |", job.jobId);
        }
        System.out.println();

        System.out.print("+");
        for (Job job : completedJobs) {
            System.out.print("--------+");
        }
        System.out.println();


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



    public void printTableData() {
        System.out.println("\nPROCESS SCHEDULE TABLE }-:");


        String headerFormat = "+-----------+---------------+---------------+---------------+---------------+---------------+---------------+---------------+%n";
        System.out.printf(headerFormat);
        System.out.printf("| %-9s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s |%n",
                "Job ID", "Arrival Time", "Burst Time", "Start Time", "Completion", "Turnaround", "Waiting", "Response");
        System.out.printf("| %-9s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s | %-13s |%n",
                "", "", "", "", "Time", "Time", "Time", "Time");
        System.out.printf(headerFormat);


        String rowFormat = "| %-9s | %-13d | %-13d | %-13d | %-13d | %-13d | %-13d | %-13d |%n";
        for (Job job : completedJobs) {
            System.out.printf(rowFormat,
                    job.jobId, job.arrivalTime, job.burstTime, job.startTime, job.completionTime,
                    job.turnaroundTime, job.waitingTime, job.responseTime);
        }
        System.out.printf(headerFormat);
    }


    public void printMetrics() {
        System.out.println("\nPERFORMANCE METRICS }-:");


        double cpuUtilization = calculateCpuUtilization();
        double throughput = calculateThroughput();
        double avgTurnaround = calculateAverageTurnaroundTime();
        double avgWaiting = calculateAverageWaitingTime();
        double avgResponse = completedJobs.stream().mapToInt(job -> job.responseTime).average().orElse(0);


        int minWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).min().orElse(0);
        int maxWaiting = completedJobs.stream().mapToInt(job -> job.waitingTime).max().orElse(0);
        int minTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).min().orElse(0);
        int maxTurnaround = completedJobs.stream().mapToInt(job -> job.turnaroundTime).max().orElse(0);


        int schedulingLength = completedJobs.isEmpty() ? 0 :
                completedJobs.get(completedJobs.size() - 1).completionTime - completedJobs.get(0).arrivalTime;


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


        System.out.println("\nPERFORMANCE INDICATORS:");
        System.out.println("CPU Utilization: " + getPerformanceIndicator(cpuUtilization, 70, 90));
        System.out.println("Waiting Time Balance: " +
                (maxWaiting - minWaiting <= 2 ? "Good" : "Could be improved"));
        System.out.println("Fairness: " +
                (maxTurnaround - minTurnaround <= avgTurnaround * 0.3 ? "Good" : "Varies significantly"));
    }

    private String getPerformanceIndicator(double value, double goodThreshold, double excellentThreshold) {
        if (value >= excellentThreshold) return "★★★★★ Excellent";
        if (value >= goodThreshold) return "★★★★ Good";
        if (value >= goodThreshold * 0.7) return "★★★ Fair";
        if (value >= goodThreshold * 0.5) return "★★ Poor";
        return "★ Very Poor";
    }


    public void printResults() {
        printGanttChart();
        printTableData();
        printMetrics();
    }


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

    public List<Job> getCompletedJobs() {
        return completedJobs;
    }
}