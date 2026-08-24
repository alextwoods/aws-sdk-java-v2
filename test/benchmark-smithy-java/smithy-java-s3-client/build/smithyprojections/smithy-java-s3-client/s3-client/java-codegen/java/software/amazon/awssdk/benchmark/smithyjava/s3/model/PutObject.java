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
 * End of support notice: As of October 1, 2025, Amazon S3 has discontinued support for Email Grantee Access Control
 * Lists (ACLs). If you attempt to use an Email Grantee ACL in a request after October 1, 2025, the request will receive
 * an <code>HTTP 405</code> (Method Not Allowed) error.
 *
 * <p>This change affects the following Amazon Web Services Regions: US East (N. Virginia), US West (N. California), US
 * West (Oregon), Asia Pacific (Singapore), Asia Pacific (Sydney), Asia Pacific (Tokyo), Europe (Ireland), and South
 * America (São Paulo).
 *
 * <p>Adds an object to a bucket.
 *
 * <ul>
 *   <li>
 *     Amazon S3 never adds partial objects; if you receive a success response, Amazon S3 added the entire object to
 *     the bucket. You cannot use <code>PutObject</code> to only update a single piece of metadata for an existing
 *     object. You must put the entire object with updated metadata if you want to update some values.
 *   </li>
 *   <li>
 *     If your bucket uses the bucket owner enforced setting for Object Ownership, ACLs are disabled and no longer
 *     affect permissions. All objects written to the bucket by any account will be owned by the bucket owner.
 *   </li>
 *   <li>
 *     <b>Directory buckets</b> - For directory buckets, you must make requests for this API operation to the Zonal
 *     endpoint. These endpoints support virtual-hosted-style requests in the format <code>https://<i>
 *     amzn-s3-demo-bucket</i>.s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com/<i>key-name</i></code>.
 *     Path-style requests are not supported. For more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">
 *     Regional and Zonal endpoints for directory buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>
 *     . For more information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a>
 *     in the <i>Amazon S3 User Guide</i>.
 *   </li>
 * </ul>
 *
 * <p>Amazon S3 is a distributed system. If it receives multiple write requests for the same object simultaneously, it
 * overwrites all but the last object written. However, Amazon S3 provides features that can modify this behavior:
 *
 * <ul>
 *   <li>
 *     <b>S3 Object Lock</b> - To prevent objects from being deleted or overwritten, you can use <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html">Amazon S3 Object
 *     Lock</a> in the <i>Amazon S3 User Guide</i>.This functionality is not supported for directory buckets.
 *   </li>
 *   <li>
 *     <b>If-None-Match</b> - Uploads the object only if the object key name does not already exist in the specified
 *     bucket. Otherwise, Amazon S3 returns a <code>412 Precondition Failed</code> error. If a conflicting operation
 *     occurs during the upload, S3 returns a <code>409 ConditionalRequestConflict</code> response. On a 409
 *     failure, retry the upload.Expects the &#42; character (asterisk).For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-requests.html">Add preconditions
 *     to S3 operations with conditional requests</a> in the <i>Amazon S3 User Guide</i> or <a href="https://datatracker.ietf.org/doc/rfc7232/">RFC 7232</a>. This
 *     functionality is not supported for S3 on Outposts.
 *   </li>
 *   <li>
 *     <b>S3 Versioning</b> - When you enable versioning for a bucket, if Amazon S3 receives multiple write requests
 *     for the same object simultaneously, it stores all versions of the objects. For each write request that is
 *     made to the same object, Amazon S3 automatically generates a unique version ID of that object being stored in
 *     Amazon S3. You can retrieve, replace, or delete any version of the object. For more information about
 *     versioning, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/AddingObjectstoVersioningEnabledBuckets.html">Adding Objects to Versioning-Enabled Buckets</a> in the <i>Amazon S3 User Guide</i>. For
 *     information about returning the versioning state of a bucket, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html">GetBucketVersioning</a>. This
 *     functionality is not supported for directory buckets.
 *   </li>
 * </ul>
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - The following permissions are required in your policies
 *         when your <code>PutObject</code> request includes specific headers.
 *
 *         <ul>
 *           <li>
 *             <b><code>s3:PutObject</code></b> - To successfully complete the <code>PutObject</code>
 *             request, you must always have the <code>s3:PutObject</code> permission on a bucket to add an
 *             object to it.
 *           </li>
 *           <li>
 *             <b><code>s3:PutObjectAcl</code></b> - To successfully change the objects ACL of your <code>
 *             PutObject</code> request, you must have the <code>s3:PutObjectAcl</code>.
 *           </li>
 *           <li>
 *             <b><code>s3:PutObjectTagging</code></b> - To successfully set the tag-set with your <code>
 *             PutObject</code> request, you must have the <code>s3:PutObjectTagging</code>.
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - To grant access to this API operation on a directory bucket, we
 *         recommend that you use the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateSession.html"><code>CreateSession</code></a> API operation for session-based
 *         authorization. Specifically, you grant the <code>s3express:CreateSession</code> permission to the
 *         directory bucket in a bucket policy or an IAM identity-based policy. Then, you make the <code>
 *         CreateSession</code> API call on the bucket to obtain a session token. With the session token in your
 *         request header, you can make API requests to this operation. After the session token expires, you
 *         make another <code>CreateSession</code> API call to generate a new session token for use. Amazon Web
 *         Services CLI or SDKs create session and refresh the session token automatically to avoid service
 *         interruptions when a session expires. For more information about authorization, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateSession.html"><code>
 *         CreateSession</code></a>.If the object is encrypted with SSE-KMS, you must also have the <code>
 *         kms:GenerateDataKey</code> and <code>kms:Decrypt</code> permissions in IAM identity-based policies
 *         and KMS key policies for the KMS key.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Data integrity with Content-MD5
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket</b> - To ensure that data is not corrupted traversing the network, use the <code>
 *         Content-MD5</code> header. When you use this header, Amazon S3 checks the object against the provided
 *         MD5 value and, if they do not match, Amazon S3 returns an error. Alternatively, when the object's
 *         ETag is its MD5 digest, you can calculate the MD5 while putting the object to Amazon S3 and compare
 *         the returned ETag to the calculated MD5 value.
 *       </li>
 *       <li>
 *         <b>Directory bucket</b> - This functionality is not supported for directory buckets.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code><i>Bucket-name</i>.s3express-<i>zone-id</i>
 *     .<i>region-code</i>.amazonaws.com</code>.
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
 *             Cannot specify both a write offset value and user-defined object metadata for existing
 *             objects.
 *           </li>
 *           <li>
 *             Checksum Type mismatch occurred, expected checksum Type: sha1, actual checksum Type: crc32c.
 *           </li>
 *           <li>
 *             Request body cannot be empty when 'write offset' is specified.
 *           </li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <p>For more information about related Amazon S3 APIs, see the following:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html">DeleteObject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To create an object.</h3>
 *
 * <p>The following example creates an object. If the bucket is versioning enabled, S3 returns version ID in response.{@snippet :
 * var input = PutObjectInput.builder()
 *                 .body(DataStream.ofBytes(Base64.getDecoder().decode("filetoupload"))).bucket("examplebucket").key("objectkey")
 *                 .build();
 *
 * var result = client.putObject(input);
 * result.equals(PutObjectOutput.builder()
 *                   .versionId("Bvq0EDKxOcXLJXNo_Lkz37eM3R4pfzyQ").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"")
 *                   .build());
 * }
 *
 * <h3>To upload an object (specify optional headers)</h3>
 *
 * <p>The following example uploads an object. The request specifies optional request headers to directs S3 to use specific storage class and use server-side encryption.{@snippet :
 * var input = PutObjectInput.builder()
 *                 .body(DataStream.ofString("HappyFace.jpg")).bucket("examplebucket").key("HappyFace.jpg").serverSideEncryption(ServerSideEncryption.AES256).storageClass(StorageClass.STANDARD_IA)
 *                 .build();
 *
 * var result = client.putObject(input);
 * result.equals(PutObjectOutput.builder()
 *                   .versionId("CG612hodqujkf8FaaNfp8U..FIhLROcp").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"").serverSideEncryption(ServerSideEncryption.AES256)
 *                   .build());
 * }
 *
 * <h3>To upload an object</h3>
 *
 * <p>The following example uploads an object to a versioning-enabled bucket. The source file is specified using Windows file syntax. S3 returns VersionId of the newly created object.{@snippet :
 * var input = PutObjectInput.builder()
 *                 .body(DataStream.ofString("HappyFace.jpg")).bucket("examplebucket").key("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.putObject(input);
 * result.equals(PutObjectOutput.builder()
 *                   .versionId("tpf3zF08nBplQK1XLOefGskR7mGDwcDk").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"")
 *                   .build());
 * }
 *
 * <h3>To upload an object and specify canned ACL.</h3>
 *
 * <p>The following example uploads and object. The request specifies optional canned ACL (access control list) to all READ access to authenticated users. If the bucket is versioning enabled, S3 returns version ID in response.{@snippet :
 * var input = PutObjectInput.builder()
 *                 .acl(ObjectCannedACL.AUTHENTICATED_READ).body(DataStream.ofBytes(Base64.getDecoder().decode("filetoupload"))).bucket("examplebucket").key("exampleobject")
 *                 .build();
 *
 * var result = client.putObject(input);
 * result.equals(PutObjectOutput.builder()
 *                   .versionId("Kirh.unyZwjQ69YxcQLA8z4F5j3kJJKr").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"")
 *                   .build());
 * }
 *
 * <h3>To upload an object and specify optional tags</h3>
 *
 * <p>The following example uploads an object. The request specifies optional object tags. The bucket is versioned, therefore S3 returns version ID of the newly created object.{@snippet :
 * var input = PutObjectInput.builder()
 *                 .body(DataStream.ofString("c:\\HappyFace.jpg")).bucket("examplebucket").key("HappyFace.jpg").tagging("key1=value1&key2=value2")
 *                 .build();
 *
 * var result = client.putObject(input);
 * result.equals(PutObjectOutput.builder()
 *                   .versionId("psM2sYY4.o1501dSx8wMvnkOzSBB.V4a").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"")
 *                   .build());
 * }
 *
 * <h3>To upload an object and specify server-side encryption and object tags</h3>
 *
 * <p>The following example uploads an object. The request specifies the optional server-side encryption option. The request also specifies optional object tags. If the bucket is versioning enabled, S3 returns version ID in response.{@snippet :
 * var input = PutObjectInput.builder()
 *                 .body(DataStream.ofBytes(Base64.getDecoder().decode("filetoupload"))).bucket("examplebucket").key("exampleobject").serverSideEncryption(ServerSideEncryption.AES256).tagging("key1=value1&key2=value2")
 *                 .build();
 *
 * var result = client.putObject(input);
 * result.equals(PutObjectOutput.builder()
 *                   .versionId("Ri.vC6qVlA4dEnjgRV4ZHsHoFIjqEMNt").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"").serverSideEncryption(ServerSideEncryption.AES256)
 *                   .build());
 * }
 *
 * <h3>To upload object and specify user-defined metadata</h3>
 *
 * <p>The following example creates an object. The request also specifies optional metadata. If the bucket is versioning enabled, S3 returns version ID in response.{@snippet :
 * var input = PutObjectInput.builder()
 *                 .body(DataStream.ofBytes(Base64.getDecoder().decode("filetoupload"))).bucket("examplebucket").key("exampleobject").metadata(Map.of(
 *                               "metadata1", "value1",
 *                               "metadata2", "value2"
 *                           ))
 *                 .build();
 *
 * var result = client.putObject(input);
 * result.equals(PutObjectOutput.builder()
 *                   .versionId("pSKidl4pHBiNwukdbcPXAIs.sshFFOc0").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"")
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class PutObject implements ApiOperation<PutObjectInput, PutObjectOutput> {

    private static final PutObject $INSTANCE = new PutObject();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutObject"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?x-id=PutObject")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(EncryptionTypeMismatch.$ID, EncryptionTypeMismatch.class, EncryptionTypeMismatch::builder)
        .putType(InvalidRequest.$ID, InvalidRequest.class, InvalidRequest::builder)
        .putType(InvalidWriteOffset.$ID, InvalidWriteOffset.class, InvalidWriteOffset::builder)
        .putType(TooManyParts.$ID, TooManyParts.class, TooManyParts::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema INPUT_STREAM_MEMBER = PutObjectInput.$SCHEMA.member("Body");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutObject instance() {
        return $INSTANCE;
    }

    private PutObject() {}

    @Override
    public ShapeBuilder<PutObjectInput> inputBuilder() {
        return PutObjectInput.builder();
    }

    @Override
    public ShapeBuilder<PutObjectOutput> outputBuilder() {
        return PutObjectOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutObjectInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutObjectOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(EncryptionTypeMismatch.$SCHEMA, InvalidRequest.$SCHEMA, InvalidWriteOffset.$SCHEMA, TooManyParts.$SCHEMA);
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
