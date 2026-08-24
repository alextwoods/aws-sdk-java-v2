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
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>When you enable versioning on a bucket for the first time, it might take a short amount of time for the change to
 * be fully propagated. While this change is propagating, you might encounter intermittent <code>HTTP 404 NoSuchKey</code>
 * errors for requests to objects created or updated after enabling versioning. We recommend that you wait for 15
 * minutes after enabling versioning before issuing write operations (<code>PUT</code> or <code>DELETE</code>) on
 * objects in the bucket.
 *
 * <p>Sets the versioning state of an existing bucket.
 *
 * <p>You can set the versioning state with one of the following values:
 *
 * <p><b>Enabled</b>—Enables versioning for the objects in the bucket. All objects added to the bucket receive a unique
 * version ID.
 *
 * <p><b>Suspended</b>—Disables versioning for the objects in the bucket. All objects added to the bucket receive the
 * version ID null.
 *
 * <p>If the versioning state has never been set on a bucket, it has no versioning state; a <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html">GetBucketVersioning</a>
 * request does not return a versioning state value.
 *
 * <p>In order to enable MFA Delete, you must be the bucket owner. If you are the bucket owner and want to enable MFA
 * Delete in the bucket versioning configuration, you must include the <code>x-amz-mfa request</code> header and the <code>
 * Status</code> and the <code>MfaDelete</code> request elements in a request to set the versioning state of the bucket.
 *
 * <p>If you have an object expiration lifecycle configuration in your non-versioned bucket and you want to maintain the
 * same permanent delete behavior when you enable versioning, you must add a noncurrent expiration policy. The
 * noncurrent expiration lifecycle configuration will manage the deletes of the noncurrent object versions in the
 * version-enabled bucket. (A version-enabled bucket maintains one current and zero or more noncurrent object versions.)
 * For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lifecycle-mgmt.html#lifecycle-and-other-bucket-config">Lifecycle and Versioning</a>.
 *
 * <p>The following operations are related to <code>PutBucketVersioning</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html">DeleteBucket</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html">GetBucketVersioning</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>Set versioning configuration on a bucket</h3>
 *
 * <p>The following example sets versioning configuration on bucket. The configuration enables versioning on the bucket.{@snippet :
 * var input = PutBucketVersioningInput.builder()
 *                 .bucket("examplebucket").versioningConfiguration(VersioningConfiguration.builder()
 *                                              .mfaDelete(MFADelete.DISABLED).status(BucketVersioningStatus.ENABLED)
 *                                              .build())
 *                 .build();
 *
 * var result = client.putBucketVersioning(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketVersioning implements ApiOperation<PutBucketVersioningInput, PutBucketVersioningOutput> {

    private static final PutBucketVersioning $INSTANCE = new PutBucketVersioning();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketVersioning"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .withMember("requestChecksumRequired", true)
                    .build()
            ),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?versioning")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketVersioning instance() {
        return $INSTANCE;
    }

    private PutBucketVersioning() {}

    @Override
    public ShapeBuilder<PutBucketVersioningInput> inputBuilder() {
        return PutBucketVersioningInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketVersioningOutput> outputBuilder() {
        return PutBucketVersioningOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketVersioningInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketVersioningOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of();
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
