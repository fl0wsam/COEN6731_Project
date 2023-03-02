package io.swagger.client.part2;

import io.swagger.client.model.LiftRide;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class SkiClient {

    private static final String POST_URL = "http://localhost:8080/SkiResorts_war_exploded/skiers";
    private static final int NUM_THREADS = 32;
    private static final int NUM_POSTS_PER_THREAD = 1000;
    private static final int TOTAL_POSTS = NUM_THREADS * NUM_POSTS_PER_THREAD;
    private static final int NUM_RETRIES = 5;

    private static final String CSV_HEADER = "start_time,request_type,latency,response_code";
    private static final String CSV_FILE_NAME = "latency_data.csv";

    private static volatile int successfulRequests = 0;
    private static volatile int failedRequests = 0;

    private static final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch endSignal = new CountDownLatch(NUM_THREADS);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(executor.submit(new PostingThread(startSignal, endSignal, NUM_POSTS_PER_THREAD)));
        }

        System.out.println("Waiting for all threads to start...");
        startSignal.countDown();
        endSignal.await();
        System.out.println("All threads have finished!");

        executor.shutdown();

        // write latency data to CSV file
        writeLatencyDataToCsv();

        // calculate performance metrics
        double meanResponseTime = calculateMeanResponseTime();
        double medianResponseTime = calculateMedianResponseTime();
        double throughput = calculateThroughput();
        double p99ResponseTime = calculateP99ResponseTime();
        double minResponseTime = calculateMinResponseTime();
        double maxResponseTime = calculateMaxResponseTime();

        // print out performance metrics
        NumberFormat formatter = new DecimalFormat("#0.00");
        System.out.println("Mean response time: " + formatter.format(meanResponseTime) + " milliseconds");
        System.out.println("Median response time: " + formatter.format(medianResponseTime) + " milliseconds");
        System.out.println("Throughput: " + formatter.format(throughput) + " requests per second");
        System.out.println("99th percentile response time: " + formatter.format(p99ResponseTime) + " milliseconds");
        System.out.println("Min response time: " + formatter.format(minResponseTime) + " milliseconds");
        System.out.println("Max response time: " + formatter.format(maxResponseTime) + " milliseconds");

        System.out.println("Successful requests: " + successfulRequests);
        System.out.println("Failed requests: " + failedRequests);
    }

    static class PostingThread implements Runnable {

        private final CountDownLatch startSignal;
        private final CountDownLatch endSignal;
        private final int numPosts;

        public PostingThread(CountDownLatch startSignal, CountDownLatch endSignal, int numPosts) {
            this.startSignal = startSignal;
            this.endSignal = endSignal;
            this.numPosts = numPosts;
        }

        @Override
        public void run() {
            try {
                startSignal.await();
                for (int i = 0; i < numPosts; i++) {
                    int statusCode = sendPostRequest();
                    int retries = 0;
                    while (statusCode >= 500 && retries < NUM_RETRIES) {
                        System.out.println("Retrying request " + i + " on thread " + Thread.currentThread().getId() +
                                " due to status code " + statusCode);
                        statusCode = sendPostRequest();
                        retries++;
                    }
                    if (statusCode == 201) {
                        successfulRequests++;
                    } else {
                        failedRequests++;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                endSignal.countDown();
            }
        }

        private int sendPostRequest() {
            int statusCode = -1;
            try {
                URL url = new URL(POST_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                String jsonInputString = LiftRide.generateRandomLiftRideJson(10000,40,360,10);
                byte[] input = jsonInputString.getBytes("utf-8");
                conn.getOutputStream().write(input);

                statusCode = conn.getResponseCode();
                conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return statusCode;
        }
    }
    private static void writeLatencyDataToCsv(List<LatencyData> latencyDataList) {
        try {
            FileWriter csvWriter = new FileWriter("latency_data.csv");
            csvWriter.append("Start Time,Request Type,Latency,Response Code\n");
            for (LatencyData latencyData : latencyDataList) {
                csvWriter.append(latencyData.getStartTime() + ",");
                csvWriter.append(latencyData.getRequestType() + ",");
                csvWriter.append(latencyData.getLatency() + ",");
                csvWriter.append(latencyData.getResponseCode() + "\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static long calculateMaxResponseTime(List<Long> latencies) {
        return Collections.max(latencies);
    }
    public static long calculateMinResponseTime(List<LatencyData> latencyDataList) {
        long minResponseTime = Long.MAX_VALUE;
        for (LatencyData latencyData : latencyDataList) {
            long responseTime = latencyData.getLatency();
            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
        }
        return minResponseTime;
    }



}
