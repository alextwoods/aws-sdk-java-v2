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
 * Exports table data to an S3 bucket. The table must have point in time recovery enabled, and you can export data from
 * any time within the point in time recovery window.
 */
@SmithyGenerated
public final class ExportTableToPointInTime implements ApiOperation<ExportTableToPointInTimeInput, ExportTableToPointInTimeOutput> {

    private static final ExportTableToPointInTime $INSTANCE = new ExportTableToPointInTime();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#ExportTableToPointInTime"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ExportConflictException.$ID, ExportConflictException.class, ExportConflictException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidExportTimeException.$ID, InvalidExportTimeException.class, InvalidExportTimeException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(PointInTimeRecoveryUnavailableException.$ID, PointInTimeRecoveryUnavailableException.class, PointInTimeRecoveryUnavailableException::builder)
        .putType(TableNotFoundException.$ID, TableNotFoundException.class, TableNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema IDEMPOTENCY_TOKEN_MEMBER = ExportTableToPointInTimeInput.$SCHEMA.member("ClientToken");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ExportTableToPointInTime instance() {
        return $INSTANCE;
    }

    private ExportTableToPointInTime() {}

    @Override
    public ShapeBuilder<ExportTableToPointInTimeInput> inputBuilder() {
        return ExportTableToPointInTimeInput.builder();
    }

    @Override
    public ShapeBuilder<ExportTableToPointInTimeOutput> outputBuilder() {
        return ExportTableToPointInTimeOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ExportTableToPointInTimeInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ExportTableToPointInTimeOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ExportConflictException.$SCHEMA, InternalServerError.$SCHEMA, InvalidExportTimeException.$SCHEMA, LimitExceededException.$SCHEMA, PointInTimeRecoveryUnavailableException.$SCHEMA, TableNotFoundException.$SCHEMA);
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
        return IDEMPOTENCY_TOKEN_MEMBER;
    }

    @Override
    public ApiService service() {
        return DynamoDBApiService.instance();
    }
    }
