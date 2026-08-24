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
 * Creates a copy of an object that is already stored in Amazon S3.
 *
 * <p>End of support notice: As of October 1, 2025, Amazon S3 has discontinued support for Email Grantee Access Control
 * Lists (ACLs). If you attempt to use an Email Grantee ACL in a request after October 1, 2025, the request will receive
 * an <code>HTTP 405</code> (Method Not Allowed) error.
 *
 * <p>This change affects the following Amazon Web Services Regions: US East (N. Virginia), US West (N. California), US
 * West (Oregon), Asia Pacific (Singapore), Asia Pacific (Sydney), Asia Pacific (Tokyo), Europe (Ireland), and South
 * America (São Paulo).
 *
 * <p>You can store individual objects of up to 50 TB in Amazon S3. You create a copy of your object up to 5 GB in size
 * in a single atomic action using this API. However, to copy an object greater than 5 GB, you must use the multipart
 * upload Upload Part - Copy (UploadPartCopy) API. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/CopyingObjctsUsingRESTMPUapi.html">Copy Object Using the REST Multipart
 * Upload API</a>.
 *
 * <p>You can copy individual objects between general purpose buckets, between directory buckets, and between general
 * purpose buckets and directory buckets.
 *
 * <ul>
 *   <li>
 *     Amazon S3 supports copy operations using Multi-Region Access Points only as a destination when using the
 *     Multi-Region Access Point ARN.
 *   </li>
 *   <li>
 *     <b>Directory buckets </b> - For directory buckets, you must make requests for this API operation to the Zonal
 *     endpoint. These endpoints support virtual-hosted-style requests in the format <code>https://<i>
 *     amzn-s3-demo-bucket</i>.s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com/<i>key-name</i></code>.
 *     Path-style requests are not supported. For more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">
 *     Regional and Zonal endpoints for directory buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>
 *     . For more information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a>
 *     in the <i>Amazon S3 User Guide</i>.
 *   </li>
 *   <li>
 *     VPC endpoints don't support cross-Region requests (including copies). If you're using VPC endpoints, your
 *     source and destination buckets should be in the same Amazon Web Services Region as your VPC endpoint.
 *   </li>
 * </ul>
 *
 * <p>Both the Region that you want to copy the object from and the Region that you want to copy the object to must be
 * enabled for your account. For more information about how to enable a Region for your account, see <a href="https://docs.aws.amazon.com/accounts/latest/reference/manage-acct-regions.html#manage-acct-regions-enable-standalone">Enable or
 * disable a Region for standalone accounts</a> in the <i>Amazon Web Services Account Management Guide</i>.
 *
 * <p>Amazon S3 transfer acceleration does not support cross-Region copies. If you request a cross-Region copy using a
 * transfer acceleration endpoint, you get a <code>400 Bad Request</code> error. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/transfer-acceleration.html">Transfer
 * Acceleration</a>.
 *
 * <dl>
 *   <dt>
 *     Authentication and authorization
 *   </dt>
 *   <dd>
 *
 *     <p>All <code>CopyObject</code> requests must be authenticated and signed by using IAM credentials (access key
 *     ID and secret access key for the IAM identities). All headers with the <code>x-amz-</code> prefix, including <code>
 *     x-amz-copy-source</code>, must be signed. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html">REST Authentication</a>.
 *
 *     <p><b>Directory buckets</b> - You must use the IAM credentials to authenticate and authorize your access to
 *     the <code>CopyObject</code> API operation, instead of using the temporary security credentials through the <code>
 *     CreateSession</code> API operation.
 *
 *     <p>Amazon Web Services CLI or SDKs handles authentication and authorization on your behalf.
 *   </dd>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p>You must have <i>read</i> access to the source object and <i>write</i> access to the destination bucket.
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - You must have permissions in an IAM policy based on the
 *         source and destination bucket types in a <code>CopyObject</code> operation.
 *
 *         <ul>
 *           <li>
 *             If the source object is in a general purpose bucket, you must have <b><code>s3:GetObject</code></b>
 *             permission to read the source object that is being copied.
 *           </li>
 *           <li>
 *             If the destination bucket is a general purpose bucket, you must have <b><code>s3:PutObject</code></b>
 *             permission to write the object copy to the destination bucket.
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - You must have permissions in a bucket policy or an IAM
 *         identity-based policy based on the source and destination bucket types in a <code>CopyObject</code>
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
 *             can't be set to <code>ReadOnly</code> on the copy destination bucket.
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
 *     Response and special errors
 *   </dt>
 *   <dd>
 *
 *     <p>When the request is an HTTP 1.1 request, the response is chunk encoded. When the request is not an HTTP
 *     1.1 request, the response would not contain the <code>Content-Length</code>. You always need to read the
 *     entire response body to check if the copy succeeds.
 *
 *     <ul>
 *       <li>
 *         If the copy is successful, you receive a response with information about the copied object.
 *       </li>
 *       <li>
 *         A copy request might return an error when Amazon S3 receives the copy request or while Amazon S3 is
 *         copying the files. A <code>200 OK</code> response can contain either a success or an error.
 *
 *         <ul>
 *           <li>
 *             If the error occurs before the copy action starts, you receive a standard Amazon S3 error.
 *           </li>
 *           <li>
 *             If the error occurs during the copy operation, the error response is embedded in the <code>
 *             200 OK</code> response. For example, in a cross-region copy, you may encounter throttling and
 *             receive a <code>200 OK</code> response. For more information, see <a href="https://repost.aws/knowledge-center/s3-resolve-200-internalerror">Resolve the Error 200
 *             response when copying objects to Amazon S3</a>. The <code>200 OK</code> status code means the
 *             copy was accepted, but it doesn't mean the copy is complete. Another example is when you
 *             disconnect from Amazon S3 before the copy is complete, Amazon S3 might cancel the copy and
 *             you may receive a <code>200 OK</code> response. You must stay connected to Amazon S3 until
 *             the entire response is successfully received and processed.If you call this API operation
 *             directly, make sure to design your application to parse the content of the response and
 *             handle it appropriately. If you use Amazon Web Services SDKs, SDKs handle this condition. The
 *             SDKs detect the embedded error and apply error handling per your configuration settings
 *             (including automatically retrying the request as appropriate). If the condition persists, the
 *             SDKs throw an exception (or, for the SDKs that don't use exceptions, they return an error).
 *           </li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Charge
 *   </dt>
 *   <dd>
 *
 *     <p>The copy request charge is based on the storage class and Region that you specify for the destination
 *     object. The request can also result in a data retrieval charge for the source if the source storage class
 *     bills for data retrieval. If the copy source is in a different region, the data transfer is billed to the
 *     copy source account. For pricing information, see <a href="http://aws.amazon.com/s3/pricing/">Amazon S3 pricing</a>.
 *   </dd>
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>Directory buckets </b> - The HTTP Host header syntax is <code><i>Bucket-name</i>.s3express-<i>
 *         zone-id</i>.<i>region-code</i>.amazonaws.com</code>.
 *       </li>
 *       <li>
 *         <b>Amazon S3 on Outposts</b> - When you use this action with S3 on Outposts through the REST API, you
 *         must direct requests to the S3 on Outposts hostname. The S3 on Outposts hostname takes the form <code><i>
 *         AccessPointName</i>-<i>AccountId</i>.<i>outpostID</i>.s3-outposts.<i>Region</i>.amazonaws.com</code>.
 *         The hostname isn't required when you use the Amazon Web Services CLI or SDKs.
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>CopyObject</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To copy an object</h3>
 *
 * <p>The following example copies an object from one bucket to another.{@snippet :
 * var input = CopyObjectInput.builder()
 *                 .bucket("destinationbucket").copySource("/sourcebucket/HappyFacejpg").key("HappyFaceCopyjpg")
 *                 .build();
 *
 * var result = client.copyObject(input);
 * result.equals(CopyObjectOutput.builder()
 *                   .copyObjectResult(CopyObjectResult.builder()
 *                                         .lastModified(Instant.parse("2016-12-15T17:38:53Z")).eTag("\"6805f2cfc46c0f04559748bb039d69ae\"")
 *                                         .build())
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class CopyObject implements ApiOperation<CopyObjectInput, CopyObjectOutput> {

    private static final CopyObject $INSTANCE = new CopyObject();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#CopyObject"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("DisableS3ExpressSessionAuth", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?x-id=CopyObject")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ObjectNotInActiveTierError.$ID, ObjectNotInActiveTierError.class, ObjectNotInActiveTierError::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static CopyObject instance() {
        return $INSTANCE;
    }

    private CopyObject() {}

    @Override
    public ShapeBuilder<CopyObjectInput> inputBuilder() {
        return CopyObjectInput.builder();
    }

    @Override
    public ShapeBuilder<CopyObjectOutput> outputBuilder() {
        return CopyObjectOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return CopyObjectInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return CopyObjectOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ObjectNotInActiveTierError.$SCHEMA);
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
