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
 * The <code>UpdateTimeToLive</code> method enables or disables Time to Live (TTL) for the specified table. A successful
 * <code>UpdateTimeToLive</code> call returns the current <code>TimeToLiveSpecification</code>. It can take up to one
 * hour for the change to fully process. Any additional <code>UpdateTimeToLive</code> calls for the same table during
 * this one hour duration result in a <code>ValidationException</code>.
 *
 * <p>TTL compares the current time in epoch time format to the time stored in the TTL attribute of an item. If the
 * epoch time value stored in the attribute is less than the current time, the item is marked as expired and
 * subsequently deleted.
 *
 * <p> The epoch time format is the number of seconds elapsed since 12:00:00 AM January 1, 1970 UTC.
 *
 * <p>DynamoDB deletes expired items on a best-effort basis to ensure availability of throughput for other data
 * operations.
 *
 * <p>DynamoDB typically deletes expired items within two days of expiration. The exact duration within which an item
 * gets deleted after expiration is specific to the nature of the workload. Items that have expired and not been deleted
 * will still show up in reads, queries, and scans.
 *
 * <p>As items are deleted, they are removed from any local secondary index and global secondary index immediately in
 * the same eventually consistent way as a standard delete operation.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/TTL.html">Time To Live</a> in the Amazon DynamoDB Developer Guide.
 */
@SmithyGenerated
public final class UpdateTimeToLive implements ApiOperation<UpdateTimeToLiveInput, UpdateTimeToLiveOutput> {

    private static final UpdateTimeToLive $INSTANCE = new UpdateTimeToLive();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#UpdateTimeToLive"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(ResourceInUseException.$ID, ResourceInUseException.class, ResourceInUseException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UpdateTimeToLive instance() {
        return $INSTANCE;
    }

    private UpdateTimeToLive() {}

    @Override
    public ShapeBuilder<UpdateTimeToLiveInput> inputBuilder() {
        return UpdateTimeToLiveInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateTimeToLiveOutput> outputBuilder() {
        return UpdateTimeToLiveOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateTimeToLiveInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateTimeToLiveOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, ResourceInUseException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
