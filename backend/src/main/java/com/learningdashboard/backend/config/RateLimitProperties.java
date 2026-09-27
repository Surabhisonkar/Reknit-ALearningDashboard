package com.learningdashboard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private Endpoint explain = new Endpoint(60, 10);
    private Endpoint visualize = new Endpoint(60, 10);

    public Endpoint getExplain() { return explain; }
    public void setExplain(Endpoint explain) { this.explain = explain; }
    public Endpoint getVisualize() { return visualize; }
    public void setVisualize(Endpoint visualize) { this.visualize = visualize; }

    public static class Endpoint {
        private long windowSeconds;
        private int maxRequests;

        public Endpoint() { }

        public Endpoint(long windowSeconds, int maxRequests) {
            this.windowSeconds = windowSeconds;
            this.maxRequests = maxRequests;
        }

        public long getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(long v) { this.windowSeconds = v; }
        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int v) { this.maxRequests = v; }
    }
}
