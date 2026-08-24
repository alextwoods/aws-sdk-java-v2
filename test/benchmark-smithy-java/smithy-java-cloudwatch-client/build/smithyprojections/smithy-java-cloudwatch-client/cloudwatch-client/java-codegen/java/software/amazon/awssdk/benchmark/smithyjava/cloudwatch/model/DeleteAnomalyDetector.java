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
 * Deletes the specified anomaly detection model from your account. For more information about how to delete an anomaly
 * detection model, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Create_Anomaly_Detection_Alarm.html#Delete_Anomaly_Detection_Model">Deleting an anomaly detection model</a> in the <i>CloudWatch User Guide</i>.
 */
@SmithyGenerated
public final class DeleteAnomalyDetector implements ApiOperation<DeleteAnomalyDetectorInput, DeleteAnomalyDetectorOutput> {

    private static final DeleteAnomalyDetector $INSTANCE = new DeleteAnomalyDetector();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DeleteAnomalyDetector"));

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
    public static DeleteAnomalyDetector instance() {
        return $INSTANCE;
    }

    private DeleteAnomalyDetector() {}

    @Override
    public ShapeBuilder<DeleteAnomalyDetectorInput> inputBuilder() {
        return DeleteAnomalyDetectorInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteAnomalyDetectorOutput> outputBuilder() {
        return DeleteAnomalyDetectorOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteAnomalyDetectorInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteAnomalyDetectorOutput.$SCHEMA;
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
