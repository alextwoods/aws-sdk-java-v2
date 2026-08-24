package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * For information about using the Amazon S3 API—including error handling—see the <a href="https://docs.aws.amazon.com/AmazonS3/latest/developerguide/Welcome.html">Amazon S3 Developer Guide</a>.
 *
 * <p>Container for all error elements.
 */
@SmithyGenerated
public final class Error implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.ERROR;
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_CODE = $SCHEMA.member("Code");
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("Message");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String key;
    private final transient String versionId;
    private final transient String code;
    private final transient String message;

    private Error(Builder builder) {
        this.key = builder.key;
        this.versionId = builder.versionId;
        this.code = builder.code;
        this.message = builder.message;
    }

    /**
     * The error key.
     */
    public String getKey() {
        return key;
    }

    /**
     * The version ID of the error.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * The error code is a string that uniquely identifies an error condition. It is meant to be read and understood by
     * programs that detect and handle errors by type. The following is a list of Amazon S3 error codes. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html">Error responses</a>.
     *
     * <ul>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> AccessDenied
     *       </li>
     *       <li>
     *         <i>Description:</i> Access Denied
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> AccountProblem
     *       </li>
     *       <li>
     *         <i>Description:</i> There is a problem with your Amazon Web Services account that prevents the
     *         action from completing successfully. Contact Amazon Web Services Support for further assistance.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> AllAccessDisabled
     *       </li>
     *       <li>
     *         <i>Description:</i> All access to this Amazon S3 resource has been disabled. Contact Amazon Web
     *         Services Support for further assistance.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> AmbiguousGrantByEmailAddress
     *       </li>
     *       <li>
     *         <i>Description:</i> The email address you provided is associated with more than one account.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> AuthorizationHeaderMalformed
     *       </li>
     *       <li>
     *         <i>Description:</i> The authorization header you provided is invalid.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> BadDigest
     *       </li>
     *       <li>
     *         <i>Description:</i> The Content-MD5 you specified did not match what we received.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> BucketAlreadyExists
     *       </li>
     *       <li>
     *         <i>Description:</i> The requested bucket name is not available. The bucket namespace is shared by
     *         all users of the system. Please select a different name and try again.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 409 Conflict
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> BucketAlreadyOwnedByYou
     *       </li>
     *       <li>
     *         <i>Description:</i> The bucket you tried to create already exists, and you own it. Amazon S3
     *         returns this error in all Amazon Web Services Regions except in the North Virginia Region. For
     *         legacy compatibility, if you re-create an existing bucket that you already own in the North
     *         Virginia Region, Amazon S3 returns 200 OK and resets the bucket access control lists (ACLs).
     *       </li>
     *       <li>
     *         <i>Code:</i> 409 Conflict (in all Regions except the North Virginia Region)
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> BucketNotEmpty
     *       </li>
     *       <li>
     *         <i>Description:</i> The bucket you tried to delete is not empty.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 409 Conflict
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> CredentialsNotSupported
     *       </li>
     *       <li>
     *         <i>Description:</i> This request does not support credentials.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> CrossLocationLoggingProhibited
     *       </li>
     *       <li>
     *         <i>Description:</i> Cross-location logging not allowed. Buckets in one geographic location cannot
     *         log information to a bucket in another location.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> EntityTooSmall
     *       </li>
     *       <li>
     *         <i>Description:</i> Your proposed upload is smaller than the minimum allowed object size.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> EntityTooLarge
     *       </li>
     *       <li>
     *         <i>Description:</i> Your proposed upload exceeds the maximum allowed object size.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> ExpiredToken
     *       </li>
     *       <li>
     *         <i>Description:</i> The provided token has expired.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> IllegalVersioningConfigurationException
     *       </li>
     *       <li>
     *         <i>Description:</i> Indicates that the versioning configuration specified in the request is
     *         invalid.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> IncompleteBody
     *       </li>
     *       <li>
     *         <i>Description:</i> You did not provide the number of bytes specified by the Content-Length HTTP
     *         header
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> IncorrectNumberOfFilesInPostRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> POST requires exactly one file upload per request.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InlineDataTooLarge
     *       </li>
     *       <li>
     *         <i>Description:</i> Inline data exceeds the maximum allowed size.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InternalError
     *       </li>
     *       <li>
     *         <i>Description:</i> We encountered an internal error. Please try again.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 500 Internal Server Error
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Server
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidAccessKeyId
     *       </li>
     *       <li>
     *         <i>Description:</i> The Amazon Web Services access key ID you provided does not exist in our
     *         records.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidAddressingHeader
     *       </li>
     *       <li>
     *         <i>Description:</i> You must specify the Anonymous role.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> N/A
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidArgument
     *       </li>
     *       <li>
     *         <i>Description:</i> Invalid Argument
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidBucketName
     *       </li>
     *       <li>
     *         <i>Description:</i> The specified bucket is not valid.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidBucketState
     *       </li>
     *       <li>
     *         <i>Description:</i> The request is not valid with the current state of the bucket.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 409 Conflict
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidDigest
     *       </li>
     *       <li>
     *         <i>Description:</i> The Content-MD5 you specified is not valid.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidEncryptionAlgorithmError
     *       </li>
     *       <li>
     *         <i>Description:</i> The encryption request you specified is not valid. The valid value is AES256.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidLocationConstraint
     *       </li>
     *       <li>
     *         <i>Description:</i> The specified location constraint is not valid. For more information about
     *         Regions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html#access-bucket-intro">How to Select a Region for Your Buckets</a>.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidObjectState
     *       </li>
     *       <li>
     *         <i>Description:</i> The action is not valid for the current state of the object.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidPart
     *       </li>
     *       <li>
     *         <i>Description:</i> One or more of the specified parts could not be found. The part might not
     *         have been uploaded, or the specified entity tag might not have matched the part's entity tag.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidPartOrder
     *       </li>
     *       <li>
     *         <i>Description:</i> The list of parts was not in ascending order. Parts list must be specified in
     *         order by part number.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidPayer
     *       </li>
     *       <li>
     *         <i>Description:</i> All access to this object has been disabled. Please contact Amazon Web
     *         Services Support for further assistance.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidPolicyDocument
     *       </li>
     *       <li>
     *         <i>Description:</i> The content of the form does not meet the conditions specified in the policy
     *         document.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRange
     *       </li>
     *       <li>
     *         <i>Description:</i> The requested range cannot be satisfied.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 416 Requested Range Not Satisfiable
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> Please use <code>AWS4-HMAC-SHA256</code>.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> SOAP requests must be made over an HTTPS connection.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> Amazon S3 Transfer Acceleration is not supported for buckets with non-DNS
     *         compliant names.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> Amazon S3 Transfer Acceleration is not supported for buckets with periods (.)
     *         in their names.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> Amazon S3 Transfer Accelerate endpoint only supports virtual style requests.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> Amazon S3 Transfer Accelerate is not configured on this bucket.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> Amazon S3 Transfer Accelerate is disabled on this bucket.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> Amazon S3 Transfer Acceleration is not supported on this bucket. Contact
     *         Amazon Web Services Support for more information.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> Amazon S3 Transfer Acceleration cannot be enabled on this bucket. Contact
     *         Amazon Web Services Support for more information.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>Code:</i> N/A
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidSecurity
     *       </li>
     *       <li>
     *         <i>Description:</i> The provided security credentials are not valid.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidSOAPRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> The SOAP request body is invalid.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidStorageClass
     *       </li>
     *       <li>
     *         <i>Description:</i> The storage class you specified is not valid.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidTargetBucketForLogging
     *       </li>
     *       <li>
     *         <i>Description:</i> The target bucket for logging does not exist, is not owned by you, or does
     *         not have the appropriate grants for the log-delivery group.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidToken
     *       </li>
     *       <li>
     *         <i>Description:</i> The provided token is malformed or otherwise invalid.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> InvalidURI
     *       </li>
     *       <li>
     *         <i>Description:</i> Couldn't parse the specified URI.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> KeyTooLongError
     *       </li>
     *       <li>
     *         <i>Description:</i> Your key is too long.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MalformedACLError
     *       </li>
     *       <li>
     *         <i>Description:</i> The XML you provided was not well-formed or did not validate against our
     *         published schema.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MalformedPOSTRequest
     *       </li>
     *       <li>
     *         <i>Description:</i> The body of your POST request is not well-formed multipart/form-data.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MalformedXML
     *       </li>
     *       <li>
     *         <i>Description:</i> This happens when the user sends malformed XML (XML that doesn't conform to
     *         the published XSD) for the configuration. The error message is, "The XML you provided was not
     *         well-formed or did not validate against our published schema."
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MaxMessageLengthExceeded
     *       </li>
     *       <li>
     *         <i>Description:</i> Your request was too big.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MaxPostPreDataLengthExceededError
     *       </li>
     *       <li>
     *         <i>Description:</i> Your POST request fields preceding the upload file were too large.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MetadataTooLarge
     *       </li>
     *       <li>
     *         <i>Description:</i> Your metadata headers exceed the maximum allowed metadata size.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MethodNotAllowed
     *       </li>
     *       <li>
     *         <i>Description:</i> The specified method is not allowed against this resource.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 405 Method Not Allowed
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MissingAttachment
     *       </li>
     *       <li>
     *         <i>Description:</i> A SOAP attachment was expected, but none were found.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> N/A
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MissingContentLength
     *       </li>
     *       <li>
     *         <i>Description:</i> You must provide the Content-Length HTTP header.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 411 Length Required
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MissingRequestBodyError
     *       </li>
     *       <li>
     *         <i>Description:</i> This happens when the user sends an empty XML document as a request. The
     *         error message is, "Request body is empty."
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MissingSecurityElement
     *       </li>
     *       <li>
     *         <i>Description:</i> The SOAP 1.1 request is missing a security element.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> MissingSecurityHeader
     *       </li>
     *       <li>
     *         <i>Description:</i> Your request is missing a required header.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NoLoggingStatusForKey
     *       </li>
     *       <li>
     *         <i>Description:</i> There is no such thing as a logging status subresource for a key.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NoSuchBucket
     *       </li>
     *       <li>
     *         <i>Description:</i> The specified bucket does not exist.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 404 Not Found
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NoSuchBucketPolicy
     *       </li>
     *       <li>
     *         <i>Description:</i> The specified bucket does not have a bucket policy.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 404 Not Found
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NoSuchKey
     *       </li>
     *       <li>
     *         <i>Description:</i> The specified key does not exist.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 404 Not Found
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NoSuchLifecycleConfiguration
     *       </li>
     *       <li>
     *         <i>Description:</i> The lifecycle configuration does not exist.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 404 Not Found
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NoSuchUpload
     *       </li>
     *       <li>
     *         <i>Description:</i> The specified multipart upload does not exist. The upload ID might be
     *         invalid, or the multipart upload might have been aborted or completed.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 404 Not Found
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NoSuchVersion
     *       </li>
     *       <li>
     *         <i>Description:</i> Indicates that the version ID specified in the request does not match an
     *         existing version.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 404 Not Found
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NotImplemented
     *       </li>
     *       <li>
     *         <i>Description:</i> A header you provided implies functionality that is not implemented.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 501 Not Implemented
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Server
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> NotSignedUp
     *       </li>
     *       <li>
     *         <i>Description:</i> Your account is not signed up for the Amazon S3 service. You must sign up
     *         before you can use Amazon S3. You can sign up at the following URL: <a href="http://aws.amazon.com/s3">Amazon S3</a>
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> OperationAborted
     *       </li>
     *       <li>
     *         <i>Description:</i> A conflicting conditional action is currently in progress against this
     *         resource. Try again.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 409 Conflict
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> PermanentRedirect
     *       </li>
     *       <li>
     *         <i>Description:</i> The bucket you are attempting to access must be addressed using the specified
     *         endpoint. Send all future requests to this endpoint.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 301 Moved Permanently
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> PreconditionFailed
     *       </li>
     *       <li>
     *         <i>Description:</i> At least one of the preconditions you specified did not hold.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 412 Precondition Failed
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> Redirect
     *       </li>
     *       <li>
     *         <i>Description:</i> Temporary redirect.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 307 Moved Temporarily
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> RestoreAlreadyInProgress
     *       </li>
     *       <li>
     *         <i>Description:</i> Object restore is already in progress.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 409 Conflict
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> RequestIsNotMultiPartContent
     *       </li>
     *       <li>
     *         <i>Description:</i> Bucket POST must be of the enclosure-type multipart/form-data.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> RequestTimeout
     *       </li>
     *       <li>
     *         <i>Description:</i> Your socket connection to the server was not read from or written to within
     *         the timeout period.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> RequestTimeTooSkewed
     *       </li>
     *       <li>
     *         <i>Description:</i> The difference between the request time and the server's time is too large.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> RequestTorrentOfBucketError
     *       </li>
     *       <li>
     *         <i>Description:</i> Requesting the torrent file of a bucket is not permitted.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> SignatureDoesNotMatch
     *       </li>
     *       <li>
     *         <i>Description:</i> The request signature we calculated does not match the signature you
     *         provided. Check your Amazon Web Services secret access key and signing method. For more
     *         information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html">REST Authentication</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/SOAPAuthentication.html">SOAP Authentication</a> for details.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 403 Forbidden
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> ServiceUnavailable
     *       </li>
     *       <li>
     *         <i>Description:</i> Service is unable to handle request.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 503 Service Unavailable
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Server
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> SlowDown
     *       </li>
     *       <li>
     *         <i>Description:</i> Reduce your request rate.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 503 Slow Down
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Server
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> TemporaryRedirect
     *       </li>
     *       <li>
     *         <i>Description:</i> You are being redirected to the bucket while DNS updates.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 307 Moved Temporarily
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> TokenRefreshRequired
     *       </li>
     *       <li>
     *         <i>Description:</i> The provided token must be refreshed.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> TooManyBuckets
     *       </li>
     *       <li>
     *         <i>Description:</i> You have attempted to create more buckets than allowed.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> UnexpectedContent
     *       </li>
     *       <li>
     *         <i>Description:</i> This request does not support content.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> UnresolvableGrantByEmailAddress
     *       </li>
     *       <li>
     *         <i>Description:</i> The email address you provided does not match any account on record.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *
     *     <ul>
     *       <li>
     *         <i>Code:</i> UserKeyMustBeSpecified
     *       </li>
     *       <li>
     *         <i>Description:</i> The bucket POST must contain the specified field name. If it is specified,
     *         check the order of the fields.
     *       </li>
     *       <li>
     *         <i>HTTP Status Code:</i> 400 Bad Request
     *       </li>
     *       <li>
     *         <i>SOAP Fault Code Prefix:</i> Client
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     */
    public String getCode() {
        return code;
    }

    /**
     * The error message contains a generic description of the error condition in English. It is intended for a human
     * audience. Simple programs display the message directly to the end user if they encounter an error condition they
     * don't know how or don't care to handle. Sophisticated programs with more exhaustive error handling and proper
     * internationalization are more likely to ignore the error message.
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Error that = (Error) other;
        return Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.code, that.code)
               && Objects.equals(this.message, that.message);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(code);
        $hc = 31 * $hc + Objects.hashCode(message);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (code != null) {
            serializer.writeString($SCHEMA_CODE, code);
        }
        if (message != null) {
            serializer.writeString($SCHEMA_MESSAGE, message);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CODE, member, code);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, message);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Error}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.key(this.key);
        builder.versionId(this.versionId);
        builder.code(this.code);
        builder.message(this.message);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Error}.
     */
    public static final class Builder implements ShapeBuilder<Error> {
        private String key;
        private String versionId;
        private String code;
        private String message;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The error key.
         *
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * The version ID of the error.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * The error code is a string that uniquely identifies an error condition. It is meant to be read and understood by
         * programs that detect and handle errors by type. The following is a list of Amazon S3 error codes. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html">Error responses</a>.
         *
         * <ul>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> AccessDenied
         *       </li>
         *       <li>
         *         <i>Description:</i> Access Denied
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> AccountProblem
         *       </li>
         *       <li>
         *         <i>Description:</i> There is a problem with your Amazon Web Services account that prevents the
         *         action from completing successfully. Contact Amazon Web Services Support for further assistance.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> AllAccessDisabled
         *       </li>
         *       <li>
         *         <i>Description:</i> All access to this Amazon S3 resource has been disabled. Contact Amazon Web
         *         Services Support for further assistance.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> AmbiguousGrantByEmailAddress
         *       </li>
         *       <li>
         *         <i>Description:</i> The email address you provided is associated with more than one account.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> AuthorizationHeaderMalformed
         *       </li>
         *       <li>
         *         <i>Description:</i> The authorization header you provided is invalid.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> BadDigest
         *       </li>
         *       <li>
         *         <i>Description:</i> The Content-MD5 you specified did not match what we received.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> BucketAlreadyExists
         *       </li>
         *       <li>
         *         <i>Description:</i> The requested bucket name is not available. The bucket namespace is shared by
         *         all users of the system. Please select a different name and try again.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 409 Conflict
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> BucketAlreadyOwnedByYou
         *       </li>
         *       <li>
         *         <i>Description:</i> The bucket you tried to create already exists, and you own it. Amazon S3
         *         returns this error in all Amazon Web Services Regions except in the North Virginia Region. For
         *         legacy compatibility, if you re-create an existing bucket that you already own in the North
         *         Virginia Region, Amazon S3 returns 200 OK and resets the bucket access control lists (ACLs).
         *       </li>
         *       <li>
         *         <i>Code:</i> 409 Conflict (in all Regions except the North Virginia Region)
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> BucketNotEmpty
         *       </li>
         *       <li>
         *         <i>Description:</i> The bucket you tried to delete is not empty.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 409 Conflict
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> CredentialsNotSupported
         *       </li>
         *       <li>
         *         <i>Description:</i> This request does not support credentials.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> CrossLocationLoggingProhibited
         *       </li>
         *       <li>
         *         <i>Description:</i> Cross-location logging not allowed. Buckets in one geographic location cannot
         *         log information to a bucket in another location.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> EntityTooSmall
         *       </li>
         *       <li>
         *         <i>Description:</i> Your proposed upload is smaller than the minimum allowed object size.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> EntityTooLarge
         *       </li>
         *       <li>
         *         <i>Description:</i> Your proposed upload exceeds the maximum allowed object size.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> ExpiredToken
         *       </li>
         *       <li>
         *         <i>Description:</i> The provided token has expired.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> IllegalVersioningConfigurationException
         *       </li>
         *       <li>
         *         <i>Description:</i> Indicates that the versioning configuration specified in the request is
         *         invalid.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> IncompleteBody
         *       </li>
         *       <li>
         *         <i>Description:</i> You did not provide the number of bytes specified by the Content-Length HTTP
         *         header
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> IncorrectNumberOfFilesInPostRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> POST requires exactly one file upload per request.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InlineDataTooLarge
         *       </li>
         *       <li>
         *         <i>Description:</i> Inline data exceeds the maximum allowed size.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InternalError
         *       </li>
         *       <li>
         *         <i>Description:</i> We encountered an internal error. Please try again.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 500 Internal Server Error
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Server
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidAccessKeyId
         *       </li>
         *       <li>
         *         <i>Description:</i> The Amazon Web Services access key ID you provided does not exist in our
         *         records.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidAddressingHeader
         *       </li>
         *       <li>
         *         <i>Description:</i> You must specify the Anonymous role.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> N/A
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidArgument
         *       </li>
         *       <li>
         *         <i>Description:</i> Invalid Argument
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidBucketName
         *       </li>
         *       <li>
         *         <i>Description:</i> The specified bucket is not valid.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidBucketState
         *       </li>
         *       <li>
         *         <i>Description:</i> The request is not valid with the current state of the bucket.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 409 Conflict
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidDigest
         *       </li>
         *       <li>
         *         <i>Description:</i> The Content-MD5 you specified is not valid.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidEncryptionAlgorithmError
         *       </li>
         *       <li>
         *         <i>Description:</i> The encryption request you specified is not valid. The valid value is AES256.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidLocationConstraint
         *       </li>
         *       <li>
         *         <i>Description:</i> The specified location constraint is not valid. For more information about
         *         Regions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html#access-bucket-intro">How to Select a Region for Your Buckets</a>.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidObjectState
         *       </li>
         *       <li>
         *         <i>Description:</i> The action is not valid for the current state of the object.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidPart
         *       </li>
         *       <li>
         *         <i>Description:</i> One or more of the specified parts could not be found. The part might not
         *         have been uploaded, or the specified entity tag might not have matched the part's entity tag.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidPartOrder
         *       </li>
         *       <li>
         *         <i>Description:</i> The list of parts was not in ascending order. Parts list must be specified in
         *         order by part number.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidPayer
         *       </li>
         *       <li>
         *         <i>Description:</i> All access to this object has been disabled. Please contact Amazon Web
         *         Services Support for further assistance.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidPolicyDocument
         *       </li>
         *       <li>
         *         <i>Description:</i> The content of the form does not meet the conditions specified in the policy
         *         document.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRange
         *       </li>
         *       <li>
         *         <i>Description:</i> The requested range cannot be satisfied.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 416 Requested Range Not Satisfiable
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> Please use <code>AWS4-HMAC-SHA256</code>.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> SOAP requests must be made over an HTTPS connection.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> Amazon S3 Transfer Acceleration is not supported for buckets with non-DNS
         *         compliant names.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> Amazon S3 Transfer Acceleration is not supported for buckets with periods (.)
         *         in their names.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> Amazon S3 Transfer Accelerate endpoint only supports virtual style requests.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> Amazon S3 Transfer Accelerate is not configured on this bucket.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> Amazon S3 Transfer Accelerate is disabled on this bucket.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> Amazon S3 Transfer Acceleration is not supported on this bucket. Contact
         *         Amazon Web Services Support for more information.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> Amazon S3 Transfer Acceleration cannot be enabled on this bucket. Contact
         *         Amazon Web Services Support for more information.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>Code:</i> N/A
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidSecurity
         *       </li>
         *       <li>
         *         <i>Description:</i> The provided security credentials are not valid.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidSOAPRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> The SOAP request body is invalid.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidStorageClass
         *       </li>
         *       <li>
         *         <i>Description:</i> The storage class you specified is not valid.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidTargetBucketForLogging
         *       </li>
         *       <li>
         *         <i>Description:</i> The target bucket for logging does not exist, is not owned by you, or does
         *         not have the appropriate grants for the log-delivery group.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidToken
         *       </li>
         *       <li>
         *         <i>Description:</i> The provided token is malformed or otherwise invalid.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> InvalidURI
         *       </li>
         *       <li>
         *         <i>Description:</i> Couldn't parse the specified URI.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> KeyTooLongError
         *       </li>
         *       <li>
         *         <i>Description:</i> Your key is too long.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MalformedACLError
         *       </li>
         *       <li>
         *         <i>Description:</i> The XML you provided was not well-formed or did not validate against our
         *         published schema.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MalformedPOSTRequest
         *       </li>
         *       <li>
         *         <i>Description:</i> The body of your POST request is not well-formed multipart/form-data.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MalformedXML
         *       </li>
         *       <li>
         *         <i>Description:</i> This happens when the user sends malformed XML (XML that doesn't conform to
         *         the published XSD) for the configuration. The error message is, "The XML you provided was not
         *         well-formed or did not validate against our published schema."
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MaxMessageLengthExceeded
         *       </li>
         *       <li>
         *         <i>Description:</i> Your request was too big.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MaxPostPreDataLengthExceededError
         *       </li>
         *       <li>
         *         <i>Description:</i> Your POST request fields preceding the upload file were too large.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MetadataTooLarge
         *       </li>
         *       <li>
         *         <i>Description:</i> Your metadata headers exceed the maximum allowed metadata size.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MethodNotAllowed
         *       </li>
         *       <li>
         *         <i>Description:</i> The specified method is not allowed against this resource.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 405 Method Not Allowed
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MissingAttachment
         *       </li>
         *       <li>
         *         <i>Description:</i> A SOAP attachment was expected, but none were found.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> N/A
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MissingContentLength
         *       </li>
         *       <li>
         *         <i>Description:</i> You must provide the Content-Length HTTP header.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 411 Length Required
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MissingRequestBodyError
         *       </li>
         *       <li>
         *         <i>Description:</i> This happens when the user sends an empty XML document as a request. The
         *         error message is, "Request body is empty."
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MissingSecurityElement
         *       </li>
         *       <li>
         *         <i>Description:</i> The SOAP 1.1 request is missing a security element.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> MissingSecurityHeader
         *       </li>
         *       <li>
         *         <i>Description:</i> Your request is missing a required header.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NoLoggingStatusForKey
         *       </li>
         *       <li>
         *         <i>Description:</i> There is no such thing as a logging status subresource for a key.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NoSuchBucket
         *       </li>
         *       <li>
         *         <i>Description:</i> The specified bucket does not exist.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 404 Not Found
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NoSuchBucketPolicy
         *       </li>
         *       <li>
         *         <i>Description:</i> The specified bucket does not have a bucket policy.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 404 Not Found
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NoSuchKey
         *       </li>
         *       <li>
         *         <i>Description:</i> The specified key does not exist.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 404 Not Found
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NoSuchLifecycleConfiguration
         *       </li>
         *       <li>
         *         <i>Description:</i> The lifecycle configuration does not exist.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 404 Not Found
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NoSuchUpload
         *       </li>
         *       <li>
         *         <i>Description:</i> The specified multipart upload does not exist. The upload ID might be
         *         invalid, or the multipart upload might have been aborted or completed.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 404 Not Found
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NoSuchVersion
         *       </li>
         *       <li>
         *         <i>Description:</i> Indicates that the version ID specified in the request does not match an
         *         existing version.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 404 Not Found
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NotImplemented
         *       </li>
         *       <li>
         *         <i>Description:</i> A header you provided implies functionality that is not implemented.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 501 Not Implemented
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Server
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> NotSignedUp
         *       </li>
         *       <li>
         *         <i>Description:</i> Your account is not signed up for the Amazon S3 service. You must sign up
         *         before you can use Amazon S3. You can sign up at the following URL: <a href="http://aws.amazon.com/s3">Amazon S3</a>
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> OperationAborted
         *       </li>
         *       <li>
         *         <i>Description:</i> A conflicting conditional action is currently in progress against this
         *         resource. Try again.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 409 Conflict
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> PermanentRedirect
         *       </li>
         *       <li>
         *         <i>Description:</i> The bucket you are attempting to access must be addressed using the specified
         *         endpoint. Send all future requests to this endpoint.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 301 Moved Permanently
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> PreconditionFailed
         *       </li>
         *       <li>
         *         <i>Description:</i> At least one of the preconditions you specified did not hold.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 412 Precondition Failed
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> Redirect
         *       </li>
         *       <li>
         *         <i>Description:</i> Temporary redirect.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 307 Moved Temporarily
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> RestoreAlreadyInProgress
         *       </li>
         *       <li>
         *         <i>Description:</i> Object restore is already in progress.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 409 Conflict
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> RequestIsNotMultiPartContent
         *       </li>
         *       <li>
         *         <i>Description:</i> Bucket POST must be of the enclosure-type multipart/form-data.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> RequestTimeout
         *       </li>
         *       <li>
         *         <i>Description:</i> Your socket connection to the server was not read from or written to within
         *         the timeout period.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> RequestTimeTooSkewed
         *       </li>
         *       <li>
         *         <i>Description:</i> The difference between the request time and the server's time is too large.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> RequestTorrentOfBucketError
         *       </li>
         *       <li>
         *         <i>Description:</i> Requesting the torrent file of a bucket is not permitted.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> SignatureDoesNotMatch
         *       </li>
         *       <li>
         *         <i>Description:</i> The request signature we calculated does not match the signature you
         *         provided. Check your Amazon Web Services secret access key and signing method. For more
         *         information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html">REST Authentication</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/SOAPAuthentication.html">SOAP Authentication</a> for details.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 403 Forbidden
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> ServiceUnavailable
         *       </li>
         *       <li>
         *         <i>Description:</i> Service is unable to handle request.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 503 Service Unavailable
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Server
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> SlowDown
         *       </li>
         *       <li>
         *         <i>Description:</i> Reduce your request rate.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 503 Slow Down
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Server
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> TemporaryRedirect
         *       </li>
         *       <li>
         *         <i>Description:</i> You are being redirected to the bucket while DNS updates.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 307 Moved Temporarily
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> TokenRefreshRequired
         *       </li>
         *       <li>
         *         <i>Description:</i> The provided token must be refreshed.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> TooManyBuckets
         *       </li>
         *       <li>
         *         <i>Description:</i> You have attempted to create more buckets than allowed.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> UnexpectedContent
         *       </li>
         *       <li>
         *         <i>Description:</i> This request does not support content.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> UnresolvableGrantByEmailAddress
         *       </li>
         *       <li>
         *         <i>Description:</i> The email address you provided does not match any account on record.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *
         *     <ul>
         *       <li>
         *         <i>Code:</i> UserKeyMustBeSpecified
         *       </li>
         *       <li>
         *         <i>Description:</i> The bucket POST must contain the specified field name. If it is specified,
         *         check the order of the fields.
         *       </li>
         *       <li>
         *         <i>HTTP Status Code:</i> 400 Bad Request
         *       </li>
         *       <li>
         *         <i>SOAP Fault Code Prefix:</i> Client
         *       </li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /**
         * The error message contains a generic description of the error condition in English. It is intended for a human
         * audience. Simple programs display the message directly to the end user if they encounter an error condition they
         * don't know how or don't care to handle. Sophisticated programs with more exhaustive error handling and proper
         * internationalization are more likely to ignore the error message.
         *
         * @return this builder.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Error build() {
            return new Error(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 1 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 2 -> code((String) SchemaUtils.validateSameMember($SCHEMA_CODE, member, value));
                case 3 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.key(de.readString(member));
                    case 1 -> builder.versionId(de.readString(member));
                    case 2 -> builder.code(de.readString(member));
                    case 3 -> builder.message(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
