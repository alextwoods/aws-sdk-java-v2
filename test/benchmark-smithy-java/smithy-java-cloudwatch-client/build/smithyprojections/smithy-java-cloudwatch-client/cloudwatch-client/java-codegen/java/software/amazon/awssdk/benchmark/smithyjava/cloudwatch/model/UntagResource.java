package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Removes one or more tags from the specified resource. Currently, alarms, dashboards, metric streams and Contributor
 * Insights rules support tagging.
 */
@SmithyGenerated
public final class UntagResource implements ApiOperation<UntagResourceInput, UntagResourceOutput> {

    private static final UntagResource $INSTANCE = new UntagResource();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#UntagResource"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConcurrentModificationException.$ID, ConcurrentModificationException.class, ConcurrentModificationException::builder)
        .putType(ConflictException.$ID, ConflictException.class, ConflictException::builder)
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UntagResource instance() {
        return $INSTANCE;
    }

    private UntagResource() {}

    @Override
    public ShapeBuilder<UntagResourceInput> inputBuilder() {
        return UntagResourceInput.builder();
    }

    @Override
    public ShapeBuilder<UntagResourceOutput> outputBuilder() {
        return UntagResourceOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UntagResourceInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UntagResourceOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConcurrentModificationException.$SCHEMA, ConflictException.$SCHEMA, InternalServiceFault.$SCHEMA, InvalidParameterValueException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
        return CloudWatchApiService.instance();
    }
    }
