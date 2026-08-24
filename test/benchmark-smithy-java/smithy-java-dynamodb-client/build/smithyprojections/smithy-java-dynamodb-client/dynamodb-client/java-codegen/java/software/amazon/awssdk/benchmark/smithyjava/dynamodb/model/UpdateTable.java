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
 * ACTIVE</code> to <code>UPDATING</code>. While it's <code>UPDATING</code>, you can't issue another <code>UpdateTable</code>
 * request. When the table returns to the <code>ACTIVE</code> state, the <code>UpdateTable</code> operation is complete.
 */
@SmithyGenerated
public final class UpdateTable implements ApiOperation<UpdateTableInput, UpdateTableOutput> {

    private static final UpdateTable $INSTANCE = new UpdateTable();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#UpdateTable"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(ResourceInUseException.$ID, ResourceInUseException.class, ResourceInUseException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UpdateTable instance() {
        return $INSTANCE;
    }

    private UpdateTable() {}

    @Override
    public ShapeBuilder<UpdateTableInput> inputBuilder() {
        return UpdateTableInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateTableOutput> outputBuilder() {
        return UpdateTableOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateTableInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateTableOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, ResourceInUseException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
