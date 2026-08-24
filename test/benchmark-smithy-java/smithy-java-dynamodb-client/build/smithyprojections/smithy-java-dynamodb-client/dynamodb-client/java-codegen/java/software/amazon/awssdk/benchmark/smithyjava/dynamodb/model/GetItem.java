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
 * The <code>GetItem</code> operation returns a set of attributes for the item with the given primary key. If there is
 * no matching item, <code>GetItem</code> does not return any data and there will be no <code>Item</code> element in the
 * response.
 *
 * <p><code>GetItem</code> provides an eventually consistent read by default. If your application requires a strongly
 * consistent read, set <code>ConsistentRead</code> to <code>true</code>. Although a strongly consistent read might take
 * more time than an eventually consistent read, it always returns the last updated value.
 *
 * <h2>Examples</h2>
 * <h3>To read an item from a table</h3>
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
 */
@SmithyGenerated
public final class GetItem implements ApiOperation<GetItemInput, GetItemOutput> {

    private static final GetItem $INSTANCE = new GetItem();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#GetItem"));

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
    public static GetItem instance() {
        return $INSTANCE;
    }

    private GetItem() {}

    @Override
    public ShapeBuilder<GetItemInput> inputBuilder() {
        return GetItemInput.builder();
    }

    @Override
    public ShapeBuilder<GetItemOutput> outputBuilder() {
        return GetItemOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetItemInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetItemOutput.$SCHEMA;
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
