package io.swagger.client.part1;

import io.swagger.client.model.LiftRide;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SkiClient {

    private static final String POST_URL = "http://localhost:8080/SkiResorts_war_exploded/skiers";
    private static final int NUM_THREADS = 32;
    private static final int NUM_POSTS_PER_THREAD = 1000;
    private static final int TOTAL_POSTS = NUM_THREADS * NUM_POSTS_PER_THREAD;
    private static final int NUM_RETRIES = 5;

    private static volatile int successfulRequests = 0;
    private static volatile int failedRequests = 0;

    public static void main(String[] args) throws InterruptedException, ExecutionException {

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
}
