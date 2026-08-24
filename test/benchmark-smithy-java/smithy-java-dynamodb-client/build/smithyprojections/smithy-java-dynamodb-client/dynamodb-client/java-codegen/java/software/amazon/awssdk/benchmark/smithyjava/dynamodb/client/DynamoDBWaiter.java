package software.amazon.awssdk.benchmark.smithyjava.dynamodb.client;

import java.util.Objects;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContributorInsightsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContributorInsightsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeExportInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeExportOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeImportInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeImportOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableOutput;
import software.amazon.smithy.java.client.waiters.Waiter;
import software.amazon.smithy.java.client.waiters.backoff.BackoffStrategy;
import software.amazon.smithy.java.client.waiters.jmespath.Comparator;
import software.amazon.smithy.java.client.waiters.jmespath.JMESPathPredicate;
import software.amazon.smithy.java.client.waiters.matching.Matcher;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Waiters for the {@link DynamoDBClient} client.
 */
@SmithyGenerated
public record DynamoDBWaiter(DynamoDBClient client) {
    public DynamoDBWaiter {
        Objects.requireNonNull(client, "client cannot be null");
    }

    public Waiter<DescribeKinesisStreamingDestinationInput, DescribeKinesisStreamingDestinationOutput> kinesisStreamingDestinationActive() {
        return Waiter.<DescribeKinesisStreamingDestinationInput, DescribeKinesisStreamingDestinationOutput>builder(client::describeKinesisStreamingDestination)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 20000L))
            .success(Matcher.output(new JMESPathPredicate("KinesisDataStreamDestinations[].DestinationStatus", "ACTIVE", Comparator.ANY_STRING_EQUALS)))
            .failure(Matcher.output(new JMESPathPredicate("length(KinesisDataStreamDestinations) > `0`  && length(KinesisDataStreamDestinations[?DestinationStatus == 'DISABLED' || DestinationStatus == 'ENABLE_FAILED']) ==  length(KinesisDataStreamDestinations)", "true", Comparator.BOOLEAN_EQUALS)))
            .build();
    }

    public Waiter<DescribeTableInput, DescribeTableOutput> tableExists() {
        return Waiter.<DescribeTableInput, DescribeTableOutput>builder(client::describeTable)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 20000L))
            .success(Matcher.output(new JMESPathPredicate("Table.TableStatus", "ACTIVE", Comparator.STRING_EQUALS)))
            .retry(Matcher.errorType("ResourceNotFoundException"))
            .build();
    }

    public Waiter<DescribeTableInput, DescribeTableOutput> tableNotExists() {
        return Waiter.<DescribeTableInput, DescribeTableOutput>builder(client::describeTable)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 20000L))
            .success(Matcher.errorType("ResourceNotFoundException"))
            .build();
    }

    public Waiter<DescribeContributorInsightsInput, DescribeContributorInsightsOutput> contributorInsightsEnabled() {
        return Waiter.<DescribeContributorInsightsInput, DescribeContributorInsightsOutput>builder(client::describeContributorInsights)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 20000L))
            .success(Matcher.output(new JMESPathPredicate("ContributorInsightsStatus", "ENABLED", Comparator.STRING_EQUALS)))
            .failure(Matcher.output(new JMESPathPredicate("ContributorInsightsStatus", "FAILED", Comparator.STRING_EQUALS)))
            .build();
    }

    public Waiter<DescribeExportInput, DescribeExportOutput> exportCompleted() {
        return Waiter.<DescribeExportInput, DescribeExportOutput>builder(client::describeExport)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 20000L))
            .success(Matcher.output(new JMESPathPredicate("ExportDescription.ExportStatus", "COMPLETED", Comparator.STRING_EQUALS)))
            .failure(Matcher.output(new JMESPathPredicate("ExportDescription.ExportStatus", "FAILED", Comparator.STRING_EQUALS)))
            .build();
    }

    public Waiter<DescribeImportInput, DescribeImportOutput> importCompleted() {
        return Waiter.<DescribeImportInput, DescribeImportOutput>builder(client::describeImport)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 20000L))
            .success(Matcher.output(new JMESPathPredicate("ImportTableDescription.ImportStatus", "COMPLETED", Comparator.STRING_EQUALS)))
            .failure(Matcher.output(new JMESPathPredicate("ImportTableDescription.ImportStatus", "CANCELLED", Comparator.STRING_EQUALS)))
            .build();
    }

}
