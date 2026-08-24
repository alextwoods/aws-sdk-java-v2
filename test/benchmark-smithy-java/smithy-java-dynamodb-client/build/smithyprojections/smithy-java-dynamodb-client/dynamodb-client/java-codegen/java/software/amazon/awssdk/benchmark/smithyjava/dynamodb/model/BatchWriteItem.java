package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.traits.OperationContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

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
 * requests. Typically, you would call <code>BatchWriteItem</code> in a loop. Each iteration would check for unprocessed
 * items and submit a new <code>BatchWriteItem</code> request with those unprocessed items until all items have been
 * processed.
 *
 * <p>If <code>BatchWriteItem</code> cannot process any items due to throttling (for example, insufficient provisioned
 * throughput on the tables in the request, or partition-level or account-level limits), it returns a <code>
 * ProvisionedThroughputExceededException</code> or a <code>ThrottlingException</code>. Both indicate that the request
 * was throttled; check the <code>ThrottlingReason</code> field in the returned exception for details.
 *
 * <p>If DynamoDB returns any unprocessed items, you should retry the batch operation on those items. However, <i>we
 * strongly recommend that you use an exponential backoff algorithm</i>. If you retry the batch operation immediately,
 * the underlying read or write requests can still fail due to throttling on the individual tables. If you delay the
 * batch operation using exponential backoff, the individual requests in the batch are much more likely to succeed.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ErrorHandling.html#Programming.Errors.BatchOperations">Batch Operations and Error Handling</a> in the <i>Amazon DynamoDB Developer Guide</i>
 * .
 *
 * <p>With <code>BatchWriteItem</code>, you can efficiently write or delete large amounts of data, such as from Amazon
 * EMR, or copy data from another database into DynamoDB. In order to improve performance with these large-scale
 * operations, <code>BatchWriteItem</code> does not behave in the same way as individual <code>PutItem</code> and <code>
 * DeleteItem</code> calls would. For example, you cannot specify conditions on individual put and delete requests, and <code>
 * BatchWriteItem</code> does not return deleted items in the response.
 *
 * <p>If you use a programming language that supports concurrency, you can use threads to write items in parallel. Your
 * application must include the necessary logic to manage the threads. With languages that don't support threading, you
 * must update or delete the specified items one at a time. In both situations, <code>BatchWriteItem</code> performs the
 * specified put and delete operations in parallel, giving you the power of the thread pool approach without having to
 * introduce complexity into your application.
 *
 * <p>Parallel processing reduces latency, but each specified put and delete request consumes the same number of write
 * capacity units whether it is processed in parallel or not. Delete operations on nonexistent items consume one write
 * capacity unit.
 *
 * <p>If one or more of the following is true, DynamoDB rejects the entire batch write operation:
 *
 * <ul>
 *   <li>
 *     One or more tables specified in the <code>BatchWriteItem</code> request does not exist.
 *   </li>
 *   <li>
 *     Primary key attributes specified on an item in the request do not match those in the corresponding table's
 *     primary key schema.
 *   </li>
 *   <li>
 *     You try to perform multiple operations on the same item in the same <code>BatchWriteItem</code> request. For
 *     example, you cannot put and delete the same item in the same <code>BatchWriteItem</code> request.
 *   </li>
 *   <li>
 *      Your request contains at least two items with identical hash and range keys (which essentially is two put
 *     operations).
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
 *     Any individual items with keys exceeding the key length limits. For a partition key, the limit is 2048 bytes
 *     and for a sort key, the limit is 1024 bytes.
 *   </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <h3>To add multiple items to a table</h3>
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
 */
@SmithyGenerated
public final class BatchWriteItem implements ApiOperation<BatchWriteItemInput, BatchWriteItemOutput> {

    private static final BatchWriteItem $INSTANCE = new BatchWriteItem();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#BatchWriteItem"),
            new OperationContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#operationContextParams"),
                Node.objectNodeBuilder()
                    .withMember("ResourceArnList", Node.objectNodeBuilder()
                        .withMember("path", "keys(RequestItems)")
                        .build())
                    .build()
            ));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(ItemCollectionSizeLimitExceededException.$ID, ItemCollectionSizeLimitExceededException.class, ItemCollectionSizeLimitExceededException::builder)
        .putType(ProvisionedThroughputExceededException.$ID, ProvisionedThroughputExceededException.class, ProvisionedThroughputExceededException::builder)
        .putType(ReplicatedWriteConflictException.$ID, ReplicatedWriteConflictException.class, ReplicatedWriteConflictException::builder)
        .putType(RequestLimitExceeded.$ID, RequestLimitExceeded.class, RequestLimitExceeded::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .putType(ThrottlingException.$ID, ThrottlingException.class, ThrottlingException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static BatchWriteItem instance() {
        return $INSTANCE;
    }

    private BatchWriteItem() {}

    @Override
    public ShapeBuilder<BatchWriteItemInput> inputBuilder() {
        return BatchWriteItemInput.builder();
    }

    @Override
    public ShapeBuilder<BatchWriteItemOutput> outputBuilder() {
        return BatchWriteItemOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return BatchWriteItemInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return BatchWriteItemOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, ItemCollectionSizeLimitExceededException.$SCHEMA, ProvisionedThroughputExceededException.$SCHEMA, ReplicatedWriteConflictException.$SCHEMA, RequestLimitExceeded.$SCHEMA, ResourceNotFoundException.$SCHEMA, ThrottlingException.$SCHEMA);
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return SCHEMES;
    }

    @Override
    public Schema inputStreamMember() {
        return null;
    }

    @Override
    public Schema outputStreamMember() {
        return null;
    }

    @Override
    public Schema idempotencyTokenMember() {
        return null;
    }

    @Override
    public ApiService service() {
        return DynamoDBApiService.instance();
    }
    }
