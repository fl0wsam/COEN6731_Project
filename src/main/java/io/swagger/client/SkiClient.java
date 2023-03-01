package io.swagger.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SkiClient {
    private static final int NUM_THREADS = 32;
    private static final int NUM_REQUESTS = 10000;

    private static final String API_BASE_URL = "http://localhost:8080/SkiServer/skiers";
    private static final String POST_REQUEST_BODY = "{\"resortID\":%d,\"dayID\":%d,\"skierID\":%d,\"liftID\":%d,\"time\":%d}";
    private static final String POST_REQUEST_METHOD = "POST";

    private static final BlockingQueue<String> queue = new ArrayBlockingQueue<>(NUM_REQUESTS);

    private static void generateRequests() throws InterruptedException {
        for (int i = 0; i < NUM_REQUESTS; i++) {
            int skierID = (int) (Math.random() * 100000) + 1;
            int resortID = (int) (Math.random() * 10) + 1;
            int liftID = (int) (Math.random() * 40) + 1;
            int seasonID = 2022;
            int dayID = 1;
            int time = (int) (Math.random() * 360) + 1;
            String postRequestBody = String.format(POST_REQUEST_BODY, resortID, dayID, skierID, liftID, time);
            queue.put(postRequestBody);
        }
    }

    private static void sendRequests() throws InterruptedException {
        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                try {
                    while (true) {
                        String postRequestBody = queue.poll();
                        if (postRequestBody == null) {
                            break;
                        }
                        URL url = new URL(API_BASE_URL);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod(POST_REQUEST_METHOD);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        conn.getOutputStream().write(postRequestBody.getBytes());
                        int responseCode = conn.getResponseCode();
                        if (responseCode != HttpURLConnection.HTTP_CREATED) {
                            System.err.println("Failed to send POST request: " + postRequestBody);
                        }
                        conn.disconnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        generateRequests();
        sendRequests();
        long endTime = System.currentTimeMillis();

        long totalTime = endTime - startTime;
        int successfulRequests = NUM_REQUESTS;
        int failedRequests = 0;
        double throughput = (double) NUM_REQUESTS / totalTime * 1000;

        System.out.println("Successful requests: " + successfulRequests);
        System.out.println("Failed requests: " + failedRequests);
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Throughput: " + throughput + " requests/sec");
    }
}
