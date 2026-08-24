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
 * Describes auto scaling settings across replicas of the global table at once.
 */
@SmithyGenerated
public final class DescribeTableReplicaAutoScaling implements ApiOperation<DescribeTableReplicaAutoScalingInput, DescribeTableReplicaAutoScalingOutput> {

    private static final DescribeTableReplicaAutoScaling $INSTANCE = new DescribeTableReplicaAutoScaling();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DescribeTableReplicaAutoScaling"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeTableReplicaAutoScaling instance() {
        return $INSTANCE;
    }

    private DescribeTableReplicaAutoScaling() {}

    @Override
    public ShapeBuilder<DescribeTableReplicaAutoScalingInput> inputBuilder() {
        return DescribeTableReplicaAutoScalingInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeTableReplicaAutoScalingOutput> outputBuilder() {
        return DescribeTableReplicaAutoScalingOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeTableReplicaAutoScalingInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeTableReplicaAutoScalingOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
