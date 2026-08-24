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
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Deletes a specific annotation from an Amazon S3 object. Use the <code>x-amz-object-if-match</code> header to perform
 * a conditional delete that only succeeds if the object's ETag matches the provided value, preventing race conditions
 * during concurrent updates.
 *
 * <p>Deleting an annotation is permanent. Annotations are not independently versioned, so there is no delete marker or
 * way to recover a deleted annotation.
 *
 * <p>To use this operation, you must have the <code>s3:DeleteObjectAnnotation</code> permission. If the object is
 * protected by Object Lock in governance mode, you must also include the <code>x-amz-bypass-governance-retention</code>
 * header.
 *
 * <p>Annotations are not supported by the following features: S3 Inventory Reports, API Gateway, S3 Storage Lens,
 * Amazon S3 File Gateway, Amazon FSx, S3 on Outposts, and S3 Express One Zone (directory buckets).
 *
 * <p>The following operations are related to <code>DeleteObjectAnnotation</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObjectAnnotation.html">PutObjectAnnotation</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAnnotation.html">GetObjectAnnotation</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectAnnotations.html">ListObjectAnnotations</a>
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class DeleteObjectAnnotation implements ApiOperation<DeleteObjectAnnotationInput, DeleteObjectAnnotationOutput> {

    private static final DeleteObjectAnnotation $INSTANCE = new DeleteObjectAnnotation();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteObjectAnnotation"),
            HttpTrait.builder().method("DELETE").code(204).uri(UriPattern.parse("/{Bucket}/{Key+}?annotation")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(NoSuchBucket.$ID, NoSuchBucket.class, NoSuchBucket::builder)
        .putType(NoSuchKey.$ID, NoSuchKey.class, NoSuchKey::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteObjectAnnotation instance() {
        return $INSTANCE;
    }

    private DeleteObjectAnnotation() {}

    @Override
    public ShapeBuilder<DeleteObjectAnnotationInput> inputBuilder() {
        return DeleteObjectAnnotationInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteObjectAnnotationOutput> outputBuilder() {
        return DeleteObjectAnnotationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteObjectAnnotationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteObjectAnnotationOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(NoSuchBucket.$SCHEMA, NoSuchKey.$SCHEMA);
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
