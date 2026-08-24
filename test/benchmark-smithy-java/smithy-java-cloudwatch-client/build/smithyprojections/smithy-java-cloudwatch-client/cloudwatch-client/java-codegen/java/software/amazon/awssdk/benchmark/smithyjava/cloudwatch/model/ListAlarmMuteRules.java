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
 * Lists alarm mute rules in your Amazon Web Services account and region.
 *
 * <p>You can filter the results by alarm name to find all mute rules targeting a specific alarm, or by status to find
 * rules that are scheduled, active, or expired.
 *
 * <p>This operation supports pagination for accounts with many mute rules. Use the <code>MaxRecords</code> and <code>
 * NextToken</code> parameters to retrieve results in multiple calls.
 *
 * <p><b>Permissions</b>
 *
 * <p>To list mute rules, you need the <code>cloudwatch:ListAlarmMuteRules</code> permission.
 */
@SmithyGenerated
public final class ListAlarmMuteRules implements ApiOperation<ListAlarmMuteRulesInput, ListAlarmMuteRulesOutput> {

    private static final ListAlarmMuteRules $INSTANCE = new ListAlarmMuteRules();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#ListAlarmMuteRules"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").items("AlarmMuteRuleSummaries").pageSize("MaxRecords").build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidNextToken.$ID, InvalidNextToken.class, InvalidNextToken::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListAlarmMuteRules instance() {
        return $INSTANCE;
    }

    private ListAlarmMuteRules() {}

    @Override
    public ShapeBuilder<ListAlarmMuteRulesInput> inputBuilder() {
        return ListAlarmMuteRulesInput.builder();
    }

    @Override
    public ShapeBuilder<ListAlarmMuteRulesOutput> outputBuilder() {
        return ListAlarmMuteRulesOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListAlarmMuteRulesInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListAlarmMuteRulesOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidNextToken.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
