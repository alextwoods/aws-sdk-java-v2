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
 * Stops replication from the DynamoDB table to the Kinesis data stream. This is done without deleting either of the
 * resources.
 */
@SmithyGenerated
public final class DisableKinesisStreamingDestination implements ApiOperation<DisableKinesisStreamingDestinationInput, DisableKinesisStreamingDestinationOutput> {

    private static final DisableKinesisStreamingDestination $INSTANCE = new DisableKinesisStreamingDestination();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DisableKinesisStreamingDestination"));

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
    public static DisableKinesisStreamingDestination instance() {
        return $INSTANCE;
    }

    private DisableKinesisStreamingDestination() {}

    @Override
    public ShapeBuilder<DisableKinesisStreamingDestinationInput> inputBuilder() {
        return DisableKinesisStreamingDestinationInput.builder();
    }

    @Override
    public ShapeBuilder<DisableKinesisStreamingDestinationOutput> outputBuilder() {
        return DisableKinesisStreamingDestinationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DisableKinesisStreamingDestinationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DisableKinesisStreamingDestinationOutput.$SCHEMA;
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
