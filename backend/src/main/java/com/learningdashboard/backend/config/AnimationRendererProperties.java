package com.learningdashboard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.animation.*}: where the Remotion render service
 * ({@code animation-renderer/} in this repo) listens. Blank URL means no
 * MP4 is rendered - the frontend plays the storyboard live instead.
 */
@ConfigurationProperties(prefix = "app.animation")
public class AnimationRendererProperties {

    private String rendererUrl = "";
    private long renderTimeoutMs = 300_000L;

    public String getRendererUrl() { return rendererUrl; }
    public void setRendererUrl(String v) { this.rendererUrl = v; }
    public long getRenderTimeoutMs() { return renderTimeoutMs; }
    public void setRenderTimeoutMs(long v) { this.renderTimeoutMs = v; }

    public boolean isEnabled() {
        return rendererUrl != null && !rendererUrl.isBlank();
    }
}
