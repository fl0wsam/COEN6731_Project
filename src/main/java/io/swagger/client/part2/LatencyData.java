package io.swagger.client.part2;

public class LatencyData {
    private long startTime;
    private long latency;
    private String requestType;
    private int responseCode;

    public LatencyData(long startTime, long latency, String requestType, int responseCode) {
        this.startTime = startTime;
        this.latency = latency;
        this.requestType = requestType;
        this.responseCode = responseCode;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLatency() {
        return latency;
    }

    public String getRequestType() {
        return requestType;
    }

    public int getResponseCode() {
        return responseCode;
    }
}

