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
 * Uploads a part in a multipart upload.
 *
 * <p>In this operation, you provide new data as a part of an object in your request. However, you have an option to
 * specify your existing Amazon S3 object as a data source for the part you are uploading. To upload a part from an
 * existing object, you use the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a> operation.
 *
 * <p>You must initiate a multipart upload (see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>) before you can upload any part. In
 * response to your initiate request, Amazon S3 returns an upload ID, a unique identifier that you must include in your
 * upload part request.
 *
 * <p>Part numbers can be any number from 1 to 10,000, inclusive. A part number uniquely identifies a part and also
 * defines its position within the object being created. If you upload a new part using the same part number that was
 * used with a previous part, the previously uploaded part is overwritten.
 *
 * <p>For information about maximum and minimum part sizes and other multipart upload specifications, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/qfacts.html">Multipart
 * upload limits</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>After you initiate multipart upload and upload one or more parts, you must either complete or abort multipart
 * upload in order to stop getting charged for storage of the uploaded parts. Only after you either complete or abort
 * multipart upload, Amazon S3 frees up the parts storage and stops charging you for the parts storage.
 *
 * <p>For more information on multipart uploads, go to <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html">Multipart Upload Overview</a> in the <i>Amazon S3 User Guide </i>
 * .
 *
 * <p><b>Directory buckets</b> - For directory buckets, you must make requests for this API operation to the Zonal
 * endpoint. These endpoints support virtual-hosted-style requests in the format <code>https://<i>amzn-s3-demo-bucket</i>
 * .s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com/<i>key-name</i></code>. Path-style requests are not
 * supported. For more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for
 * directory buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>. For more information about endpoints
 * in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - To perform a multipart upload with encryption using an
 *         Key Management Service key, the requester must have permission to the <code>kms:Decrypt</code> and <code>
 *         kms:GenerateDataKey</code> actions on the key. The requester must also have permissions for the <code>
 *         kms:GenerateDataKey</code> action for the <code>CreateMultipartUpload</code> API. Then, the requester
 *         needs permissions for the <code>kms:Decrypt</code> action on the <code>UploadPart</code> and <code>
 *         UploadPartCopy</code> APIs.These permissions are required because Amazon S3 must decrypt and read
 *         data from the encrypted file parts before it completes the multipart upload. For more information
 *         about KMS permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingKMSEncryption.html">Protecting data using server-side encryption with KMS</a> in the <i>
 *         Amazon S3 User Guide</i>. For information about the permissions required to use the multipart upload
 *         API, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuAndPermissions.html">Multipart upload and permissions</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html#mpuAndPermissions">Multipart upload API and permissions</a> in
 *         the <i>Amazon S3 User Guide</i>.
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
 *     Data integrity
 *   </dt>
 *   <dd>
 *
 *     <p><b>General purpose bucket</b> - To ensure that data is not corrupted traversing the network, specify the <code>
 *     Content-MD5</code> header in the upload part request. Amazon S3 checks the part data against the provided MD5
 *     value. If they do not match, Amazon S3 returns an error. If the upload request is signed with Signature
 *     Version 4, then Amazon Web Services S3 uses the <code>x-amz-content-sha256</code> header as a checksum
 *     instead of <code>Content-MD5</code>. For more information see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-auth-using-authorization-header.html">Authenticating Requests: Using the
 *     Authorization Header (Amazon Web Services Signature Version 4)</a>.
 *
 *     <p><b>Directory buckets</b> - MD5 is not supported by directory buckets. You can use checksum algorithms to
 *     check object integrity.
 *   </dd>
 *   <dt>
 *     Encryption
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket</b> - Server-side encryption is for data encryption at rest. Amazon S3
 *         encrypts your data as it writes it to disks in its data centers and decrypts it when you access it.
 *         You have mutually exclusive options to protect data using server-side encryption in Amazon S3,
 *         depending on how you choose to manage the encryption keys. Specifically, the encryption key options
 *         are Amazon S3 managed keys (SSE-S3), Amazon Web Services KMS keys (SSE-KMS), and Customer-Provided
 *         Keys (SSE-C). Amazon S3 encrypts data with server-side encryption using Amazon S3 managed keys
 *         (SSE-S3) by default. You can optionally tell Amazon S3 to encrypt data at rest using server-side
 *         encryption with other key options. The option you use depends on whether you want to use KMS keys
 *         (SSE-KMS) or provide your own encryption key (SSE-C).Server-side encryption is supported by the S3
 *         Multipart Upload operations. Unless you are using a customer-provided encryption key (SSE-C), you
 *         don't need to specify the encryption parameters in each UploadPart request. Instead, you only need to
 *         specify the server-side encryption parameters in the initial Initiate Multipart request. For more
 *         information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>.If you have server-side encryption with
 *         customer-provided keys (SSE-C) blocked for your general purpose bucket, you will get an HTTP 403
 *         Access Denied error when you specify the SSE-C request headers while writing new data to your bucket.
 *         For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/blocking-unblocking-s3-c-encryption-gpb.html">Blocking or unblocking SSE-C for a general purpose bucket</a>.If you
 *         request server-side encryption using a customer-provided encryption key (SSE-C) in your initiate
 *         multipart upload request, you must provide identical encryption information in each part upload using
 *         the following request headers.
 *
 *         <ul>
 *           <li>
 *             x-amz-server-side-encryption-customer-algorithm
 *           </li>
 *           <li>
 *             x-amz-server-side-encryption-customer-key
 *           </li>
 *           <li>
 *             x-amz-server-side-encryption-customer-key-MD5
 *           </li>
 *         </ul> For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingServerSideEncryption.html">Using Server-Side Encryption</a> in the <i>Amazon S3 User Guide</i>
 *         .
 *       </li>
 *       <li>
 *         <b>Directory buckets </b> - For directory buckets, there are only two supported options for
 *         server-side encryption: server-side encryption with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>
 *         ) and server-side encryption with KMS keys (SSE-KMS) (<code>aws:kms</code>).
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Special errors
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         Error Code: <code>NoSuchUpload</code>
 *
 *         <ul>
 *           <li>
 *             Description: The specified multipart upload does not exist. The upload ID might be invalid,
 *             or the multipart upload might have been aborted or completed.
 *           </li>
 *           <li>
 *             HTTP Status Code: 404 Not Found
 *           </li>
 *           <li>
 *             SOAP Fault Code Prefix: Client
 *           </li>
 *         </ul>
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
 * <p>The following operations are related to <code>UploadPart</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html">AbortMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html">ListParts</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListMultipartUploads.html">ListMultipartUploads</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To upload a part</h3>
 *
 * <p>The following example uploads part 1 of a multipart upload. The example specifies a file name for the part data. The Upload ID is same that is returned by the initiate multipart upload.{@snippet :
 * var input = UploadPartInput.builder()
 *                 .body(DataStream.ofBytes(Base64.getDecoder().decode("fileToUpload"))).bucket("examplebucket").key("examplelargeobject").partNumber(1).uploadId("xadcOB_7YPBOJuoFiQ9cz4P3Pe6FIZwO4f7wN93uHsNBEw97pl5eNwzExg0LAT2dUN91cOmrEQHDsP3WA60CEg--")
 *                 .build();
 *
 * var result = client.uploadPart(input);
 * result.equals(UploadPartOutput.builder()
 *                   .eTag("\"d8c2eafd90c266e19ab9dcacc479f8af\"")
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class UploadPart implements ApiOperation<UploadPartInput, UploadPartOutput> {

    private static final UploadPart $INSTANCE = new UploadPart();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#UploadPart"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?x-id=UploadPart")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema INPUT_STREAM_MEMBER = UploadPartInput.$SCHEMA.member("Body");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UploadPart instance() {
        return $INSTANCE;
    }

    private UploadPart() {}

    @Override
    public ShapeBuilder<UploadPartInput> inputBuilder() {
        return UploadPartInput.builder();
    }

    @Override
    public ShapeBuilder<UploadPartOutput> outputBuilder() {
        return UploadPartOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UploadPartInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UploadPartOutput.$SCHEMA;
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
