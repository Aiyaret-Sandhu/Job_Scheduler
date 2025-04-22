package org.example;

import static org.example.utils.Banner.printBanner;
import org.example.schedulers.FCFS;
import org.example.visualizers.FCFSVisualizer;

import org.example.schedulers.RR;
import org.example.visualizers.RRVisualizer;

import org.example.schedulers.SJF;
import org.example.visualizers.SJFVisualizer;

import org.example.schedulers.PS;
import org.example.visualizers.PSVisualizer;

import java.util.Scanner;


public class Main {

    public static void main(String[] args) {
        printBanner();

        Scanner scanner = new Scanner(System.in);
    
        System.out.println("CPU Scheduling Algorithm Simulator");
        System.out.println("1. First-Come, First-Served (FCFS)");
        System.out.println("2. Round Robin (RR)");
        System.out.println("3. Shortest Job First (SJF)");
        System.out.println("4. Priority Scheduling (PS)");  // Add this line
        // Add other algorithms as needed
        System.out.print("Select algorithm: ");
        
        int choice = scanner.nextInt();
        
        switch (choice) {
            case 1:
                System.out.println("1. Enter custom data");
                System.out.println("2. Use dummy data");
                System.out.print("Select option: ");
                int fcfsOption = scanner.nextInt();
                if (fcfsOption == 1) {
                    runFCFS(scanner, false);
                } else {
                    runFCFS(scanner, true);
                }
                break;
            case 2:
                System.out.println("1. Enter custom data");
                System.out.println("2. Use dummy data (Preemptive)");
                System.out.println("3. Use dummy data (Non-Preemptive)");
                System.out.print("Select option: ");
                int rrOption = scanner.nextInt();
                if (rrOption == 1) {
                    runRR(scanner, false, true);  // Custom data, default to preemptive
                } else if (rrOption == 2) {
                    runRR(scanner, true, true);   // Dummy data, preemptive
                } else {
                    runRR(scanner, true, false);  // Dummy data, non-preemptive
                }
                break;
            case 3:
                System.out.println("1. Enter custom data");
                System.out.println("2. Use dummy data (Preemptive)");
                System.out.println("3. Use dummy data (Non-Preemptive)");
                System.out.print("Select option: ");
                int sjfOption = scanner.nextInt();
                if (sjfOption == 1) {
                    runSJF(scanner, false, true);  // Custom data, default to preemptive
                } else if (sjfOption == 2) {
                    runSJF(scanner, true, true);   // Dummy data, preemptive
                } else {
                    runSJF(scanner, true, false);  // Dummy data, non-preemptive
                }
                break;
            case 4:  // Add case for PS
                System.out.println("1. Enter custom data");
                System.out.println("2. Use dummy data (Preemptive)");
                System.out.println("3. Use dummy data (Non-Preemptive)");
                System.out.print("Select option: ");
                int psOption = scanner.nextInt();
                if (psOption == 1) {
                    runPS(scanner, false, true);  // Custom data, default to preemptive
                } else if (psOption == 2) {
                    runPS(scanner, true, true);   // Dummy data, preemptive
                } else {
                    runPS(scanner, true, false);  // Dummy data, non-preemptive
                }
                break;
            // Add other cases for other algorithms
            default:
                System.out.println("Invalid choice");
        }
        
        scanner.close();
    }

    private static void runFCFS(Scanner scanner, boolean useDummyData) {
        FCFS scheduler = new FCFS();
        
        if (useDummyData) {
            // Add dummy data
            scheduler.addJob("P1", 0, 5);
            scheduler.addJob("P2", 1, 3);
            scheduler.addJob("P3", 2, 8);
            scheduler.addJob("P4", 3, 6);
        } else {
            // Get user input for jobs
            System.out.print("Enter number of processes: ");
            int n = scanner.nextInt();
            
            for (int i = 0; i < n; i++) {
                System.out.println("\nProcess " + (i+1) + ":");
                System.out.print("ID: ");
                String id = scanner.next();
                System.out.print("Arrival time: ");
                int arrival = scanner.nextInt();
                System.out.print("Burst time: ");
                int burst = scanner.nextInt();
                
                scheduler.addJob(id, arrival, burst);
            }
        }
        
        // Execute the scheduling algorithm
        scheduler.execute();
        
        // Print the results
        scheduler.printGanttChart();
        scheduler.printTableData();
        scheduler.printMetrics();

        // Show algorithm information if requested
        System.out.print("\nShow algorithm information? (y/n): ");
        if (scanner.next().equalsIgnoreCase("y")) {
            scheduler.printAlgorithmInfo();
        }
        
        // Ask if user wants to see the visualization
        System.out.print("\nLaunch graphical visualizer? (y/n): ");
        String launchGui = scanner.next();

        if (launchGui.equalsIgnoreCase("y")) {
            System.out.println("Launching FCFS Visualizer...");
            FCFSVisualizer.launchVisualizer(scheduler.getCompletedJobs());
        }
    }

    private static void runRR(Scanner scanner, boolean useDummyData, boolean preemptive) {
        RR scheduler = new RR();
        int quantum;
        
        if (useDummyData) {
            // Set time quantum for dummy data
            quantum = 2;
            scheduler.setTimeQuantum(quantum);
            scheduler.setPreemptive(preemptive);
            
            // Add dummy data with different patterns to show algorithm behavior
            scheduler.addJob("P1", 0, 5);  // Early arrival, medium burst
            scheduler.addJob("P2", 1, 3);  // Early arrival, short burst
            scheduler.addJob("P3", 2, 8);  // Early arrival, long burst
            scheduler.addJob("P4", 5, 2);  // Later arrival, short burst
            scheduler.addJob("P5", 6, 4);  // Later arrival, medium burst
            
            System.out.println("Using dummy data with time quantum = " + quantum);
            System.out.println("Mode: " + (preemptive ? "Preemptive" : "Non-Preemptive"));
        } else {
            // Get time quantum from user
            System.out.print("Enter time quantum: ");
            quantum = scanner.nextInt();
            scheduler.setTimeQuantum(quantum);
            
            // Get scheduling mode from user
            System.out.println("Select mode:");
            System.out.println("1. Preemptive");
            System.out.println("2. Non-Preemptive");
            System.out.print("Choice: ");
            int modeChoice = scanner.nextInt();
            scheduler.setPreemptive(modeChoice == 1);
            
            // Get job data from user
            System.out.print("Enter number of processes: ");
            int n = scanner.nextInt();
            
            for (int i = 0; i < n; i++) {
                System.out.println("\nProcess " + (i+1) + ":");
                System.out.print("ID: ");
                String id = scanner.next();
                System.out.print("Arrival time: ");
                int arrival = scanner.nextInt();
                System.out.print("Burst time: ");
                int burst = scanner.nextInt();
                
                scheduler.addJob(id, arrival, burst);
            }
        }
        
        // Execute the scheduling algorithm
        scheduler.execute();
        
        // Print the results
        System.out.println("\nScheduling Mode: " + scheduler.getModeDescription());
        scheduler.printGanttChart();
        scheduler.printTableData();
        scheduler.printMetrics();
        
        // Show algorithm information if requested
        System.out.print("\nShow algorithm information? (y/n): ");
        if (scanner.next().equalsIgnoreCase("y")) {
            scheduler.printAlgorithmInfo();
        }

        System.out.print("\nLaunch graphical visualizer? (y/n): ");
        String launchGui = scanner.next();
        
        if (launchGui.equalsIgnoreCase("y")) {
            System.out.println("Launching Round Robin Visualizer...");
            RRVisualizer.launchVisualizer(
                scheduler.getCompletedJobs(),
                quantum,
                scheduler.isPreemptive()
            );
        }
    }

    private static void runSJF(Scanner scanner, boolean useDummyData, boolean preemptive) {
        SJF scheduler = new SJF();
        scheduler.setPreemptive(preemptive);
        
        if (useDummyData) {
            // Add dummy data with different patterns to show algorithm behavior
            scheduler.addJob("P1", 0, 5);  // Early arrival, medium burst
            scheduler.addJob("P2", 1, 3);  // Early arrival, short burst
            scheduler.addJob("P3", 2, 8);  // Early arrival, long burst
            scheduler.addJob("P4", 5, 2);  // Later arrival, shortest burst
            scheduler.addJob("P5", 6, 1);  // Latest arrival, shortest burst
            
            System.out.println("Using dummy data for SJF");
            System.out.println("Mode: " + (preemptive ? "Preemptive (SRTF)" : "Non-Preemptive"));
        } else {
            // Get scheduling mode from user if not already set
            if (!useDummyData) {
                System.out.println("Select mode:");
                System.out.println("1. Preemptive (SRTF - Shortest Remaining Time First)");
                System.out.println("2. Non-Preemptive");
                System.out.print("Choice: ");
                int modeChoice = scanner.nextInt();
                scheduler.setPreemptive(modeChoice == 1);
            }
            
            // Get job data from user
            System.out.print("Enter number of processes: ");
            int n = scanner.nextInt();
            
            for (int i = 0; i < n; i++) {
                System.out.println("\nProcess " + (i+1) + ":");
                System.out.print("ID: ");
                String id = scanner.next();
                System.out.print("Arrival time: ");
                int arrival = scanner.nextInt();
                System.out.print("Burst time: ");
                int burst = scanner.nextInt();
                
                scheduler.addJob(id, arrival, burst);
            }
        }
        
        // Execute the scheduling algorithm
        scheduler.execute();
        
        // Print the results
        System.out.println("\nScheduling Mode: " + (preemptive ? "Preemptive (SRTF)" : "Non-Preemptive SJF"));
        scheduler.printGanttChart();
        scheduler.printTableData();
        scheduler.printMetrics();
        
        // Show algorithm information if requested
        System.out.print("\nShow algorithm information? (y/n): ");
        if (scanner.next().equalsIgnoreCase("y")) {
            scheduler.printAlgorithmInfo();
        }

        System.out.print("\nLaunch graphical visualizer? (y/n): ");
        String launchGui = scanner.next();
        
        if (launchGui.equalsIgnoreCase("y")) {
            System.out.println("Launching SJF Visualizer...");
            SJFVisualizer.launchVisualizer(
                scheduler.getCompletedJobs(),
                preemptive
            );
        }
    }

    private static void runPS(Scanner scanner, boolean useDummyData, boolean preemptive) {
        PS scheduler = new PS();
        scheduler.setPreemptive(preemptive);
        
        if (useDummyData) {
            // Add dummy data with different patterns to show algorithm behavior and priorities
            scheduler.addJob("P1", 0, 5, 3);  // Early arrival, medium burst, medium priority
            scheduler.addJob("P2", 1, 3, 1);  // Early arrival, short burst, high priority
            scheduler.addJob("P3", 2, 8, 4);  // Early arrival, long burst, low priority
            scheduler.addJob("P4", 3, 2, 2);  // Later arrival, short burst, high-medium priority
            scheduler.addJob("P5", 4, 4, 5);  // Latest arrival, medium burst, lowest priority
            
            System.out.println("Using dummy data for Priority Scheduling");
            System.out.println("Mode: " + (preemptive ? "Preemptive" : "Non-Preemptive"));
            System.out.println("Note: Lower priority value means higher priority");
        } else {
            // Get scheduling mode from user if not already set
            if (!useDummyData) {
                System.out.println("Select mode:");
                System.out.println("1. Preemptive (Higher priority job can interrupt running job)");
                System.out.println("2. Non-Preemptive");
                System.out.print("Choice: ");
                int modeChoice = scanner.nextInt();
                scheduler.setPreemptive(modeChoice == 1);
            }
            
            // Get job data from user
            System.out.print("Enter number of processes: ");
            int n = scanner.nextInt();
            
            System.out.println("Note: Lower priority value means higher priority (1 = highest)");
            
            for (int i = 0; i < n; i++) {
                System.out.println("\nProcess " + (i+1) + ":");
                System.out.print("ID: ");
                String id = scanner.next();
                System.out.print("Arrival time: ");
                int arrival = scanner.nextInt();
                System.out.print("Burst time: ");
                int burst = scanner.nextInt();
                System.out.print("Priority (1=highest): ");
                int priority = scanner.nextInt();
                
                scheduler.addJob(id, arrival, burst, priority);
            }
        }
        
        // Execute the scheduling algorithm
        scheduler.execute();
        
        // Print the results
        System.out.println("\nScheduling Mode: " + scheduler.getModeDescription());
        scheduler.printGanttChart();
        scheduler.printTableData();
        scheduler.printMetrics();
        
        // Show algorithm information if requested
        System.out.print("\nShow algorithm information? (y/n): ");
        if (scanner.next().equalsIgnoreCase("y")) {
            scheduler.printAlgorithmInfo();
        }

        System.out.print("\nLaunch graphical visualizer? (y/n): ");
        String launchGui = scanner.next();
        
        if (launchGui.equalsIgnoreCase("y")) {
            System.out.println("Launching Priority Scheduling Visualizer...");
            PSVisualizer.launchVisualizer(
                scheduler.getCompletedJobs(),
                scheduler.getAllTimeSlices()
            );
        }
    }
}