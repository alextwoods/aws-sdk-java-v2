package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Attaches an annotation to an Amazon S3 object. An annotation is a named payload of 1 byte to 1 MiB that you can
 * associate with a specific object or object version. Each object can have up to 1,000 annotations.
 *
 * <p>For annotation naming rules and restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/annotations-overview.html">Annotation naming guidelines</a> in the <i>Amazon S3 User
 * Guide</i>.
 *
 * <p>Annotations inherit the encryption of their parent object. For objects without server-side encryption, annotations
 * are encrypted with SSE-S3 (the default for new objects). Objects encrypted with SSE-C cannot have annotations.
 *
 * <p>To use this operation, you must have the <code>s3:PutObjectAnnotation</code> permission. If the bucket has
 * Requester Pays enabled, you must include the <code>x-amz-request-payer</code> header.
 *
 * <p>Annotations are not supported by the following features: S3 Inventory Reports, API Gateway, S3 Storage Lens,
 * Amazon S3 File Gateway, Amazon FSx, S3 on Outposts, and S3 Express One Zone (directory buckets).
 *
 * <p>The following operations are related to <code>PutObjectAnnotation</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAnnotation.html">GetObjectAnnotation</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectAnnotations.html">ListObjectAnnotations</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjectAnnotation.html">DeleteObjectAnnotation</a>
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class PutObjectAnnotation implements ApiOperation<PutObjectAnnotationInput, PutObjectAnnotationOutput> {

    private static final PutObjectAnnotation $INSTANCE = new PutObjectAnnotation();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutObjectAnnotation"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?annotation")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(AnnotationLimitExceeded.$ID, AnnotationLimitExceeded.class, AnnotationLimitExceeded::builder)
        .putType(AnnotationNameTooLong.$ID, AnnotationNameTooLong.class, AnnotationNameTooLong::builder)
        .putType(InvalidAnnotationName.$ID, InvalidAnnotationName.class, InvalidAnnotationName::builder)
        .putType(InvalidRequest.$ID, InvalidRequest.class, InvalidRequest::builder)
        .putType(NoSuchBucket.$ID, NoSuchBucket.class, NoSuchBucket::builder)
        .putType(NoSuchKey.$ID, NoSuchKey.class, NoSuchKey::builder)
        .putType(UnsupportedMediaType.$ID, UnsupportedMediaType.class, UnsupportedMediaType::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema INPUT_STREAM_MEMBER = PutObjectAnnotationInput.$SCHEMA.member("AnnotationPayload");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutObjectAnnotation instance() {
        return $INSTANCE;
    }

    private PutObjectAnnotation() {}

    @Override
    public ShapeBuilder<PutObjectAnnotationInput> inputBuilder() {
        return PutObjectAnnotationInput.builder();
    }

    @Override
    public ShapeBuilder<PutObjectAnnotationOutput> outputBuilder() {
        return PutObjectAnnotationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutObjectAnnotationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutObjectAnnotationOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(AnnotationLimitExceeded.$SCHEMA, AnnotationNameTooLong.$SCHEMA, InvalidAnnotationName.$SCHEMA, InvalidRequest.$SCHEMA, NoSuchBucket.$SCHEMA, NoSuchKey.$SCHEMA, UnsupportedMediaType.$SCHEMA);
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return SCHEMES;
    }

    @Override
    public Schema inputStreamMember() {
        return INPUT_STREAM_MEMBER;
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
