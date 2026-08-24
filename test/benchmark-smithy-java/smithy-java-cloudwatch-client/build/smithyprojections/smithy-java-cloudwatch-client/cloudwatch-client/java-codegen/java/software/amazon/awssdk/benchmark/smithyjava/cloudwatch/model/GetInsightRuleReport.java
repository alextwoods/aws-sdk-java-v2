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
 * This operation returns the time series data collected by a Contributor Insights rule. The data includes the identity
 * and number of contributors to the log group.
 *
 * <p>You can also optionally return one or more statistics about each data point in the time series. These statistics
 * can include the following:
 *
 * <ul>
 *   <li>
 *     <code>UniqueContributors</code> -- the number of unique contributors for each data point.
 *   </li>
 *   <li>
 *     <code>MaxContributorValue</code> -- the value of the top contributor for each data point. The identity of the
 *     contributor might change for each data point in the graph.If this rule aggregates by COUNT, the top
 *     contributor for each data point is the contributor with the most occurrences in that period. If the rule
 *     aggregates by SUM, the top contributor is the contributor with the highest sum in the log field specified by
 *     the rule's <code>Value</code>, during that period.
 *   </li>
 *   <li>
 *     <code>SampleCount</code> -- the number of data points matched by the rule.
 *   </li>
 *   <li>
 *     <code>Sum</code> -- the sum of the values from all contributors during the time period represented by that
 *     data point.
 *   </li>
 *   <li>
 *     <code>Minimum</code> -- the minimum value from a single observation during the time period represented by
 *     that data point.
 *   </li>
 *   <li>
 *     <code>Maximum</code> -- the maximum value from a single observation during the time period represented by
 *     that data point.
 *   </li>
 *   <li>
 *     <code>Average</code> -- the average value from all contributors during the time period represented by that
 *     data point.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class GetInsightRuleReport implements ApiOperation<GetInsightRuleReportInput, GetInsightRuleReportOutput> {

    private static final GetInsightRuleReport $INSTANCE = new GetInsightRuleReport();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetInsightRuleReport"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
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
    public static GetInsightRuleReport instance() {
        return $INSTANCE;
    }

    private GetInsightRuleReport() {}

    @Override
    public ShapeBuilder<GetInsightRuleReportInput> inputBuilder() {
        return GetInsightRuleReportInput.builder();
    }

    @Override
    public ShapeBuilder<GetInsightRuleReportOutput> outputBuilder() {
        return GetInsightRuleReportOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetInsightRuleReportInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetInsightRuleReportOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidParameterValueException.$SCHEMA, MissingRequiredParameterException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
