package com.learningdashboard.backend.generation.job;

import com.learningdashboard.backend.config.AwsProperties;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * SQS-backed implementation. The message body is just the job id — the
 * worker always re-reads the job row from the database rather than trusting
 * message content as the source of truth, so a stale/duplicate message
 * (SQS is at-least-once delivery) is harmless: it re-processes the same
 * row idempotently (see {@link GenerationJobWorker}).
 *
 * <p>A dead-letter queue ({@code GENERATION_DLQ_URL}) should be
 * configured on the SQS queue itself (redrive policy, maxReceiveCount ~5)
 * — that's an SQS queue setting, not application code; see infra/.
 */
@Component
public class SqsJobQueue implements JobQueue {

    private static final Logger log = LoggerFactory.getLogger(SqsJobQueue.class);

    private final SqsClient sqsClient;
    private final AwsProperties.Sqs sqsProperties;

    public SqsJobQueue(SqsClient sqsClient, AwsProperties awsProperties) {
        this.sqsClient = sqsClient;
        this.sqsProperties = awsProperties.getSqs();
    }

    @Override
    public void enqueue(UUID jobId) {
        requireQueueConfigured();
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsProperties.getGenerationQueueUrl())
                .messageBody(jobId.toString())
                .build());
    }

    @Override
    public List<QueuedJob> poll() {
        requireQueueConfigured();
        var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.getGenerationQueueUrl())
                .maxNumberOfMessages(sqsProperties.getMaxMessagesPerPoll())
                .waitTimeSeconds(sqsProperties.getWaitTimeSeconds())
                .visibilityTimeout(sqsProperties.getVisibilityTimeoutSeconds())
                .build());

        return response.messages().stream()
                .map(this::toQueuedJob)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public void acknowledge(QueuedJob queuedJob) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(sqsProperties.getGenerationQueueUrl())
                .receiptHandle(queuedJob.receiptHandle())
                .build());
    }

    @Override
    public void release(QueuedJob queuedJob) {
        // Make it immediately visible again for another worker attempt,
        // instead of waiting out the full visibility timeout.
        sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                .queueUrl(sqsProperties.getGenerationQueueUrl())
                .receiptHandle(queuedJob.receiptHandle())
                .visibilityTimeout(0)
                .build());
    }

    private QueuedJob toQueuedJob(Message message) {
        try {
            return new QueuedJob(UUID.fromString(message.body()), message.receiptHandle());
        } catch (IllegalArgumentException e) {
            log.error("Discarding malformed SQS message body (not a UUID): {}", message.messageId());
            return null;
        }
    }

    private void requireQueueConfigured() {
        if (sqsProperties.getGenerationQueueUrl() == null || sqsProperties.getGenerationQueueUrl().isBlank()) {
            throw new IllegalStateException("GENERATION_QUEUE_URL is not configured.");
        }
    }
}
