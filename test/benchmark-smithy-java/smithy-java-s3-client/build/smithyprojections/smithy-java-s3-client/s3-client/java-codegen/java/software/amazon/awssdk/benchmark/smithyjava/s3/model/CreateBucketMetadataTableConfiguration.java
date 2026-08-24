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
 * We recommend that you create your S3 Metadata configurations by using the V2 <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucketMetadataConfiguration.html">CreateBucketMetadataConfiguration</a>
 * API operation. We no longer recommend using the V1 <code>CreateBucketMetadataTableConfiguration</code> API operation.
 *
 * <p>If you created your S3 Metadata configuration before July 15, 2025, we recommend that you delete and re-create
 * your configuration by using <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucketMetadataConfiguration.html">CreateBucketMetadataConfiguration</a> so that you can expire journal table records and
 * create a live inventory table.
 *
 * <p>Creates a V1 S3 Metadata configuration for a general purpose bucket. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/metadata-tables-overview.html">Accelerating
 * data discovery with S3 Metadata</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p>To use this operation, you must have the following permissions. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/metadata-tables-permissions.html">Setting up
 *     permissions for configuring metadata tables</a> in the <i>Amazon S3 User Guide</i>.
 *
 *     <p>If you want to encrypt your metadata tables with server-side encryption with Key Management Service (KMS)
 *     keys (SSE-KMS), you need additional permissions. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/metadata-tables-permissions.html"> Setting up permissions for
 *     configuring metadata tables</a> in the <i>Amazon S3 User Guide</i>.
 *
 *     <p>If you also want to integrate your table bucket with Amazon Web Services analytics services so that you
 *     can query your metadata table, you need additional permissions. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-tables-integrating-aws.html"> Integrating
 *     Amazon S3 Tables with Amazon Web Services analytics services</a> in the <i>Amazon S3 User Guide</i>.
 *
 *     <ul>
 *       <li>
 *         <code>s3:CreateBucketMetadataTableConfiguration</code>
 *       </li>
 *       <li>
 *         <code>s3tables:CreateNamespace</code>
 *       </li>
 *       <li>
 *         <code>s3tables:GetTable</code>
 *       </li>
 *       <li>
 *         <code>s3tables:CreateTable</code>
 *       </li>
 *       <li>
 *         <code>s3tables:PutTablePolicy</code>
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>CreateBucketMetadataTableConfiguration</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketMetadataTableConfiguration.html">DeleteBucketMetadataTableConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketMetadataTableConfiguration.html">GetBucketMetadataTableConfiguration</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class CreateBucketMetadataTableConfiguration implements ApiOperation<CreateBucketMetadataTableConfigurationInput, CreateBucketMetadataTableConfigurationOutput> {

    private static final CreateBucketMetadataTableConfiguration $INSTANCE = new CreateBucketMetadataTableConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#CreateBucketMetadataTableConfiguration"),
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
            HttpTrait.builder().method("POST").code(200).uri(UriPattern.parse("/{Bucket}?metadataTable")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static CreateBucketMetadataTableConfiguration instance() {
        return $INSTANCE;
    }

    private CreateBucketMetadataTableConfiguration() {}

    @Override
    public ShapeBuilder<CreateBucketMetadataTableConfigurationInput> inputBuilder() {
        return CreateBucketMetadataTableConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<CreateBucketMetadataTableConfigurationOutput> outputBuilder() {
        return CreateBucketMetadataTableConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return CreateBucketMetadataTableConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return CreateBucketMetadataTableConfigurationOutput.$SCHEMA;
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
