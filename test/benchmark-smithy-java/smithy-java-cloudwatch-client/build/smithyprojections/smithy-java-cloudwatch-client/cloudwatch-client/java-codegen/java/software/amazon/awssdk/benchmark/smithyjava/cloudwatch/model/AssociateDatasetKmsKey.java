package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Associates an Amazon Web Services Key Management Service (Amazon Web Services KMS) customer managed key with the
 * specified dataset. After this operation completes, all data published to the dataset is encrypted at rest using the
 * specified KMS key. Callers must have <code>kms:Decrypt</code> permission on the key to read the encrypted data.
 *
 * <p>Only the <code>default</code> dataset is supported. The <code>default</code> dataset is implicit for every account
 * in every Region — you do not need to create it before calling this operation.
 *
 * <p>You can call <code>AssociateDatasetKmsKey</code> on a dataset that is already associated with a KMS key to replace
 * the existing key with a different one. The caller must have <code>kms:Decrypt</code> permission on both the current
 * key and the new key.
 *
 * <p>If the currently associated key has been deleted, is scheduled for deletion, is pending import, is unavailable, or
 * has been disabled, Amazon CloudWatch does not require <code>kms:Decrypt</code> permission on the current key and the
 * rotation proceeds. If the key was only disabled, consider re-enabling it instead of rotating, because re-enabling
 * allows Amazon CloudWatch to resume decrypting your existing metric data encrypted with that key.
 *
 * <p>The KMS key that you specify must meet all of the following requirements:
 *
 * <ul>
 *   <li>
 *     It must be a symmetric encryption KMS key (key spec <code>SYMMETRIC_DEFAULT</code>, key usage <code>
 *     ENCRYPT_DECRYPT</code>). Asymmetric keys, HMAC keys, and key material types other than <code>
 *     SYMMETRIC_DEFAULT</code> are not supported.
 *   </li>
 *   <li>
 *     It must be enabled and not pending deletion.
 *   </li>
 *   <li>
 *     Its key policy must grant the CloudWatch service principal (<code>cloudwatch.amazonaws.com</code>) these
 *     permissions: <code>kms:DescribeKey</code>, <code>kms:GenerateDataKey</code>, <code>kms:Encrypt</code>, <code>
 *     kms:Decrypt</code>, and <code>kms:ReEncrypt&#42;</code>. Amazon CloudWatch requires these permissions to manage
 *     the data on your behalf.
 *   </li>
 *   <li>
 *     The calling principal must have <code>kms:Decrypt</code> permission on the key.
 *   </li>
 *   <li>
 *     It must be specified as a fully qualified key ARN. Key IDs, aliases, and alias ARNs are not accepted.
 *   </li>
 *   <li>
 *     It must be in the same Amazon Web Services Region as the dataset.
 *   </li>
 * </ul>
 *
 * <p>Before completing the association, Amazon CloudWatch validates the key by performing a series of dry-run KMS
 * operations. Service-principal checks run first to verify that the key policy grants the required access to Amazon
 * CloudWatch. These checks include <code>kms:DescribeKey</code>, <code>kms:GenerateDataKey</code>, <code>kms:Encrypt</code>
 * , <code>kms:Decrypt</code>, and <code>kms:ReEncrypt&#42;</code>. After those succeed, a <code>kms:Decrypt</code> dry-run
 * is run with the caller's credentials to verify that the calling principal can use the new key. When you are replacing
 * an existing key, the caller's <code>kms:Decrypt</code> dry-run is also run on the current key.
 *
 * <p>If any of these checks on the new key fails, the operation fails and the existing key association (if any) remains
 * unchanged. Common failure causes include the new key being disabled, the key policy not granting the required
 * permissions to Amazon CloudWatch, or the caller lacking <code>kms:Decrypt</code> permission on the new key.
 *
 * <p>For more information about using customer managed keys with Amazon CloudWatch, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cmk-encryption.html">Encryption at rest with
 * customer managed keys</a> in the <i>Amazon CloudWatch User Guide</i>.
 */
@SmithyGenerated
public final class AssociateDatasetKmsKey implements ApiOperation<AssociateDatasetKmsKeyInput, AssociateDatasetKmsKeyOutput> {

    private static final AssociateDatasetKmsKey $INSTANCE = new AssociateDatasetKmsKey();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#AssociateDatasetKmsKey"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConflictException.$ID, ConflictException.class, ConflictException::builder)
        .putType(KmsAccessDeniedException.$ID, KmsAccessDeniedException.class, KmsAccessDeniedException::builder)
        .putType(KmsKeyDisabledException.$ID, KmsKeyDisabledException.class, KmsKeyDisabledException::builder)
        .putType(KmsKeyNotFoundException.$ID, KmsKeyNotFoundException.class, KmsKeyNotFoundException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static AssociateDatasetKmsKey instance() {
        return $INSTANCE;
    }

    private AssociateDatasetKmsKey() {}

    @Override
    public ShapeBuilder<AssociateDatasetKmsKeyInput> inputBuilder() {
        return AssociateDatasetKmsKeyInput.builder();
    }

    @Override
    public ShapeBuilder<AssociateDatasetKmsKeyOutput> outputBuilder() {
        return AssociateDatasetKmsKeyOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return AssociateDatasetKmsKeyInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return AssociateDatasetKmsKeyOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConflictException.$SCHEMA, KmsAccessDeniedException.$SCHEMA, KmsKeyDisabledException.$SCHEMA, KmsKeyNotFoundException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
        return CloudWatchApiService.instance();
    }
    }
