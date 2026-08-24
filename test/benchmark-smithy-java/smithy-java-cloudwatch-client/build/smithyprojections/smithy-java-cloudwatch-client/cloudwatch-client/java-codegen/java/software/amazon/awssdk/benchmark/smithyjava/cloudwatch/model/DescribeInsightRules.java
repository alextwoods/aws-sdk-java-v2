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
 * Returns a list of all the Contributor Insights rules in your account.
 *
 * <p>For more information about Contributor Insights, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights.html">Using Contributor Insights to Analyze High-Cardinality
 * Data</a>.
 */
@SmithyGenerated
public final class DescribeInsightRules implements ApiOperation<DescribeInsightRulesInput, DescribeInsightRulesOutput> {

    private static final DescribeInsightRules $INSTANCE = new DescribeInsightRules();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DescribeInsightRules"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").pageSize("MaxResults").build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidNextToken.$ID, InvalidNextToken.class, InvalidNextToken::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeInsightRules instance() {
        return $INSTANCE;
    }

    private DescribeInsightRules() {}

    @Override
    public ShapeBuilder<DescribeInsightRulesInput> inputBuilder() {
        return DescribeInsightRulesInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeInsightRulesOutput> outputBuilder() {
        return DescribeInsightRulesOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeInsightRulesInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeInsightRulesOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidNextToken.$SCHEMA);
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
