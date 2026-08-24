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
 * Returns information about the status of Kinesis streaming.
 */
@SmithyGenerated
public final class DescribeKinesisStreamingDestination implements ApiOperation<DescribeKinesisStreamingDestinationInput, DescribeKinesisStreamingDestinationOutput> {

    private static final DescribeKinesisStreamingDestination $INSTANCE = new DescribeKinesisStreamingDestination();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DescribeKinesisStreamingDestination"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeKinesisStreamingDestination instance() {
        return $INSTANCE;
    }

    private DescribeKinesisStreamingDestination() {}

    @Override
    public ShapeBuilder<DescribeKinesisStreamingDestinationInput> inputBuilder() {
        return DescribeKinesisStreamingDestinationInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeKinesisStreamingDestinationOutput> outputBuilder() {
        return DescribeKinesisStreamingDestinationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeKinesisStreamingDestinationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeKinesisStreamingDestinationOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
