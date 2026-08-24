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
 * Completes a multipart upload by assembling previously uploaded parts.
 *
 * <p>You first initiate the multipart upload and then upload all parts using the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart</a> operation or the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">
 * UploadPartCopy</a> operation. After successfully uploading all relevant parts of an upload, you call this <code>
 * CompleteMultipartUpload</code> operation to complete the upload. Upon receiving this request, Amazon S3 concatenates
 * all the parts in ascending order by part number to create a new object. In the CompleteMultipartUpload request, you
 * must provide the parts list and ensure that the parts list is complete. The CompleteMultipartUpload API operation
 * concatenates the parts that you provide in the list. For each part in the list, you must provide the <code>PartNumber</code>
 * value and the <code>ETag</code> value that are returned after that part was uploaded.
 *
 * <p>The processing of a CompleteMultipartUpload request could take several minutes to finalize. After Amazon S3 begins
 * processing the request, it sends an HTTP response header that specifies a <code>200 OK</code> response. While
 * processing is in progress, Amazon S3 periodically sends white space characters to keep the connection from timing
 * out. A request could fail after the initial <code>200 OK</code> response has been sent. This means that a <code>200
 * OK</code> response can contain either a success or an error. The error response might be embedded in the <code>200 OK</code>
 * response. If you call this API operation directly, make sure to design your application to parse the contents of the
 * response and handle it appropriately. If you use Amazon Web Services SDKs, SDKs handle this condition. The SDKs
 * detect the embedded error and apply error handling per your configuration settings (including automatically retrying
 * the request as appropriate). If the condition persists, the SDKs throw an exception (or, for the SDKs that don't use
 * exceptions, they return an error).
 *
 * <p>Note that if <code>CompleteMultipartUpload</code> fails, applications should be prepared to retry any failed
 * requests (including 500 error responses). For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ErrorBestPractices.html">Amazon S3 Error Best Practices</a>.
 *
 * <p>You can't use <code>Content-Type: application/x-www-form-urlencoded</code> for the CompleteMultipartUpload
 * requests. Also, if you don't provide a <code>Content-Type</code> header, <code>CompleteMultipartUpload</code> can
 * still return a <code>200 OK</code> response.
 *
 * <p>For more information about multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/uploadobjusingmpu.html">Uploading Objects Using Multipart Upload</a> in the <i>Amazon
 * S3 User Guide</i>.
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
 *         <b>General purpose bucket permissions</b> - For information about permissions required to use the
 *         multipart upload API, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuAndPermissions.html">Multipart Upload and Permissions</a> in the <i>Amazon S3 User Guide</i>.
 *         If you provide an <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_Checksum.html">additional checksum value</a> in your <code>MultipartUpload</code> requests and
 *         the object is encrypted with Key Management Service, you must have permission to use the <code>
 *         kms:Decrypt</code> action for the <code>CompleteMultipartUpload</code> request to succeed.
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
 *     Special errors
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         Error Code: <code>EntityTooSmall</code>
 *
 *         <ul>
 *           <li>
 *             Description: Your proposed upload is smaller than the minimum allowed object size. Each part
 *             must be at least 5 MB in size, except the last part.
 *           </li>
 *           <li>
 *             HTTP Status Code: 400 Bad Request
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         Error Code: <code>InvalidPart</code>
 *
 *         <ul>
 *           <li>
 *             Description: One or more of the specified parts could not be found. The part might not have
 *             been uploaded, or the specified ETag might not have matched the uploaded part's ETag.
 *           </li>
 *           <li>
 *             HTTP Status Code: 400 Bad Request
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         Error Code: <code>InvalidPartOrder</code>
 *
 *         <ul>
 *           <li>
 *             Description: The list of parts was not in ascending order. The parts list must be specified
 *             in order by part number.
 *           </li>
 *           <li>
 *             HTTP Status Code: 400 Bad Request
 *           </li>
 *         </ul>
 *       </li>
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
 * <p>The following operations are related to <code>CompleteMultipartUpload</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart</a>
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
 * <h3>To complete multipart upload</h3>
 *
 * <p>The following example completes a multipart upload.{@snippet :
 * var input = CompleteMultipartUploadInput.builder()
 *                 .bucket("examplebucket").key("bigobject").multipartUpload(CompletedMultipartUpload.builder()
 *                                      .parts(List.of(
 *                                                 CompletedPart.builder()
 *                                                     .partNumber(1).eTag("\"d8c2eafd90c266e19ab9dcacc479f8af\"")
 *                                                     .build()
 *                                                 ,
 *                                                 CompletedPart.builder()
 *                                                     .partNumber(2).eTag("\"d8c2eafd90c266e19ab9dcacc479f8af\"")
 *                                                     .build()
 *                                             ))
 *                                      .build()).uploadId("7YPBOJuoFiQ9cz4P3Pe6FIZwO4f7wN93uHsNBEw97pl5eNwzExg0LAT2dUN91cOmrEQHDsP3WA60CEg--")
 *                 .build();
 *
 * var result = client.completeMultipartUpload(input);
 * result.equals(CompleteMultipartUploadOutput.builder()
 *                   .eTag("\"4d9031c7644d8081c2829f4ea23c55f7-2\"").bucket("acexamplebucket").location("https://examplebucket.s3.<Region>.amazonaws.com/bigobject").key("bigobject")
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class CompleteMultipartUpload implements ApiOperation<CompleteMultipartUploadInput, CompleteMultipartUploadOutput> {

    private static final CompleteMultipartUpload $INSTANCE = new CompleteMultipartUpload();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#CompleteMultipartUpload"),
            HttpTrait.builder().method("POST").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static CompleteMultipartUpload instance() {
        return $INSTANCE;
    }

    private CompleteMultipartUpload() {}

    @Override
    public ShapeBuilder<CompleteMultipartUploadInput> inputBuilder() {
        return CompleteMultipartUploadInput.builder();
    }

    @Override
    public ShapeBuilder<CompleteMultipartUploadOutput> outputBuilder() {
        return CompleteMultipartUploadOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return CompleteMultipartUploadInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return CompleteMultipartUploadOutput.$SCHEMA;
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
