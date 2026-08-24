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
 * This operation is not supported for directory buckets or Amazon S3 on Outposts buckets.
 *
 * <p> Updates the server-side encryption type of an existing encrypted object in a general purpose bucket. You can use
 * the <code>UpdateObjectEncryption</code> operation to change encrypted objects from server-side encryption with Amazon
 * S3 managed keys (SSE-S3) to server-side encryption with Key Management Service (KMS) keys (SSE-KMS), or to apply S3
 * Bucket Keys. You can also use the <code>UpdateObjectEncryption</code> operation to change the customer-managed KMS
 * key used to encrypt your data so that you can comply with custom key-rotation standards.
 *
 * <p>Using the <code>UpdateObjectEncryption</code> operation, you can atomically update the server-side encryption type
 * of an existing object in a general purpose bucket without any data movement. The <code>UpdateObjectEncryption</code>
 * operation uses envelope encryption to re-encrypt the data key used to encrypt and decrypt your object with your newly
 * specified server-side encryption type. In other words, when you use the <code>UpdateObjectEncryption</code>
 * operation, your data isn't copied, archived objects in the S3 Glacier Flexible Retrieval and S3 Glacier Deep Archive
 * storage classes aren't restored, and objects in the S3 Intelligent-Tiering storage class aren't moved between tiers.
 * Additionally, the <code>UpdateObjectEncryption</code> operation preserves all object metadata properties, including
 * the storage class, creation date, last modified date, ETag, and checksum properties. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/update-sse-encryption.html">
 * Updating server-side encryption for existing objects</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>By default, all <code>UpdateObjectEncryption</code> requests that specify a customer-managed KMS key are
 * restricted to KMS keys that are owned by the bucket owner's Amazon Web Services account. If you're using
 * Organizations, you can request the ability to use KMS keys owned by other member accounts within your organization by
 * contacting Amazon Web Services Support.
 *
 * <p>Source objects that are unencrypted, or encrypted with either dual-layer server-side encryption with KMS keys
 * (DSSE-KMS) or server-side encryption with customer-provided keys (SSE-C) aren't supported by this operation.
 * Additionally, you cannot specify SSE-S3 encryption as the requested new encryption type <code>UpdateObjectEncryption</code>
 * request.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         To use the <code>UpdateObjectEncryption</code> operation, you must have the following permissions:
 *
 *         <ul>
 *           <li>
 *             <code>s3:UpdateObjectEncryption</code>
 *           </li>
 *           <li>
 *             <code>kms:Encrypt</code>
 *           </li>
 *           <li>
 *             <code>kms:Decrypt</code>
 *           </li>
 *           <li>
 *             <code>kms:GenerateDataKey</code>
 *           </li>
 *           <li>
 *             <code>kms:ReEncrypt&#42;</code>
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         If you're using Organizations, to use this operation with customer-managed KMS keys from other Amazon
 *         Web Services accounts within your organization, you must have the <code>organizations:DescribeAccount</code>
 *         permission.
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <dl>
 *   <dt>
 *     Errors
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         You might receive an <code>InvalidRequest</code> error for several reasons. Depending on the reason
 *         for the error, you might receive one of the following messages:
 *
 *         <ul>
 *           <li>
 *             The <code>UpdateObjectEncryption</code> operation doesn't supported unencrypted source
 *             objects. Only source objects encrypted with SSE-S3 or SSE-KMS are supported.
 *           </li>
 *           <li>
 *             The <code>UpdateObjectEncryption</code> operation doesn't support source objects with the
 *             encryption type DSSE-KMS or SSE-C. Only source objects encrypted with SSE-S3 or SSE-KMS are
 *             supported.
 *           </li>
 *           <li>
 *             The <code>UpdateObjectEncryption</code> operation doesn't support updating the encryption
 *             type to DSSE-KMS or SSE-C. Modify the request to specify SSE-KMS for the updated encryption
 *             type, and then try again.
 *           </li>
 *           <li>
 *             Requests that modify an object encryption configuration require Amazon Web Services Signature
 *             Version 4. Modify the request to use Amazon Web Services Signature Version 4, and then try
 *             again.
 *           </li>
 *           <li>
 *             Requests that modify an object encryption configuration require a valid new encryption type.
 *             Valid values are <code>SSEKMS</code>. Modify the request to specify SSE-KMS for the updated
 *             encryption type, and then try again.
 *           </li>
 *           <li>
 *             Requests that modify an object's encryption type to SSE-KMS require an Amazon Web Services
 *             KMS key Amazon Resource Name (ARN). Modify the request to specify a KMS key ARN, and then try
 *             again.
 *           </li>
 *           <li>
 *             Requests that modify an object's encryption type to SSE-KMS require a valid Amazon Web
 *             Services KMS key Amazon Resource Name (ARN). Confirm that you have a correctly formatted KMS
 *             key ARN in your request, and then try again.
 *           </li>
 *           <li>
 *             The <code>BucketKeyEnabled</code> value isn't valid. Valid values are <code>true</code> or <code>
 *             false</code>. Modify the request to specify a valid value, and then try again.
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         You might receive an <code>AccessDenied</code> error for several reasons. Depending on the reason for
 *         the error, you might receive one of the following messages:
 *
 *         <ul>
 *           <li>
 *             The Amazon Web Services KMS key in the request must be owned by the same account as the
 *             bucket. Modify the request to specify a KMS key from the same account, and then try again.
 *           </li>
 *           <li>
 *             The bucket owner's account was approved to make <code>UpdateObjectEncryption</code> requests
 *             that use any Amazon Web Services KMS key in their organization, but the bucket owner's
 *             account isn't part of an organization in Organizations. Make sure that the bucket owner's
 *             account and the specified KMS key belong to the same organization, and then try again.
 *           </li>
 *           <li>
 *             The specified Amazon Web Services KMS key must be from the same organization in Organizations
 *             as the bucket. Specify a KMS key that belongs to the same organization as the bucket, and
 *             then try again.
 *           </li>
 *           <li>
 *             The encryption type for the specified object can’t be updated because that object is
 *             protected by S3 Object Lock. If the object has a governance-mode retention period or a legal
 *             hold, you must first remove the Object Lock status on the object before you issue your <code>
 *             UpdateObjectEncryption</code> request. You can't use the <code>UpdateObjectEncryption</code>
 *             operation with objects that have an Object Lock compliance mode retention period applied to
 *             them.
 *           </li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 */
@SmithyGenerated
public final class UpdateObjectEncryption implements ApiOperation<UpdateObjectEncryptionInput, UpdateObjectEncryptionOutput> {

    private static final UpdateObjectEncryption $INSTANCE = new UpdateObjectEncryption();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#UpdateObjectEncryption"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .withMember("requestChecksumRequired", true)
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?encryption")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(AccessDenied.$ID, AccessDenied.class, AccessDenied::builder)
        .putType(InvalidRequest.$ID, InvalidRequest.class, InvalidRequest::builder)
        .putType(NoSuchKey.$ID, NoSuchKey.class, NoSuchKey::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UpdateObjectEncryption instance() {
        return $INSTANCE;
    }

    private UpdateObjectEncryption() {}

    @Override
    public ShapeBuilder<UpdateObjectEncryptionInput> inputBuilder() {
        return UpdateObjectEncryptionInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateObjectEncryptionOutput> outputBuilder() {
        return UpdateObjectEncryptionOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateObjectEncryptionInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateObjectEncryptionOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(AccessDenied.$SCHEMA, InvalidRequest.$SCHEMA, NoSuchKey.$SCHEMA);
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
