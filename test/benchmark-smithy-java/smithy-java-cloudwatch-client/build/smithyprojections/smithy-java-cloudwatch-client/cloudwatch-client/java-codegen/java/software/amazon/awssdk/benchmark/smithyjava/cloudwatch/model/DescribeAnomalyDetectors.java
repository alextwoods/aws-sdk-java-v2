package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Lists the anomaly detection models that you have created in your account. For single metric anomaly detectors, you
 * can list all of the models in your account or filter the results to only the models that are related to a certain
 * namespace, metric name, or metric dimension. For metric math anomaly detectors, you can list them by adding
 * <code>METRIC_MATH</code> to the <code>AnomalyDetectorTypes</code> array. This will return all metric math anomaly
 * detectors in your account.
 */
@SmithyGenerated
public final class DescribeAnomalyDetectors implements ApiOperation<DescribeAnomalyDetectorsInput, DescribeAnomalyDetectorsOutput> {

    private static final DescribeAnomalyDetectors $INSTANCE = new DescribeAnomalyDetectors();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DescribeAnomalyDetectors"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").items("AnomalyDetectors").pageSize("MaxResults").build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidNextToken.$ID, InvalidNextToken.class, InvalidNextToken::builder)
        .putType(InvalidParameterCombinationException.$ID, InvalidParameterCombinationException.class, InvalidParameterCombinationException::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeAnomalyDetectors instance() {
        return $INSTANCE;
    }

    private DescribeAnomalyDetectors() {}

    @Override
    public ShapeBuilder<DescribeAnomalyDetectorsInput> inputBuilder() {
        return DescribeAnomalyDetectorsInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeAnomalyDetectorsOutput> outputBuilder() {
        return DescribeAnomalyDetectorsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeAnomalyDetectorsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeAnomalyDetectorsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServiceFault.$SCHEMA, InvalidNextToken.$SCHEMA, InvalidParameterCombinationException.$SCHEMA, InvalidParameterValueException.$SCHEMA);
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
