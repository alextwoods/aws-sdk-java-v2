package software.amazon.awssdk.benchmark.smithyjava.s3.client;

import java.util.Objects;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadBucketInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadBucketOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadObjectInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadObjectOutput;
import software.amazon.smithy.java.client.waiters.Waiter;
import software.amazon.smithy.java.client.waiters.backoff.BackoffStrategy;
import software.amazon.smithy.java.client.waiters.matching.Matcher;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Waiters for the {@link S3Client} client.
 */
@SmithyGenerated
public record S3Waiter(S3Client client) {
    public S3Waiter {
        Objects.requireNonNull(client, "client cannot be null");
    }

    public Waiter<HeadObjectInput, HeadObjectOutput> objectExists() {
        return Waiter.<HeadObjectInput, HeadObjectOutput>builder(client::headObject)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 5000L))
            .success(Matcher.success(true))
            .retry(Matcher.errorType("NotFound"))
            .build();
    }

    public Waiter<HeadObjectInput, HeadObjectOutput> objectNotExists() {
        return Waiter.<HeadObjectInput, HeadObjectOutput>builder(client::headObject)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 5000L))
            .success(Matcher.errorType("NotFound"))
            .build();
    }

    public Waiter<HeadBucketInput, HeadBucketOutput> bucketExists() {
        return Waiter.<HeadBucketInput, HeadBucketOutput>builder(client::headBucket)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 5000L))
            .success(Matcher.success(true))
            .retry(Matcher.errorType("NotFound"))
            .build();
    }

    public Waiter<HeadBucketInput, HeadBucketOutput> bucketNotExists() {
        return Waiter.<HeadBucketInput, HeadBucketOutput>builder(client::headBucket)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 5000L))
            .success(Matcher.errorType("NotFound"))
            .build();
    }

}
