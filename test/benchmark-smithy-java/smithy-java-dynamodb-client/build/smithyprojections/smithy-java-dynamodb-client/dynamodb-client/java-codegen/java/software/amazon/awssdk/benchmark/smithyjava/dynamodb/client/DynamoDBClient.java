package software.amazon.awssdk.benchmark.smithyjava.dynamodb.client;

import java.io.IOException;
import java.util.List;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BackupInUseException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BackupNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchExecuteStatementInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchExecuteStatementOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchGetItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchGetItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchWriteItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchWriteItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ContinuousBackupsUnavailableException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateBackupInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateBackupOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateGlobalTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateGlobalTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteBackupInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteBackupOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteResourcePolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteResourcePolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeBackupInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeBackupOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContinuousBackupsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContinuousBackupsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContributorInsightsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContributorInsightsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeEndpointsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeEndpointsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeExportInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeExportOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableSettingsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableSettingsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeImportInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeImportOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeLimitsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeLimitsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableReplicaAutoScalingInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableReplicaAutoScalingOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTimeToLiveInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTimeToLiveOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DisableKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DisableKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DuplicateItemException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DynamoDBApiService;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.EnableKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.EnableKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteStatementInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteStatementOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteTransactionInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteTransactionOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExportConflictException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExportNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExportTableToPointInTimeInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExportTableToPointInTimeOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetResourcePolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetResourcePolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GlobalTableAlreadyExistsException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GlobalTableNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.IdempotentParameterMismatchException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ImportConflictException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ImportNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ImportTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ImportTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.IndexNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.InternalServerError;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.InvalidEndpointException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.InvalidExportTimeException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.InvalidRestoreTimeException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ItemCollectionSizeLimitExceededException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.LimitExceededException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListBackupsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListBackupsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListContributorInsights;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListContributorInsightsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListContributorInsightsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListExports;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListExportsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListExportsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListGlobalTablesInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListGlobalTablesOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListImports;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListImportsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListImportsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTables;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTablesInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTablesOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTagsOfResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTagsOfResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PointInTimeRecoveryUnavailableException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PolicyNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutResourcePolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutResourcePolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.Query;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.QueryInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.QueryOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ReplicaAlreadyExistsException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ReplicaNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ReplicatedWriteConflictException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RequestLimitExceeded;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableFromBackupInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableFromBackupOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableToPointInTimeInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableToPointInTimeOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.Scan;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ScanInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ScanOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.SearchVectorsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.SearchVectorsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TableAlreadyExistsException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TableInUseException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TableNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TagResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TagResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ThrottlingException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactGetItemsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactGetItemsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactWriteItemsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactWriteItemsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactionConflictException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactionInProgressException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UntagResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UntagResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContinuousBackupsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContinuousBackupsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContributorInsightsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContributorInsightsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableSettingsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableSettingsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableReplicaAutoScalingInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableReplicaAutoScalingOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTimeToLiveInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTimeToLiveOutput;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4AuthScheme;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.aws.client.core.AwsCredentialChainPlugin;
import software.amazon.smithy.java.client.core.Client;
import software.amazon.smithy.java.client.core.ClientConfig;
import software.amazon.smithy.java.client.core.ClientPlugin;
import software.amazon.smithy.java.client.core.ProtocolSettings;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.client.core.auth.scheme.AuthSchemeFactory;
import software.amazon.smithy.java.client.core.pagination.Paginator;
import software.amazon.smithy.java.client.http.JavaHttpClientTransport;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.rulesengine.RulesEngineBuilder;
import software.amazon.smithy.java.rulesengine.RulesEngineSettings;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Amazon DynamoDB
 *
 * <p>Amazon DynamoDB is a fully managed NoSQL database service that provides fast and predictable performance with
 * seamless scalability. DynamoDB lets you offload the administrative burdens of operating and scaling a distributed
 * database, so that you don't have to worry about hardware provisioning, setup and configuration, replication, software
 * patching, or cluster scaling.
 *
 * <p>With DynamoDB, you can create database tables that can store and retrieve any amount of data, and serve any level
 * of request traffic. You can scale up or scale down your tables' throughput capacity without downtime or performance
 * degradation, and use the Amazon Web Services Management Console to monitor resource utilization and performance
 * metrics.
 *
 * <p>DynamoDB automatically spreads the data and traffic for your tables over a sufficient number of servers to handle
 * your throughput and storage requirements, while maintaining consistent and fast performance. All of your data is
 * stored on solid state disks (SSDs) and automatically replicated across multiple Availability Zones in an Amazon Web
 * Services Region, providing built-in high availability and data durability.
 */
@SmithyGenerated
public interface DynamoDBClient {

    /**
     * This operation allows you to perform batch reads or writes on data stored in DynamoDB, using PartiQL. Each read
     * statement in a <code>BatchExecuteStatement</code> must specify an equality condition on all key attributes. This
     * enforces that each <code>SELECT</code> statement in a batch returns at most a single item. For more information,
     * see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.multiplestatements.batching.html">Running batch operations with PartiQL for DynamoDB </a>.
     *
     * <p>The entire batch must consist of either read statements or write statements, you cannot mix both in one batch.
     *
     * <p>A HTTP 200 response does not mean that all statements in the BatchExecuteStatement succeeded. Error details
     * for individual statements can be found under the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_BatchStatementResponse.html#DDB-Type-BatchStatementResponse-Error">Error</a> field of the <code>BatchStatementResponse</code>
     * for each statement.
     *
     * @throws InternalServerError
     * @throws RequestLimitExceeded
     * @throws ThrottlingException
     */
    default BatchExecuteStatementOutput batchExecuteStatement(BatchExecuteStatementInput input) {
        return batchExecuteStatement(input, null);
    }

    /**
     * This operation allows you to perform batch reads or writes on data stored in DynamoDB, using PartiQL. Each read
     * statement in a <code>BatchExecuteStatement</code> must specify an equality condition on all key attributes. This
     * enforces that each <code>SELECT</code> statement in a batch returns at most a single item. For more information,
     * see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.multiplestatements.batching.html">Running batch operations with PartiQL for DynamoDB </a>.
     *
     * <p>The entire batch must consist of either read statements or write statements, you cannot mix both in one batch.
     *
     * <p>A HTTP 200 response does not mean that all statements in the BatchExecuteStatement succeeded. Error details
     * for individual statements can be found under the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_BatchStatementResponse.html#DDB-Type-BatchStatementResponse-Error">Error</a> field of the <code>BatchStatementResponse</code>
     * for each statement.
     *
     * @throws InternalServerError
     * @throws RequestLimitExceeded
     * @throws ThrottlingException
     */
    BatchExecuteStatementOutput batchExecuteStatement(BatchExecuteStatementInput input, RequestOverrideConfig overrideConfig);

    /**
     * The <code>BatchGetItem</code> operation returns the attributes of one or more items from one or more tables. You
     * identify requested items by primary key.
     *
     * <p>A single operation can retrieve up to 16 MB of data, which can contain as many as 100 items. <code>
     * BatchGetItem</code> returns a partial result if the response size limit is exceeded, the table's provisioned
     * throughput is exceeded, more than 1MB per partition is requested, or an internal processing failure occurs. If a
     * partial result is returned, the operation returns a value for <code>UnprocessedKeys</code>. You can use this
     * value to retry the operation starting with the next item to get.
     *
     * <p>If you request more than 100 items, <code>BatchGetItem</code> returns a <code>ValidationException</code> with
     * the message "Too many items requested for the BatchGetItem call."
     *
     * <p>For example, if you ask to retrieve 100 items, but each individual item is 300 KB in size, the system returns
     * 52 items (so as not to exceed the 16 MB limit). It also returns an appropriate <code>UnprocessedKeys</code> value
     * so you can get the next page of results. If desired, your application can include its own logic to assemble the
     * pages of results into one dataset.
     *
     * <p>If <i>none</i> of the items can be processed due to insufficient provisioned throughput on all of the tables
     * in the request, then <code>BatchGetItem</code> returns a <code>ProvisionedThroughputExceededException</code>. If <i>
     * at least one</i> of the items is successfully processed, then <code>BatchGetItem</code> completes successfully,
     * while returning the keys of the unread items in <code>UnprocessedKeys</code>.
     *
     * <p>If DynamoDB returns any unprocessed items, you should retry the batch operation on those items. However, <i>we
     * strongly recommend that you use an exponential backoff algorithm</i>. If you retry the batch operation
     * immediately, the underlying read or write requests can still fail due to throttling on the individual tables. If
     * you delay the batch operation using exponential backoff, the individual requests in the batch are much more
     * likely to succeed.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ErrorHandling.html#BatchOperations">Batch Operations and Error Handling</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     *
     * <p>By default, <code>BatchGetItem</code> performs eventually consistent reads on every table in the request. If
     * you want strongly consistent reads instead, you can set <code>ConsistentRead</code> to <code>true</code> for any
     * or all tables.
     *
     * <p>In order to minimize response latency, <code>BatchGetItem</code> may retrieve items in parallel.
     *
     * <p>When designing your application, keep in mind that DynamoDB does not return items in any particular order. To
     * help parse the response by item, include the primary key values for the items in your request in the <code>
     * ProjectionExpression</code> parameter.
     *
     * <p>If a requested item does not exist, it is not returned in the result. Requests for nonexistent items consume
     * the minimum read capacity units according to the type of read. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#CapacityUnitCalculations">Working with Tables</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * <p><code>BatchGetItem</code> will result in a <code>ValidationException</code> if the same key is specified
     * multiple times.
     *
     * <h4>Examples</h4>
     * <h5>To retrieve multiple items from a table</h5>
     *
     * <p>This example reads multiple items from the Music table using a batch of three GetItem requests.  Only the AlbumTitle attribute is returned.{@snippet :
     * var input = BatchGetItemInput.builder()
     *                 .requestItems(Map.of("Music", KeysAndAttributes.builder()
     *                                                     .keys(List.of(
     *                                                               Map.of(
     *                                                                   "Artist", AttributeValue.SMember("No One You Know"),
     *                                                                   "SongTitle", AttributeValue.SMember("Call Me Today")
     *                                                               ),
     *                                                               Map.of(
     *                                                                   "Artist", AttributeValue.SMember("Acme Band"),
     *                                                                   "SongTitle", AttributeValue.SMember("Happy Day")
     *                                                               ),
     *                                                               Map.of(
     *                                                                   "Artist", AttributeValue.SMember("No One You Know"),
     *                                                                   "SongTitle", AttributeValue.SMember("Scared of My Shadow")
     *                                                               )
     *                                                           )).projectionExpression("AlbumTitle")
     *                                                     .build()))
     *                 .build();
     *
     * var result = client.batchGetItem(input);
     * result.equals(BatchGetItemOutput.builder()
     *                   .responses(Map.of("Music", List.of(
     *                                                    Map.of("AlbumTitle", AttributeValue.SMember("Somewhat Famous")),
     *                                                    Map.of("AlbumTitle", AttributeValue.SMember("Blue Sky Blues")),
     *                                                    Map.of("AlbumTitle", AttributeValue.SMember("Louder Than Ever"))
     *                                                )))
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    default BatchGetItemOutput batchGetItem(BatchGetItemInput input) {
        return batchGetItem(input, null);
    }

    /**
     * The <code>BatchGetItem</code> operation returns the attributes of one or more items from one or more tables. You
     * identify requested items by primary key.
     *
     * <p>A single operation can retrieve up to 16 MB of data, which can contain as many as 100 items. <code>
     * BatchGetItem</code> returns a partial result if the response size limit is exceeded, the table's provisioned
     * throughput is exceeded, more than 1MB per partition is requested, or an internal processing failure occurs. If a
     * partial result is returned, the operation returns a value for <code>UnprocessedKeys</code>. You can use this
     * value to retry the operation starting with the next item to get.
     *
     * <p>If you request more than 100 items, <code>BatchGetItem</code> returns a <code>ValidationException</code> with
     * the message "Too many items requested for the BatchGetItem call."
     *
     * <p>For example, if you ask to retrieve 100 items, but each individual item is 300 KB in size, the system returns
     * 52 items (so as not to exceed the 16 MB limit). It also returns an appropriate <code>UnprocessedKeys</code> value
     * so you can get the next page of results. If desired, your application can include its own logic to assemble the
     * pages of results into one dataset.
     *
     * <p>If <i>none</i> of the items can be processed due to insufficient provisioned throughput on all of the tables
     * in the request, then <code>BatchGetItem</code> returns a <code>ProvisionedThroughputExceededException</code>. If <i>
     * at least one</i> of the items is successfully processed, then <code>BatchGetItem</code> completes successfully,
     * while returning the keys of the unread items in <code>UnprocessedKeys</code>.
     *
     * <p>If DynamoDB returns any unprocessed items, you should retry the batch operation on those items. However, <i>we
     * strongly recommend that you use an exponential backoff algorithm</i>. If you retry the batch operation
     * immediately, the underlying read or write requests can still fail due to throttling on the individual tables. If
     * you delay the batch operation using exponential backoff, the individual requests in the batch are much more
     * likely to succeed.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ErrorHandling.html#BatchOperations">Batch Operations and Error Handling</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     *
     * <p>By default, <code>BatchGetItem</code> performs eventually consistent reads on every table in the request. If
     * you want strongly consistent reads instead, you can set <code>ConsistentRead</code> to <code>true</code> for any
     * or all tables.
     *
     * <p>In order to minimize response latency, <code>BatchGetItem</code> may retrieve items in parallel.
     *
     * <p>When designing your application, keep in mind that DynamoDB does not return items in any particular order. To
     * help parse the response by item, include the primary key values for the items in your request in the <code>
     * ProjectionExpression</code> parameter.
     *
     * <p>If a requested item does not exist, it is not returned in the result. Requests for nonexistent items consume
     * the minimum read capacity units according to the type of read. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#CapacityUnitCalculations">Working with Tables</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * <p><code>BatchGetItem</code> will result in a <code>ValidationException</code> if the same key is specified
     * multiple times.
     *
     * <h4>Examples</h4>
     * <h5>To retrieve multiple items from a table</h5>
     *
     * <p>This example reads multiple items from the Music table using a batch of three GetItem requests.  Only the AlbumTitle attribute is returned.{@snippet :
     * var input = BatchGetItemInput.builder()
     *                 .requestItems(Map.of("Music", KeysAndAttributes.builder()
     *                                                     .keys(List.of(
     *                                                               Map.of(
     *                                                                   "Artist", AttributeValue.SMember("No One You Know"),
     *                                                                   "SongTitle", AttributeValue.SMember("Call Me Today")
     *                                                               ),
     *                                                               Map.of(
     *                                                                   "Artist", AttributeValue.SMember("Acme Band"),
     *                                                                   "SongTitle", AttributeValue.SMember("Happy Day")
     *                                                               ),
     *                                                               Map.of(
     *                                                                   "Artist", AttributeValue.SMember("No One You Know"),
     *                                                                   "SongTitle", AttributeValue.SMember("Scared of My Shadow")
     *                                                               )
     *                                                           )).projectionExpression("AlbumTitle")
     *                                                     .build()))
     *                 .build();
     *
     * var result = client.batchGetItem(input);
     * result.equals(BatchGetItemOutput.builder()
     *                   .responses(Map.of("Music", List.of(
     *                                                    Map.of("AlbumTitle", AttributeValue.SMember("Somewhat Famous")),
     *                                                    Map.of("AlbumTitle", AttributeValue.SMember("Blue Sky Blues")),
     *                                                    Map.of("AlbumTitle", AttributeValue.SMember("Louder Than Ever"))
     *                                                )))
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    BatchGetItemOutput batchGetItem(BatchGetItemInput input, RequestOverrideConfig overrideConfig);

    /**
     * The <code>BatchWriteItem</code> operation puts or deletes multiple items in one or more tables. A single call to <code>
     * BatchWriteItem</code> can transmit up to 16MB of data over the network, consisting of up to 25 item put or delete
     * operations. While individual items can be up to 400 KB once stored, it's important to note that an item's
     * representation might be greater than 400KB while being sent in DynamoDB's JSON format for the API call. For more
     * details on this distinction, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html">Naming Rules and Data Types</a>.
     *
     * <p><code>BatchWriteItem</code> cannot update items. If you perform a <code>BatchWriteItem</code> operation on an
     * existing item, that item's values will be overwritten by the operation and it will appear like it was updated. To
     * update items, we recommend you use the <code>UpdateItem</code> action.
     *
     * <p>The individual <code>PutItem</code> and <code>DeleteItem</code> operations specified in <code>BatchWriteItem</code>
     * are atomic; however <code>BatchWriteItem</code> as a whole is not. If any requested operations fail because the
     * table's provisioned throughput is exceeded or an internal processing failure occurs, the failed operations are
     * returned in the <code>UnprocessedItems</code> response parameter. You can investigate and optionally resend the
     * requests. Typically, you would call <code>BatchWriteItem</code> in a loop. Each iteration would check for
     * unprocessed items and submit a new <code>BatchWriteItem</code> request with those unprocessed items until all
     * items have been processed.
     *
     * <p>If <code>BatchWriteItem</code> cannot process any items due to throttling (for example, insufficient
     * provisioned throughput on the tables in the request, or partition-level or account-level limits), it returns a <code>
     * ProvisionedThroughputExceededException</code> or a <code>ThrottlingException</code>. Both indicate that the
     * request was throttled; check the <code>ThrottlingReason</code> field in the returned exception for details.
     *
     * <p>If DynamoDB returns any unprocessed items, you should retry the batch operation on those items. However, <i>we
     * strongly recommend that you use an exponential backoff algorithm</i>. If you retry the batch operation
     * immediately, the underlying read or write requests can still fail due to throttling on the individual tables. If
     * you delay the batch operation using exponential backoff, the individual requests in the batch are much more
     * likely to succeed.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ErrorHandling.html#Programming.Errors.BatchOperations">Batch Operations and Error Handling</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     *
     * <p>With <code>BatchWriteItem</code>, you can efficiently write or delete large amounts of data, such as from
     * Amazon EMR, or copy data from another database into DynamoDB. In order to improve performance with these
     * large-scale operations, <code>BatchWriteItem</code> does not behave in the same way as individual <code>PutItem</code>
     * and <code>DeleteItem</code> calls would. For example, you cannot specify conditions on individual put and delete
     * requests, and <code>BatchWriteItem</code> does not return deleted items in the response.
     *
     * <p>If you use a programming language that supports concurrency, you can use threads to write items in parallel.
     * Your application must include the necessary logic to manage the threads. With languages that don't support
     * threading, you must update or delete the specified items one at a time. In both situations, <code>BatchWriteItem</code>
     * performs the specified put and delete operations in parallel, giving you the power of the thread pool approach
     * without having to introduce complexity into your application.
     *
     * <p>Parallel processing reduces latency, but each specified put and delete request consumes the same number of
     * write capacity units whether it is processed in parallel or not. Delete operations on nonexistent items consume
     * one write capacity unit.
     *
     * <p>If one or more of the following is true, DynamoDB rejects the entire batch write operation:
     *
     * <ul>
     *   <li>
     *     One or more tables specified in the <code>BatchWriteItem</code> request does not exist.
     *   </li>
     *   <li>
     *     Primary key attributes specified on an item in the request do not match those in the corresponding
     *     table's primary key schema.
     *   </li>
     *   <li>
     *     You try to perform multiple operations on the same item in the same <code>BatchWriteItem</code> request.
     *     For example, you cannot put and delete the same item in the same <code>BatchWriteItem</code> request.
     *   </li>
     *   <li>
     *      Your request contains at least two items with identical hash and range keys (which essentially is two
     *     put operations).
     *   </li>
     *   <li>
     *     There are more than 25 requests in the batch.
     *   </li>
     *   <li>
     *     Any individual item in a batch exceeds 400 KB.
     *   </li>
     *   <li>
     *     The total request size exceeds 16 MB.
     *   </li>
     *   <li>
     *     Any individual items with keys exceeding the key length limits. For a partition key, the limit is 2048
     *     bytes and for a sort key, the limit is 1024 bytes.
     *   </li>
     * </ul>
     *
     * <h4>Examples</h4>
     * <h5>To add multiple items to a table</h5>
     *
     * <p>This example adds three new items to the Music table using a batch of three PutItem requests.{@snippet :
     * var input = BatchWriteItemInput.builder()
     *                 .requestItems(Map.of("Music", List.of(
     *                                                     WriteRequest.builder()
     *                                                         .putRequest(PutRequest.builder()
     *                                                                         .item(Map.of(
     *                                                                                   "AlbumTitle", AttributeValue.SMember("Somewhat Famous"),
     *                                                                                   "SongTitle", AttributeValue.SMember("Call Me Today"),
     *                                                                                   "Artist", AttributeValue.SMember("No One You Know")
     *                                                                               ))
     *                                                                         .build())
     *                                                         .build()
     *                                                     ,
     *                                                     WriteRequest.builder()
     *                                                         .putRequest(PutRequest.builder()
     *                                                                         .item(Map.of(
     *                                                                                   "AlbumTitle", AttributeValue.SMember("Songs About Life"),
     *                                                                                   "SongTitle", AttributeValue.SMember("Happy Day"),
     *                                                                                   "Artist", AttributeValue.SMember("Acme Band")
     *                                                                               ))
     *                                                                         .build())
     *                                                         .build()
     *                                                     ,
     *                                                     WriteRequest.builder()
     *                                                         .putRequest(PutRequest.builder()
     *                                                                         .item(Map.of(
     *                                                                                   "AlbumTitle", AttributeValue.SMember("Blue Sky Blues"),
     *                                                                                   "SongTitle", AttributeValue.SMember("Scared of My Shadow"),
     *                                                                                   "Artist", AttributeValue.SMember("No One You Know")
     *                                                                               ))
     *                                                                         .build())
     *                                                         .build()
     *                                                 )))
     *                 .build();
     *
     * var result = client.batchWriteItem(input);
     * result.equals();
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws ReplicatedWriteConflictException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    default BatchWriteItemOutput batchWriteItem(BatchWriteItemInput input) {
        return batchWriteItem(input, null);
    }

    /**
     * The <code>BatchWriteItem</code> operation puts or deletes multiple items in one or more tables. A single call to <code>
     * BatchWriteItem</code> can transmit up to 16MB of data over the network, consisting of up to 25 item put or delete
     * operations. While individual items can be up to 400 KB once stored, it's important to note that an item's
     * representation might be greater than 400KB while being sent in DynamoDB's JSON format for the API call. For more
     * details on this distinction, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html">Naming Rules and Data Types</a>.
     *
     * <p><code>BatchWriteItem</code> cannot update items. If you perform a <code>BatchWriteItem</code> operation on an
     * existing item, that item's values will be overwritten by the operation and it will appear like it was updated. To
     * update items, we recommend you use the <code>UpdateItem</code> action.
     *
     * <p>The individual <code>PutItem</code> and <code>DeleteItem</code> operations specified in <code>BatchWriteItem</code>
     * are atomic; however <code>BatchWriteItem</code> as a whole is not. If any requested operations fail because the
     * table's provisioned throughput is exceeded or an internal processing failure occurs, the failed operations are
     * returned in the <code>UnprocessedItems</code> response parameter. You can investigate and optionally resend the
     * requests. Typically, you would call <code>BatchWriteItem</code> in a loop. Each iteration would check for
     * unprocessed items and submit a new <code>BatchWriteItem</code> request with those unprocessed items until all
     * items have been processed.
     *
     * <p>If <code>BatchWriteItem</code> cannot process any items due to throttling (for example, insufficient
     * provisioned throughput on the tables in the request, or partition-level or account-level limits), it returns a <code>
     * ProvisionedThroughputExceededException</code> or a <code>ThrottlingException</code>. Both indicate that the
     * request was throttled; check the <code>ThrottlingReason</code> field in the returned exception for details.
     *
     * <p>If DynamoDB returns any unprocessed items, you should retry the batch operation on those items. However, <i>we
     * strongly recommend that you use an exponential backoff algorithm</i>. If you retry the batch operation
     * immediately, the underlying read or write requests can still fail due to throttling on the individual tables. If
     * you delay the batch operation using exponential backoff, the individual requests in the batch are much more
     * likely to succeed.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ErrorHandling.html#Programming.Errors.BatchOperations">Batch Operations and Error Handling</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     *
     * <p>With <code>BatchWriteItem</code>, you can efficiently write or delete large amounts of data, such as from
     * Amazon EMR, or copy data from another database into DynamoDB. In order to improve performance with these
     * large-scale operations, <code>BatchWriteItem</code> does not behave in the same way as individual <code>PutItem</code>
     * and <code>DeleteItem</code> calls would. For example, you cannot specify conditions on individual put and delete
     * requests, and <code>BatchWriteItem</code> does not return deleted items in the response.
     *
     * <p>If you use a programming language that supports concurrency, you can use threads to write items in parallel.
     * Your application must include the necessary logic to manage the threads. With languages that don't support
     * threading, you must update or delete the specified items one at a time. In both situations, <code>BatchWriteItem</code>
     * performs the specified put and delete operations in parallel, giving you the power of the thread pool approach
     * without having to introduce complexity into your application.
     *
     * <p>Parallel processing reduces latency, but each specified put and delete request consumes the same number of
     * write capacity units whether it is processed in parallel or not. Delete operations on nonexistent items consume
     * one write capacity unit.
     *
     * <p>If one or more of the following is true, DynamoDB rejects the entire batch write operation:
     *
     * <ul>
     *   <li>
     *     One or more tables specified in the <code>BatchWriteItem</code> request does not exist.
     *   </li>
     *   <li>
     *     Primary key attributes specified on an item in the request do not match those in the corresponding
     *     table's primary key schema.
     *   </li>
     *   <li>
     *     You try to perform multiple operations on the same item in the same <code>BatchWriteItem</code> request.
     *     For example, you cannot put and delete the same item in the same <code>BatchWriteItem</code> request.
     *   </li>
     *   <li>
     *      Your request contains at least two items with identical hash and range keys (which essentially is two
     *     put operations).
     *   </li>
     *   <li>
     *     There are more than 25 requests in the batch.
     *   </li>
     *   <li>
     *     Any individual item in a batch exceeds 400 KB.
     *   </li>
     *   <li>
     *     The total request size exceeds 16 MB.
     *   </li>
     *   <li>
     *     Any individual items with keys exceeding the key length limits. For a partition key, the limit is 2048
     *     bytes and for a sort key, the limit is 1024 bytes.
     *   </li>
     * </ul>
     *
     * <h4>Examples</h4>
     * <h5>To add multiple items to a table</h5>
     *
     * <p>This example adds three new items to the Music table using a batch of three PutItem requests.{@snippet :
     * var input = BatchWriteItemInput.builder()
     *                 .requestItems(Map.of("Music", List.of(
     *                                                     WriteRequest.builder()
     *                                                         .putRequest(PutRequest.builder()
     *                                                                         .item(Map.of(
     *                                                                                   "AlbumTitle", AttributeValue.SMember("Somewhat Famous"),
     *                                                                                   "SongTitle", AttributeValue.SMember("Call Me Today"),
     *                                                                                   "Artist", AttributeValue.SMember("No One You Know")
     *                                                                               ))
     *                                                                         .build())
     *                                                         .build()
     *                                                     ,
     *                                                     WriteRequest.builder()
     *                                                         .putRequest(PutRequest.builder()
     *                                                                         .item(Map.of(
     *                                                                                   "AlbumTitle", AttributeValue.SMember("Songs About Life"),
     *                                                                                   "SongTitle", AttributeValue.SMember("Happy Day"),
     *                                                                                   "Artist", AttributeValue.SMember("Acme Band")
     *                                                                               ))
     *                                                                         .build())
     *                                                         .build()
     *                                                     ,
     *                                                     WriteRequest.builder()
     *                                                         .putRequest(PutRequest.builder()
     *                                                                         .item(Map.of(
     *                                                                                   "AlbumTitle", AttributeValue.SMember("Blue Sky Blues"),
     *                                                                                   "SongTitle", AttributeValue.SMember("Scared of My Shadow"),
     *                                                                                   "Artist", AttributeValue.SMember("No One You Know")
     *                                                                               ))
     *                                                                         .build())
     *                                                         .build()
     *                                                 )))
     *                 .build();
     *
     * var result = client.batchWriteItem(input);
     * result.equals();
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws ReplicatedWriteConflictException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    BatchWriteItemOutput batchWriteItem(BatchWriteItemInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates a backup for an existing table.
     *
     * <p> Each time you create an on-demand backup, the entire table data is backed up. There is no limit to the number
     * of on-demand backups that can be taken.
     *
     * <p> When you create an on-demand backup, a time marker of the request is cataloged, and the backup is created
     * asynchronously, by applying all changes until the time of the request to the last full table snapshot. Backup
     * requests are processed instantaneously and become available for restore within minutes.
     *
     * <p>You can call <code>CreateBackup</code> at a maximum rate of 50 times per second.
     *
     * <p>All backups in DynamoDB work without consuming any provisioned throughput on the table.
     *
     * <p> If you submit a backup request on 2018-12-14 at 14:25:00, the backup is guaranteed to contain all data
     * committed to the table up to 14:24:00, and data committed after 14:26:00 will not be. The backup might contain
     * data modifications made between 14:24:00 and 14:26:00. On-demand backup does not support causal consistency.
     *
     * <p> Along with data, the following are also included on the backups:
     *
     * <ul>
     *   <li>
     *     Global secondary indexes (GSIs)
     *   </li>
     *   <li>
     *     Local secondary indexes (LSIs)
     *   </li>
     *   <li>
     *     Streams
     *   </li>
     *   <li>
     *     Provisioned read and write capacity
     *   </li>
     * </ul>
     *
     * @throws BackupInUseException
     * @throws ContinuousBackupsUnavailableException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws TableInUseException
     * @throws TableNotFoundException
     */
    default CreateBackupOutput createBackup(CreateBackupInput input) {
        return createBackup(input, null);
    }

    /**
     * Creates a backup for an existing table.
     *
     * <p> Each time you create an on-demand backup, the entire table data is backed up. There is no limit to the number
     * of on-demand backups that can be taken.
     *
     * <p> When you create an on-demand backup, a time marker of the request is cataloged, and the backup is created
     * asynchronously, by applying all changes until the time of the request to the last full table snapshot. Backup
     * requests are processed instantaneously and become available for restore within minutes.
     *
     * <p>You can call <code>CreateBackup</code> at a maximum rate of 50 times per second.
     *
     * <p>All backups in DynamoDB work without consuming any provisioned throughput on the table.
     *
     * <p> If you submit a backup request on 2018-12-14 at 14:25:00, the backup is guaranteed to contain all data
     * committed to the table up to 14:24:00, and data committed after 14:26:00 will not be. The backup might contain
     * data modifications made between 14:24:00 and 14:26:00. On-demand backup does not support causal consistency.
     *
     * <p> Along with data, the following are also included on the backups:
     *
     * <ul>
     *   <li>
     *     Global secondary indexes (GSIs)
     *   </li>
     *   <li>
     *     Local secondary indexes (LSIs)
     *   </li>
     *   <li>
     *     Streams
     *   </li>
     *   <li>
     *     Provisioned read and write capacity
     *   </li>
     * </ul>
     *
     * @throws BackupInUseException
     * @throws ContinuousBackupsUnavailableException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws TableInUseException
     * @throws TableNotFoundException
     */
    CreateBackupOutput createBackup(CreateBackupInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates a global table from an existing table. A global table creates a replication relationship between two or
     * more DynamoDB tables with the same table name in the provided Regions.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * <p>If you want to add a new replica table to a global table, each of the following conditions must be true:
     *
     * <ul>
     *   <li>
     *     The table must have the same primary key as all of the other replicas.
     *   </li>
     *   <li>
     *     The table must have the same name as all of the other replicas.
     *   </li>
     *   <li>
     *     The table must have DynamoDB Streams enabled, with the stream containing both the new and the old images
     *     of the item.
     *   </li>
     *   <li>
     *     None of the replica tables in the global table can contain any data.
     *   </li>
     * </ul>
     *
     * <p> If global secondary indexes are specified, then the following conditions must also be met:
     *
     * <ul>
     *   <li>
     *      The global secondary indexes must have the same name.
     *   </li>
     *   <li>
     *      The global secondary indexes must have the same hash key and sort key (if present).
     *   </li>
     * </ul>
     *
     * <p> If local secondary indexes are specified, then the following conditions must also be met:
     *
     * <ul>
     *   <li>
     *      The local secondary indexes must have the same name.
     *   </li>
     *   <li>
     *      The local secondary indexes must have the same hash key and sort key (if present).
     *   </li>
     * </ul>
     *
     * <p> Write capacity settings should be set consistently across your replica tables and secondary indexes. DynamoDB
     * strongly recommends enabling auto scaling to manage the write capacity settings for all of your global tables
     * replicas and indexes.
     *
     * <p> If you prefer to manage write capacity settings manually, you should provision equal replicated write
     * capacity units to your replica tables. You should also provision equal replicated write capacity units to
     * matching secondary indexes across your global table.
     *
     * @throws GlobalTableAlreadyExistsException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws TableNotFoundException
     */
    default CreateGlobalTableOutput createGlobalTable(CreateGlobalTableInput input) {
        return createGlobalTable(input, null);
    }

    /**
     * Creates a global table from an existing table. A global table creates a replication relationship between two or
     * more DynamoDB tables with the same table name in the provided Regions.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * <p>If you want to add a new replica table to a global table, each of the following conditions must be true:
     *
     * <ul>
     *   <li>
     *     The table must have the same primary key as all of the other replicas.
     *   </li>
     *   <li>
     *     The table must have the same name as all of the other replicas.
     *   </li>
     *   <li>
     *     The table must have DynamoDB Streams enabled, with the stream containing both the new and the old images
     *     of the item.
     *   </li>
     *   <li>
     *     None of the replica tables in the global table can contain any data.
     *   </li>
     * </ul>
     *
     * <p> If global secondary indexes are specified, then the following conditions must also be met:
     *
     * <ul>
     *   <li>
     *      The global secondary indexes must have the same name.
     *   </li>
     *   <li>
     *      The global secondary indexes must have the same hash key and sort key (if present).
     *   </li>
     * </ul>
     *
     * <p> If local secondary indexes are specified, then the following conditions must also be met:
     *
     * <ul>
     *   <li>
     *      The local secondary indexes must have the same name.
     *   </li>
     *   <li>
     *      The local secondary indexes must have the same hash key and sort key (if present).
     *   </li>
     * </ul>
     *
     * <p> Write capacity settings should be set consistently across your replica tables and secondary indexes. DynamoDB
     * strongly recommends enabling auto scaling to manage the write capacity settings for all of your global tables
     * replicas and indexes.
     *
     * <p> If you prefer to manage write capacity settings manually, you should provision equal replicated write
     * capacity units to your replica tables. You should also provision equal replicated write capacity units to
     * matching secondary indexes across your global table.
     *
     * @throws GlobalTableAlreadyExistsException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws TableNotFoundException
     */
    CreateGlobalTableOutput createGlobalTable(CreateGlobalTableInput input, RequestOverrideConfig overrideConfig);

    /**
     * The <code>CreateTable</code> operation adds a new table to your account. In an Amazon Web Services account, table
     * names must be unique within each Region. That is, you can have two tables with same name if you create the tables
     * in different Regions.
     *
     * <p><code>CreateTable</code> is an asynchronous operation. Upon receiving a <code>CreateTable</code> request,
     * DynamoDB immediately returns a response with a <code>TableStatus</code> of <code>CREATING</code>. After the table
     * is created, DynamoDB sets the <code>TableStatus</code> to <code>ACTIVE</code>. You can perform read and write
     * operations only on an <code>ACTIVE</code> table.
     *
     * <p>You can optionally define secondary indexes on the new table, as part of the <code>CreateTable</code>
     * operation. If you want to create multiple tables with secondary indexes on them, you must create the tables
     * sequentially. Only one table with secondary indexes can be in the <code>CREATING</code> state at any given time.
     *
     * <p>You can use the <code>DescribeTable</code> action to check the table status.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     */
    default CreateTableOutput createTable(CreateTableInput input) {
        return createTable(input, null);
    }

    /**
     * The <code>CreateTable</code> operation adds a new table to your account. In an Amazon Web Services account, table
     * names must be unique within each Region. That is, you can have two tables with same name if you create the tables
     * in different Regions.
     *
     * <p><code>CreateTable</code> is an asynchronous operation. Upon receiving a <code>CreateTable</code> request,
     * DynamoDB immediately returns a response with a <code>TableStatus</code> of <code>CREATING</code>. After the table
     * is created, DynamoDB sets the <code>TableStatus</code> to <code>ACTIVE</code>. You can perform read and write
     * operations only on an <code>ACTIVE</code> table.
     *
     * <p>You can optionally define secondary indexes on the new table, as part of the <code>CreateTable</code>
     * operation. If you want to create multiple tables with secondary indexes on them, you must create the tables
     * sequentially. Only one table with secondary indexes can be in the <code>CREATING</code> state at any given time.
     *
     * <p>You can use the <code>DescribeTable</code> action to check the table status.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     */
    CreateTableOutput createTable(CreateTableInput input, RequestOverrideConfig overrideConfig);

    /**
     * Deletes an existing backup of a table.
     *
     * <p>You can call <code>DeleteBackup</code> at a maximum rate of 10 times per second.
     *
     * @throws BackupInUseException
     * @throws BackupNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     */
    default DeleteBackupOutput deleteBackup(DeleteBackupInput input) {
        return deleteBackup(input, null);
    }

    /**
     * Deletes an existing backup of a table.
     *
     * <p>You can call <code>DeleteBackup</code> at a maximum rate of 10 times per second.
     *
     * @throws BackupInUseException
     * @throws BackupNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     */
    DeleteBackupOutput deleteBackup(DeleteBackupInput input, RequestOverrideConfig overrideConfig);

    /**
     * Deletes a single item in a table by primary key. You can perform a conditional delete operation that deletes the
     * item if it exists, or if it has an expected attribute value.
     *
     * <p>In addition to deleting an item, you can also return the item's attribute values in the same operation, using
     * the <code>ReturnValues</code> parameter.
     *
     * <p>Unless you specify conditions, the <code>DeleteItem</code> is an idempotent operation; running it multiple
     * times on the same item or attribute does <i>not</i> result in an error response.
     *
     * <p>Conditional deletes are useful for deleting items only if specific conditions are met. If those conditions are
     * met, DynamoDB performs the delete. Otherwise, the item is not deleted.
     *
     * <h4>Examples</h4>
     * <h5>To delete an item</h5>
     *
     * <p>This example deletes an item from the Music table.{@snippet :
     * var input = DeleteItemInput.builder()
     *                 .tableName("Music").key(Map.of(
     *                          "Artist", AttributeValue.SMember("No One You Know"),
     *                          "SongTitle", AttributeValue.SMember("Scared of My Shadow")
     *                      ))
     *                 .build();
     *
     * var result = client.deleteItem(input);
     * result.equals(DeleteItemOutput.builder()
     *                   .consumedCapacity(ConsumedCapacity.builder()
     *                                         .capacityUnits(1).tableName("Music")
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws ConditionalCheckFailedException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws ReplicatedWriteConflictException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionConflictException
     */
    default DeleteItemOutput deleteItem(DeleteItemInput input) {
        return deleteItem(input, null);
    }

    /**
     * Deletes a single item in a table by primary key. You can perform a conditional delete operation that deletes the
     * item if it exists, or if it has an expected attribute value.
     *
     * <p>In addition to deleting an item, you can also return the item's attribute values in the same operation, using
     * the <code>ReturnValues</code> parameter.
     *
     * <p>Unless you specify conditions, the <code>DeleteItem</code> is an idempotent operation; running it multiple
     * times on the same item or attribute does <i>not</i> result in an error response.
     *
     * <p>Conditional deletes are useful for deleting items only if specific conditions are met. If those conditions are
     * met, DynamoDB performs the delete. Otherwise, the item is not deleted.
     *
     * <h4>Examples</h4>
     * <h5>To delete an item</h5>
     *
     * <p>This example deletes an item from the Music table.{@snippet :
     * var input = DeleteItemInput.builder()
     *                 .tableName("Music").key(Map.of(
     *                          "Artist", AttributeValue.SMember("No One You Know"),
     *                          "SongTitle", AttributeValue.SMember("Scared of My Shadow")
     *                      ))
     *                 .build();
     *
     * var result = client.deleteItem(input);
     * result.equals(DeleteItemOutput.builder()
     *                   .consumedCapacity(ConsumedCapacity.builder()
     *                                         .capacityUnits(1).tableName("Music")
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws ConditionalCheckFailedException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws ReplicatedWriteConflictException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionConflictException
     */
    DeleteItemOutput deleteItem(DeleteItemInput input, RequestOverrideConfig overrideConfig);

    /**
     * Deletes the resource-based policy attached to the resource, which can be a table or stream.
     *
     * <p><code>DeleteResourcePolicy</code> is an idempotent operation; running it multiple times on the same resource <i>
     * doesn't</i> result in an error response, unless you specify an <code>ExpectedRevisionId</code>, which will then
     * return a <code>PolicyNotFoundException</code>.
     *
     * <p>To make sure that you don't inadvertently lock yourself out of your own resources, the root principal in your
     * Amazon Web Services account can perform <code>DeleteResourcePolicy</code> requests, even if your resource-based
     * policy explicitly denies the root principal's access.
     *
     * <p><code>DeleteResourcePolicy</code> is an asynchronous operation. If you issue a <code>GetResourcePolicy</code>
     * request immediately after running the <code>DeleteResourcePolicy</code> request, DynamoDB might still return the
     * deleted policy. This is because the policy for your resource might not have been deleted yet. Wait for a few
     * seconds, and then try the <code>GetResourcePolicy</code> request again.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws PolicyNotFoundException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default DeleteResourcePolicyOutput deleteResourcePolicy(DeleteResourcePolicyInput input) {
        return deleteResourcePolicy(input, null);
    }

    /**
     * Deletes the resource-based policy attached to the resource, which can be a table or stream.
     *
     * <p><code>DeleteResourcePolicy</code> is an idempotent operation; running it multiple times on the same resource <i>
     * doesn't</i> result in an error response, unless you specify an <code>ExpectedRevisionId</code>, which will then
     * return a <code>PolicyNotFoundException</code>.
     *
     * <p>To make sure that you don't inadvertently lock yourself out of your own resources, the root principal in your
     * Amazon Web Services account can perform <code>DeleteResourcePolicy</code> requests, even if your resource-based
     * policy explicitly denies the root principal's access.
     *
     * <p><code>DeleteResourcePolicy</code> is an asynchronous operation. If you issue a <code>GetResourcePolicy</code>
     * request immediately after running the <code>DeleteResourcePolicy</code> request, DynamoDB might still return the
     * deleted policy. This is because the policy for your resource might not have been deleted yet. Wait for a few
     * seconds, and then try the <code>GetResourcePolicy</code> request again.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws PolicyNotFoundException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    DeleteResourcePolicyOutput deleteResourcePolicy(DeleteResourcePolicyInput input, RequestOverrideConfig overrideConfig);

    /**
     * The <code>DeleteTable</code> operation deletes a table and all of its items. After a <code>DeleteTable</code>
     * request, the specified table is in the <code>DELETING</code> state until DynamoDB completes the deletion. If the
     * table is in the <code>ACTIVE</code> state, you can delete it. If a table is in <code>CREATING</code> or <code>
     * UPDATING</code> states, then DynamoDB returns a <code>ResourceInUseException</code>. If the specified table does
     * not exist, DynamoDB returns a <code>ResourceNotFoundException</code>. If table is already in the <code>DELETING</code>
     * state, no error is returned.
     *
     * <p>DynamoDB might continue to accept data read and write operations, such as <code>GetItem</code> and <code>
     * PutItem</code>, on a table in the <code>DELETING</code> state until the table deletion is complete. For the full
     * list of table states, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TableDescription.html#DDB-Type-TableDescription-TableStatus">TableStatus</a>.
     *
     * <p>When you delete a table, any indexes on that table are also deleted.
     *
     * <p>If you have DynamoDB Streams enabled on the table, then the corresponding stream on that table goes into the <code>
     * DISABLED</code> state, and the stream is automatically deleted after 24 hours.
     *
     * <p>Use the <code>DescribeTable</code> action to check the status of the table.
     *
     * <h4>Examples</h4>
     * <h5>To delete a table</h5>
     *
     * <p>This example deletes the Music table.{@snippet :
     * var input = DeleteTableInput.builder()
     *                 .tableName("Music")
     *                 .build();
     *
     * var result = client.deleteTable(input);
     * result.equals(DeleteTableOutput.builder()
     *                   .tableDescription(TableDescription.builder()
     *                                         .tableStatus(TableStatus.DELETING).tableSizeBytes(0).itemCount(0).tableName("Music").provisionedThroughput(ProvisionedThroughputDescription.builder()
     *                                                                    .numberOfDecreasesToday(1).writeCapacityUnits(5).readCapacityUnits(5)
     *                                                                    .build())
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default DeleteTableOutput deleteTable(DeleteTableInput input) {
        return deleteTable(input, null);
    }

    /**
     * The <code>DeleteTable</code> operation deletes a table and all of its items. After a <code>DeleteTable</code>
     * request, the specified table is in the <code>DELETING</code> state until DynamoDB completes the deletion. If the
     * table is in the <code>ACTIVE</code> state, you can delete it. If a table is in <code>CREATING</code> or <code>
     * UPDATING</code> states, then DynamoDB returns a <code>ResourceInUseException</code>. If the specified table does
     * not exist, DynamoDB returns a <code>ResourceNotFoundException</code>. If table is already in the <code>DELETING</code>
     * state, no error is returned.
     *
     * <p>DynamoDB might continue to accept data read and write operations, such as <code>GetItem</code> and <code>
     * PutItem</code>, on a table in the <code>DELETING</code> state until the table deletion is complete. For the full
     * list of table states, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TableDescription.html#DDB-Type-TableDescription-TableStatus">TableStatus</a>.
     *
     * <p>When you delete a table, any indexes on that table are also deleted.
     *
     * <p>If you have DynamoDB Streams enabled on the table, then the corresponding stream on that table goes into the <code>
     * DISABLED</code> state, and the stream is automatically deleted after 24 hours.
     *
     * <p>Use the <code>DescribeTable</code> action to check the status of the table.
     *
     * <h4>Examples</h4>
     * <h5>To delete a table</h5>
     *
     * <p>This example deletes the Music table.{@snippet :
     * var input = DeleteTableInput.builder()
     *                 .tableName("Music")
     *                 .build();
     *
     * var result = client.deleteTable(input);
     * result.equals(DeleteTableOutput.builder()
     *                   .tableDescription(TableDescription.builder()
     *                                         .tableStatus(TableStatus.DELETING).tableSizeBytes(0).itemCount(0).tableName("Music").provisionedThroughput(ProvisionedThroughputDescription.builder()
     *                                                                    .numberOfDecreasesToday(1).writeCapacityUnits(5).readCapacityUnits(5)
     *                                                                    .build())
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    DeleteTableOutput deleteTable(DeleteTableInput input, RequestOverrideConfig overrideConfig);

    /**
     * Describes an existing backup of a table.
     *
     * <p>You can call <code>DescribeBackup</code> at a maximum rate of 10 times per second.
     *
     * @throws BackupNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    default DescribeBackupOutput describeBackup(DescribeBackupInput input) {
        return describeBackup(input, null);
    }

    /**
     * Describes an existing backup of a table.
     *
     * <p>You can call <code>DescribeBackup</code> at a maximum rate of 10 times per second.
     *
     * @throws BackupNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    DescribeBackupOutput describeBackup(DescribeBackupInput input, RequestOverrideConfig overrideConfig);

    /**
     * Checks the status of continuous backups and point in time recovery on the specified table. Continuous backups are
     * <code>ENABLED</code> on all tables at table creation. If point in time recovery is enabled, <code>
     * PointInTimeRecoveryStatus</code> will be set to ENABLED.
     *
     * <p> After continuous backups and point in time recovery are enabled, you can restore to any point in time within <code>
     * EarliestRestorableDateTime</code> and <code>LatestRestorableDateTime</code>.
     *
     * <p><code>LatestRestorableDateTime</code> is typically 5 minutes before the current time. You can restore your
     * table to any point in time in the last 35 days. You can set the recovery period to any value between 1 and 35
     * days.
     *
     * <p>You can call <code>DescribeContinuousBackups</code> at a maximum rate of 10 times per second.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws TableNotFoundException
     */
    default DescribeContinuousBackupsOutput describeContinuousBackups(DescribeContinuousBackupsInput input) {
        return describeContinuousBackups(input, null);
    }

    /**
     * Checks the status of continuous backups and point in time recovery on the specified table. Continuous backups are
     * <code>ENABLED</code> on all tables at table creation. If point in time recovery is enabled, <code>
     * PointInTimeRecoveryStatus</code> will be set to ENABLED.
     *
     * <p> After continuous backups and point in time recovery are enabled, you can restore to any point in time within <code>
     * EarliestRestorableDateTime</code> and <code>LatestRestorableDateTime</code>.
     *
     * <p><code>LatestRestorableDateTime</code> is typically 5 minutes before the current time. You can restore your
     * table to any point in time in the last 35 days. You can set the recovery period to any value between 1 and 35
     * days.
     *
     * <p>You can call <code>DescribeContinuousBackups</code> at a maximum rate of 10 times per second.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws TableNotFoundException
     */
    DescribeContinuousBackupsOutput describeContinuousBackups(DescribeContinuousBackupsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns information about contributor insights for a given table or global secondary index.
     *
     * @throws InternalServerError
     * @throws ResourceNotFoundException
     */
    default DescribeContributorInsightsOutput describeContributorInsights(DescribeContributorInsightsInput input) {
        return describeContributorInsights(input, null);
    }

    /**
     * Returns information about contributor insights for a given table or global secondary index.
     *
     * @throws InternalServerError
     * @throws ResourceNotFoundException
     */
    DescribeContributorInsightsOutput describeContributorInsights(DescribeContributorInsightsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns the regional endpoint information. For more information on policy permissions, please see <a
     * href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/inter-network-traffic-privacy.html#inter-network-traffic-DescribeEndpoints">Internetwork
     * traffic privacy</a>.
     */
    default DescribeEndpointsOutput describeEndpoints(DescribeEndpointsInput input) {
        return describeEndpoints(input, null);
    }

    /**
     * Returns the regional endpoint information. For more information on policy permissions, please see <a
     * href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/inter-network-traffic-privacy.html#inter-network-traffic-DescribeEndpoints">Internetwork
     * traffic privacy</a>.
     */
    DescribeEndpointsOutput describeEndpoints(DescribeEndpointsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Describes an existing table export.
     *
     * @throws ExportNotFoundException
     * @throws InternalServerError
     * @throws LimitExceededException
     */
    default DescribeExportOutput describeExport(DescribeExportInput input) {
        return describeExport(input, null);
    }

    /**
     * Describes an existing table export.
     *
     * @throws ExportNotFoundException
     * @throws InternalServerError
     * @throws LimitExceededException
     */
    DescribeExportOutput describeExport(DescribeExportInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns information about the specified global table.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * @throws GlobalTableNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    default DescribeGlobalTableOutput describeGlobalTable(DescribeGlobalTableInput input) {
        return describeGlobalTable(input, null);
    }

    /**
     * Returns information about the specified global table.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * @throws GlobalTableNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    DescribeGlobalTableOutput describeGlobalTable(DescribeGlobalTableInput input, RequestOverrideConfig overrideConfig);

    /**
     * Describes Region-specific settings for a global table.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * @throws GlobalTableNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    default DescribeGlobalTableSettingsOutput describeGlobalTableSettings(DescribeGlobalTableSettingsInput input) {
        return describeGlobalTableSettings(input, null);
    }

    /**
     * Describes Region-specific settings for a global table.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * @throws GlobalTableNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    DescribeGlobalTableSettingsOutput describeGlobalTableSettings(DescribeGlobalTableSettingsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Represents the properties of the import.
     *
     * @throws ImportNotFoundException
     */
    default DescribeImportOutput describeImport(DescribeImportInput input) {
        return describeImport(input, null);
    }

    /**
     * Represents the properties of the import.
     *
     * @throws ImportNotFoundException
     */
    DescribeImportOutput describeImport(DescribeImportInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns information about the status of Kinesis streaming.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ResourceNotFoundException
     */
    default DescribeKinesisStreamingDestinationOutput describeKinesisStreamingDestination(DescribeKinesisStreamingDestinationInput input) {
        return describeKinesisStreamingDestination(input, null);
    }

    /**
     * Returns information about the status of Kinesis streaming.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ResourceNotFoundException
     */
    DescribeKinesisStreamingDestinationOutput describeKinesisStreamingDestination(DescribeKinesisStreamingDestinationInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns the current provisioned-capacity quotas for your Amazon Web Services account in a Region, both for the
     * Region as a whole and for any one DynamoDB table that you create there.
     *
     * <p>When you establish an Amazon Web Services account, the account has initial quotas on the maximum read capacity
     * units and write capacity units that you can provision across all of your DynamoDB tables in a given Region. Also,
     * there are per-table quotas that apply when you create a table there. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service,
     * Account, and Table Quotas</a> page in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * <p>Although you can increase these quotas by filing a case at <a href="https://console.aws.amazon.com/support/home#/">Amazon Web Services Support Center</a>,
     * obtaining the increase is not instantaneous. The <code>DescribeLimits</code> action lets you write code to
     * compare the capacity you are currently using to those quotas imposed by your account so that you have enough time
     * to apply for an increase before you hit a quota.
     *
     * <p>For example, you could use one of the Amazon Web Services SDKs to do the following:
     *
     * <ol>
     *   <li>
     *     Call <code>DescribeLimits</code> for a particular Region to obtain your current account quotas on
     *     provisioned capacity there.
     *   </li>
     *   <li>
     *     Create a variable to hold the aggregate read capacity units provisioned for all your tables in that
     *     Region, and one to hold the aggregate write capacity units. Zero them both.
     *   </li>
     *   <li>
     *     Call <code>ListTables</code> to obtain a list of all your DynamoDB tables.
     *   </li>
     *   <li>
     *     For each table name listed by <code>ListTables</code>, do the following:
     *
     *     <ul>
     *       <li>
     *         Call <code>DescribeTable</code> with the table name.
     *       </li>
     *       <li>
     *         Use the data returned by <code>DescribeTable</code> to add the read capacity units and write
     *         capacity units provisioned for the table itself to your variables.
     *       </li>
     *       <li>
     *         If the table has one or more global secondary indexes (GSIs), loop over these GSIs and add their
     *         provisioned capacity values to your variables as well.
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *     Report the account quotas for that Region returned by <code>DescribeLimits</code>, along with the total
     *     current provisioned capacity levels you have calculated.
     *   </li>
     * </ol>
     *
     * <p>This will let you see whether you are getting close to your account-level quotas.
     *
     * <p>The per-table quotas apply only when you are creating a new table. They restrict the sum of the provisioned
     * capacity of the new table itself and all its global secondary indexes.
     *
     * <p>For existing tables and their GSIs, DynamoDB doesn't let you increase provisioned capacity extremely rapidly,
     * but the only quota that applies is that the aggregate provisioned capacity over all your tables and GSIs cannot
     * exceed either of the per-account quotas.
     *
     * <p><code>DescribeLimits</code> should only be called periodically. You can expect throttling errors if you call
     * it more than once in a minute.
     *
     * <p>The <code>DescribeLimits</code> Request element has no content.
     *
     * <h4>Examples</h4>
     * <h5>To determine capacity limits per table and account, in the current AWS region</h5>
     *
     * <p>The following example returns the maximum read and write capacity units per table, and for the AWS account, in the current AWS region.{@snippet :
     * var input = DescribeLimitsInput.builder()
     *
     *                 .build();
     *
     * var result = client.describeLimits(input);
     * result.equals(DescribeLimitsOutput.builder()
     *                   .tableMaxWriteCapacityUnits(10000).tableMaxReadCapacityUnits(10000).accountMaxReadCapacityUnits(20000).accountMaxWriteCapacityUnits(20000)
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    default DescribeLimitsOutput describeLimits(DescribeLimitsInput input) {
        return describeLimits(input, null);
    }

    /**
     * Returns the current provisioned-capacity quotas for your Amazon Web Services account in a Region, both for the
     * Region as a whole and for any one DynamoDB table that you create there.
     *
     * <p>When you establish an Amazon Web Services account, the account has initial quotas on the maximum read capacity
     * units and write capacity units that you can provision across all of your DynamoDB tables in a given Region. Also,
     * there are per-table quotas that apply when you create a table there. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service,
     * Account, and Table Quotas</a> page in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * <p>Although you can increase these quotas by filing a case at <a href="https://console.aws.amazon.com/support/home#/">Amazon Web Services Support Center</a>,
     * obtaining the increase is not instantaneous. The <code>DescribeLimits</code> action lets you write code to
     * compare the capacity you are currently using to those quotas imposed by your account so that you have enough time
     * to apply for an increase before you hit a quota.
     *
     * <p>For example, you could use one of the Amazon Web Services SDKs to do the following:
     *
     * <ol>
     *   <li>
     *     Call <code>DescribeLimits</code> for a particular Region to obtain your current account quotas on
     *     provisioned capacity there.
     *   </li>
     *   <li>
     *     Create a variable to hold the aggregate read capacity units provisioned for all your tables in that
     *     Region, and one to hold the aggregate write capacity units. Zero them both.
     *   </li>
     *   <li>
     *     Call <code>ListTables</code> to obtain a list of all your DynamoDB tables.
     *   </li>
     *   <li>
     *     For each table name listed by <code>ListTables</code>, do the following:
     *
     *     <ul>
     *       <li>
     *         Call <code>DescribeTable</code> with the table name.
     *       </li>
     *       <li>
     *         Use the data returned by <code>DescribeTable</code> to add the read capacity units and write
     *         capacity units provisioned for the table itself to your variables.
     *       </li>
     *       <li>
     *         If the table has one or more global secondary indexes (GSIs), loop over these GSIs and add their
     *         provisioned capacity values to your variables as well.
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *     Report the account quotas for that Region returned by <code>DescribeLimits</code>, along with the total
     *     current provisioned capacity levels you have calculated.
     *   </li>
     * </ol>
     *
     * <p>This will let you see whether you are getting close to your account-level quotas.
     *
     * <p>The per-table quotas apply only when you are creating a new table. They restrict the sum of the provisioned
     * capacity of the new table itself and all its global secondary indexes.
     *
     * <p>For existing tables and their GSIs, DynamoDB doesn't let you increase provisioned capacity extremely rapidly,
     * but the only quota that applies is that the aggregate provisioned capacity over all your tables and GSIs cannot
     * exceed either of the per-account quotas.
     *
     * <p><code>DescribeLimits</code> should only be called periodically. You can expect throttling errors if you call
     * it more than once in a minute.
     *
     * <p>The <code>DescribeLimits</code> Request element has no content.
     *
     * <h4>Examples</h4>
     * <h5>To determine capacity limits per table and account, in the current AWS region</h5>
     *
     * <p>The following example returns the maximum read and write capacity units per table, and for the AWS account, in the current AWS region.{@snippet :
     * var input = DescribeLimitsInput.builder()
     *
     *                 .build();
     *
     * var result = client.describeLimits(input);
     * result.equals(DescribeLimitsOutput.builder()
     *                   .tableMaxWriteCapacityUnits(10000).tableMaxReadCapacityUnits(10000).accountMaxReadCapacityUnits(20000).accountMaxWriteCapacityUnits(20000)
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    DescribeLimitsOutput describeLimits(DescribeLimitsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns information about the table, including the current status of the table, when it was created, the primary
     * key schema, and any indexes on the table.
     *
     * <p>If you issue a <code>DescribeTable</code> request immediately after a <code>CreateTable</code> request,
     * DynamoDB might return a <code>ResourceNotFoundException</code>. This is because <code>DescribeTable</code> uses
     * an eventually consistent query, and the metadata for your table might not be available at that moment. Wait for a
     * few seconds, and then try the <code>DescribeTable</code> request again.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ResourceNotFoundException
     */
    default DescribeTableOutput describeTable(DescribeTableInput input) {
        return describeTable(input, null);
    }

    /**
     * Returns information about the table, including the current status of the table, when it was created, the primary
     * key schema, and any indexes on the table.
     *
     * <p>If you issue a <code>DescribeTable</code> request immediately after a <code>CreateTable</code> request,
     * DynamoDB might return a <code>ResourceNotFoundException</code>. This is because <code>DescribeTable</code> uses
     * an eventually consistent query, and the metadata for your table might not be available at that moment. Wait for a
     * few seconds, and then try the <code>DescribeTable</code> request again.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ResourceNotFoundException
     */
    DescribeTableOutput describeTable(DescribeTableInput input, RequestOverrideConfig overrideConfig);

    /**
     * Describes auto scaling settings across replicas of the global table at once.
     *
     * @throws InternalServerError
     * @throws ResourceNotFoundException
     */
    default DescribeTableReplicaAutoScalingOutput describeTableReplicaAutoScaling(DescribeTableReplicaAutoScalingInput input) {
        return describeTableReplicaAutoScaling(input, null);
    }

    /**
     * Describes auto scaling settings across replicas of the global table at once.
     *
     * @throws InternalServerError
     * @throws ResourceNotFoundException
     */
    DescribeTableReplicaAutoScalingOutput describeTableReplicaAutoScaling(DescribeTableReplicaAutoScalingInput input, RequestOverrideConfig overrideConfig);

    /**
     * Gives a description of the Time to Live (TTL) status on the specified table.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ResourceNotFoundException
     */
    default DescribeTimeToLiveOutput describeTimeToLive(DescribeTimeToLiveInput input) {
        return describeTimeToLive(input, null);
    }

    /**
     * Gives a description of the Time to Live (TTL) status on the specified table.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ResourceNotFoundException
     */
    DescribeTimeToLiveOutput describeTimeToLive(DescribeTimeToLiveInput input, RequestOverrideConfig overrideConfig);

    /**
     * Stops replication from the DynamoDB table to the Kinesis data stream. This is done without deleting either of the
     * resources.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default DisableKinesisStreamingDestinationOutput disableKinesisStreamingDestination(DisableKinesisStreamingDestinationInput input) {
        return disableKinesisStreamingDestination(input, null);
    }

    /**
     * Stops replication from the DynamoDB table to the Kinesis data stream. This is done without deleting either of the
     * resources.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    DisableKinesisStreamingDestinationOutput disableKinesisStreamingDestination(DisableKinesisStreamingDestinationInput input, RequestOverrideConfig overrideConfig);

    /**
     * Starts table data replication to the specified Kinesis data stream at a timestamp chosen during the enable
     * workflow. If this operation doesn't return results immediately, use DescribeKinesisStreamingDestination to check
     * if streaming to the Kinesis data stream is ACTIVE.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default EnableKinesisStreamingDestinationOutput enableKinesisStreamingDestination(EnableKinesisStreamingDestinationInput input) {
        return enableKinesisStreamingDestination(input, null);
    }

    /**
     * Starts table data replication to the specified Kinesis data stream at a timestamp chosen during the enable
     * workflow. If this operation doesn't return results immediately, use DescribeKinesisStreamingDestination to check
     * if streaming to the Kinesis data stream is ACTIVE.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    EnableKinesisStreamingDestinationOutput enableKinesisStreamingDestination(EnableKinesisStreamingDestinationInput input, RequestOverrideConfig overrideConfig);

    /**
     * This operation allows you to perform reads and singleton writes on data stored in DynamoDB, using PartiQL.
     *
     * <p>For PartiQL reads (<code>SELECT</code> statement), if the total number of processed items exceeds the maximum
     * dataset size limit of 1 MB, the read stops and results are returned to the user as a <code>LastEvaluatedKey</code>
     * value to continue the read in a subsequent operation. If the filter criteria in <code>WHERE</code> clause does
     * not match any data, the read will return an empty result set.
     *
     * <p>A single <code>SELECT</code> statement response can return up to the maximum number of items (if using the
     * Limit parameter) or a maximum of 1 MB of data (and then apply any filtering to the results using <code>WHERE</code>
     * clause). If <code>LastEvaluatedKey</code> is present in the response, you need to paginate the result set. If <code>
     * NextToken</code> is present, you need to paginate the result set and include <code>NextToken</code>.
     *
     * @throws ConditionalCheckFailedException
     * @throws DuplicateItemException
     * @throws InternalServerError
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionConflictException
     */
    default ExecuteStatementOutput executeStatement(ExecuteStatementInput input) {
        return executeStatement(input, null);
    }

    /**
     * This operation allows you to perform reads and singleton writes on data stored in DynamoDB, using PartiQL.
     *
     * <p>For PartiQL reads (<code>SELECT</code> statement), if the total number of processed items exceeds the maximum
     * dataset size limit of 1 MB, the read stops and results are returned to the user as a <code>LastEvaluatedKey</code>
     * value to continue the read in a subsequent operation. If the filter criteria in <code>WHERE</code> clause does
     * not match any data, the read will return an empty result set.
     *
     * <p>A single <code>SELECT</code> statement response can return up to the maximum number of items (if using the
     * Limit parameter) or a maximum of 1 MB of data (and then apply any filtering to the results using <code>WHERE</code>
     * clause). If <code>LastEvaluatedKey</code> is present in the response, you need to paginate the result set. If <code>
     * NextToken</code> is present, you need to paginate the result set and include <code>NextToken</code>.
     *
     * @throws ConditionalCheckFailedException
     * @throws DuplicateItemException
     * @throws InternalServerError
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionConflictException
     */
    ExecuteStatementOutput executeStatement(ExecuteStatementInput input, RequestOverrideConfig overrideConfig);

    /**
     * This operation allows you to perform transactional reads or writes on data stored in DynamoDB, using PartiQL.
     *
     * <p>The entire transaction must consist of either read statements or write statements, you cannot mix both in one
     * transaction. The EXISTS function is an exception and can be used to check the condition of specific attributes of
     * the item in a similar manner to <code>ConditionCheck</code> in the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/transaction-apis.html#transaction-apis-txwriteitems">TransactWriteItems</a> API.
     *
     * @throws IdempotentParameterMismatchException
     * @throws InternalServerError
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionCanceledException
     * @throws TransactionInProgressException
     */
    default ExecuteTransactionOutput executeTransaction(ExecuteTransactionInput input) {
        return executeTransaction(input, null);
    }

    /**
     * This operation allows you to perform transactional reads or writes on data stored in DynamoDB, using PartiQL.
     *
     * <p>The entire transaction must consist of either read statements or write statements, you cannot mix both in one
     * transaction. The EXISTS function is an exception and can be used to check the condition of specific attributes of
     * the item in a similar manner to <code>ConditionCheck</code> in the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/transaction-apis.html#transaction-apis-txwriteitems">TransactWriteItems</a> API.
     *
     * @throws IdempotentParameterMismatchException
     * @throws InternalServerError
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionCanceledException
     * @throws TransactionInProgressException
     */
    ExecuteTransactionOutput executeTransaction(ExecuteTransactionInput input, RequestOverrideConfig overrideConfig);

    /**
     * Exports table data to an S3 bucket. The table must have point in time recovery enabled, and you can export data
     * from any time within the point in time recovery window.
     *
     * @throws ExportConflictException
     * @throws InternalServerError
     * @throws InvalidExportTimeException
     * @throws LimitExceededException
     * @throws PointInTimeRecoveryUnavailableException
     * @throws TableNotFoundException
     */
    default ExportTableToPointInTimeOutput exportTableToPointInTime(ExportTableToPointInTimeInput input) {
        return exportTableToPointInTime(input, null);
    }

    /**
     * Exports table data to an S3 bucket. The table must have point in time recovery enabled, and you can export data
     * from any time within the point in time recovery window.
     *
     * @throws ExportConflictException
     * @throws InternalServerError
     * @throws InvalidExportTimeException
     * @throws LimitExceededException
     * @throws PointInTimeRecoveryUnavailableException
     * @throws TableNotFoundException
     */
    ExportTableToPointInTimeOutput exportTableToPointInTime(ExportTableToPointInTimeInput input, RequestOverrideConfig overrideConfig);

    /**
     * The <code>GetItem</code> operation returns a set of attributes for the item with the given primary key. If there
     * is no matching item, <code>GetItem</code> does not return any data and there will be no <code>Item</code> element
     * in the response.
     *
     * <p><code>GetItem</code> provides an eventually consistent read by default. If your application requires a
     * strongly consistent read, set <code>ConsistentRead</code> to <code>true</code>. Although a strongly consistent
     * read might take more time than an eventually consistent read, it always returns the last updated value.
     *
     * <h4>Examples</h4>
     * <h5>To read an item from a table</h5>
     *
     * <p>This example retrieves an item from the Music table. The table has a partition key and a sort key (Artist and SongTitle), so you must specify both of these attributes.{@snippet :
     * var input = GetItemInput.builder()
     *                 .tableName("Music").key(Map.of(
     *                          "Artist", AttributeValue.SMember("Acme Band"),
     *                          "SongTitle", AttributeValue.SMember("Happy Day")
     *                      ))
     *                 .build();
     *
     * var result = client.getItem(input);
     * result.equals(GetItemOutput.builder()
     *                   .item(Map.of(
     *                             "AlbumTitle", AttributeValue.SMember("Songs About Life"),
     *                             "SongTitle", AttributeValue.SMember("Happy Day"),
     *                             "Artist", AttributeValue.SMember("Acme Band")
     *                         ))
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    default GetItemOutput getItem(GetItemInput input) {
        return getItem(input, null);
    }

    /**
     * The <code>GetItem</code> operation returns a set of attributes for the item with the given primary key. If there
     * is no matching item, <code>GetItem</code> does not return any data and there will be no <code>Item</code> element
     * in the response.
     *
     * <p><code>GetItem</code> provides an eventually consistent read by default. If your application requires a
     * strongly consistent read, set <code>ConsistentRead</code> to <code>true</code>. Although a strongly consistent
     * read might take more time than an eventually consistent read, it always returns the last updated value.
     *
     * <h4>Examples</h4>
     * <h5>To read an item from a table</h5>
     *
     * <p>This example retrieves an item from the Music table. The table has a partition key and a sort key (Artist and SongTitle), so you must specify both of these attributes.{@snippet :
     * var input = GetItemInput.builder()
     *                 .tableName("Music").key(Map.of(
     *                          "Artist", AttributeValue.SMember("Acme Band"),
     *                          "SongTitle", AttributeValue.SMember("Happy Day")
     *                      ))
     *                 .build();
     *
     * var result = client.getItem(input);
     * result.equals(GetItemOutput.builder()
     *                   .item(Map.of(
     *                             "AlbumTitle", AttributeValue.SMember("Songs About Life"),
     *                             "SongTitle", AttributeValue.SMember("Happy Day"),
     *                             "Artist", AttributeValue.SMember("Acme Band")
     *                         ))
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    GetItemOutput getItem(GetItemInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns the resource-based policy document attached to the resource, which can be a table or stream, in JSON
     * format.
     *
     * <p><code>GetResourcePolicy</code> follows an <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.ReadConsistency.html"><i>eventually consistent</i></a> model. The following list
     * describes the outcomes when you issue the <code>GetResourcePolicy</code> request immediately after issuing
     * another request:
     *
     * <ul>
     *   <li>
     *     If you issue a <code>GetResourcePolicy</code> request immediately after a <code>PutResourcePolicy</code>
     *     request, DynamoDB might return a <code>PolicyNotFoundException</code>.
     *   </li>
     *   <li>
     *     If you issue a <code>GetResourcePolicy</code>request immediately after a <code>DeleteResourcePolicy</code>
     *     request, DynamoDB might return the policy that was present before the deletion request.
     *   </li>
     *   <li>
     *     If you issue a <code>GetResourcePolicy</code> request immediately after a <code>CreateTable</code>
     *     request, which includes a resource-based policy, DynamoDB might return a <code>ResourceNotFoundException</code>
     *     or a <code>PolicyNotFoundException</code>.
     *   </li>
     * </ul>
     *
     * <p>Because <code>GetResourcePolicy</code> uses an <i>eventually consistent</i> query, the metadata for your
     * policy or table might not be available at that moment. Wait for a few seconds, and then retry the <code>
     * GetResourcePolicy</code> request.
     *
     * <p>After a <code>GetResourcePolicy</code> request returns a policy created using the <code>PutResourcePolicy</code>
     * request, the policy will be applied in the authorization of requests to the resource. Because this process is
     * eventually consistent, it will take some time to apply the policy to all requests to a resource. Policies that
     * you attach while creating a table using the <code>CreateTable</code> request will always be applied to all
     * requests for that table.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws PolicyNotFoundException
     * @throws ResourceNotFoundException
     */
    default GetResourcePolicyOutput getResourcePolicy(GetResourcePolicyInput input) {
        return getResourcePolicy(input, null);
    }

    /**
     * Returns the resource-based policy document attached to the resource, which can be a table or stream, in JSON
     * format.
     *
     * <p><code>GetResourcePolicy</code> follows an <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.ReadConsistency.html"><i>eventually consistent</i></a> model. The following list
     * describes the outcomes when you issue the <code>GetResourcePolicy</code> request immediately after issuing
     * another request:
     *
     * <ul>
     *   <li>
     *     If you issue a <code>GetResourcePolicy</code> request immediately after a <code>PutResourcePolicy</code>
     *     request, DynamoDB might return a <code>PolicyNotFoundException</code>.
     *   </li>
     *   <li>
     *     If you issue a <code>GetResourcePolicy</code>request immediately after a <code>DeleteResourcePolicy</code>
     *     request, DynamoDB might return the policy that was present before the deletion request.
     *   </li>
     *   <li>
     *     If you issue a <code>GetResourcePolicy</code> request immediately after a <code>CreateTable</code>
     *     request, which includes a resource-based policy, DynamoDB might return a <code>ResourceNotFoundException</code>
     *     or a <code>PolicyNotFoundException</code>.
     *   </li>
     * </ul>
     *
     * <p>Because <code>GetResourcePolicy</code> uses an <i>eventually consistent</i> query, the metadata for your
     * policy or table might not be available at that moment. Wait for a few seconds, and then retry the <code>
     * GetResourcePolicy</code> request.
     *
     * <p>After a <code>GetResourcePolicy</code> request returns a policy created using the <code>PutResourcePolicy</code>
     * request, the policy will be applied in the authorization of requests to the resource. Because this process is
     * eventually consistent, it will take some time to apply the policy to all requests to a resource. Policies that
     * you attach while creating a table using the <code>CreateTable</code> request will always be applied to all
     * requests for that table.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws PolicyNotFoundException
     * @throws ResourceNotFoundException
     */
    GetResourcePolicyOutput getResourcePolicy(GetResourcePolicyInput input, RequestOverrideConfig overrideConfig);

    /**
     * Imports table data from an S3 bucket.
     *
     * @throws ImportConflictException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     */
    default ImportTableOutput importTable(ImportTableInput input) {
        return importTable(input, null);
    }

    /**
     * Imports table data from an S3 bucket.
     *
     * @throws ImportConflictException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     */
    ImportTableOutput importTable(ImportTableInput input, RequestOverrideConfig overrideConfig);

    /**
     * List DynamoDB backups that are associated with an Amazon Web Services account and weren't made with Amazon Web
     * Services Backup. To list these backups for a given table, specify <code>TableName</code>. <code>ListBackups</code>
     * returns a paginated list of results with at most 1 MB worth of items in a page. You can also specify a maximum
     * number of entries to be returned in a page.
     *
     * <p>In the request, start time is inclusive, but end time is exclusive. Note that these boundaries are for the
     * time at which the original backup was requested.
     *
     * <p>You can call <code>ListBackups</code> a maximum of five times per second.
     *
     * <p>If you want to retrieve the complete list of backups made with Amazon Web Services Backup, use the <a href="https://docs.aws.amazon.com/aws-backup/latest/devguide/API_ListBackupJobs.html">Amazon
     * Web Services Backup list API.</a>
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    default ListBackupsOutput listBackups(ListBackupsInput input) {
        return listBackups(input, null);
    }

    /**
     * List DynamoDB backups that are associated with an Amazon Web Services account and weren't made with Amazon Web
     * Services Backup. To list these backups for a given table, specify <code>TableName</code>. <code>ListBackups</code>
     * returns a paginated list of results with at most 1 MB worth of items in a page. You can also specify a maximum
     * number of entries to be returned in a page.
     *
     * <p>In the request, start time is inclusive, but end time is exclusive. Note that these boundaries are for the
     * time at which the original backup was requested.
     *
     * <p>You can call <code>ListBackups</code> a maximum of five times per second.
     *
     * <p>If you want to retrieve the complete list of backups made with Amazon Web Services Backup, use the <a href="https://docs.aws.amazon.com/aws-backup/latest/devguide/API_ListBackupJobs.html">Amazon
     * Web Services Backup list API.</a>
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    ListBackupsOutput listBackups(ListBackupsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a list of ContributorInsightsSummary for a table and all its global secondary indexes.
     *
     * @throws InternalServerError
     * @throws ResourceNotFoundException
     */
    default ListContributorInsightsOutput listContributorInsights(ListContributorInsightsInput input) {
        return listContributorInsights(input, null);
    }

    /**
     * Returns a list of ContributorInsightsSummary for a table and all its global secondary indexes.
     *
     * @throws InternalServerError
     * @throws ResourceNotFoundException
     */
    ListContributorInsightsOutput listContributorInsights(ListContributorInsightsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listContributorInsights} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListContributorInsightsOutput> listContributorInsightsPaginator(ListContributorInsightsInput input) {
        return Paginator.paginate(input, ListContributorInsights.instance(), this::listContributorInsights);
    }

    /**
     * Lists completed exports within the past 90 days, in reverse alphanumeric order of <code>ExportArn</code>.
     *
     * @throws InternalServerError
     * @throws LimitExceededException
     */
    default ListExportsOutput listExports(ListExportsInput input) {
        return listExports(input, null);
    }

    /**
     * Lists completed exports within the past 90 days, in reverse alphanumeric order of <code>ExportArn</code>.
     *
     * @throws InternalServerError
     * @throws LimitExceededException
     */
    ListExportsOutput listExports(ListExportsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listExports} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListExportsOutput> listExportsPaginator(ListExportsInput input) {
        return Paginator.paginate(input, ListExports.instance(), this::listExports);
    }

    /**
     * Lists all global tables that have a replica in the specified Region.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    default ListGlobalTablesOutput listGlobalTables(ListGlobalTablesInput input) {
        return listGlobalTables(input, null);
    }

    /**
     * Lists all global tables that have a replica in the specified Region.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    ListGlobalTablesOutput listGlobalTables(ListGlobalTablesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Lists completed imports within the past 90 days.
     *
     * @throws LimitExceededException
     */
    default ListImportsOutput listImports(ListImportsInput input) {
        return listImports(input, null);
    }

    /**
     * Lists completed imports within the past 90 days.
     *
     * @throws LimitExceededException
     */
    ListImportsOutput listImports(ListImportsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listImports} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListImportsOutput> listImportsPaginator(ListImportsInput input) {
        return Paginator.paginate(input, ListImports.instance(), this::listImports);
    }

    /**
     * Returns an array of table names associated with the current account and endpoint. The output from
     * <code>ListTables</code> is paginated, with each page returning a maximum of 100 table names.
     *
     * <h4>Examples</h4>
     * <h5>To list tables</h5>
     *
     * <p>This example lists all of the tables associated with the current AWS account and endpoint.{@snippet :
     * var input = ListTablesInput.builder()
     *
     *                 .build();
     *
     * var result = client.listTables(input);
     * result.equals(ListTablesOutput.builder()
     *                   .tableNames(List.of(
     *                                   "Forum",
     *                                   "ProductCatalog",
     *                                   "Reply",
     *                                   "Thread"
     *                               ))
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    default ListTablesOutput listTables(ListTablesInput input) {
        return listTables(input, null);
    }

    /**
     * Returns an array of table names associated with the current account and endpoint. The output from
     * <code>ListTables</code> is paginated, with each page returning a maximum of 100 table names.
     *
     * <h4>Examples</h4>
     * <h5>To list tables</h5>
     *
     * <p>This example lists all of the tables associated with the current AWS account and endpoint.{@snippet :
     * var input = ListTablesInput.builder()
     *
     *                 .build();
     *
     * var result = client.listTables(input);
     * result.equals(ListTablesOutput.builder()
     *                   .tableNames(List.of(
     *                                   "Forum",
     *                                   "ProductCatalog",
     *                                   "Reply",
     *                                   "Thread"
     *                               ))
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     */
    ListTablesOutput listTables(ListTablesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listTables} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListTablesOutput> listTablesPaginator(ListTablesInput input) {
        return Paginator.paginate(input, ListTables.instance(), this::listTables);
    }

    /**
     * List all tags on an Amazon DynamoDB resource. You can call ListTagsOfResource up to 10 times per second, per
     * account.
     *
     * <p>For an overview on tagging DynamoDB resources, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ResourceNotFoundException
     */
    default ListTagsOfResourceOutput listTagsOfResource(ListTagsOfResourceInput input) {
        return listTagsOfResource(input, null);
    }

    /**
     * List all tags on an Amazon DynamoDB resource. You can call ListTagsOfResource up to 10 times per second, per
     * account.
     *
     * <p>For an overview on tagging DynamoDB resources, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ResourceNotFoundException
     */
    ListTagsOfResourceOutput listTagsOfResource(ListTagsOfResourceInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates a new item, or replaces an old item with a new item. If an item that has the same primary key as the new
     * item already exists in the specified table, the new item completely replaces the existing item. You can perform a
     * conditional put operation (add a new item if one with the specified primary key doesn't exist), or replace an
     * existing item if it has certain attribute values. You can return the item's attribute values in the same
     * operation, using the <code>ReturnValues</code> parameter.
     *
     * <p>When you add an item, the primary key attributes are the only required attributes.
     *
     * <p>Empty String and Binary attribute values are allowed. Attribute values of type String and Binary must have a
     * length greater than zero if the attribute is used as a key attribute for a table or index. Set type attributes
     * cannot be empty.
     *
     * <p>Invalid Requests with empty values will be rejected with a <code>ValidationException</code> exception.
     *
     * <p>To prevent a new item from replacing an existing item, use a conditional expression that contains the <code>
     * attribute_not_exists</code> function with the name of the attribute being used as the partition key for the
     * table. Since every record must contain that attribute, the <code>attribute_not_exists</code> function will only
     * succeed if no matching item exists.
     *
     * <p>To determine whether <code>PutItem</code> overwrote an existing item, use <code>ReturnValues</code> set to <code>
     * ALL_OLD</code>. If the response includes the <code>Attributes</code> element, an existing item was overwritten.
     *
     * <p>For more information about <code>PutItem</code>, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithItems.html">Working with Items</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     *
     * <h4>Examples</h4>
     * <h5>To add an item to a table</h5>
     *
     * <p>This example adds a new item to the Music table.{@snippet :
     * var input = PutItemInput.builder()
     *                 .tableName("Music").item(Map.of(
     *                           "AlbumTitle", AttributeValue.SMember("Somewhat Famous"),
     *                           "SongTitle", AttributeValue.SMember("Call Me Today"),
     *                           "Artist", AttributeValue.SMember("No One You Know")
     *                       )).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
     *                 .build();
     *
     * var result = client.putItem(input);
     * result.equals(PutItemOutput.builder()
     *                   .consumedCapacity(ConsumedCapacity.builder()
     *                                         .capacityUnits(1).tableName("Music")
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws ConditionalCheckFailedException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws ReplicatedWriteConflictException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionConflictException
     */
    default PutItemOutput putItem(PutItemInput input) {
        return putItem(input, null);
    }

    /**
     * Creates a new item, or replaces an old item with a new item. If an item that has the same primary key as the new
     * item already exists in the specified table, the new item completely replaces the existing item. You can perform a
     * conditional put operation (add a new item if one with the specified primary key doesn't exist), or replace an
     * existing item if it has certain attribute values. You can return the item's attribute values in the same
     * operation, using the <code>ReturnValues</code> parameter.
     *
     * <p>When you add an item, the primary key attributes are the only required attributes.
     *
     * <p>Empty String and Binary attribute values are allowed. Attribute values of type String and Binary must have a
     * length greater than zero if the attribute is used as a key attribute for a table or index. Set type attributes
     * cannot be empty.
     *
     * <p>Invalid Requests with empty values will be rejected with a <code>ValidationException</code> exception.
     *
     * <p>To prevent a new item from replacing an existing item, use a conditional expression that contains the <code>
     * attribute_not_exists</code> function with the name of the attribute being used as the partition key for the
     * table. Since every record must contain that attribute, the <code>attribute_not_exists</code> function will only
     * succeed if no matching item exists.
     *
     * <p>To determine whether <code>PutItem</code> overwrote an existing item, use <code>ReturnValues</code> set to <code>
     * ALL_OLD</code>. If the response includes the <code>Attributes</code> element, an existing item was overwritten.
     *
     * <p>For more information about <code>PutItem</code>, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithItems.html">Working with Items</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     *
     * <h4>Examples</h4>
     * <h5>To add an item to a table</h5>
     *
     * <p>This example adds a new item to the Music table.{@snippet :
     * var input = PutItemInput.builder()
     *                 .tableName("Music").item(Map.of(
     *                           "AlbumTitle", AttributeValue.SMember("Somewhat Famous"),
     *                           "SongTitle", AttributeValue.SMember("Call Me Today"),
     *                           "Artist", AttributeValue.SMember("No One You Know")
     *                       )).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
     *                 .build();
     *
     * var result = client.putItem(input);
     * result.equals(PutItemOutput.builder()
     *                   .consumedCapacity(ConsumedCapacity.builder()
     *                                         .capacityUnits(1).tableName("Music")
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws ConditionalCheckFailedException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws ReplicatedWriteConflictException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionConflictException
     */
    PutItemOutput putItem(PutItemInput input, RequestOverrideConfig overrideConfig);

    /**
     * Attaches a resource-based policy document to the resource, which can be a table or stream. When you attach a
     * resource-based policy using this API, the policy application is <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.ReadConsistency.html"><i>eventually consistent</i></a>.
     *
     * <p><code>PutResourcePolicy</code> is an idempotent operation; running it multiple times on the same resource
     * using the same policy document will return the same revision ID. If you specify an <code>ExpectedRevisionId</code>
     * that doesn't match the current policy's <code>RevisionId</code>, the <code>PolicyNotFoundException</code> will be
     * returned.
     *
     * <p><code>PutResourcePolicy</code> is an asynchronous operation. If you issue a <code>GetResourcePolicy</code>
     * request immediately after a <code>PutResourcePolicy</code> request, DynamoDB might return your previous policy,
     * if there was one, or return the <code>PolicyNotFoundException</code>. This is because <code>GetResourcePolicy</code>
     * uses an eventually consistent query, and the metadata for your policy or table might not be available at that
     * moment. Wait for a few seconds, and then try the <code>GetResourcePolicy</code> request again.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws PolicyNotFoundException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default PutResourcePolicyOutput putResourcePolicy(PutResourcePolicyInput input) {
        return putResourcePolicy(input, null);
    }

    /**
     * Attaches a resource-based policy document to the resource, which can be a table or stream. When you attach a
     * resource-based policy using this API, the policy application is <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.ReadConsistency.html"><i>eventually consistent</i></a>.
     *
     * <p><code>PutResourcePolicy</code> is an idempotent operation; running it multiple times on the same resource
     * using the same policy document will return the same revision ID. If you specify an <code>ExpectedRevisionId</code>
     * that doesn't match the current policy's <code>RevisionId</code>, the <code>PolicyNotFoundException</code> will be
     * returned.
     *
     * <p><code>PutResourcePolicy</code> is an asynchronous operation. If you issue a <code>GetResourcePolicy</code>
     * request immediately after a <code>PutResourcePolicy</code> request, DynamoDB might return your previous policy,
     * if there was one, or return the <code>PolicyNotFoundException</code>. This is because <code>GetResourcePolicy</code>
     * uses an eventually consistent query, and the metadata for your policy or table might not be available at that
     * moment. Wait for a few seconds, and then try the <code>GetResourcePolicy</code> request again.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws PolicyNotFoundException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    PutResourcePolicyOutput putResourcePolicy(PutResourcePolicyInput input, RequestOverrideConfig overrideConfig);

    /**
     * You must provide the name of the partition key attribute and a single value for that attribute. <code>Query</code>
     * returns all items with that partition key value. Optionally, you can provide a sort key attribute and use a
     * comparison operator to refine the search results.
     *
     * <p>Use the <code>KeyConditionExpression</code> parameter to provide a specific value for the partition key. The <code>
     * Query</code> operation will return all of the items from the table or index with that partition key value. You
     * can optionally narrow the scope of the <code>Query</code> operation by specifying a sort key value and a
     * comparison operator in <code>KeyConditionExpression</code>. To further refine the <code>Query</code> results, you
     * can optionally provide a <code>FilterExpression</code>. A <code>FilterExpression</code> determines which items
     * within the results should be returned to you. All of the other results are discarded.
     *
     * <p> A <code>Query</code> operation always returns a result set. If no matching items are found, the result set
     * will be empty. Queries that do not return results consume the minimum number of read capacity units for that type
     * of read operation.
     *
     * <p> DynamoDB calculates the number of read capacity units consumed based on item size, not on the amount of data
     * that is returned to an application. The number of capacity units consumed will be the same whether you request
     * all of the attributes (the default behavior) or just some of them (using a projection expression). The number
     * will also be the same whether or not you use a <code>FilterExpression</code>.
     *
     * <p><code>Query</code> results are always sorted by the sort key value. If the data type of the sort key is
     * Number, the results are returned in numeric order; otherwise, the results are returned in order of UTF-8 bytes.
     * By default, the sort order is ascending. To reverse the order, set the <code>ScanIndexForward</code> parameter to
     * false.
     *
     * <p> A single <code>Query</code> operation will read up to the maximum number of items set (if using the <code>
     * Limit</code> parameter) or a maximum of 1 MB of data and then apply any filtering to the results using <code>
     * FilterExpression</code>. If <code>LastEvaluatedKey</code> is present in the response, you will need to paginate
     * the result set. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#Query.Pagination">Paginating the Results</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     *
     * <p><code>FilterExpression</code> is applied after a <code>Query</code> finishes, but before the results are
     * returned. A <code>FilterExpression</code> cannot contain partition key or sort key attributes. You need to
     * specify those attributes in the <code>KeyConditionExpression</code>.
     *
     * <p> A <code>Query</code> operation can return an empty result set and a <code>LastEvaluatedKey</code> if all the
     * items read for the page of results are filtered out.
     *
     * <p>You can query a table, a local secondary index, or a global secondary index. For a query on a table or on a
     * local secondary index, you can set the <code>ConsistentRead</code> parameter to <code>true</code> and obtain a
     * strongly consistent result. Global secondary indexes support eventually consistent reads only, so do not specify <code>
     * ConsistentRead</code> when querying a global secondary index.
     *
     * <h4>Examples</h4>
     * <h5>To query an item</h5>
     *
     * <p>This example queries items in the Music table. The table has a partition key and sort key (Artist and SongTitle), but this query only specifies the partition key value. It returns song titles by the artist named "No One You Know".{@snippet :
     * var input = QueryInput.builder()
     *                 .tableName("Music").projectionExpression("SongTitle").keyConditionExpression("Artist = :v1").expressionAttributeValues(Map.of(":v1", AttributeValue.SMember("No One You Know")))
     *                 .build();
     *
     * var result = client.query(input);
     * result.equals(QueryOutput.builder()
     *                   .count(2).items(List.of(Map.of("SongTitle", AttributeValue.SMember("Call Me Today")))).scannedCount(2).consumedCapacity(ConsumedCapacity.builder()
     *
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    default QueryOutput query(QueryInput input) {
        return query(input, null);
    }

    /**
     * You must provide the name of the partition key attribute and a single value for that attribute. <code>Query</code>
     * returns all items with that partition key value. Optionally, you can provide a sort key attribute and use a
     * comparison operator to refine the search results.
     *
     * <p>Use the <code>KeyConditionExpression</code> parameter to provide a specific value for the partition key. The <code>
     * Query</code> operation will return all of the items from the table or index with that partition key value. You
     * can optionally narrow the scope of the <code>Query</code> operation by specifying a sort key value and a
     * comparison operator in <code>KeyConditionExpression</code>. To further refine the <code>Query</code> results, you
     * can optionally provide a <code>FilterExpression</code>. A <code>FilterExpression</code> determines which items
     * within the results should be returned to you. All of the other results are discarded.
     *
     * <p> A <code>Query</code> operation always returns a result set. If no matching items are found, the result set
     * will be empty. Queries that do not return results consume the minimum number of read capacity units for that type
     * of read operation.
     *
     * <p> DynamoDB calculates the number of read capacity units consumed based on item size, not on the amount of data
     * that is returned to an application. The number of capacity units consumed will be the same whether you request
     * all of the attributes (the default behavior) or just some of them (using a projection expression). The number
     * will also be the same whether or not you use a <code>FilterExpression</code>.
     *
     * <p><code>Query</code> results are always sorted by the sort key value. If the data type of the sort key is
     * Number, the results are returned in numeric order; otherwise, the results are returned in order of UTF-8 bytes.
     * By default, the sort order is ascending. To reverse the order, set the <code>ScanIndexForward</code> parameter to
     * false.
     *
     * <p> A single <code>Query</code> operation will read up to the maximum number of items set (if using the <code>
     * Limit</code> parameter) or a maximum of 1 MB of data and then apply any filtering to the results using <code>
     * FilterExpression</code>. If <code>LastEvaluatedKey</code> is present in the response, you will need to paginate
     * the result set. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#Query.Pagination">Paginating the Results</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     *
     * <p><code>FilterExpression</code> is applied after a <code>Query</code> finishes, but before the results are
     * returned. A <code>FilterExpression</code> cannot contain partition key or sort key attributes. You need to
     * specify those attributes in the <code>KeyConditionExpression</code>.
     *
     * <p> A <code>Query</code> operation can return an empty result set and a <code>LastEvaluatedKey</code> if all the
     * items read for the page of results are filtered out.
     *
     * <p>You can query a table, a local secondary index, or a global secondary index. For a query on a table or on a
     * local secondary index, you can set the <code>ConsistentRead</code> parameter to <code>true</code> and obtain a
     * strongly consistent result. Global secondary indexes support eventually consistent reads only, so do not specify <code>
     * ConsistentRead</code> when querying a global secondary index.
     *
     * <h4>Examples</h4>
     * <h5>To query an item</h5>
     *
     * <p>This example queries items in the Music table. The table has a partition key and sort key (Artist and SongTitle), but this query only specifies the partition key value. It returns song titles by the artist named "No One You Know".{@snippet :
     * var input = QueryInput.builder()
     *                 .tableName("Music").projectionExpression("SongTitle").keyConditionExpression("Artist = :v1").expressionAttributeValues(Map.of(":v1", AttributeValue.SMember("No One You Know")))
     *                 .build();
     *
     * var result = client.query(input);
     * result.equals(QueryOutput.builder()
     *                   .count(2).items(List.of(Map.of("SongTitle", AttributeValue.SMember("Call Me Today")))).scannedCount(2).consumedCapacity(ConsumedCapacity.builder()
     *
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    QueryOutput query(QueryInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #query} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<QueryOutput> queryPaginator(QueryInput input) {
        return Paginator.paginate(input, Query.instance(), this::query);
    }

    /**
     * Creates a new table from an existing backup. Any number of users can execute up to 50 concurrent restores (any
     * type of restore) in a given account.
     *
     * <p>You can call <code>RestoreTableFromBackup</code> at a maximum rate of 10 times per second.
     *
     * <p>You must manually set up the following on the restored table:
     *
     * <ul>
     *   <li>
     *     Auto scaling policies
     *   </li>
     *   <li>
     *     IAM policies
     *   </li>
     *   <li>
     *     Amazon CloudWatch metrics and alarms
     *   </li>
     *   <li>
     *     Tags
     *   </li>
     *   <li>
     *     Stream settings
     *   </li>
     *   <li>
     *     Time to Live (TTL) settings
     *   </li>
     * </ul>
     *
     * @throws BackupInUseException
     * @throws BackupNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws TableAlreadyExistsException
     * @throws TableInUseException
     */
    default RestoreTableFromBackupOutput restoreTableFromBackup(RestoreTableFromBackupInput input) {
        return restoreTableFromBackup(input, null);
    }

    /**
     * Creates a new table from an existing backup. Any number of users can execute up to 50 concurrent restores (any
     * type of restore) in a given account.
     *
     * <p>You can call <code>RestoreTableFromBackup</code> at a maximum rate of 10 times per second.
     *
     * <p>You must manually set up the following on the restored table:
     *
     * <ul>
     *   <li>
     *     Auto scaling policies
     *   </li>
     *   <li>
     *     IAM policies
     *   </li>
     *   <li>
     *     Amazon CloudWatch metrics and alarms
     *   </li>
     *   <li>
     *     Tags
     *   </li>
     *   <li>
     *     Stream settings
     *   </li>
     *   <li>
     *     Time to Live (TTL) settings
     *   </li>
     * </ul>
     *
     * @throws BackupInUseException
     * @throws BackupNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws TableAlreadyExistsException
     * @throws TableInUseException
     */
    RestoreTableFromBackupOutput restoreTableFromBackup(RestoreTableFromBackupInput input, RequestOverrideConfig overrideConfig);

    /**
     * Restores the specified table to the specified point in time within <code>EarliestRestorableDateTime</code> and <code>
     * LatestRestorableDateTime</code>. You can restore your table to any point in time in the last 35 days. You can set
     * the recovery period to any value between 1 and 35 days. Any number of users can execute up to 50 concurrent
     * restores (any type of restore) in a given account.
     *
     * <p>When you restore using point in time recovery, DynamoDB restores your table data to the state based on the
     * selected date and time (day:hour:minute:second) to a new table.
     *
     * <p>Along with data, the following are also included on the new restored table using point in time recovery:
     *
     * <ul>
     *   <li>
     *     Global secondary indexes (GSIs)
     *   </li>
     *   <li>
     *     Local secondary indexes (LSIs)
     *   </li>
     *   <li>
     *     Provisioned read and write capacity
     *   </li>
     *   <li>
     *     Encryption settings All these settings come from the current settings of the source table at the time of
     *     restore.
     *   </li>
     * </ul>
     *
     * <p>You must manually set up the following on the restored table:
     *
     * <ul>
     *   <li>
     *     Auto scaling policies
     *   </li>
     *   <li>
     *     IAM policies
     *   </li>
     *   <li>
     *     Amazon CloudWatch metrics and alarms
     *   </li>
     *   <li>
     *     Tags
     *   </li>
     *   <li>
     *     Stream settings
     *   </li>
     *   <li>
     *     Time to Live (TTL) settings
     *   </li>
     *   <li>
     *     Point in time recovery settings
     *   </li>
     * </ul>
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws InvalidRestoreTimeException
     * @throws LimitExceededException
     * @throws PointInTimeRecoveryUnavailableException
     * @throws TableAlreadyExistsException
     * @throws TableInUseException
     * @throws TableNotFoundException
     */
    default RestoreTableToPointInTimeOutput restoreTableToPointInTime(RestoreTableToPointInTimeInput input) {
        return restoreTableToPointInTime(input, null);
    }

    /**
     * Restores the specified table to the specified point in time within <code>EarliestRestorableDateTime</code> and <code>
     * LatestRestorableDateTime</code>. You can restore your table to any point in time in the last 35 days. You can set
     * the recovery period to any value between 1 and 35 days. Any number of users can execute up to 50 concurrent
     * restores (any type of restore) in a given account.
     *
     * <p>When you restore using point in time recovery, DynamoDB restores your table data to the state based on the
     * selected date and time (day:hour:minute:second) to a new table.
     *
     * <p>Along with data, the following are also included on the new restored table using point in time recovery:
     *
     * <ul>
     *   <li>
     *     Global secondary indexes (GSIs)
     *   </li>
     *   <li>
     *     Local secondary indexes (LSIs)
     *   </li>
     *   <li>
     *     Provisioned read and write capacity
     *   </li>
     *   <li>
     *     Encryption settings All these settings come from the current settings of the source table at the time of
     *     restore.
     *   </li>
     * </ul>
     *
     * <p>You must manually set up the following on the restored table:
     *
     * <ul>
     *   <li>
     *     Auto scaling policies
     *   </li>
     *   <li>
     *     IAM policies
     *   </li>
     *   <li>
     *     Amazon CloudWatch metrics and alarms
     *   </li>
     *   <li>
     *     Tags
     *   </li>
     *   <li>
     *     Stream settings
     *   </li>
     *   <li>
     *     Time to Live (TTL) settings
     *   </li>
     *   <li>
     *     Point in time recovery settings
     *   </li>
     * </ul>
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws InvalidRestoreTimeException
     * @throws LimitExceededException
     * @throws PointInTimeRecoveryUnavailableException
     * @throws TableAlreadyExistsException
     * @throws TableInUseException
     * @throws TableNotFoundException
     */
    RestoreTableToPointInTimeOutput restoreTableToPointInTime(RestoreTableToPointInTimeInput input, RequestOverrideConfig overrideConfig);

    /**
     * The <code>Scan</code> operation returns one or more items and item attributes by accessing every item in a table
     * or a secondary index. To have DynamoDB return fewer items, you can provide a <code>FilterExpression</code>
     * operation.
     *
     * <p>If the total size of scanned items exceeds the maximum dataset size limit of 1 MB, the scan completes and
     * results are returned to the user. The <code>LastEvaluatedKey</code> value is also returned and the requestor can
     * use the <code>LastEvaluatedKey</code> to continue the scan in a subsequent operation. Each scan response also
     * includes number of items that were scanned (ScannedCount) as part of the request. If using a <code>
     * FilterExpression</code>, a scan result can result in no items meeting the criteria and the <code>Count</code>
     * will result in zero. If you did not use a <code>FilterExpression</code> in the scan request, then <code>Count</code>
     * is the same as <code>ScannedCount</code>.
     *
     * <p><code>Count</code> and <code>ScannedCount</code> only return the count of items specific to a single scan
     * request and, unless the table is less than 1MB, do not represent the total number of items in the table.
     *
     * <p>A single <code>Scan</code> operation first reads up to the maximum number of items set (if using the <code>
     * Limit</code> parameter) or a maximum of 1 MB of data and then applies any filtering to the results if a <code>
     * FilterExpression</code> is provided. If <code>LastEvaluatedKey</code> is present in the response, pagination is
     * required to complete the full table scan. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.Pagination">Paginating the Results</a> in the <i>
     * Amazon DynamoDB Developer Guide</i>.
     *
     * <p><code>Scan</code> operations proceed sequentially; however, for faster performance on a large table or
     * secondary index, applications can request a parallel <code>Scan</code> operation by providing the <code>Segment</code>
     * and <code>TotalSegments</code> parameters. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.ParallelScan">Parallel Scan</a> in the <i>Amazon
     * DynamoDB Developer Guide</i>.
     *
     * <p>By default, a <code>Scan</code> uses eventually consistent reads when accessing the items in a table.
     * Therefore, the results from an eventually consistent <code>Scan</code> may not include the latest item changes at
     * the time the scan iterates through each item in the table. If you require a strongly consistent read of each item
     * as the scan iterates through the items in the table, you can set the <code>ConsistentRead</code> parameter to
     * true. Strong consistency only relates to the consistency of the read at the item level.
     *
     * <p> DynamoDB does not provide snapshot isolation for a scan operation when the <code>ConsistentRead</code>
     * parameter is set to true. Thus, a DynamoDB scan operation does not guarantee that all reads in a scan see a
     * consistent snapshot of the table when the scan operation was requested.
     *
     * <h4>Examples</h4>
     * <h5>To scan a table</h5>
     *
     * <p>This example scans the entire Music table, and then narrows the results to songs by the artist "No One You Know". For each item, only the album title and song title are returned.{@snippet :
     * var input = ScanInput.builder()
     *                 .tableName("Music").filterExpression("Artist = :a").projectionExpression("#ST, #AT").expressionAttributeNames(Map.of(
     *                                               "#ST", "SongTitle",
     *                                               "#AT", "AlbumTitle"
     *                                           )).expressionAttributeValues(Map.of(":a", AttributeValue.SMember("No One You Know")))
     *                 .build();
     *
     * var result = client.scan(input);
     * result.equals(ScanOutput.builder()
     *                   .count(2).items(List.of(
     *                              Map.of(
     *                                  "SongTitle", AttributeValue.SMember("Call Me Today"),
     *                                  "AlbumTitle", AttributeValue.SMember("Somewhat Famous")
     *                              ),
     *                              Map.of(
     *                                  "SongTitle", AttributeValue.SMember("Scared of My Shadow"),
     *                                  "AlbumTitle", AttributeValue.SMember("Blue Sky Blues")
     *                              )
     *                          )).scannedCount(3).consumedCapacity(ConsumedCapacity.builder()
     *
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    default ScanOutput scan(ScanInput input) {
        return scan(input, null);
    }

    /**
     * The <code>Scan</code> operation returns one or more items and item attributes by accessing every item in a table
     * or a secondary index. To have DynamoDB return fewer items, you can provide a <code>FilterExpression</code>
     * operation.
     *
     * <p>If the total size of scanned items exceeds the maximum dataset size limit of 1 MB, the scan completes and
     * results are returned to the user. The <code>LastEvaluatedKey</code> value is also returned and the requestor can
     * use the <code>LastEvaluatedKey</code> to continue the scan in a subsequent operation. Each scan response also
     * includes number of items that were scanned (ScannedCount) as part of the request. If using a <code>
     * FilterExpression</code>, a scan result can result in no items meeting the criteria and the <code>Count</code>
     * will result in zero. If you did not use a <code>FilterExpression</code> in the scan request, then <code>Count</code>
     * is the same as <code>ScannedCount</code>.
     *
     * <p><code>Count</code> and <code>ScannedCount</code> only return the count of items specific to a single scan
     * request and, unless the table is less than 1MB, do not represent the total number of items in the table.
     *
     * <p>A single <code>Scan</code> operation first reads up to the maximum number of items set (if using the <code>
     * Limit</code> parameter) or a maximum of 1 MB of data and then applies any filtering to the results if a <code>
     * FilterExpression</code> is provided. If <code>LastEvaluatedKey</code> is present in the response, pagination is
     * required to complete the full table scan. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.Pagination">Paginating the Results</a> in the <i>
     * Amazon DynamoDB Developer Guide</i>.
     *
     * <p><code>Scan</code> operations proceed sequentially; however, for faster performance on a large table or
     * secondary index, applications can request a parallel <code>Scan</code> operation by providing the <code>Segment</code>
     * and <code>TotalSegments</code> parameters. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.ParallelScan">Parallel Scan</a> in the <i>Amazon
     * DynamoDB Developer Guide</i>.
     *
     * <p>By default, a <code>Scan</code> uses eventually consistent reads when accessing the items in a table.
     * Therefore, the results from an eventually consistent <code>Scan</code> may not include the latest item changes at
     * the time the scan iterates through each item in the table. If you require a strongly consistent read of each item
     * as the scan iterates through the items in the table, you can set the <code>ConsistentRead</code> parameter to
     * true. Strong consistency only relates to the consistency of the read at the item level.
     *
     * <p> DynamoDB does not provide snapshot isolation for a scan operation when the <code>ConsistentRead</code>
     * parameter is set to true. Thus, a DynamoDB scan operation does not guarantee that all reads in a scan see a
     * consistent snapshot of the table when the scan operation was requested.
     *
     * <h4>Examples</h4>
     * <h5>To scan a table</h5>
     *
     * <p>This example scans the entire Music table, and then narrows the results to songs by the artist "No One You Know". For each item, only the album title and song title are returned.{@snippet :
     * var input = ScanInput.builder()
     *                 .tableName("Music").filterExpression("Artist = :a").projectionExpression("#ST, #AT").expressionAttributeNames(Map.of(
     *                                               "#ST", "SongTitle",
     *                                               "#AT", "AlbumTitle"
     *                                           )).expressionAttributeValues(Map.of(":a", AttributeValue.SMember("No One You Know")))
     *                 .build();
     *
     * var result = client.scan(input);
     * result.equals(ScanOutput.builder()
     *                   .count(2).items(List.of(
     *                              Map.of(
     *                                  "SongTitle", AttributeValue.SMember("Call Me Today"),
     *                                  "AlbumTitle", AttributeValue.SMember("Somewhat Famous")
     *                              ),
     *                              Map.of(
     *                                  "SongTitle", AttributeValue.SMember("Scared of My Shadow"),
     *                                  "AlbumTitle", AttributeValue.SMember("Blue Sky Blues")
     *                              )
     *                          )).scannedCount(3).consumedCapacity(ConsumedCapacity.builder()
     *
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    ScanOutput scan(ScanInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #scan} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ScanOutput> scanPaginator(ScanInput input) {
        return Paginator.paginate(input, Scan.instance(), this::scan);
    }

    /**
     * Performs a vector similarity search on a vector index associated with an Amazon DynamoDB table, and returns the
     * most similar items sorted by similarity score based on the distance function configured for the index.
     *
     * <p>Score interpretation depends on the distance function:
     *
     * <ul>
     *   <li>
     *     <code>COSINE</code> - Returns the items with the <i>k smallest</i> scores. Scores range from 0
     *     (identical) to 2 (opposite). Lower scores indicate higher similarity.
     *   </li>
     *   <li>
     *     <code>EUCLIDEAN</code> - Returns the items with the <i>k smallest</i> scores. Scores represent the
     *     Euclidean distance between vectors. Lower scores indicate higher similarity.
     *   </li>
     *   <li>
     *     <code>DOT_PRODUCT</code> - Returns the items with the <i>k highest</i> scores. Higher scores indicate
     *     higher similarity.
     *   </li>
     * </ul>
     *
     * <h4>Examples</h4>
     * <h5>To search for similar vectors</h5>
     *
     * <p>This example searches the Products table for the top 3 items most similar to a provided vector, using the 'cosine-product-idx' vector index. The SearchConditionExpression filters results to the 'Electronics' category. The operation returns only the ProductName and Price attributes.{@snippet :
     * var input = SearchVectorsInput.builder()
     *                 .tableName("Products").indexName("cosine-product-idx").searchVector(List.of(
     *                                   AttributeValue.NMember("0.12"),
     *                                   AttributeValue.NMember("0.85"),
     *                                   AttributeValue.NMember("0.44"),
     *                                   AttributeValue.NMember("0.67")
     *                               )).topK(3).searchConditionExpression("Category = :cat").projectionExpression("ProductName, Price").expressionAttributeValues(Map.of(":cat", AttributeValue.SMember("Electronics"))).returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
     *                 .build();
     *
     * var result = client.searchVectors(input);
     * result.equals(SearchVectorsOutput.builder()
     *                   .searchResults(List.of(
     *                                      SearchResultItem.builder()
     *                                          .item(Map.of(
     *                                                    "ProductName", AttributeValue.SMember("Wireless Headphones"),
     *                                                    "Price", AttributeValue.NMember("79.99")
     *                                                )).score(0.95)
     *                                          .build()
     *                                      ,
     *                                      SearchResultItem.builder()
     *                                          .item(Map.of(
     *                                                    "ProductName", AttributeValue.SMember("Bluetooth Speaker"),
     *                                                    "Price", AttributeValue.NMember("49.99")
     *                                                )).score(0.87)
     *                                          .build()
     *                                      ,
     *                                      SearchResultItem.builder()
     *                                          .item(Map.of(
     *                                                    "ProductName", AttributeValue.SMember("USB-C Hub"),
     *                                                    "Price", AttributeValue.NMember("34.99")
     *                                                )).score(0.82)
     *                                          .build()
     *                                  )).consumedCapacity(VectorCapacity.builder()
     *                                         .vectorSearchRequestBytes(1024)
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    default SearchVectorsOutput searchVectors(SearchVectorsInput input) {
        return searchVectors(input, null);
    }

    /**
     * Performs a vector similarity search on a vector index associated with an Amazon DynamoDB table, and returns the
     * most similar items sorted by similarity score based on the distance function configured for the index.
     *
     * <p>Score interpretation depends on the distance function:
     *
     * <ul>
     *   <li>
     *     <code>COSINE</code> - Returns the items with the <i>k smallest</i> scores. Scores range from 0
     *     (identical) to 2 (opposite). Lower scores indicate higher similarity.
     *   </li>
     *   <li>
     *     <code>EUCLIDEAN</code> - Returns the items with the <i>k smallest</i> scores. Scores represent the
     *     Euclidean distance between vectors. Lower scores indicate higher similarity.
     *   </li>
     *   <li>
     *     <code>DOT_PRODUCT</code> - Returns the items with the <i>k highest</i> scores. Higher scores indicate
     *     higher similarity.
     *   </li>
     * </ul>
     *
     * <h4>Examples</h4>
     * <h5>To search for similar vectors</h5>
     *
     * <p>This example searches the Products table for the top 3 items most similar to a provided vector, using the 'cosine-product-idx' vector index. The SearchConditionExpression filters results to the 'Electronics' category. The operation returns only the ProductName and Price attributes.{@snippet :
     * var input = SearchVectorsInput.builder()
     *                 .tableName("Products").indexName("cosine-product-idx").searchVector(List.of(
     *                                   AttributeValue.NMember("0.12"),
     *                                   AttributeValue.NMember("0.85"),
     *                                   AttributeValue.NMember("0.44"),
     *                                   AttributeValue.NMember("0.67")
     *                               )).topK(3).searchConditionExpression("Category = :cat").projectionExpression("ProductName, Price").expressionAttributeValues(Map.of(":cat", AttributeValue.SMember("Electronics"))).returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
     *                 .build();
     *
     * var result = client.searchVectors(input);
     * result.equals(SearchVectorsOutput.builder()
     *                   .searchResults(List.of(
     *                                      SearchResultItem.builder()
     *                                          .item(Map.of(
     *                                                    "ProductName", AttributeValue.SMember("Wireless Headphones"),
     *                                                    "Price", AttributeValue.NMember("79.99")
     *                                                )).score(0.95)
     *                                          .build()
     *                                      ,
     *                                      SearchResultItem.builder()
     *                                          .item(Map.of(
     *                                                    "ProductName", AttributeValue.SMember("Bluetooth Speaker"),
     *                                                    "Price", AttributeValue.NMember("49.99")
     *                                                )).score(0.87)
     *                                          .build()
     *                                      ,
     *                                      SearchResultItem.builder()
     *                                          .item(Map.of(
     *                                                    "ProductName", AttributeValue.SMember("USB-C Hub"),
     *                                                    "Price", AttributeValue.NMember("34.99")
     *                                                )).score(0.82)
     *                                          .build()
     *                                  )).consumedCapacity(VectorCapacity.builder()
     *                                         .vectorSearchRequestBytes(1024)
     *                                         .build())
     *                   .build());
     * }
     *
     * @throws InternalServerError
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     */
    SearchVectorsOutput searchVectors(SearchVectorsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Associate a set of tags with an Amazon DynamoDB resource. You can then activate these user-defined tags so that
     * they appear on the Billing and Cost Management console for cost allocation tracking. You can call TagResource up
     * to five times per second, per account.
     *
     * <ul>
     *   <li>
     *     <code>TagResource</code> is an asynchronous operation. If you issue a <a>ListTagsOfResource</a> request
     *     immediately after a <code>TagResource</code> request, DynamoDB might return your previous tag set, if
     *     there was one, or an empty tag set. This is because <code>ListTagsOfResource</code> uses an eventually
     *     consistent query, and the metadata for your tags or table might not be available at that moment. Wait for
     *     a few seconds, and then try the <code>ListTagsOfResource</code> request again.
     *   </li>
     *   <li>
     *     The application or removal of tags using <code>TagResource</code> and <code>UntagResource</code> APIs is
     *     eventually consistent. <code>ListTagsOfResource</code> API will only reflect the changes after a few
     *     seconds.
     *   </li>
     * </ul>
     *
     * <p>For an overview on tagging DynamoDB resources, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default TagResourceOutput tagResource(TagResourceInput input) {
        return tagResource(input, null);
    }

    /**
     * Associate a set of tags with an Amazon DynamoDB resource. You can then activate these user-defined tags so that
     * they appear on the Billing and Cost Management console for cost allocation tracking. You can call TagResource up
     * to five times per second, per account.
     *
     * <ul>
     *   <li>
     *     <code>TagResource</code> is an asynchronous operation. If you issue a <a>ListTagsOfResource</a> request
     *     immediately after a <code>TagResource</code> request, DynamoDB might return your previous tag set, if
     *     there was one, or an empty tag set. This is because <code>ListTagsOfResource</code> uses an eventually
     *     consistent query, and the metadata for your tags or table might not be available at that moment. Wait for
     *     a few seconds, and then try the <code>ListTagsOfResource</code> request again.
     *   </li>
     *   <li>
     *     The application or removal of tags using <code>TagResource</code> and <code>UntagResource</code> APIs is
     *     eventually consistent. <code>ListTagsOfResource</code> API will only reflect the changes after a few
     *     seconds.
     *   </li>
     * </ul>
     *
     * <p>For an overview on tagging DynamoDB resources, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    TagResourceOutput tagResource(TagResourceInput input, RequestOverrideConfig overrideConfig);

    /**
     * <code>TransactGetItems</code> is a synchronous operation that atomically retrieves multiple items from one or
     * more tables (but not from indexes) in a single account and Region. A <code>TransactGetItems</code> call can
     * contain up to 100 <code>TransactGetItem</code> objects, each of which contains a <code>Get</code> structure that
     * specifies an item to retrieve from a table in the account and Region. A call to <code>TransactGetItems</code>
     * cannot retrieve items from tables in more than one Amazon Web Services account or Region. The aggregate size of
     * the items in the transaction cannot exceed 4 MB.
     *
     * <p>DynamoDB rejects the entire <code>TransactGetItems</code> request if any of the following is true:
     *
     * <ul>
     *   <li>
     *     A conflicting operation is in the process of updating an item to be read.
     *   </li>
     *   <li>
     *     There is insufficient provisioned capacity for the transaction to be completed.
     *   </li>
     *   <li>
     *     There is a user error, such as an invalid data format.
     *   </li>
     *   <li>
     *     The aggregate size of the items in the transaction exceeded 4 MB.
     *   </li>
     * </ul>
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionCanceledException
     */
    default TransactGetItemsOutput transactGetItems(TransactGetItemsInput input) {
        return transactGetItems(input, null);
    }

    /**
     * <code>TransactGetItems</code> is a synchronous operation that atomically retrieves multiple items from one or
     * more tables (but not from indexes) in a single account and Region. A <code>TransactGetItems</code> call can
     * contain up to 100 <code>TransactGetItem</code> objects, each of which contains a <code>Get</code> structure that
     * specifies an item to retrieve from a table in the account and Region. A call to <code>TransactGetItems</code>
     * cannot retrieve items from tables in more than one Amazon Web Services account or Region. The aggregate size of
     * the items in the transaction cannot exceed 4 MB.
     *
     * <p>DynamoDB rejects the entire <code>TransactGetItems</code> request if any of the following is true:
     *
     * <ul>
     *   <li>
     *     A conflicting operation is in the process of updating an item to be read.
     *   </li>
     *   <li>
     *     There is insufficient provisioned capacity for the transaction to be completed.
     *   </li>
     *   <li>
     *     There is a user error, such as an invalid data format.
     *   </li>
     *   <li>
     *     The aggregate size of the items in the transaction exceeded 4 MB.
     *   </li>
     * </ul>
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionCanceledException
     */
    TransactGetItemsOutput transactGetItems(TransactGetItemsInput input, RequestOverrideConfig overrideConfig);

    /**
     * <code>TransactWriteItems</code> is a synchronous write operation that groups up to 100 action requests. These
     * actions can target items in different tables, but not in different Amazon Web Services accounts or Regions, and
     * no two actions can target the same item. For example, you cannot both <code>ConditionCheck</code> and <code>
     * Update</code> the same item. The aggregate size of the items in the transaction cannot exceed 4 MB.
     *
     * <p>The actions are completed atomically so that either all of them succeed, or all of them fail. They are defined
     * by the following objects:
     *
     * <ul>
     *   <li>
     *     <code>Put</code>  —   Initiates a <code>PutItem</code> operation to write a new item. This structure
     *     specifies the primary key of the item to be written, the name of the table to write it in, an optional
     *     condition expression that must be satisfied for the write to succeed, a list of the item's attributes,
     *     and a field indicating whether to retrieve the item's attributes if the condition is not met.
     *   </li>
     *   <li>
     *     <code>Update</code>  —   Initiates an <code>UpdateItem</code> operation to update an existing item. This
     *     structure specifies the primary key of the item to be updated, the name of the table where it resides, an
     *     optional condition expression that must be satisfied for the update to succeed, an expression that
     *     defines one or more attributes to be updated, and a field indicating whether to retrieve the item's
     *     attributes if the condition is not met.
     *   </li>
     *   <li>
     *     <code>Delete</code>  —   Initiates a <code>DeleteItem</code> operation to delete an existing item. This
     *     structure specifies the primary key of the item to be deleted, the name of the table where it resides, an
     *     optional condition expression that must be satisfied for the deletion to succeed, and a field indicating
     *     whether to retrieve the item's attributes if the condition is not met.
     *   </li>
     *   <li>
     *     <code>ConditionCheck</code>  —   Applies a condition to an item that is not being modified by the
     *     transaction. This structure specifies the primary key of the item to be checked, the name of the table
     *     where it resides, a condition expression that must be satisfied for the transaction to succeed, and a
     *     field indicating whether to retrieve the item's attributes if the condition is not met.
     *   </li>
     * </ul>
     *
     * <p>DynamoDB rejects the entire <code>TransactWriteItems</code> request if any of the following is true:
     *
     * <ul>
     *   <li>
     *     A condition in one of the condition expressions is not met.
     *   </li>
     *   <li>
     *     An ongoing operation is in the process of updating the same item.
     *   </li>
     *   <li>
     *     There is insufficient provisioned capacity for the transaction to be completed.
     *   </li>
     *   <li>
     *     An item size becomes too large (bigger than 400 KB), a local secondary index (LSI) becomes too large, or
     *     a similar validation error occurs because of changes made by the transaction.
     *   </li>
     *   <li>
     *     The aggregate size of the items in the transaction exceeds 4 MB.
     *   </li>
     *   <li>
     *     There is a user error, such as an invalid data format.
     *   </li>
     * </ul>
     *
     * @throws IdempotentParameterMismatchException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionCanceledException
     * @throws TransactionInProgressException
     */
    default TransactWriteItemsOutput transactWriteItems(TransactWriteItemsInput input) {
        return transactWriteItems(input, null);
    }

    /**
     * <code>TransactWriteItems</code> is a synchronous write operation that groups up to 100 action requests. These
     * actions can target items in different tables, but not in different Amazon Web Services accounts or Regions, and
     * no two actions can target the same item. For example, you cannot both <code>ConditionCheck</code> and <code>
     * Update</code> the same item. The aggregate size of the items in the transaction cannot exceed 4 MB.
     *
     * <p>The actions are completed atomically so that either all of them succeed, or all of them fail. They are defined
     * by the following objects:
     *
     * <ul>
     *   <li>
     *     <code>Put</code>  —   Initiates a <code>PutItem</code> operation to write a new item. This structure
     *     specifies the primary key of the item to be written, the name of the table to write it in, an optional
     *     condition expression that must be satisfied for the write to succeed, a list of the item's attributes,
     *     and a field indicating whether to retrieve the item's attributes if the condition is not met.
     *   </li>
     *   <li>
     *     <code>Update</code>  —   Initiates an <code>UpdateItem</code> operation to update an existing item. This
     *     structure specifies the primary key of the item to be updated, the name of the table where it resides, an
     *     optional condition expression that must be satisfied for the update to succeed, an expression that
     *     defines one or more attributes to be updated, and a field indicating whether to retrieve the item's
     *     attributes if the condition is not met.
     *   </li>
     *   <li>
     *     <code>Delete</code>  —   Initiates a <code>DeleteItem</code> operation to delete an existing item. This
     *     structure specifies the primary key of the item to be deleted, the name of the table where it resides, an
     *     optional condition expression that must be satisfied for the deletion to succeed, and a field indicating
     *     whether to retrieve the item's attributes if the condition is not met.
     *   </li>
     *   <li>
     *     <code>ConditionCheck</code>  —   Applies a condition to an item that is not being modified by the
     *     transaction. This structure specifies the primary key of the item to be checked, the name of the table
     *     where it resides, a condition expression that must be satisfied for the transaction to succeed, and a
     *     field indicating whether to retrieve the item's attributes if the condition is not met.
     *   </li>
     * </ul>
     *
     * <p>DynamoDB rejects the entire <code>TransactWriteItems</code> request if any of the following is true:
     *
     * <ul>
     *   <li>
     *     A condition in one of the condition expressions is not met.
     *   </li>
     *   <li>
     *     An ongoing operation is in the process of updating the same item.
     *   </li>
     *   <li>
     *     There is insufficient provisioned capacity for the transaction to be completed.
     *   </li>
     *   <li>
     *     An item size becomes too large (bigger than 400 KB), a local secondary index (LSI) becomes too large, or
     *     a similar validation error occurs because of changes made by the transaction.
     *   </li>
     *   <li>
     *     The aggregate size of the items in the transaction exceeds 4 MB.
     *   </li>
     *   <li>
     *     There is a user error, such as an invalid data format.
     *   </li>
     * </ul>
     *
     * @throws IdempotentParameterMismatchException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ProvisionedThroughputExceededException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionCanceledException
     * @throws TransactionInProgressException
     */
    TransactWriteItemsOutput transactWriteItems(TransactWriteItemsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Removes the association of tags from an Amazon DynamoDB resource. You can call <code>UntagResource</code> up to
     * five times per second, per account.
     *
     * <ul>
     *   <li>
     *     <code>UntagResource</code> is an asynchronous operation. If you issue a <a>ListTagsOfResource</a> request
     *     immediately after an <code>UntagResource</code> request, DynamoDB might return your previous tag set, if
     *     there was one, or an empty tag set. This is because <code>ListTagsOfResource</code> uses an eventually
     *     consistent query, and the metadata for your tags or table might not be available at that moment. Wait for
     *     a few seconds, and then try the <code>ListTagsOfResource</code> request again.
     *   </li>
     *   <li>
     *     The application or removal of tags using <code>TagResource</code> and <code>UntagResource</code> APIs is
     *     eventually consistent. <code>ListTagsOfResource</code> API will only reflect the changes after a few
     *     seconds.
     *   </li>
     * </ul>
     *
     * <p>For an overview on tagging DynamoDB resources, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default UntagResourceOutput untagResource(UntagResourceInput input) {
        return untagResource(input, null);
    }

    /**
     * Removes the association of tags from an Amazon DynamoDB resource. You can call <code>UntagResource</code> up to
     * five times per second, per account.
     *
     * <ul>
     *   <li>
     *     <code>UntagResource</code> is an asynchronous operation. If you issue a <a>ListTagsOfResource</a> request
     *     immediately after an <code>UntagResource</code> request, DynamoDB might return your previous tag set, if
     *     there was one, or an empty tag set. This is because <code>ListTagsOfResource</code> uses an eventually
     *     consistent query, and the metadata for your tags or table might not be available at that moment. Wait for
     *     a few seconds, and then try the <code>ListTagsOfResource</code> request again.
     *   </li>
     *   <li>
     *     The application or removal of tags using <code>TagResource</code> and <code>UntagResource</code> APIs is
     *     eventually consistent. <code>ListTagsOfResource</code> API will only reflect the changes after a few
     *     seconds.
     *   </li>
     * </ul>
     *
     * <p>For an overview on tagging DynamoDB resources, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    UntagResourceOutput untagResource(UntagResourceInput input, RequestOverrideConfig overrideConfig);

    /**
     * <code>UpdateContinuousBackups</code> enables or disables point in time recovery for the specified table. A
     * successful <code>UpdateContinuousBackups</code> call returns the current <code>ContinuousBackupsDescription</code>
     * . Continuous backups are <code>ENABLED</code> on all tables at table creation. If point in time recovery is
     * enabled, <code>PointInTimeRecoveryStatus</code> will be set to ENABLED.
     *
     * <p> Once continuous backups and point in time recovery are enabled, you can restore to any point in time within <code>
     * EarliestRestorableDateTime</code> and <code>LatestRestorableDateTime</code>.
     *
     * <p><code>LatestRestorableDateTime</code> is typically 5 minutes before the current time. You can restore your
     * table to any point in time in the last 35 days. You can set the <code>RecoveryPeriodInDays</code> to any value
     * between 1 and 35 days.
     *
     * @throws ContinuousBackupsUnavailableException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws TableNotFoundException
     */
    default UpdateContinuousBackupsOutput updateContinuousBackups(UpdateContinuousBackupsInput input) {
        return updateContinuousBackups(input, null);
    }

    /**
     * <code>UpdateContinuousBackups</code> enables or disables point in time recovery for the specified table. A
     * successful <code>UpdateContinuousBackups</code> call returns the current <code>ContinuousBackupsDescription</code>
     * . Continuous backups are <code>ENABLED</code> on all tables at table creation. If point in time recovery is
     * enabled, <code>PointInTimeRecoveryStatus</code> will be set to ENABLED.
     *
     * <p> Once continuous backups and point in time recovery are enabled, you can restore to any point in time within <code>
     * EarliestRestorableDateTime</code> and <code>LatestRestorableDateTime</code>.
     *
     * <p><code>LatestRestorableDateTime</code> is typically 5 minutes before the current time. You can restore your
     * table to any point in time in the last 35 days. You can set the <code>RecoveryPeriodInDays</code> to any value
     * between 1 and 35 days.
     *
     * @throws ContinuousBackupsUnavailableException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws TableNotFoundException
     */
    UpdateContinuousBackupsOutput updateContinuousBackups(UpdateContinuousBackupsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Updates the status for contributor insights for a specific table or index. CloudWatch Contributor Insights for
     * DynamoDB graphs display the partition key and (if applicable) sort key of frequently accessed items and
     * frequently throttled items in plaintext. If you require the use of Amazon Web Services Key Management Service
     * (KMS) to encrypt this table’s partition key and sort key data with an Amazon Web Services managed key or customer
     * managed key, you should not enable CloudWatch Contributor Insights for DynamoDB for this table.
     *
     * @throws InternalServerError
     * @throws ResourceNotFoundException
     */
    default UpdateContributorInsightsOutput updateContributorInsights(UpdateContributorInsightsInput input) {
        return updateContributorInsights(input, null);
    }

    /**
     * Updates the status for contributor insights for a specific table or index. CloudWatch Contributor Insights for
     * DynamoDB graphs display the partition key and (if applicable) sort key of frequently accessed items and
     * frequently throttled items in plaintext. If you require the use of Amazon Web Services Key Management Service
     * (KMS) to encrypt this table’s partition key and sort key data with an Amazon Web Services managed key or customer
     * managed key, you should not enable CloudWatch Contributor Insights for DynamoDB for this table.
     *
     * @throws InternalServerError
     * @throws ResourceNotFoundException
     */
    UpdateContributorInsightsOutput updateContributorInsights(UpdateContributorInsightsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Adds or removes replicas in the specified global table. The global table must already exist to be able to use
     * this operation. Any replica to be added must be empty, have the same name as the global table, have the same key
     * schema, have DynamoDB Streams enabled, and have the same provisioned and maximum write capacity units.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * <p> If you are using global tables <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Version 2019.11.21</a> (Current) you can use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_UpdateTable.html">UpdateTable</a> instead.
     *
     * <p> Although you can use <code>UpdateGlobalTable</code> to add replicas and remove replicas in a single request,
     * for simplicity we recommend that you issue separate requests for adding or removing replicas.
     *
     * <p> If global secondary indexes are specified, then the following conditions must also be met:
     *
     * <ul>
     *   <li>
     *      The global secondary indexes must have the same name.
     *   </li>
     *   <li>
     *      The global secondary indexes must have the same hash key and sort key (if present).
     *   </li>
     *   <li>
     *      The global secondary indexes must have the same provisioned and maximum write capacity units.
     *   </li>
     * </ul>
     *
     * @throws GlobalTableNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ReplicaAlreadyExistsException
     * @throws ReplicaNotFoundException
     * @throws TableNotFoundException
     */
    default UpdateGlobalTableOutput updateGlobalTable(UpdateGlobalTableInput input) {
        return updateGlobalTable(input, null);
    }

    /**
     * Adds or removes replicas in the specified global table. The global table must already exist to be able to use
     * this operation. Any replica to be added must be empty, have the same name as the global table, have the same key
     * schema, have DynamoDB Streams enabled, and have the same provisioned and maximum write capacity units.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * <p> If you are using global tables <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Version 2019.11.21</a> (Current) you can use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_UpdateTable.html">UpdateTable</a> instead.
     *
     * <p> Although you can use <code>UpdateGlobalTable</code> to add replicas and remove replicas in a single request,
     * for simplicity we recommend that you issue separate requests for adding or removing replicas.
     *
     * <p> If global secondary indexes are specified, then the following conditions must also be met:
     *
     * <ul>
     *   <li>
     *      The global secondary indexes must have the same name.
     *   </li>
     *   <li>
     *      The global secondary indexes must have the same hash key and sort key (if present).
     *   </li>
     *   <li>
     *      The global secondary indexes must have the same provisioned and maximum write capacity units.
     *   </li>
     * </ul>
     *
     * @throws GlobalTableNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ReplicaAlreadyExistsException
     * @throws ReplicaNotFoundException
     * @throws TableNotFoundException
     */
    UpdateGlobalTableOutput updateGlobalTable(UpdateGlobalTableInput input, RequestOverrideConfig overrideConfig);

    /**
     * Updates settings for a global table.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * @throws GlobalTableNotFoundException
     * @throws IndexNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ReplicaNotFoundException
     * @throws ResourceInUseException
     */
    default UpdateGlobalTableSettingsOutput updateGlobalTableSettings(UpdateGlobalTableSettingsInput input) {
        return updateGlobalTableSettings(input, null);
    }

    /**
     * Updates settings for a global table.
     *
     * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
     * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
     * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
     *
     * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To
     * update existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading
     * global tables</a>.
     *
     * @throws GlobalTableNotFoundException
     * @throws IndexNotFoundException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ReplicaNotFoundException
     * @throws ResourceInUseException
     */
    UpdateGlobalTableSettingsOutput updateGlobalTableSettings(UpdateGlobalTableSettingsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Edits an existing item's attributes, or adds a new item to the table if it does not already exist. You can put,
     * delete, or add attribute values. You can also perform a conditional update on an existing item (insert a new
     * attribute name-value pair if it doesn't exist, or replace an existing name-value pair if it has certain expected
     * attribute values).
     *
     * <p>You can also return the item's attribute values in the same <code>UpdateItem</code> operation using the <code>
     * ReturnValues</code> parameter.
     *
     * <h4>Examples</h4>
     * <h5>To update an item in a table</h5>
     *
     * <p>This example updates an item in the Music table. It adds a new attribute (Year) and modifies the AlbumTitle attribute.  All of the attributes in the item, as they appear after the update, are returned in the response.{@snippet :
     * var input = UpdateItemInput.builder()
     *                 .tableName("Music").key(Map.of(
     *                          "Artist", AttributeValue.SMember("Acme Band"),
     *                          "SongTitle", AttributeValue.SMember("Happy Day")
     *                      )).updateExpression("SET #Y = :y, #AT = :t").expressionAttributeNames(Map.of(
     *                                               "#Y", "Year",
     *                                               "#AT", "AlbumTitle"
     *                                           )).expressionAttributeValues(Map.of(
     *                                                ":y", AttributeValue.NMember("2015"),
     *                                                ":t", AttributeValue.SMember("Louder Than Ever")
     *                                            )).returnValues(ReturnValue.ALL_NEW)
     *                 .build();
     *
     * var result = client.updateItem(input);
     * result.equals(UpdateItemOutput.builder()
     *                   .attributes(Map.of(
     *                                   "AlbumTitle", AttributeValue.SMember("Louder Than Ever"),
     *                                   "Artist", AttributeValue.SMember("Acme Band"),
     *                                   "Year", AttributeValue.NMember("2015"),
     *                                   "SongTitle", AttributeValue.SMember("Happy Day")
     *                               ))
     *                   .build());
     * }
     *
     * @throws ConditionalCheckFailedException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws ReplicatedWriteConflictException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionConflictException
     */
    default UpdateItemOutput updateItem(UpdateItemInput input) {
        return updateItem(input, null);
    }

    /**
     * Edits an existing item's attributes, or adds a new item to the table if it does not already exist. You can put,
     * delete, or add attribute values. You can also perform a conditional update on an existing item (insert a new
     * attribute name-value pair if it doesn't exist, or replace an existing name-value pair if it has certain expected
     * attribute values).
     *
     * <p>You can also return the item's attribute values in the same <code>UpdateItem</code> operation using the <code>
     * ReturnValues</code> parameter.
     *
     * <h4>Examples</h4>
     * <h5>To update an item in a table</h5>
     *
     * <p>This example updates an item in the Music table. It adds a new attribute (Year) and modifies the AlbumTitle attribute.  All of the attributes in the item, as they appear after the update, are returned in the response.{@snippet :
     * var input = UpdateItemInput.builder()
     *                 .tableName("Music").key(Map.of(
     *                          "Artist", AttributeValue.SMember("Acme Band"),
     *                          "SongTitle", AttributeValue.SMember("Happy Day")
     *                      )).updateExpression("SET #Y = :y, #AT = :t").expressionAttributeNames(Map.of(
     *                                               "#Y", "Year",
     *                                               "#AT", "AlbumTitle"
     *                                           )).expressionAttributeValues(Map.of(
     *                                                ":y", AttributeValue.NMember("2015"),
     *                                                ":t", AttributeValue.SMember("Louder Than Ever")
     *                                            )).returnValues(ReturnValue.ALL_NEW)
     *                 .build();
     *
     * var result = client.updateItem(input);
     * result.equals(UpdateItemOutput.builder()
     *                   .attributes(Map.of(
     *                                   "AlbumTitle", AttributeValue.SMember("Louder Than Ever"),
     *                                   "Artist", AttributeValue.SMember("Acme Band"),
     *                                   "Year", AttributeValue.NMember("2015"),
     *                                   "SongTitle", AttributeValue.SMember("Happy Day")
     *                               ))
     *                   .build());
     * }
     *
     * @throws ConditionalCheckFailedException
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws ItemCollectionSizeLimitExceededException
     * @throws ProvisionedThroughputExceededException
     * @throws ReplicatedWriteConflictException
     * @throws RequestLimitExceeded
     * @throws ResourceNotFoundException
     * @throws ThrottlingException
     * @throws TransactionConflictException
     */
    UpdateItemOutput updateItem(UpdateItemInput input, RequestOverrideConfig overrideConfig);

    /**
     * The command to update the Kinesis stream destination.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default UpdateKinesisStreamingDestinationOutput updateKinesisStreamingDestination(UpdateKinesisStreamingDestinationInput input) {
        return updateKinesisStreamingDestination(input, null);
    }

    /**
     * The command to update the Kinesis stream destination.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    UpdateKinesisStreamingDestinationOutput updateKinesisStreamingDestination(UpdateKinesisStreamingDestinationInput input, RequestOverrideConfig overrideConfig);

    /**
     * Modifies the provisioned throughput settings, global secondary indexes, or DynamoDB Streams settings for a given
     * table.
     *
     * <p>You can only perform one of the following operations at once:
     *
     * <ul>
     *   <li>
     *     Modify the provisioned throughput settings of the table.
     *   </li>
     *   <li>
     *     Remove a global secondary index from the table.
     *   </li>
     *   <li>
     *     Create a new global secondary index on the table. After the index begins backfilling, you can use <code>
     *     UpdateTable</code> to perform other operations.
     *   </li>
     * </ul>
     *
     * <p><code>UpdateTable</code> is an asynchronous operation; while it's executing, the table status changes from <code>
     * ACTIVE</code> to <code>UPDATING</code>. While it's <code>UPDATING</code>, you can't issue another <code>
     * UpdateTable</code> request. When the table returns to the <code>ACTIVE</code> state, the <code>UpdateTable</code>
     * operation is complete.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default UpdateTableOutput updateTable(UpdateTableInput input) {
        return updateTable(input, null);
    }

    /**
     * Modifies the provisioned throughput settings, global secondary indexes, or DynamoDB Streams settings for a given
     * table.
     *
     * <p>You can only perform one of the following operations at once:
     *
     * <ul>
     *   <li>
     *     Modify the provisioned throughput settings of the table.
     *   </li>
     *   <li>
     *     Remove a global secondary index from the table.
     *   </li>
     *   <li>
     *     Create a new global secondary index on the table. After the index begins backfilling, you can use <code>
     *     UpdateTable</code> to perform other operations.
     *   </li>
     * </ul>
     *
     * <p><code>UpdateTable</code> is an asynchronous operation; while it's executing, the table status changes from <code>
     * ACTIVE</code> to <code>UPDATING</code>. While it's <code>UPDATING</code>, you can't issue another <code>
     * UpdateTable</code> request. When the table returns to the <code>ACTIVE</code> state, the <code>UpdateTable</code>
     * operation is complete.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    UpdateTableOutput updateTable(UpdateTableInput input, RequestOverrideConfig overrideConfig);

    /**
     * Updates auto scaling settings on your global tables at once.
     *
     * @throws InternalServerError
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default UpdateTableReplicaAutoScalingOutput updateTableReplicaAutoScaling(UpdateTableReplicaAutoScalingInput input) {
        return updateTableReplicaAutoScaling(input, null);
    }

    /**
     * Updates auto scaling settings on your global tables at once.
     *
     * @throws InternalServerError
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    UpdateTableReplicaAutoScalingOutput updateTableReplicaAutoScaling(UpdateTableReplicaAutoScalingInput input, RequestOverrideConfig overrideConfig);

    /**
     * The <code>UpdateTimeToLive</code> method enables or disables Time to Live (TTL) for the specified table. A
     * successful <code>UpdateTimeToLive</code> call returns the current <code>TimeToLiveSpecification</code>. It can
     * take up to one hour for the change to fully process. Any additional <code>UpdateTimeToLive</code> calls for the
     * same table during this one hour duration result in a <code>ValidationException</code>.
     *
     * <p>TTL compares the current time in epoch time format to the time stored in the TTL attribute of an item. If the
     * epoch time value stored in the attribute is less than the current time, the item is marked as expired and
     * subsequently deleted.
     *
     * <p> The epoch time format is the number of seconds elapsed since 12:00:00 AM January 1, 1970 UTC.
     *
     * <p>DynamoDB deletes expired items on a best-effort basis to ensure availability of throughput for other data
     * operations.
     *
     * <p>DynamoDB typically deletes expired items within two days of expiration. The exact duration within which an
     * item gets deleted after expiration is specific to the nature of the workload. Items that have expired and not
     * been deleted will still show up in reads, queries, and scans.
     *
     * <p>As items are deleted, they are removed from any local secondary index and global secondary index immediately
     * in the same eventually consistent way as a standard delete operation.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/TTL.html">Time To Live</a> in the Amazon DynamoDB Developer Guide.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    default UpdateTimeToLiveOutput updateTimeToLive(UpdateTimeToLiveInput input) {
        return updateTimeToLive(input, null);
    }

    /**
     * The <code>UpdateTimeToLive</code> method enables or disables Time to Live (TTL) for the specified table. A
     * successful <code>UpdateTimeToLive</code> call returns the current <code>TimeToLiveSpecification</code>. It can
     * take up to one hour for the change to fully process. Any additional <code>UpdateTimeToLive</code> calls for the
     * same table during this one hour duration result in a <code>ValidationException</code>.
     *
     * <p>TTL compares the current time in epoch time format to the time stored in the TTL attribute of an item. If the
     * epoch time value stored in the attribute is less than the current time, the item is marked as expired and
     * subsequently deleted.
     *
     * <p> The epoch time format is the number of seconds elapsed since 12:00:00 AM January 1, 1970 UTC.
     *
     * <p>DynamoDB deletes expired items on a best-effort basis to ensure availability of throughput for other data
     * operations.
     *
     * <p>DynamoDB typically deletes expired items within two days of expiration. The exact duration within which an
     * item gets deleted after expiration is specific to the nature of the workload. Items that have expired and not
     * been deleted will still show up in reads, queries, and scans.
     *
     * <p>As items are deleted, they are removed from any local secondary index and global secondary index immediately
     * in the same eventually consistent way as a standard delete operation.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/TTL.html">Time To Live</a> in the Amazon DynamoDB Developer Guide.
     *
     * @throws InternalServerError
     * @throws InvalidEndpointException
     * @throws LimitExceededException
     * @throws ResourceInUseException
     * @throws ResourceNotFoundException
     */
    UpdateTimeToLiveOutput updateTimeToLive(UpdateTimeToLiveInput input, RequestOverrideConfig overrideConfig);

    /**
     * Create a new {@link DynamoDBWaiter} instance that uses this client for polling.
     *
     * @return new {@link DynamoDBWaiter} instance.
     */
    DynamoDBWaiter waiter();

    /**
     * @return Configuration in use by client.
     */
    ClientConfig config();

    /**
     * Create a Builder for {@link DynamoDBClient}.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create a {@link RequestOverrideConfig} builder for this client.
     */
    static RequestOverrideBuilder requestOverrideBuilder() {
        return new RequestOverrideBuilder();
    }

    /**
     * Builder for {@link DynamoDBClient}.
     */
    final class Builder extends Client.Builder<DynamoDBClient, Builder> {
        private final AwsCredentialChainPlugin awsCredentialChainPlugin = new AwsCredentialChainPlugin();
        private final List<ClientPlugin> defaultPlugins = List.of(awsCredentialChainPlugin);

        private static final ProtocolSettings protocolSettings = ProtocolSettings.builder()
                .service(ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810"))
                .serviceVersion("2012-08-10")
                .serviceSchema(DynamoDBApiService.instance().schema())
                .build();
        private static final AwsJson1_0Trait protocolTrait = new AwsJson1_0Trait.Provider().createTrait(
            ShapeId.from("aws.protocols#awsJson1_0"),
            Node.objectNode()
        );

        private static final SigV4Trait sigv4Scheme = (SigV4Trait) new SigV4Trait.Provider().createTrait(
            ShapeId.from("aws.auth#sigv4"),
            Node.objectNodeBuilder()
                .withMember("name", "dynamodb")
                .build()
        );
        private static final AuthSchemeFactory<SigV4Trait> sigv4SchemeFactory = new SigV4AuthScheme.Factory();

        private Builder() {
            configBuilder()
                .putSupportedAuthSchemes(sigv4SchemeFactory.createAuthScheme(sigv4Scheme))
                .service(DynamoDBApiService.instance());
        }

        @Override
        public DynamoDBClient build() {
            for (var plugin : defaultPlugins) {
                addPlugin(plugin);
            }
            if (configBuilder().protocol() == null) {
                configBuilder().protocol(new AwsJson1Protocol.Factory().createProtocol(protocolSettings, protocolTrait));
            }
            if (configBuilder().transport() == null) {
                configBuilder().transport(new JavaHttpClientTransport.Factory().createTransport(Document.EMPTY_MAP, Document.EMPTY_MAP));
            }
            try (var stream = getClass().getResourceAsStream("/META-INF/endpoints/DynamoDB_20120810.bdd")) {
                var bytecode = new RulesEngineBuilder().load(stream.readAllBytes());
                putConfig(RulesEngineSettings.BYTECODE, bytecode);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load BDD bytecode binary file", e);
            }

            return new DynamoDBClientImpl(this);
        }
    }

    /**
     * Builder used to create a {@link RequestOverrideConfig} for {@link DynamoDBClient} operations.
     */
    final class RequestOverrideBuilder extends RequestOverrideConfig.OverrideBuilder<RequestOverrideBuilder> {}
}
