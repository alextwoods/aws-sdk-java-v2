package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Creates a new item, or replaces an old item with a new item. If an item that has the same primary key as the new item
 * already exists in the specified table, the new item completely replaces the existing item. You can perform a
 * conditional put operation (add a new item if one with the specified primary key doesn't exist), or replace an
 * existing item if it has certain attribute values. You can return the item's attribute values in the same operation,
 * using the <code>ReturnValues</code> parameter.
 *
 * <p>When you add an item, the primary key attributes are the only required attributes.
 *
 * <p>Empty String and Binary attribute values are allowed. Attribute values of type String and Binary must have a
 * length greater than zero if the attribute is used as a key attribute for a table or index. Set type attributes cannot
 * be empty.
 *
 * <p>Invalid Requests with empty values will be rejected with a <code>ValidationException</code> exception.
 *
 * <p>To prevent a new item from replacing an existing item, use a conditional expression that contains the <code>
 * attribute_not_exists</code> function with the name of the attribute being used as the partition key for the table.
 * Since every record must contain that attribute, the <code>attribute_not_exists</code> function will only succeed if
 * no matching item exists.
 *
 * <p>To determine whether <code>PutItem</code> overwrote an existing item, use <code>ReturnValues</code> set to <code>
 * ALL_OLD</code>. If the response includes the <code>Attributes</code> element, an existing item was overwritten.
 *
 * <p>For more information about <code>PutItem</code>, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithItems.html">Working with Items</a> in the <i>Amazon DynamoDB Developer
 * Guide</i>.
 *
 * <h2>Examples</h2>
 * <h3>To add an item to a table</h3>
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
 */
@SmithyGenerated
public final class PutItem implements ApiOperation<PutItemInput, PutItemOutput> {

    private static final PutItem $INSTANCE = new PutItem();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#PutItem"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConditionalCheckFailedException.$ID, ConditionalCheckFailedException.class, ConditionalCheckFailedException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(ItemCollectionSizeLimitExceededException.$ID, ItemCollectionSizeLimitExceededException.class, ItemCollectionSizeLimitExceededException::builder)
        .putType(ProvisionedThroughputExceededException.$ID, ProvisionedThroughputExceededException.class, ProvisionedThroughputExceededException::builder)
        .putType(ReplicatedWriteConflictException.$ID, ReplicatedWriteConflictException.class, ReplicatedWriteConflictException::builder)
        .putType(RequestLimitExceeded.$ID, RequestLimitExceeded.class, RequestLimitExceeded::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .putType(ThrottlingException.$ID, ThrottlingException.class, ThrottlingException::builder)
        .putType(TransactionConflictException.$ID, TransactionConflictException.class, TransactionConflictException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutItem instance() {
        return $INSTANCE;
    }

    private PutItem() {}

    @Override
    public ShapeBuilder<PutItemInput> inputBuilder() {
        return PutItemInput.builder();
    }

    @Override
    public ShapeBuilder<PutItemOutput> outputBuilder() {
        return PutItemOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutItemInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutItemOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConditionalCheckFailedException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, ItemCollectionSizeLimitExceededException.$SCHEMA, ProvisionedThroughputExceededException.$SCHEMA, ReplicatedWriteConflictException.$SCHEMA, RequestLimitExceeded.$SCHEMA, ResourceNotFoundException.$SCHEMA, ThrottlingException.$SCHEMA, TransactionConflictException.$SCHEMA);
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
