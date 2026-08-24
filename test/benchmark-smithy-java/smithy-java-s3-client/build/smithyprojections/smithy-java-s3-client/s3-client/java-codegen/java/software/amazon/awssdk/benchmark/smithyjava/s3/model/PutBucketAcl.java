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
 * End of support notice: As of October 1, 2025, Amazon S3 has discontinued support for Email Grantee Access Control
 * Lists (ACLs). If you attempt to use an Email Grantee ACL in a request after October 1, 2025, the request will receive
 * an <code>HTTP 405</code> (Method Not Allowed) error.
 *
 * <p>This change affects the following Amazon Web Services Regions: US East (N. Virginia), US West (N. California), US
 * West (Oregon), Asia Pacific (Singapore), Asia Pacific (Sydney), Asia Pacific (Tokyo), Europe (Ireland), and South
 * America (São Paulo).
 *
 * <p>This operation is not supported for directory buckets.
 *
 * <p>Sets the permissions on an existing bucket using access control lists (ACL). For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/S3_ACLs_UsingACLs.html">Using
 * ACLs</a>. To set the ACL of a bucket, you must have the <code>WRITE_ACP</code> permission.
 *
 * <p>You can use one of the following two ways to set a bucket's permissions:
 *
 * <ul>
 *   <li>
 *     Specify the ACL in the request body
 *   </li>
 *   <li>
 *     Specify permissions using request headers
 *   </li>
 * </ul>
 *
 * <p>You cannot specify access permission using both the body and the request headers.
 *
 * <p>Depending on your application needs, you may choose to set the ACL on a bucket using either the request body or
 * the headers. For example, if you have an existing application that updates a bucket ACL using the request body, then
 * you can continue to use that approach.
 *
 * <p>If your bucket uses the bucket owner enforced setting for S3 Object Ownership, ACLs are disabled and no longer
 * affect permissions. You must use policies to grant access to your bucket and the objects in it. Requests to set ACLs
 * or update ACLs fail and return the <code>AccessControlListNotSupported</code> error code. Requests to read ACLs are
 * still supported. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html">Controlling object ownership</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p>You can set access permissions by using one of the following methods:
 *
 *     <ul>
 *       <li>
 *         Specify a canned ACL with the <code>x-amz-acl</code> request header. Amazon S3 supports a set of
 *         predefined ACLs, known as <i>canned ACLs</i>. Each canned ACL has a predefined set of grantees and
 *         permissions. Specify the canned ACL name as the value of <code>x-amz-acl</code>. If you use this
 *         header, you cannot use other access control-specific headers in your request. For more information,
 *         see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#CannedACL">Canned ACL</a>.
 *       </li>
 *       <li>
 *         Specify access permissions explicitly with the <code>x-amz-grant-read</code>, <code>
 *         x-amz-grant-read-acp</code>, <code>x-amz-grant-write-acp</code>, and <code>x-amz-grant-full-control</code>
 *         headers. When using these headers, you specify explicit access permissions and grantees (Amazon Web
 *         Services accounts or Amazon S3 groups) who will receive the permission. If you use these ACL-specific
 *         headers, you cannot use the <code>x-amz-acl</code> header to set a canned ACL. These parameters map
 *         to the set of permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
 *         Control List (ACL) Overview</a>.You specify each grantee as a type=value pair, where the type is one
 *         of the following:
 *
 *         <ul>
 *           <li>
 *             <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services
 *             account
 *           </li>
 *           <li>
 *             <code>uri</code> – if you are granting permissions to a predefined group
 *           </li>
 *           <li>
 *             <code>emailAddress</code> – if the value specified is the email address of an Amazon Web
 *             Services accountUsing email addresses to specify a grantee is only supported in the following
 *             Amazon Web Services Regions:
 *
 *             <ul>
 *               <li>
 *                 US East (N. Virginia)
 *               </li>
 *               <li>
 *                 US West (N. California)
 *               </li>
 *               <li>
 *                  US West (Oregon)
 *               </li>
 *               <li>
 *                  Asia Pacific (Singapore)
 *               </li>
 *               <li>
 *                 Asia Pacific (Sydney)
 *               </li>
 *               <li>
 *                 Asia Pacific (Tokyo)
 *               </li>
 *               <li>
 *                 Europe (Ireland)
 *               </li>
 *               <li>
 *                 South America (São Paulo)
 *               </li>
 *             </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and
 *             Endpoints</a> in the Amazon Web Services General Reference.
 *           </li>
 *         </ul>For example, the following <code>x-amz-grant-write</code> header grants create, overwrite, and
 *         delete objects permission to LogDelivery group predefined by Amazon S3 and two Amazon Web Services
 *         accounts identified by their email addresses.<code>x-amz-grant-write:
 *         uri="http://acs.amazonaws.com/groups/s3/LogDelivery", id="111122223333", id="555566667777" </code>
 *       </li>
 *     </ul>
 *
 *     <p>You can use either a canned ACL or specify access permissions explicitly. You cannot do both.
 *   </dd>
 *   <dt>
 *     Grantee Values
 *   </dt>
 *   <dd>
 *
 *     <p>You can specify the person (grantee) to whom you're assigning access rights (using request elements) in
 *     the following ways. For examples of how to specify these grantee values in JSON format, see the Amazon Web
 *     Services CLI example in <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/enable-server-access-logging.html"> Enabling Amazon S3 server access logging</a> in the <i>Amazon S3 User Guide</i>.
 *
 *     <ul>
 *       <li>
 *         By the person's ID:<code>&lt;Grantee xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *         xsi:type="CanonicalUser"&gt;&lt;ID&gt;&lt;&gt;ID&lt;&gt;
 *         &lt;DisplayName&gt;&lt;&gt;GranteesEmail&lt;&gt;</code>DisplayName is optional and ignored in the
 *         request
 *       </li>
 *       <li>
 *         By URI:<code>&lt;Grantee xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *         xsi:type="Group"&gt;&lt;URI&gt;&lt;&gt;http://acs.amazonaws.com/groups/global/AuthenticatedUsers&lt;&gt;</code>
 *       </li>
 *       <li>
 *         By Email address:<code>&lt;Grantee xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *         xsi:type="AmazonCustomerByEmail"&gt;&lt;EmailAddress&gt;&lt;&gt;Grantees{@literal @}email.com&lt;&gt;
 *         &amp;</code>The grantee is resolved to the CanonicalUser and, in a response to a GET Object acl
 *         request, appears as the CanonicalUser. Using email addresses to specify a grantee is only supported
 *         in the following Amazon Web Services Regions:
 *
 *         <ul>
 *           <li>
 *             US East (N. Virginia)
 *           </li>
 *           <li>
 *             US West (N. California)
 *           </li>
 *           <li>
 *              US West (Oregon)
 *           </li>
 *           <li>
 *              Asia Pacific (Singapore)
 *           </li>
 *           <li>
 *             Asia Pacific (Sydney)
 *           </li>
 *           <li>
 *             Asia Pacific (Tokyo)
 *           </li>
 *           <li>
 *             Europe (Ireland)
 *           </li>
 *           <li>
 *             South America (São Paulo)
 *           </li>
 *         </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a>
 *         in the Amazon Web Services General Reference.
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>PutBucketAcl</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html">DeleteBucket</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAcl.html">GetObjectAcl</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>Put bucket acl</h3>
 *
 * <p>The following example replaces existing ACL on a bucket. The ACL grants the bucket owner (specified using the owner ID) and write permission to the LogDelivery group. Because this is a replace operation, you must specify all the grants in your request. To incrementally add or remove ACL grants, you might use the console.{@snippet :
 * var input = PutBucketAclInput.builder()
 *                 .bucket("examplebucket").grantFullControl("id=examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484").grantWrite("uri=http://acs.amazonaws.com/groups/s3/LogDelivery")
 *                 .build();
 *
 * var result = client.putBucketAcl(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketAcl implements ApiOperation<PutBucketAclInput, PutBucketAclOutput> {

    private static final PutBucketAcl $INSTANCE = new PutBucketAcl();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketAcl"),
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
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?acl")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketAcl instance() {
        return $INSTANCE;
    }

    private PutBucketAcl() {}

    @Override
    public ShapeBuilder<PutBucketAclInput> inputBuilder() {
        return PutBucketAclInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketAclOutput> outputBuilder() {
        return PutBucketAclOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketAclInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketAclOutput.$SCHEMA;
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
