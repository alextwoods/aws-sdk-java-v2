package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Lists the annotations attached to an Amazon S3 object. Results are paginated, with a maximum of 1,000 annotations per
 * object. Use the <code>AnnotationPrefix</code> parameter to filter the results by name prefix.
 *
 * <p>To use this operation, you must have the <code>s3:ListObjectAnnotations</code> permission.
 *
 * <p>Annotations are not supported by the following features: S3 Inventory Reports, API Gateway, S3 Storage Lens,
 * Amazon S3 File Gateway, Amazon FSx, S3 on Outposts, and S3 Express One Zone (directory buckets).
 *
 * <p>The following operations are related to <code>ListObjectAnnotations</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObjectAnnotation.html">PutObjectAnnotation</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAnnotation.html">GetObjectAnnotation</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjectAnnotation.html">DeleteObjectAnnotation</a>
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class ListObjectAnnotations implements ApiOperation<ListObjectAnnotationsInput, ListObjectAnnotationsOutput> {

    private static final ListObjectAnnotations $INSTANCE = new ListObjectAnnotations();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#ListObjectAnnotations"),
            PaginatedTrait.builder().inputToken("ContinuationToken").outputToken("NextContinuationToken").items("Annotations").pageSize("MaxAnnotationResults").build(),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?annotation&x-id=ListObjectAnnotations")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidPrefix.$ID, InvalidPrefix.class, InvalidPrefix::builder)
        .putType(NoSuchBucket.$ID, NoSuchBucket.class, NoSuchBucket::builder)
        .putType(NoSuchKey.$ID, NoSuchKey.class, NoSuchKey::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListObjectAnnotations instance() {
        return $INSTANCE;
    }

    private ListObjectAnnotations() {}

    @Override
    public ShapeBuilder<ListObjectAnnotationsInput> inputBuilder() {
        return ListObjectAnnotationsInput.builder();
    }

    @Override
    public ShapeBuilder<ListObjectAnnotationsOutput> outputBuilder() {
        return ListObjectAnnotationsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListObjectAnnotationsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListObjectAnnotationsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidPrefix.$SCHEMA, NoSuchBucket.$SCHEMA, NoSuchKey.$SCHEMA);
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
        return S3ApiService.instance();
    }
    }
