package org.example;

import static org.example.utils.Banner.printBanner;
import org.example.schedulers.FCFS;
import org.example.visualizers.FCFSVisualizer;

public class Main {

    public static void main(String[] args) {
        printBanner();

        FCFS scheduler = new FCFS();

        // Add some jobs
        scheduler.addJob("P1", 0, 5);
        scheduler.addJob("P2", 1, 3);
        scheduler.addJob("P3", 2, 8);
        scheduler.addJob("P4", 3, 6);

        // Execute and print results
        scheduler.execute();
        scheduler.printResults();

        // Launch JavaFX visualization
        FCFSVisualizer.launchVisualizer(scheduler.getCompletedJobs());
    }
}