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
 * Creates a managed Contributor Insights rule for a specified Amazon Web Services resource. When you enable a managed
 * rule, you create a Contributor Insights rule that collects data from Amazon Web Services services. You cannot edit
 * these rules with <code>PutInsightRule</code>. The rules can be enabled, disabled, and deleted using <code>
 * EnableInsightRules</code>, <code>DisableInsightRules</code>, and <code>DeleteInsightRules</code>. If a previously
 * created managed rule is currently disabled, a subsequent call to this API will re-enable it. Use <code>
 * ListManagedInsightRules</code> to describe all available rules.
 *
 * <pre>{@code
 *     </p>
 *
 * }</pre>
 */
@SmithyGenerated
public final class PutManagedInsightRules implements ApiOperation<PutManagedInsightRulesInput, PutManagedInsightRulesOutput> {

    private static final PutManagedInsightRules $INSTANCE = new PutManagedInsightRules();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutManagedInsightRules"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .putType(MissingRequiredParameterException.$ID, MissingRequiredParameterException.class, MissingRequiredParameterException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutManagedInsightRules instance() {
        return $INSTANCE;
    }

    private PutManagedInsightRules() {}

    @Override
    public ShapeBuilder<PutManagedInsightRulesInput> inputBuilder() {
        return PutManagedInsightRulesInput.builder();
    }

    @Override
    public ShapeBuilder<PutManagedInsightRulesOutput> outputBuilder() {
        return PutManagedInsightRulesOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutManagedInsightRulesInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutManagedInsightRulesOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidParameterValueException.$SCHEMA, MissingRequiredParameterException.$SCHEMA);
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
