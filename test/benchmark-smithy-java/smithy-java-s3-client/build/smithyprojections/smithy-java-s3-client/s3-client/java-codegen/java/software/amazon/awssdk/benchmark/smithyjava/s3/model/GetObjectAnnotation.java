package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Retrieves an annotation from an Amazon S3 object. To use this operation, you must have the <code>
 * s3:GetObjectAnnotation</code> permission.
 *
 * <p>If checksum mode is enabled via the <code>x-amz-checksum-mode</code> header, Amazon S3 returns the stored checksum
 * in the response headers for client-side validation.
 *
 * <p>Annotations are not supported by the following features: S3 Inventory Reports, API Gateway, S3 Storage Lens,
 * Amazon S3 File Gateway, Amazon FSx, S3 on Outposts, and S3 Express One Zone (directory buckets).
 *
 * <p>The following operations are related to <code>GetObjectAnnotation</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObjectAnnotation.html">PutObjectAnnotation</a>
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
public final class GetObjectAnnotation implements ApiOperation<GetObjectAnnotationInput, GetObjectAnnotationOutput> {

    private static final GetObjectAnnotation $INSTANCE = new GetObjectAnnotation();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetObjectAnnotation"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestValidationModeMember", "ChecksumMode")
                    .withMember("responseAlgorithms", ArrayNode.builder()
                        .withValue("CRC64NVME")
                        .withValue("CRC32")
                        .withValue("CRC32C")
                        .withValue("SHA256")
                        .withValue("SHA1")
                        .withValue("SHA512")
                        .withValue("MD5")
                        .withValue("XXHASH64")
                        .withValue("XXHASH3")
                        .withValue("XXHASH128")
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?annotation&x-id=GetObjectAnnotation")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(NoSuchAnnotation.$ID, NoSuchAnnotation.class, NoSuchAnnotation::builder)
        .putType(NoSuchBucket.$ID, NoSuchBucket.class, NoSuchBucket::builder)
        .putType(NoSuchKey.$ID, NoSuchKey.class, NoSuchKey::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema OUTPUT_STREAM_MEMBER = GetObjectAnnotationOutput.$SCHEMA.member("AnnotationPayload");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetObjectAnnotation instance() {
        return $INSTANCE;
    }

    private GetObjectAnnotation() {}

    @Override
    public ShapeBuilder<GetObjectAnnotationInput> inputBuilder() {
        return GetObjectAnnotationInput.builder();
    }

    @Override
    public ShapeBuilder<GetObjectAnnotationOutput> outputBuilder() {
        return GetObjectAnnotationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetObjectAnnotationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetObjectAnnotationOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(NoSuchAnnotation.$SCHEMA, NoSuchBucket.$SCHEMA, NoSuchKey.$SCHEMA);
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
        return OUTPUT_STREAM_MEMBER;
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
