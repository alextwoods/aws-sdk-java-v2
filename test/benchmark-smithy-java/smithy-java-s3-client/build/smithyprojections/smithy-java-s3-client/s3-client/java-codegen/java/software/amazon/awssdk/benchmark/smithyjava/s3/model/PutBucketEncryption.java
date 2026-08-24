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
 * This operation configures default encryption and Amazon S3 Bucket Keys for an existing bucket. You can also <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_BlockedEncryptionTypes.html">block
 * encryption types</a> using this operation.
 *
 * <p><b>Directory buckets </b> - For directory buckets, you must make requests for this API operation to the Regional
 * endpoint. These endpoints support path-style requests in the format <code>https://s3express-control.<i>region-code</i>
 * .amazonaws.com/<i>bucket-name</i></code>. Virtual-hosted-style requests aren't supported. For more information about
 * endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for directory buckets in Availability Zones</a>
 * in the <i>Amazon S3 User Guide</i>. For more information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for
 * directory buckets in Local Zones</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>By default, all buckets have a default encryption configuration that uses server-side encryption with Amazon S3
 * managed keys (SSE-S3).
 *
 * <ul>
 *   <li>
 *     <b>General purpose buckets</b>
 *
 *     <ul>
 *       <li>
 *         You can optionally configure default encryption for a bucket by using server-side encryption with Key
 *         Management Service (KMS) keys (SSE-KMS) or dual-layer server-side encryption with Amazon Web Services
 *         KMS keys (DSSE-KMS). If you specify default encryption by using SSE-KMS, you can also configure <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-key.html">
 *         Amazon S3 Bucket Keys</a>. For information about the bucket default encryption feature, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-encryption.html">Amazon
 *         S3 Bucket Default Encryption</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *       <li>
 *         If you use PutBucketEncryption to set your <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-encryption.html">default bucket encryption</a> to SSE-KMS, you should
 *         verify that your KMS key ID is correct. Amazon S3 doesn't validate the KMS key ID provided in
 *         PutBucketEncryption requests.
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     <b>Directory buckets </b> - You can optionally configure default encryption for a bucket by using server-side
 *     encryption with Key Management Service (KMS) keys (SSE-KMS).
 *
 *     <ul>
 *       <li>
 *         We recommend that the bucket's default encryption uses the desired encryption configuration and you
 *         don't override the bucket default encryption in your <code>CreateSession</code> requests or <code>PUT</code>
 *         object requests. Then, new objects are automatically encrypted with the desired encryption settings.
 *         For more information about the encryption overriding behaviors in directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-specifying-kms-encryption.html">
 *         Specifying server-side encryption with KMS for new object uploads</a>.
 *       </li>
 *       <li>
 *         Your SSE-KMS configuration can only support 1 <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed key</a> per directory bucket's
 *         lifetime. The <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#aws-managed-cmk">Amazon Web Services managed key</a> (<code>aws/s3</code>) isn't supported.
 *       </li>
 *       <li>
 *         S3 Bucket Keys are always enabled for <code>GET</code> and <code>PUT</code> operations in a directory
 *         bucket and can’t be disabled. S3 Bucket Keys aren't supported, when you copy SSE-KMS encrypted
 *         objects from general purpose buckets to directory buckets, from directory buckets to general purpose
 *         buckets, or between directory buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-objects-Batch-Ops">the Copy
 *         operation in Batch Operations</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-import-job">the import jobs</a>. In this case, Amazon S3 makes a call to
 *         KMS every time a copy request is made for a KMS-encrypted object.
 *       </li>
 *       <li>
 *         When you specify an <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">KMS customer managed key</a> for encryption in your directory bucket, only use
 *         the key ID or key ARN. The key alias format of the KMS key isn't supported.
 *       </li>
 *       <li>
 *         For directory buckets, if you use PutBucketEncryption to set your <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-encryption.html">default bucket encryption</a> to
 *         SSE-KMS, Amazon S3 validates the KMS key ID provided in PutBucketEncryption requests.
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>If you're specifying a customer managed KMS key, we recommend using a fully qualified KMS key ARN. If you use a
 * KMS key alias instead, then KMS resolves the key within the requester’s account. This behavior can result in data
 * that's encrypted with a KMS key that belongs to the requester, and not the bucket owner.
 *
 * <p>Also, this action requires Amazon Web Services Signature Version 4. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-authenticating-requests.html"> Authenticating
 * Requests (Amazon Web Services Signature Version 4)</a>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - The <code>s3:PutEncryptionConfiguration</code> permission
 *         is required in a policy. The bucket owner has this permission by default. The bucket owner can grant
 *         this permission to others. For more information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-actions.html#using-with-s3-actions-related-to-bucket-subresources">Permissions Related to
 *         Bucket Operations</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing Access Permissions to Your Amazon S3 Resources</a> in the <i>
 *         Amazon S3 User Guide</i>.
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - To grant access to this API operation, you must have the <code>
 *         s3express:PutEncryptionConfiguration</code> permission in an IAM identity-based policy instead of a
 *         bucket policy. Cross-account access to this API operation isn't supported. This operation can only be
 *         performed by the Amazon Web Services account that owns the resource. For more information about
 *         directory bucket policies and permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam.html">Amazon Web Services Identity and Access Management
 *         (IAM) for S3 Express One Zone</a> in the <i>Amazon S3 User Guide</i>.To set a directory bucket
 *         default encryption with SSE-KMS, you must also have the <code>kms:GenerateDataKey</code> and the <code>
 *         kms:Decrypt</code> permissions in IAM identity-based policies and KMS key policies for the target KMS
 *         key.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code>s3express-control.<i>region-code</i>
 *     .amazonaws.com</code>.
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>PutBucketEncryption</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketEncryption.html">GetBucketEncryption</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketEncryption.html">DeleteBucketEncryption</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class PutBucketEncryption implements ApiOperation<PutBucketEncryptionInput, PutBucketEncryptionOutput> {

    private static final PutBucketEncryption $INSTANCE = new PutBucketEncryption();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketEncryption"),
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
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?encryption")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketEncryption instance() {
        return $INSTANCE;
    }

    private PutBucketEncryption() {}

    @Override
    public ShapeBuilder<PutBucketEncryptionInput> inputBuilder() {
        return PutBucketEncryptionInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketEncryptionOutput> outputBuilder() {
        return PutBucketEncryptionOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketEncryptionInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketEncryptionOutput.$SCHEMA;
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
