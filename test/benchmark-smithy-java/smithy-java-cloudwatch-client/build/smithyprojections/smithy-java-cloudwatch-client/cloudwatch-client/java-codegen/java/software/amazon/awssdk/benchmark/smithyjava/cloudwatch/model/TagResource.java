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
 * Assigns one or more tags (key-value pairs) to the specified CloudWatch resource. Currently, the only CloudWatch
 * resources that can be tagged are alarms, dashboards, metric streams and Contributor Insights rules.
 *
 * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions by
 * granting a user permission to access or change only resources with certain tag values.
 *
 * <p>Tags don't have any semantic meaning to Amazon Web Services and are interpreted strictly as strings of characters.
 *
 * <p>You can use the <code>TagResource</code> action with an alarm that already has tags. If you specify a new tag key
 * for the alarm, this tag is appended to the list of tags associated with the alarm. If you specify a tag key that is
 * already associated with the alarm, the new tag value that you specify replaces the previous value for that tag.
 *
 * <p>You can associate as many as 50 tags with a CloudWatch resource.
 */
@SmithyGenerated
public final class TagResource implements ApiOperation<TagResourceInput, TagResourceOutput> {

    private static final TagResource $INSTANCE = new TagResource();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#TagResource"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConcurrentModificationException.$ID, ConcurrentModificationException.class, ConcurrentModificationException::builder)
        .putType(ConflictException.$ID, ConflictException.class, ConflictException::builder)
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static TagResource instance() {
        return $INSTANCE;
    }

    private TagResource() {}

    @Override
    public ShapeBuilder<TagResourceInput> inputBuilder() {
        return TagResourceInput.builder();
    }

    @Override
    public ShapeBuilder<TagResourceOutput> outputBuilder() {
        return TagResourceOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return TagResourceInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return TagResourceOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConcurrentModificationException.$SCHEMA, ConflictException.$SCHEMA, InternalServiceFault.$SCHEMA, InvalidParameterValueException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
