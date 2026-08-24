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
 * Associate a set of tags with an Amazon DynamoDB resource. You can then activate these user-defined tags so that they
 * appear on the Billing and Cost Management console for cost allocation tracking. You can call TagResource up to five
 * times per second, per account.
 *
 * <ul>
 *   <li>
 *     <code>TagResource</code> is an asynchronous operation. If you issue a <a>ListTagsOfResource</a> request
 *     immediately after a <code>TagResource</code> request, DynamoDB might return your previous tag set, if there
 *     was one, or an empty tag set. This is because <code>ListTagsOfResource</code> uses an eventually consistent
 *     query, and the metadata for your tags or table might not be available at that moment. Wait for a few seconds,
 *     and then try the <code>ListTagsOfResource</code> request again.
 *   </li>
 *   <li>
 *     The application or removal of tags using <code>TagResource</code> and <code>UntagResource</code> APIs is
 *     eventually consistent. <code>ListTagsOfResource</code> API will only reflect the changes after a few seconds.
 *   </li>
 * </ul>
 *
 * <p>For an overview on tagging DynamoDB resources, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a> in the <i>Amazon DynamoDB Developer
 * Guide</i>.
 */
@SmithyGenerated
public final class TagResource implements ApiOperation<TagResourceInput, TagResourceOutput> {

    private static final TagResource $INSTANCE = new TagResource();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#TagResource"));

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
