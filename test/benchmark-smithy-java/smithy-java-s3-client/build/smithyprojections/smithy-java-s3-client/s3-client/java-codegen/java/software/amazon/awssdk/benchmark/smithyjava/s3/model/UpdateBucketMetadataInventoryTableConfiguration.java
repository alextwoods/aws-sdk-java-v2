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
 * Enables or disables a live inventory table for an S3 Metadata configuration on a general purpose bucket. For more
 * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/metadata-tables-overview.html">Accelerating data discovery with S3 Metadata</a> in the <i>Amazon S3 User Guide</i>.
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
 *     <p>If you want to encrypt your inventory table with server-side encryption with Key Management Service (KMS)
 *     keys (SSE-KMS), you need additional permissions in your KMS key policy. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/metadata-tables-permissions.html"> Setting
 *     up permissions for configuring metadata tables</a> in the <i>Amazon S3 User Guide</i>.
 *
 *     <ul>
 *       <li>
 *         <code>s3:UpdateBucketMetadataInventoryTableConfiguration</code>
 *       </li>
 *       <li>
 *         <code>s3tables:CreateTableBucket</code>
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
 *       <li>
 *         <code>s3tables:PutTableEncryption</code>
 *       </li>
 *       <li>
 *         <code>kms:DescribeKey</code>
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>UpdateBucketMetadataInventoryTableConfiguration</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucketMetadataConfiguration.html">CreateBucketMetadataConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketMetadataConfiguration.html">DeleteBucketMetadataConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketMetadataConfiguration.html">GetBucketMetadataConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UpdateBucketMetadataJournalTableConfiguration.html">UpdateBucketMetadataJournalTableConfiguration</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class UpdateBucketMetadataInventoryTableConfiguration implements ApiOperation<UpdateBucketMetadataInventoryTableConfigurationInput, UpdateBucketMetadataInventoryTableConfigurationOutput> {

    private static final UpdateBucketMetadataInventoryTableConfiguration $INSTANCE = new UpdateBucketMetadataInventoryTableConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#UpdateBucketMetadataInventoryTableConfiguration"),
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
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?metadataInventoryTable")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UpdateBucketMetadataInventoryTableConfiguration instance() {
        return $INSTANCE;
    }

    private UpdateBucketMetadataInventoryTableConfiguration() {}

    @Override
    public ShapeBuilder<UpdateBucketMetadataInventoryTableConfigurationInput> inputBuilder() {
        return UpdateBucketMetadataInventoryTableConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateBucketMetadataInventoryTableConfigurationOutput> outputBuilder() {
        return UpdateBucketMetadataInventoryTableConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateBucketMetadataInventoryTableConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateBucketMetadataInventoryTableConfigurationOutput.$SCHEMA;
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
