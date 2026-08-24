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
 * Enables enrichment and PromQL access for CloudWatch vended metrics for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">supported Amazon Web Services resources</a>
 * in the account. Once enabled, metrics that contain a resource identifier dimension (for example, EC2 <code>
 * CPUUtilization</code> with an <code>InstanceId</code> dimension) are enriched with resource ARN and resource tag
 * labels and become queryable using PromQL.
 *
 * <p>Before calling this operation, you must enable resource tags on telemetry for your account. For more information,
 * see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/EnableResourceTagsOnTelemetry.html">Enable resource tags on telemetry</a>.
 */
@SmithyGenerated
public final class StartOTelEnrichment implements ApiOperation<StartOTelEnrichmentInput, StartOTelEnrichmentOutput> {

    private static final StartOTelEnrichment $INSTANCE = new StartOTelEnrichment();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#StartOTelEnrichment"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static StartOTelEnrichment instance() {
        return $INSTANCE;
    }

    private StartOTelEnrichment() {}

    @Override
    public ShapeBuilder<StartOTelEnrichmentInput> inputBuilder() {
        return StartOTelEnrichmentInput.builder();
    }

    @Override
    public ShapeBuilder<StartOTelEnrichmentOutput> outputBuilder() {
        return StartOTelEnrichmentOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return StartOTelEnrichmentInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return StartOTelEnrichmentOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of();
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
