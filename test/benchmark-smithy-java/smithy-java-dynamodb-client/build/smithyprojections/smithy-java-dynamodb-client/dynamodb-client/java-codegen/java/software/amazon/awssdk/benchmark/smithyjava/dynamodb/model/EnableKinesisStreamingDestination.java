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
 * Starts table data replication to the specified Kinesis data stream at a timestamp chosen during the enable workflow.
 * If this operation doesn't return results immediately, use DescribeKinesisStreamingDestination to check if streaming
 * to the Kinesis data stream is ACTIVE.
 */
@SmithyGenerated
public final class EnableKinesisStreamingDestination implements ApiOperation<EnableKinesisStreamingDestinationInput, EnableKinesisStreamingDestinationOutput> {

    private static final EnableKinesisStreamingDestination $INSTANCE = new EnableKinesisStreamingDestination();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#EnableKinesisStreamingDestination"));

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
    public static EnableKinesisStreamingDestination instance() {
        return $INSTANCE;
    }

    private EnableKinesisStreamingDestination() {}

    @Override
    public ShapeBuilder<EnableKinesisStreamingDestinationInput> inputBuilder() {
        return EnableKinesisStreamingDestinationInput.builder();
    }

    @Override
    public ShapeBuilder<EnableKinesisStreamingDestinationOutput> outputBuilder() {
        return EnableKinesisStreamingDestinationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return EnableKinesisStreamingDestinationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return EnableKinesisStreamingDestinationOutput.$SCHEMA;
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
