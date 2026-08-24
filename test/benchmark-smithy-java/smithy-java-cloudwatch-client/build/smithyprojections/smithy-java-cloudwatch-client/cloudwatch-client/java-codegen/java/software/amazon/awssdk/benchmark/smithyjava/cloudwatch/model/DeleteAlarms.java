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
 * Deletes the specified alarms. You can delete up to 100 alarms in one operation. However, this total can include no
 * more than one composite alarm. For example, you could delete 99 metric alarms and one composite alarms with one
 * operation, but you can't delete two composite alarms with one operation. Log alarms cannot be batch deleted.
 *
 * <p> If you specify any incorrect alarm names, the alarms you specify with correct names are still deleted. Other
 * syntax errors might result in no alarms being deleted. To confirm that alarms were deleted successfully, you can use
 * the <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DescribeAlarms.html">DescribeAlarms</a> operation after using <code>DeleteAlarms</code>.
 *
 * <p>It is possible to create a loop or cycle of composite alarms, where composite alarm A depends on composite alarm
 * B, and composite alarm B also depends on composite alarm A. In this scenario, you can't delete any composite alarm
 * that is part of the cycle because there is always still a composite alarm that depends on that alarm that you want to
 * delete.
 *
 * <p>To get out of such a situation, you must break the cycle by changing the rule of one of the composite alarms in
 * the cycle to remove a dependency that creates the cycle. The simplest change to make to break a cycle is to change
 * the <code>AlarmRule</code> of one of the alarms to <code>false</code>.
 *
 * <p>Additionally, the evaluation of composite alarms stops if CloudWatch detects a cycle in the evaluation path.
 */
@SmithyGenerated
public final class DeleteAlarms implements ApiOperation<DeleteAlarmsInput, DeleteAlarmsOutput> {

    private static final DeleteAlarms $INSTANCE = new DeleteAlarms();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DeleteAlarms"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ResourceConflict.$ID, ResourceConflict.class, ResourceConflict::builder)
        .putType(ResourceNotFound.$ID, ResourceNotFound.class, ResourceNotFound::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteAlarms instance() {
        return $INSTANCE;
    }

    private DeleteAlarms() {}

    @Override
    public ShapeBuilder<DeleteAlarmsInput> inputBuilder() {
        return DeleteAlarmsInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteAlarmsOutput> outputBuilder() {
        return DeleteAlarmsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteAlarmsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteAlarmsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ResourceConflict.$SCHEMA, ResourceNotFound.$SCHEMA);
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
