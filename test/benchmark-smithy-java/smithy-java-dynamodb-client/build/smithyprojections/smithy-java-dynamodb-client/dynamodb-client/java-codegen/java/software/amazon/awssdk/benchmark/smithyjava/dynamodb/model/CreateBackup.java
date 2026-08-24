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
 * Creates a backup for an existing table.
 *
 * <p> Each time you create an on-demand backup, the entire table data is backed up. There is no limit to the number of
 * on-demand backups that can be taken.
 *
 * <p> When you create an on-demand backup, a time marker of the request is cataloged, and the backup is created
 * asynchronously, by applying all changes until the time of the request to the last full table snapshot. Backup
 * requests are processed instantaneously and become available for restore within minutes.
 *
 * <p>You can call <code>CreateBackup</code> at a maximum rate of 50 times per second.
 *
 * <p>All backups in DynamoDB work without consuming any provisioned throughput on the table.
 *
 * <p> If you submit a backup request on 2018-12-14 at 14:25:00, the backup is guaranteed to contain all data committed
 * to the table up to 14:24:00, and data committed after 14:26:00 will not be. The backup might contain data
 * modifications made between 14:24:00 and 14:26:00. On-demand backup does not support causal consistency.
 *
 * <p> Along with data, the following are also included on the backups:
 *
 * <ul>
 *   <li>
 *     Global secondary indexes (GSIs)
 *   </li>
 *   <li>
 *     Local secondary indexes (LSIs)
 *   </li>
 *   <li>
 *     Streams
 *   </li>
 *   <li>
 *     Provisioned read and write capacity
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class CreateBackup implements ApiOperation<CreateBackupInput, CreateBackupOutput> {

    private static final CreateBackup $INSTANCE = new CreateBackup();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#CreateBackup"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(BackupInUseException.$ID, BackupInUseException.class, BackupInUseException::builder)
        .putType(ContinuousBackupsUnavailableException.$ID, ContinuousBackupsUnavailableException.class, ContinuousBackupsUnavailableException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(TableInUseException.$ID, TableInUseException.class, TableInUseException::builder)
        .putType(TableNotFoundException.$ID, TableNotFoundException.class, TableNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static CreateBackup instance() {
        return $INSTANCE;
    }

    private CreateBackup() {}

    @Override
    public ShapeBuilder<CreateBackupInput> inputBuilder() {
        return CreateBackupInput.builder();
    }

    @Override
    public ShapeBuilder<CreateBackupOutput> outputBuilder() {
        return CreateBackupOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return CreateBackupInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return CreateBackupOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(BackupInUseException.$SCHEMA, ContinuousBackupsUnavailableException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, TableInUseException.$SCHEMA, TableNotFoundException.$SCHEMA);
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
