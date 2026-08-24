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
 * The <code>Scan</code> operation returns one or more items and item attributes by accessing every item in a table or a
 * secondary index. To have DynamoDB return fewer items, you can provide a <code>FilterExpression</code> operation.
 *
 * <p>If the total size of scanned items exceeds the maximum dataset size limit of 1 MB, the scan completes and results
 * are returned to the user. The <code>LastEvaluatedKey</code> value is also returned and the requestor can use the <code>
 * LastEvaluatedKey</code> to continue the scan in a subsequent operation. Each scan response also includes number of
 * items that were scanned (ScannedCount) as part of the request. If using a <code>FilterExpression</code>, a scan
 * result can result in no items meeting the criteria and the <code>Count</code> will result in zero. If you did not use
 * a <code>FilterExpression</code> in the scan request, then <code>Count</code> is the same as <code>ScannedCount</code>
 * .
 *
 * <p><code>Count</code> and <code>ScannedCount</code> only return the count of items specific to a single scan request
 * and, unless the table is less than 1MB, do not represent the total number of items in the table.
 *
 * <p>A single <code>Scan</code> operation first reads up to the maximum number of items set (if using the <code>Limit</code>
 * parameter) or a maximum of 1 MB of data and then applies any filtering to the results if a <code>FilterExpression</code>
 * is provided. If <code>LastEvaluatedKey</code> is present in the response, pagination is required to complete the full
 * table scan. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.Pagination">Paginating the Results</a> in the <i>Amazon DynamoDB Developer Guide</i>.
 *
 * <p><code>Scan</code> operations proceed sequentially; however, for faster performance on a large table or secondary
 * index, applications can request a parallel <code>Scan</code> operation by providing the <code>Segment</code> and <code>
 * TotalSegments</code> parameters. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.ParallelScan">Parallel Scan</a> in the <i>Amazon DynamoDB Developer
 * Guide</i>.
 *
 * <p>By default, a <code>Scan</code> uses eventually consistent reads when accessing the items in a table. Therefore,
 * the results from an eventually consistent <code>Scan</code> may not include the latest item changes at the time the
 * scan iterates through each item in the table. If you require a strongly consistent read of each item as the scan
 * iterates through the items in the table, you can set the <code>ConsistentRead</code> parameter to true. Strong
 * consistency only relates to the consistency of the read at the item level.
 *
 * <p> DynamoDB does not provide snapshot isolation for a scan operation when the <code>ConsistentRead</code> parameter
 * is set to true. Thus, a DynamoDB scan operation does not guarantee that all reads in a scan see a consistent snapshot
 * of the table when the scan operation was requested.
 *
 * <h2>Examples</h2>
 * <h3>To scan a table</h3>
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
 */
@SmithyGenerated
public final class Scan implements ApiOperation<ScanInput, ScanOutput> {

    private static final Scan $INSTANCE = new Scan();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#Scan"),
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
    public static Scan instance() {
        return $INSTANCE;
    }

    private Scan() {}

    @Override
    public ShapeBuilder<ScanInput> inputBuilder() {
        return ScanInput.builder();
    }

    @Override
    public ShapeBuilder<ScanOutput> outputBuilder() {
        return ScanOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ScanInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ScanOutput.$SCHEMA;
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
