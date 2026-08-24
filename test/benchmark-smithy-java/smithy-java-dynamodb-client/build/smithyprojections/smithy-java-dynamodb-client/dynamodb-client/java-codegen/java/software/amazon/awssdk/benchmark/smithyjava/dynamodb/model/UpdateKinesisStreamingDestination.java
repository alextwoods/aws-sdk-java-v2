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
 * The command to update the Kinesis stream destination.
 */
@SmithyGenerated
public final class UpdateKinesisStreamingDestination implements ApiOperation<UpdateKinesisStreamingDestinationInput, UpdateKinesisStreamingDestinationOutput> {

    private static final UpdateKinesisStreamingDestination $INSTANCE = new UpdateKinesisStreamingDestination();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#UpdateKinesisStreamingDestination"));

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
    public static UpdateKinesisStreamingDestination instance() {
        return $INSTANCE;
    }

    private UpdateKinesisStreamingDestination() {}

    @Override
    public ShapeBuilder<UpdateKinesisStreamingDestinationInput> inputBuilder() {
        return UpdateKinesisStreamingDestinationInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateKinesisStreamingDestinationOutput> outputBuilder() {
        return UpdateKinesisStreamingDestinationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateKinesisStreamingDestinationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateKinesisStreamingDestinationOutput.$SCHEMA;
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
