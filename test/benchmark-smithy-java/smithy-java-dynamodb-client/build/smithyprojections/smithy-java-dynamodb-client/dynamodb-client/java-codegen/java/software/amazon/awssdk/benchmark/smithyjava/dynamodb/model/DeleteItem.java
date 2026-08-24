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
 * Deletes a single item in a table by primary key. You can perform a conditional delete operation that deletes the item
 * if it exists, or if it has an expected attribute value.
 *
 * <p>In addition to deleting an item, you can also return the item's attribute values in the same operation, using the <code>
 * ReturnValues</code> parameter.
 *
 * <p>Unless you specify conditions, the <code>DeleteItem</code> is an idempotent operation; running it multiple times
 * on the same item or attribute does <i>not</i> result in an error response.
 *
 * <p>Conditional deletes are useful for deleting items only if specific conditions are met. If those conditions are
 * met, DynamoDB performs the delete. Otherwise, the item is not deleted.
 *
 * <h2>Examples</h2>
 * <h3>To delete an item</h3>
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
 */
@SmithyGenerated
public final class DeleteItem implements ApiOperation<DeleteItemInput, DeleteItemOutput> {

    private static final DeleteItem $INSTANCE = new DeleteItem();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DeleteItem"));

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
    public static DeleteItem instance() {
        return $INSTANCE;
    }

    private DeleteItem() {}

    @Override
    public ShapeBuilder<DeleteItemInput> inputBuilder() {
        return DeleteItemInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteItemOutput> outputBuilder() {
        return DeleteItemOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteItemInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteItemOutput.$SCHEMA;
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
