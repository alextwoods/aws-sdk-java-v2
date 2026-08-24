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
 * Creates a Contributor Insights rule. Rules evaluate log events in a CloudWatch Logs log group, enabling you to find
 * contributor data for the log events in that log group. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights.html">Using Contributor Insights to
 * Analyze High-Cardinality Data</a>.
 *
 * <p>If you create a rule, delete it, and then re-create it with the same name, historical data from the first time the
 * rule was created might not be available.
 */
@SmithyGenerated
public final class PutInsightRule implements ApiOperation<PutInsightRuleInput, PutInsightRuleOutput> {

    private static final PutInsightRule $INSTANCE = new PutInsightRule();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutInsightRule"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
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
    public static PutInsightRule instance() {
        return $INSTANCE;
    }

    private PutInsightRule() {}

    @Override
    public ShapeBuilder<PutInsightRuleInput> inputBuilder() {
        return PutInsightRuleInput.builder();
    }

    @Override
    public ShapeBuilder<PutInsightRuleOutput> outputBuilder() {
        return PutInsightRuleOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutInsightRuleInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutInsightRuleOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidParameterValueException.$SCHEMA, LimitExceededException.$SCHEMA, MissingRequiredParameterException.$SCHEMA);
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
