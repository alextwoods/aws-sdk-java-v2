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
 * Creates an anomaly detection model for a CloudWatch metric. You can use the model to display a band of expected
 * normal values when the metric is graphed.
 *
 * <p>If you have enabled unified cross-account observability, and this account is a monitoring account, the metric can
 * be in the same account or a source account. You can specify the account ID in the object you specify in the <code>
 * SingleMetricAnomalyDetector</code> parameter.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Anomaly_Detection.html">CloudWatch Anomaly Detection</a>.
 */
@SmithyGenerated
public final class PutAnomalyDetector implements ApiOperation<PutAnomalyDetectorInput, PutAnomalyDetectorOutput> {

    private static final PutAnomalyDetector $INSTANCE = new PutAnomalyDetector();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutAnomalyDetector"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidParameterCombinationException.$ID, InvalidParameterCombinationException.class, InvalidParameterCombinationException::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(MissingRequiredParameterException.$ID, MissingRequiredParameterException.class, MissingRequiredParameterException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutAnomalyDetector instance() {
        return $INSTANCE;
    }

    private PutAnomalyDetector() {}

    @Override
    public ShapeBuilder<PutAnomalyDetectorInput> inputBuilder() {
        return PutAnomalyDetectorInput.builder();
    }

    @Override
    public ShapeBuilder<PutAnomalyDetectorOutput> outputBuilder() {
        return PutAnomalyDetectorOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutAnomalyDetectorInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutAnomalyDetectorOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServiceFault.$SCHEMA, InvalidParameterCombinationException.$SCHEMA, InvalidParameterValueException.$SCHEMA, LimitExceededException.$SCHEMA, MissingRequiredParameterException.$SCHEMA);
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
