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
 * List DynamoDB backups that are associated with an Amazon Web Services account and weren't made with Amazon Web
 * Services Backup. To list these backups for a given table, specify <code>TableName</code>. <code>ListBackups</code>
 * returns a paginated list of results with at most 1 MB worth of items in a page. You can also specify a maximum number
 * of entries to be returned in a page.
 *
 * <p>In the request, start time is inclusive, but end time is exclusive. Note that these boundaries are for the time at
 * which the original backup was requested.
 *
 * <p>You can call <code>ListBackups</code> a maximum of five times per second.
 *
 * <p>If you want to retrieve the complete list of backups made with Amazon Web Services Backup, use the <a href="https://docs.aws.amazon.com/aws-backup/latest/devguide/API_ListBackupJobs.html">Amazon Web
 * Services Backup list API.</a>
 */
@SmithyGenerated
public final class ListBackups implements ApiOperation<ListBackupsInput, ListBackupsOutput> {

    private static final ListBackups $INSTANCE = new ListBackups();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#ListBackups"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListBackups instance() {
        return $INSTANCE;
    }

    private ListBackups() {}

    @Override
    public ShapeBuilder<ListBackupsInput> inputBuilder() {
        return ListBackupsInput.builder();
    }

    @Override
    public ShapeBuilder<ListBackupsOutput> outputBuilder() {
        return ListBackupsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListBackupsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListBackupsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA);
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
