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
 * Returns a list that contains the number of managed Contributor Insights rules in your account.
 *
 * <pre>{@code
 *     </p>
 *
 * }</pre>
 */
@SmithyGenerated
public final class ListManagedInsightRules implements ApiOperation<ListManagedInsightRulesInput, ListManagedInsightRulesOutput> {

    private static final ListManagedInsightRules $INSTANCE = new ListManagedInsightRules();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#ListManagedInsightRules"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").pageSize("MaxResults").build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidNextToken.$ID, InvalidNextToken.class, InvalidNextToken::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .putType(MissingRequiredParameterException.$ID, MissingRequiredParameterException.class, MissingRequiredParameterException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListManagedInsightRules instance() {
        return $INSTANCE;
    }

    private ListManagedInsightRules() {}

    @Override
    public ShapeBuilder<ListManagedInsightRulesInput> inputBuilder() {
        return ListManagedInsightRulesInput.builder();
    }

    @Override
    public ShapeBuilder<ListManagedInsightRulesOutput> outputBuilder() {
        return ListManagedInsightRulesOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListManagedInsightRulesInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListManagedInsightRulesOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidNextToken.$SCHEMA, InvalidParameterValueException.$SCHEMA, MissingRequiredParameterException.$SCHEMA);
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
