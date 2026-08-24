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
 * The <code>BatchGetItem</code> operation returns the attributes of one or more items from one or more tables. You
 * identify requested items by primary key.
 *
 * <p>A single operation can retrieve up to 16 MB of data, which can contain as many as 100 items. <code>BatchGetItem</code>
 * returns a partial result if the response size limit is exceeded, the table's provisioned throughput is exceeded, more
 * than 1MB per partition is requested, or an internal processing failure occurs. If a partial result is returned, the
 * operation returns a value for <code>UnprocessedKeys</code>. You can use this value to retry the operation starting
 * with the next item to get.
 *
 * <p>If you request more than 100 items, <code>BatchGetItem</code> returns a <code>ValidationException</code> with the
 * message "Too many items requested for the BatchGetItem call."
 *
 * <p>For example, if you ask to retrieve 100 items, but each individual item is 300 KB in size, the system returns 52
 * items (so as not to exceed the 16 MB limit). It also returns an appropriate <code>UnprocessedKeys</code> value so you
 * can get the next page of results. If desired, your application can include its own logic to assemble the pages of
 * results into one dataset.
 *
 * <p>If <i>none</i> of the items can be processed due to insufficient provisioned throughput on all of the tables in
 * the request, then <code>BatchGetItem</code> returns a <code>ProvisionedThroughputExceededException</code>. If <i>at
 * least one</i> of the items is successfully processed, then <code>BatchGetItem</code> completes successfully, while
 * returning the keys of the unread items in <code>UnprocessedKeys</code>.
 *
 * <p>If DynamoDB returns any unprocessed items, you should retry the batch operation on those items. However, <i>we
 * strongly recommend that you use an exponential backoff algorithm</i>. If you retry the batch operation immediately,
 * the underlying read or write requests can still fail due to throttling on the individual tables. If you delay the
 * batch operation using exponential backoff, the individual requests in the batch are much more likely to succeed.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ErrorHandling.html#BatchOperations">Batch Operations and Error Handling</a> in the <i>Amazon DynamoDB Developer Guide</i>
 * .
 *
 * <p>By default, <code>BatchGetItem</code> performs eventually consistent reads on every table in the request. If you
 * want strongly consistent reads instead, you can set <code>ConsistentRead</code> to <code>true</code> for any or all
 * tables.
 *
 * <p>In order to minimize response latency, <code>BatchGetItem</code> may retrieve items in parallel.
 *
 * <p>When designing your application, keep in mind that DynamoDB does not return items in any particular order. To help
 * parse the response by item, include the primary key values for the items in your request in the <code>
 * ProjectionExpression</code> parameter.
 *
 * <p>If a requested item does not exist, it is not returned in the result. Requests for nonexistent items consume the
 * minimum read capacity units according to the type of read. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#CapacityUnitCalculations">Working with Tables</a> in
 * the <i>Amazon DynamoDB Developer Guide</i>.
 *
 * <p><code>BatchGetItem</code> will result in a <code>ValidationException</code> if the same key is specified multiple
 * times.
 *
 * <h2>Examples</h2>
 * <h3>To retrieve multiple items from a table</h3>
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
 */
@SmithyGenerated
public final class BatchGetItem implements ApiOperation<BatchGetItemInput, BatchGetItemOutput> {

    private static final BatchGetItem $INSTANCE = new BatchGetItem();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#BatchGetItem"),
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
        .putType(ProvisionedThroughputExceededException.$ID, ProvisionedThroughputExceededException.class, ProvisionedThroughputExceededException::builder)
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
    public static BatchGetItem instance() {
        return $INSTANCE;
    }

    private BatchGetItem() {}

    @Override
    public ShapeBuilder<BatchGetItemInput> inputBuilder() {
        return BatchGetItemInput.builder();
    }

    @Override
    public ShapeBuilder<BatchGetItemOutput> outputBuilder() {
        return BatchGetItemOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return BatchGetItemInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return BatchGetItemOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, ProvisionedThroughputExceededException.$SCHEMA, RequestLimitExceeded.$SCHEMA, ResourceNotFoundException.$SCHEMA, ThrottlingException.$SCHEMA);
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
