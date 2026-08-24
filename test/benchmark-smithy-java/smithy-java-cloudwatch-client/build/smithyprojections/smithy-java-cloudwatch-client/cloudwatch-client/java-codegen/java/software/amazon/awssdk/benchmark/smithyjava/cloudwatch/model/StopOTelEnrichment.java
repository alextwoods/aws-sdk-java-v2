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
 * Disables enrichment and PromQL access for CloudWatch vended metrics for <a
 * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">supported
 * Amazon Web Services resources</a> in the account. After disabling, these metrics are no longer enriched with resource
 * ARN and resource tag labels, and cannot be queried using PromQL.
 */
@SmithyGenerated
public final class StopOTelEnrichment implements ApiOperation<StopOTelEnrichmentInput, StopOTelEnrichmentOutput> {

    private static final StopOTelEnrichment $INSTANCE = new StopOTelEnrichment();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#StopOTelEnrichment"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static StopOTelEnrichment instance() {
        return $INSTANCE;
    }

    private StopOTelEnrichment() {}

    @Override
    public ShapeBuilder<StopOTelEnrichmentInput> inputBuilder() {
        return StopOTelEnrichmentInput.builder();
    }

    @Override
    public ShapeBuilder<StopOTelEnrichmentOutput> outputBuilder() {
        return StopOTelEnrichmentOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return StopOTelEnrichmentInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return StopOTelEnrichmentOutput.$SCHEMA;
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
