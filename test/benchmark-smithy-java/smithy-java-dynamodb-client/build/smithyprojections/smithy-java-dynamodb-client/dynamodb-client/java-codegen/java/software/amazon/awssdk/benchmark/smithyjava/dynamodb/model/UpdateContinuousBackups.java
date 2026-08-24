package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * <code>UpdateContinuousBackups</code> enables or disables point in time recovery for the specified table. A successful
 * <code>UpdateContinuousBackups</code> call returns the current <code>ContinuousBackupsDescription</code>. Continuous
 * backups are <code>ENABLED</code> on all tables at table creation. If point in time recovery is enabled, <code>
 * PointInTimeRecoveryStatus</code> will be set to ENABLED.
 *
 * <p> Once continuous backups and point in time recovery are enabled, you can restore to any point in time within <code>
 * EarliestRestorableDateTime</code> and <code>LatestRestorableDateTime</code>.
 *
 * <p><code>LatestRestorableDateTime</code> is typically 5 minutes before the current time. You can restore your table
 * to any point in time in the last 35 days. You can set the <code>RecoveryPeriodInDays</code> to any value between 1
 * and 35 days.
 */
@SmithyGenerated
public final class UpdateContinuousBackups implements ApiOperation<UpdateContinuousBackupsInput, UpdateContinuousBackupsOutput> {

    private static final UpdateContinuousBackups $INSTANCE = new UpdateContinuousBackups();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#UpdateContinuousBackups"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ContinuousBackupsUnavailableException.$ID, ContinuousBackupsUnavailableException.class, ContinuousBackupsUnavailableException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(TableNotFoundException.$ID, TableNotFoundException.class, TableNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UpdateContinuousBackups instance() {
        return $INSTANCE;
    }

    private UpdateContinuousBackups() {}

    @Override
    public ShapeBuilder<UpdateContinuousBackupsInput> inputBuilder() {
        return UpdateContinuousBackupsInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateContinuousBackupsOutput> outputBuilder() {
        return UpdateContinuousBackupsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateContinuousBackupsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateContinuousBackupsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ContinuousBackupsUnavailableException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, TableNotFoundException.$SCHEMA);
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
        return DynamoDBApiService.instance();
    }
    }
