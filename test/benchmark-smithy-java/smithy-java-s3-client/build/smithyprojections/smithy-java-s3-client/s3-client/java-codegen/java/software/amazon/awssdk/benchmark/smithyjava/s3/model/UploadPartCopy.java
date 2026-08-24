package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
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
 * Uploads a part by copying data from an existing object as data source. To specify the data source, you add the
 * request header <code>x-amz-copy-source</code> in your request. To specify a byte range, you add the request header <code>
 * x-amz-copy-source-range</code> in your request.
 *
 * <p>For information about maximum and minimum part sizes and other multipart upload specifications, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/qfacts.html">Multipart
 * upload limits</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>Instead of copying data from an existing object as part data, you might use the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart</a> action to upload
 * new data as a part of an object in your request.
 *
 * <p>You must initiate a multipart upload before you can upload any part. In response to your initiate request, Amazon
 * S3 returns the upload ID, a unique identifier that you must include in your upload part request.
 *
 * <p>For conceptual information about multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/uploadobjusingmpu.html">Uploading Objects Using Multipart Upload</a> in the <i>
 * Amazon S3 User Guide</i>. For information about copying objects using a single atomic action vs. a multipart upload,
 * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ObjectOperations.html">Operations on Objects</a> in the <i>Amazon S3 User Guide</i>.
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
 *     Authentication and authorization
 *   </dt>
 *   <dd>
 *
 *     <p>All <code>UploadPartCopy</code> requests must be authenticated and signed by using IAM credentials (access
 *     key ID and secret access key for the IAM identities). All headers with the <code>x-amz-</code> prefix,
 *     including <code>x-amz-copy-source</code>, must be signed. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html">REST Authentication</a>
 *     .
 *
 *     <p><b>Directory buckets</b> - You must use IAM credentials to authenticate and authorize your access to the <code>
 *     UploadPartCopy</code> API operation, instead of using the temporary security credentials through the <code>
 *     CreateSession</code> API operation.
 *
 *     <p>Amazon Web Services CLI or SDKs handles authentication and authorization on your behalf.
 *   </dd>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p>You must have <code>READ</code> access to the source object and <code>WRITE</code> access to the
 *     destination bucket.
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - You must have the permissions in a policy based on the
 *         bucket types of your source bucket and destination bucket in an <code>UploadPartCopy</code>
 *         operation.
 *
 *         <ul>
 *           <li>
 *             If the source object is in a general purpose bucket, you must have the <b><code>s3:GetObject</code></b>
 *             permission to read the source object that is being copied.
 *           </li>
 *           <li>
 *             If the destination bucket is a general purpose bucket, you must have the <b><code>
 *             s3:PutObject</code></b> permission to write the object copy to the destination bucket.
 *           </li>
 *           <li>
 *             To perform a multipart upload with encryption using an Key Management Service key, the
 *             requester must have permission to the <code>kms:Decrypt</code> and <code>kms:GenerateDataKey</code>
 *             actions on the key. The requester must also have permissions for the <code>
 *             kms:GenerateDataKey</code> action for the <code>CreateMultipartUpload</code> API. Then, the
 *             requester needs permissions for the <code>kms:Decrypt</code> action on the <code>UploadPart</code>
 *             and <code>UploadPartCopy</code> APIs. These permissions are required because Amazon S3 must
 *             decrypt and read data from the encrypted file parts before it completes the multipart upload.
 *             For more information about KMS permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingKMSEncryption.html">Protecting data using server-side
 *             encryption with KMS</a> in the <i>Amazon S3 User Guide</i>. For information about the
 *             permissions required to use the multipart upload API, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuAndPermissions.html">Multipart upload and permissions</a>
 *             and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html#mpuAndPermissions">Multipart upload API and permissions</a> in the <i>Amazon S3 User Guide</i>.
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - You must have permissions in a bucket policy or an IAM
 *         identity-based policy based on the source and destination bucket types in an <code>UploadPartCopy</code>
 *         operation.
 *
 *         <ul>
 *           <li>
 *             If the source object that you want to copy is in a directory bucket, you must have the <b><code>
 *             s3express:CreateSession</code></b> permission in the <code>Action</code> element of a policy
 *             to read the object. If no session mode is specified, the session will be created with the
 *             maximum allowable privilege, attempting <code>ReadWrite</code> first, then <code>ReadOnly</code>
 *             if <code>ReadWrite</code> is not permitted. If you want to explicitly restrict the access to
 *             be read-only, you can set the <code>s3express:SessionMode</code> condition key to <code>
 *             ReadOnly</code> on the copy source bucket.
 *           </li>
 *           <li>
 *             If the copy destination is a directory bucket, you must have the <b><code>
 *             s3express:CreateSession</code></b> permission in the <code>Action</code> element of a policy
 *             to write the object to the destination. The <code>s3express:SessionMode</code> condition key
 *             cannot be set to <code>ReadOnly</code> on the copy destination.
 *           </li>
 *         </ul>If the object is encrypted with SSE-KMS, you must also have the <code>kms:GenerateDataKey</code>
 *         and <code>kms:Decrypt</code> permissions in IAM identity-based policies and KMS key policies for the
 *         KMS key.For example policies, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam-example-bucket-policies.html">Example bucket policies for S3 Express One Zone</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam-identity-policies.html">
 *         Amazon Web Services Identity and Access Management (IAM) identity-based policies for S3 Express One
 *         Zone</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Encryption
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose buckets </b> - For information about using server-side encryption with
 *         customer-provided encryption keys with the <code>UploadPartCopy</code> operation, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>
 *         and <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart</a>. If you have server-side encryption with customer-provided keys (SSE-C) blocked
 *         for your general purpose bucket, you will get an HTTP 403 Access Denied error when you specify the
 *         SSE-C request headers while writing new data to your bucket. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/blocking-unblocking-s3-c-encryption-gpb.html">Blocking or
 *         unblocking SSE-C for a general purpose bucket</a>.
 *       </li>
 *       <li>
 *         <b>Directory buckets </b> - For directory buckets, there are only two supported options for
 *         server-side encryption: server-side encryption with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>
 *         ) and server-side encryption with KMS keys (SSE-KMS) (<code>aws:kms</code>). For more information,
 *         see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-serv-side-encryption.html">Protecting data with server-side encryption</a> in the <i>Amazon S3 User Guide</i>.For
 *         directory buckets, when you perform a <code>CreateMultipartUpload</code> operation and an <code>
 *         UploadPartCopy</code> operation, the request headers you provide in the <code>CreateMultipartUpload</code>
 *         request must match the default encryption configuration of the destination bucket. S3 Bucket Keys
 *         aren't supported, when you copy SSE-KMS encrypted objects from general purpose buckets to directory
 *         buckets, from directory buckets to general purpose buckets, or between directory buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">
 *         UploadPartCopy</a>. In this case, Amazon S3 makes a call to KMS every time a copy request is made for
 *         a KMS-encrypted object.
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
 *         </ul>
 *       </li>
 *       <li>
 *         Error Code: <code>InvalidRequest</code>
 *
 *         <ul>
 *           <li>
 *             Description: The specified copy source is not supported as a byte-range copy source.
 *           </li>
 *           <li>
 *             HTTP Status Code: 400 Bad Request
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
 * <p>The following operations are related to <code>UploadPartCopy</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart</a>
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
 * <h3>To upload a part by copying byte range from an existing object as data source</h3>
 *
 * <p>The following example uploads a part of a multipart upload by copying a specified byte range from an existing object as data source.{@snippet :
 * var input = UploadPartCopyInput.builder()
 *                 .bucket("examplebucket").copySource("/bucketname/sourceobjectkey").copySourceRange("bytes=1-100000").key("examplelargeobject").partNumber(2).uploadId("exampleuoh_10OhKhT7YukE9bjzTPRiuaCotmZM_pFngJFir9OZNrSr5cWa3cq3LZSUsfjI4FI7PkP91We7Nrw--")
 *                 .build();
 *
 * var result = client.uploadPartCopy(input);
 * result.equals(UploadPartCopyOutput.builder()
 *                   .copyPartResult(CopyPartResult.builder()
 *                                       .lastModified(Instant.parse("2016-12-29T21:44:28Z")).eTag("\"65d16d19e65a7508a51f043180edcc36\"")
 *                                       .build())
 *                   .build());
 * }
 *
 * <h3>To upload a part by copying data from an existing object as data source</h3>
 *
 * <p>The following example uploads a part of a multipart upload by copying data from an existing object as data source.{@snippet :
 * var input = UploadPartCopyInput.builder()
 *                 .bucket("examplebucket").copySource("/bucketname/sourceobjectkey").key("examplelargeobject").partNumber(1).uploadId("exampleuoh_10OhKhT7YukE9bjzTPRiuaCotmZM_pFngJFir9OZNrSr5cWa3cq3LZSUsfjI4FI7PkP91We7Nrw--")
 *                 .build();
 *
 * var result = client.uploadPartCopy(input);
 * result.equals(UploadPartCopyOutput.builder()
 *                   .copyPartResult(CopyPartResult.builder()
 *                                       .lastModified(Instant.parse("2016-12-29T21:24:43Z")).eTag("\"b0c6f0e7e054ab8fa2536a2677f8734d\"")
 *                                       .build())
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class UploadPartCopy implements ApiOperation<UploadPartCopyInput, UploadPartCopyOutput> {

    private static final UploadPartCopy $INSTANCE = new UploadPartCopy();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#UploadPartCopy"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("DisableS3ExpressSessionAuth", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?x-id=UploadPartCopy")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UploadPartCopy instance() {
        return $INSTANCE;
    }

    private UploadPartCopy() {}

    @Override
    public ShapeBuilder<UploadPartCopyInput> inputBuilder() {
        return UploadPartCopyInput.builder();
    }

    @Override
    public ShapeBuilder<UploadPartCopyOutput> outputBuilder() {
        return UploadPartCopyOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UploadPartCopyInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UploadPartCopyOutput.$SCHEMA;
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
