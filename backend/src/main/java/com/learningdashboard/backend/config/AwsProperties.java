package com.learningdashboard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.aws.*}. Deliberately has no credential fields —
 * credentials always come from the AWS SDK v2 default provider chain
 * (ECS task role in production, environment/profile locally). See
 * {@link AwsClientConfig}.
 */
@ConfigurationProperties(prefix = "app.aws")
public class AwsProperties {

    private String region = "us-east-1";
    private Sqs sqs = new Sqs();
    private S3 s3 = new S3();

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Sqs getSqs() { return sqs; }
    public void setSqs(Sqs sqs) { this.sqs = sqs; }
    public S3 getS3() { return s3; }
    public void setS3(S3 s3) { this.s3 = s3; }

    public static class Sqs {
        private String generationQueueUrl;
        private String generationDlqUrl;
        private int waitTimeSeconds = 20;
        private int visibilityTimeoutSeconds = 120;
        private int maxMessagesPerPoll = 5;

        public String getGenerationQueueUrl() { return generationQueueUrl; }
        public void setGenerationQueueUrl(String v) { this.generationQueueUrl = v; }
        public String getGenerationDlqUrl() { return generationDlqUrl; }
        public void setGenerationDlqUrl(String v) { this.generationDlqUrl = v; }
        public int getWaitTimeSeconds() { return waitTimeSeconds; }
        public void setWaitTimeSeconds(int v) { this.waitTimeSeconds = v; }
        public int getVisibilityTimeoutSeconds() { return visibilityTimeoutSeconds; }
        public void setVisibilityTimeoutSeconds(int v) { this.visibilityTimeoutSeconds = v; }
        public int getMaxMessagesPerPoll() { return maxMessagesPerPoll; }
        public void setMaxMessagesPerPoll(int v) { this.maxMessagesPerPoll = v; }
    }

    public static class S3 {
        private String artifactsBucket;
        private int presignTtlMinutes = 60;

        public String getArtifactsBucket() { return artifactsBucket; }
        public void setArtifactsBucket(String v) { this.artifactsBucket = v; }
        public int getPresignTtlMinutes() { return presignTtlMinutes; }
        public void setPresignTtlMinutes(int v) { this.presignTtlMinutes = v; }
    }
}
