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
 * Returns information about the metric stream that you specify.
 */
@SmithyGenerated
public final class GetMetricStream implements ApiOperation<GetMetricStreamInput, GetMetricStreamOutput> {

    private static final GetMetricStream $INSTANCE = new GetMetricStream();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetMetricStream"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidParameterCombinationException.$ID, InvalidParameterCombinationException.class, InvalidParameterCombinationException::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .putType(MissingRequiredParameterException.$ID, MissingRequiredParameterException.class, MissingRequiredParameterException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetMetricStream instance() {
        return $INSTANCE;
    }

    private GetMetricStream() {}

    @Override
    public ShapeBuilder<GetMetricStreamInput> inputBuilder() {
        return GetMetricStreamInput.builder();
    }

    @Override
    public ShapeBuilder<GetMetricStreamOutput> outputBuilder() {
        return GetMetricStreamOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetMetricStreamInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetMetricStreamOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServiceFault.$SCHEMA, InvalidParameterCombinationException.$SCHEMA, InvalidParameterValueException.$SCHEMA, MissingRequiredParameterException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
