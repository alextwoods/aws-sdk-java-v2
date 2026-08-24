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
 * Edits an existing item's attributes, or adds a new item to the table if it does not already exist. You can put,
 * delete, or add attribute values. You can also perform a conditional update on an existing item (insert a new
 * attribute name-value pair if it doesn't exist, or replace an existing name-value pair if it has certain expected
 * attribute values).
 *
 * <p>You can also return the item's attribute values in the same <code>UpdateItem</code> operation using the <code>
 * ReturnValues</code> parameter.
 *
 * <h2>Examples</h2>
 * <h3>To update an item in a table</h3>
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
 */
@SmithyGenerated
public final class UpdateItem implements ApiOperation<UpdateItemInput, UpdateItemOutput> {

    private static final UpdateItem $INSTANCE = new UpdateItem();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#UpdateItem"));

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
    public static UpdateItem instance() {
        return $INSTANCE;
    }

    private UpdateItem() {}

    @Override
    public ShapeBuilder<UpdateItemInput> inputBuilder() {
        return UpdateItemInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateItemOutput> outputBuilder() {
        return UpdateItemOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateItemInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateItemOutput.$SCHEMA;
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
