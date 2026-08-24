package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * You must provide the name of the partition key attribute and a single value for that attribute. <code>Query</code>
 * returns all items with that partition key value. Optionally, you can provide a sort key attribute and use a
 * comparison operator to refine the search results.
 *
 * <p>Use the <code>KeyConditionExpression</code> parameter to provide a specific value for the partition key. The <code>
 * Query</code> operation will return all of the items from the table or index with that partition key value. You can
 * optionally narrow the scope of the <code>Query</code> operation by specifying a sort key value and a comparison
 * operator in <code>KeyConditionExpression</code>. To further refine the <code>Query</code> results, you can optionally
 * provide a <code>FilterExpression</code>. A <code>FilterExpression</code> determines which items within the results
 * should be returned to you. All of the other results are discarded.
 *
 * <p> A <code>Query</code> operation always returns a result set. If no matching items are found, the result set will
 * be empty. Queries that do not return results consume the minimum number of read capacity units for that type of read
 * operation.
 *
 * <p> DynamoDB calculates the number of read capacity units consumed based on item size, not on the amount of data that
 * is returned to an application. The number of capacity units consumed will be the same whether you request all of the
 * attributes (the default behavior) or just some of them (using a projection expression). The number will also be the
 * same whether or not you use a <code>FilterExpression</code>.
 *
 * <p><code>Query</code> results are always sorted by the sort key value. If the data type of the sort key is Number,
 * the results are returned in numeric order; otherwise, the results are returned in order of UTF-8 bytes. By default,
 * the sort order is ascending. To reverse the order, set the <code>ScanIndexForward</code> parameter to false.
 *
 * <p> A single <code>Query</code> operation will read up to the maximum number of items set (if using the <code>Limit</code>
 * parameter) or a maximum of 1 MB of data and then apply any filtering to the results using <code>FilterExpression</code>
 * . If <code>LastEvaluatedKey</code> is present in the response, you will need to paginate the result set. For more
 * information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#Query.Pagination">Paginating the Results</a> in the <i>Amazon DynamoDB Developer Guide</i>.
 *
 * <p><code>FilterExpression</code> is applied after a <code>Query</code> finishes, but before the results are returned.
 * A <code>FilterExpression</code> cannot contain partition key or sort key attributes. You need to specify those
 * attributes in the <code>KeyConditionExpression</code>.
 *
 * <p> A <code>Query</code> operation can return an empty result set and a <code>LastEvaluatedKey</code> if all the
 * items read for the page of results are filtered out.
 *
 * <p>You can query a table, a local secondary index, or a global secondary index. For a query on a table or on a local
 * secondary index, you can set the <code>ConsistentRead</code> parameter to <code>true</code> and obtain a strongly
 * consistent result. Global secondary indexes support eventually consistent reads only, so do not specify <code>
 * ConsistentRead</code> when querying a global secondary index.
 *
 * <h2>Examples</h2>
 * <h3>To query an item</h3>
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
 */
@SmithyGenerated
public final class Query implements ApiOperation<QueryInput, QueryOutput> {

    private static final Query $INSTANCE = new Query();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#Query"),
            PaginatedTrait.builder().inputToken("ExclusiveStartKey").outputToken("LastEvaluatedKey").items("Items").pageSize("Limit").build());

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
    public static Query instance() {
        return $INSTANCE;
    }

    private Query() {}

    @Override
    public ShapeBuilder<QueryInput> inputBuilder() {
        return QueryInput.builder();
    }

    @Override
    public ShapeBuilder<QueryOutput> outputBuilder() {
        return QueryOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return QueryInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return QueryOutput.$SCHEMA;
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
