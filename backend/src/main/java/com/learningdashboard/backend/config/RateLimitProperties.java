package com.learningdashboard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private Endpoint explain = new Endpoint(60, 10);
    private Endpoint visualize = new Endpoint(60, 10);
    /** Spark "Ask the AI": questions and saving a chat as a note share this limit (Phase 8). */
    private Endpoint ask = new Endpoint(60, 20);

    public Endpoint getExplain() { return explain; }
    public void setExplain(Endpoint explain) { this.explain = explain; }
    public Endpoint getVisualize() { return visualize; }
    public void setVisualize(Endpoint visualize) { this.visualize = visualize; }
    public Endpoint getAsk() { return ask; }
    public void setAsk(Endpoint ask) { this.ask = ask; }

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
