package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.client;

import java.io.IOException;
import java.util.List;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.AssociateDatasetKmsKeyInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.AssociateDatasetKmsKeyOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.CloudWatchApiService;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ConcurrentModificationException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ConflictException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DashboardInvalidInputError;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DashboardNotFoundError;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmMuteRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmMuteRuleOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAnomalyDetectorInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAnomalyDetectorOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteDashboardsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteDashboardsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteMetricStreamInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteMetricStreamOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmContributorsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmContributorsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmHistory;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmHistoryInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmHistoryOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarms;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmsForMetricInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmsForMetricOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAnomalyDetectors;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAnomalyDetectorsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAnomalyDetectorsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeInsightRules;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableAlarmActionsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableAlarmActionsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisassociateDatasetKmsKeyInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisassociateDatasetKmsKeyOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableAlarmActionsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableAlarmActionsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetAlarmMuteRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetAlarmMuteRuleOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDashboardInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDashboardOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDatasetInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDatasetOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetInsightRuleReportInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetInsightRuleReportOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricData;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricDataInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricDataOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStatisticsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStatisticsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStreamInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStreamOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricWidgetImageInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricWidgetImageOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetOTelEnrichmentInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetOTelEnrichmentOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.InternalServiceFault;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.InvalidFormatFault;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.InvalidNextToken;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.InvalidParameterCombinationException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.KmsAccessDeniedException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.KmsKeyDisabledException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.KmsKeyNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.LimitExceededException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.LimitExceededFault;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListAlarmMuteRules;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListAlarmMuteRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListAlarmMuteRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListDashboards;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListDashboardsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListDashboardsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListManagedInsightRules;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListManagedInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListManagedInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListMetricStreams;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListMetricStreamsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListMetricStreamsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListMetrics;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListMetricsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListMetricsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListTagsForResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListTagsForResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MissingRequiredParameterException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAlarmMuteRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAlarmMuteRuleOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAnomalyDetectorInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAnomalyDetectorOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutCompositeAlarmInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutCompositeAlarmOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutDashboardInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutDashboardOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutInsightRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutInsightRuleOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutLogAlarmInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutLogAlarmOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutManagedInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutManagedInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricAlarmInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricAlarmOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricDataInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricDataOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricStreamInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricStreamOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ResourceConflict;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ResourceNotFound;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ResourceNotFoundException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.SetAlarmStateInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.SetAlarmStateOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartMetricStreamsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartMetricStreamsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartOTelEnrichmentInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartOTelEnrichmentOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopMetricStreamsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopMetricStreamsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopOTelEnrichmentInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopOTelEnrichmentOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.TagResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.TagResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.UntagResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.UntagResourceOutput;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4AuthScheme;
import software.amazon.smithy.java.aws.client.awsquery.AwsQueryClientProtocol;
import software.amazon.smithy.java.aws.client.core.AwsCredentialChainPlugin;
import software.amazon.smithy.java.client.core.Client;
import software.amazon.smithy.java.client.core.ClientConfig;
import software.amazon.smithy.java.client.core.ClientPlugin;
import software.amazon.smithy.java.client.core.ProtocolSettings;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.client.core.auth.scheme.AuthSchemeFactory;
import software.amazon.smithy.java.client.core.pagination.Paginator;
import software.amazon.smithy.java.client.http.JavaHttpClientTransport;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.rulesengine.RulesEngineBuilder;
import software.amazon.smithy.java.rulesengine.RulesEngineSettings;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Amazon CloudWatch enables you to publish, monitor, and manage various metrics, as well as configure alarm actions
 * based on data from metrics. This guide provides detailed information about CloudWatch actions, data types,
 * parameters, and errors. For more information about CloudWatch features, see <a href="https://aws.amazon.com/cloudwatch">Amazon CloudWatch</a> and the <i>
 * Amazon CloudWatch User Guide</i>.
 *
 * <p>For information about the metrics that other Amazon Web Services products send to CloudWatch, see the <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/aws-services-cloudwatch-metrics.html">Amazon
 * CloudWatch Metrics and Dimensions Reference</a> in the <i>Amazon CloudWatch User Guide</i>.
 *
 * <p>Use the following links to get started using the CloudWatch Query API:
 *
 * <p>: An alphabetical list of all CloudWatch actions.
 *
 * <p>: An alphabetical list of all CloudWatch data types.
 *
 * <p><a>CommonParameters</a>: Parameters that all Query actions can use.
 *
 * <p><a>CommonErrors</a>: Client and server errors that all actions can return.
 *
 * <p><a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#cw_region">Regions and Endpoints</a>: Supported regions and endpoints for all Amazon Web Services products.
 *
 * <p>Alternatively, you can use one of the <a href="https://aws.amazon.com/tools/#sdk">Amazon Web Services SDKs</a> to access CloudWatch using an API tailored
 * to your programming language or platform.
 *
 * <p>Developers in the Amazon Web Services developer community also provide their own libraries, which you can find at
 * the following Amazon Web Services developer centers:
 *
 * <p><a href="http://aws.amazon.com/java/">Java Developer Center</a>
 *
 * <p><a href="http://aws.amazon.com/javascript/">JavaScript Developer Center</a>
 *
 * <p><a href="http://aws.amazon.com/mobile/">Amazon Web Services Mobile Services</a>
 *
 * <p><a href="http://aws.amazon.com/php/">PHP Developer Center</a>
 *
 * <p><a href="http://aws.amazon.com/python/">Python Developer Center</a>
 *
 * <p><a href="http://aws.amazon.com/ruby/">Ruby Developer Center</a>
 *
 * <p><a href="http://aws.amazon.com/net/">Windows and .NET Developer Center</a>
 */
@SmithyGenerated
public interface CloudWatchClient {

    /**
     * Associates an Amazon Web Services Key Management Service (Amazon Web Services KMS) customer managed key with the
     * specified dataset. After this operation completes, all data published to the dataset is encrypted at rest using
     * the specified KMS key. Callers must have <code>kms:Decrypt</code> permission on the key to read the encrypted
     * data.
     *
     * <p>Only the <code>default</code> dataset is supported. The <code>default</code> dataset is implicit for every
     * account in every Region — you do not need to create it before calling this operation.
     *
     * <p>You can call <code>AssociateDatasetKmsKey</code> on a dataset that is already associated with a KMS key to
     * replace the existing key with a different one. The caller must have <code>kms:Decrypt</code> permission on both
     * the current key and the new key.
     *
     * <p>If the currently associated key has been deleted, is scheduled for deletion, is pending import, is
     * unavailable, or has been disabled, Amazon CloudWatch does not require <code>kms:Decrypt</code> permission on the
     * current key and the rotation proceeds. If the key was only disabled, consider re-enabling it instead of rotating,
     * because re-enabling allows Amazon CloudWatch to resume decrypting your existing metric data encrypted with that
     * key.
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
     *     kms:Decrypt</code>, and <code>kms:ReEncrypt&#42;</code>. Amazon CloudWatch requires these permissions to
     *     manage the data on your behalf.
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
     * CloudWatch. These checks include <code>kms:DescribeKey</code>, <code>kms:GenerateDataKey</code>, <code>
     * kms:Encrypt</code>, <code>kms:Decrypt</code>, and <code>kms:ReEncrypt&#42;</code>. After those succeed, a <code>
     * kms:Decrypt</code> dry-run is run with the caller's credentials to verify that the calling principal can use the
     * new key. When you are replacing an existing key, the caller's <code>kms:Decrypt</code> dry-run is also run on the
     * current key.
     *
     * <p>If any of these checks on the new key fails, the operation fails and the existing key association (if any)
     * remains unchanged. Common failure causes include the new key being disabled, the key policy not granting the
     * required permissions to Amazon CloudWatch, or the caller lacking <code>kms:Decrypt</code> permission on the new
     * key.
     *
     * <p>For more information about using customer managed keys with Amazon CloudWatch, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cmk-encryption.html">Encryption at rest with
     * customer managed keys</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * @throws ConflictException
     * @throws KmsAccessDeniedException
     * @throws KmsKeyDisabledException
     * @throws KmsKeyNotFoundException
     * @throws ResourceNotFoundException
     */
    default AssociateDatasetKmsKeyOutput associateDatasetKmsKey(AssociateDatasetKmsKeyInput input) {
        return associateDatasetKmsKey(input, null);
    }

    /**
     * Associates an Amazon Web Services Key Management Service (Amazon Web Services KMS) customer managed key with the
     * specified dataset. After this operation completes, all data published to the dataset is encrypted at rest using
     * the specified KMS key. Callers must have <code>kms:Decrypt</code> permission on the key to read the encrypted
     * data.
     *
     * <p>Only the <code>default</code> dataset is supported. The <code>default</code> dataset is implicit for every
     * account in every Region — you do not need to create it before calling this operation.
     *
     * <p>You can call <code>AssociateDatasetKmsKey</code> on a dataset that is already associated with a KMS key to
     * replace the existing key with a different one. The caller must have <code>kms:Decrypt</code> permission on both
     * the current key and the new key.
     *
     * <p>If the currently associated key has been deleted, is scheduled for deletion, is pending import, is
     * unavailable, or has been disabled, Amazon CloudWatch does not require <code>kms:Decrypt</code> permission on the
     * current key and the rotation proceeds. If the key was only disabled, consider re-enabling it instead of rotating,
     * because re-enabling allows Amazon CloudWatch to resume decrypting your existing metric data encrypted with that
     * key.
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
     *     kms:Decrypt</code>, and <code>kms:ReEncrypt&#42;</code>. Amazon CloudWatch requires these permissions to
     *     manage the data on your behalf.
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
     * CloudWatch. These checks include <code>kms:DescribeKey</code>, <code>kms:GenerateDataKey</code>, <code>
     * kms:Encrypt</code>, <code>kms:Decrypt</code>, and <code>kms:ReEncrypt&#42;</code>. After those succeed, a <code>
     * kms:Decrypt</code> dry-run is run with the caller's credentials to verify that the calling principal can use the
     * new key. When you are replacing an existing key, the caller's <code>kms:Decrypt</code> dry-run is also run on the
     * current key.
     *
     * <p>If any of these checks on the new key fails, the operation fails and the existing key association (if any)
     * remains unchanged. Common failure causes include the new key being disabled, the key policy not granting the
     * required permissions to Amazon CloudWatch, or the caller lacking <code>kms:Decrypt</code> permission on the new
     * key.
     *
     * <p>For more information about using customer managed keys with Amazon CloudWatch, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cmk-encryption.html">Encryption at rest with
     * customer managed keys</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * @throws ConflictException
     * @throws KmsAccessDeniedException
     * @throws KmsKeyDisabledException
     * @throws KmsKeyNotFoundException
     * @throws ResourceNotFoundException
     */
    AssociateDatasetKmsKeyOutput associateDatasetKmsKey(AssociateDatasetKmsKeyInput input, RequestOverrideConfig overrideConfig);

    /**
     * Deletes a specific alarm mute rule.
     *
     * <p>When you delete a mute rule, any alarms that are currently being muted by that rule are immediately unmuted.
     * If those alarms are in an ALARM state, their configured actions will trigger.
     *
     * <p>This operation is idempotent. If you delete a mute rule that does not exist, the operation succeeds without
     * returning an error.
     *
     * <p><b>Permissions</b>
     *
     * <p>To delete a mute rule, you need the <code>cloudwatch:DeleteAlarmMuteRule</code> permission on the alarm mute
     * rule resource.
     */
    default DeleteAlarmMuteRuleOutput deleteAlarmMuteRule(DeleteAlarmMuteRuleInput input) {
        return deleteAlarmMuteRule(input, null);
    }

    /**
     * Deletes a specific alarm mute rule.
     *
     * <p>When you delete a mute rule, any alarms that are currently being muted by that rule are immediately unmuted.
     * If those alarms are in an ALARM state, their configured actions will trigger.
     *
     * <p>This operation is idempotent. If you delete a mute rule that does not exist, the operation succeeds without
     * returning an error.
     *
     * <p><b>Permissions</b>
     *
     * <p>To delete a mute rule, you need the <code>cloudwatch:DeleteAlarmMuteRule</code> permission on the alarm mute
     * rule resource.
     */
    DeleteAlarmMuteRuleOutput deleteAlarmMuteRule(DeleteAlarmMuteRuleInput input, RequestOverrideConfig overrideConfig);

    /**
     * Deletes the specified alarms. You can delete up to 100 alarms in one operation. However, this total can include
     * no more than one composite alarm. For example, you could delete 99 metric alarms and one composite alarms with
     * one operation, but you can't delete two composite alarms with one operation. Log alarms cannot be batch deleted.
     *
     * <p> If you specify any incorrect alarm names, the alarms you specify with correct names are still deleted. Other
     * syntax errors might result in no alarms being deleted. To confirm that alarms were deleted successfully, you can
     * use the <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DescribeAlarms.html">DescribeAlarms</a> operation after using <code>DeleteAlarms</code>.
     *
     * <p>It is possible to create a loop or cycle of composite alarms, where composite alarm A depends on composite
     * alarm B, and composite alarm B also depends on composite alarm A. In this scenario, you can't delete any
     * composite alarm that is part of the cycle because there is always still a composite alarm that depends on that
     * alarm that you want to delete.
     *
     * <p>To get out of such a situation, you must break the cycle by changing the rule of one of the composite alarms
     * in the cycle to remove a dependency that creates the cycle. The simplest change to make to break a cycle is to
     * change the <code>AlarmRule</code> of one of the alarms to <code>false</code>.
     *
     * <p>Additionally, the evaluation of composite alarms stops if CloudWatch detects a cycle in the evaluation path.
     *
     * @throws ResourceConflict
     * @throws ResourceNotFound
     */
    default DeleteAlarmsOutput deleteAlarms(DeleteAlarmsInput input) {
        return deleteAlarms(input, null);
    }

    /**
     * Deletes the specified alarms. You can delete up to 100 alarms in one operation. However, this total can include
     * no more than one composite alarm. For example, you could delete 99 metric alarms and one composite alarms with
     * one operation, but you can't delete two composite alarms with one operation. Log alarms cannot be batch deleted.
     *
     * <p> If you specify any incorrect alarm names, the alarms you specify with correct names are still deleted. Other
     * syntax errors might result in no alarms being deleted. To confirm that alarms were deleted successfully, you can
     * use the <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DescribeAlarms.html">DescribeAlarms</a> operation after using <code>DeleteAlarms</code>.
     *
     * <p>It is possible to create a loop or cycle of composite alarms, where composite alarm A depends on composite
     * alarm B, and composite alarm B also depends on composite alarm A. In this scenario, you can't delete any
     * composite alarm that is part of the cycle because there is always still a composite alarm that depends on that
     * alarm that you want to delete.
     *
     * <p>To get out of such a situation, you must break the cycle by changing the rule of one of the composite alarms
     * in the cycle to remove a dependency that creates the cycle. The simplest change to make to break a cycle is to
     * change the <code>AlarmRule</code> of one of the alarms to <code>false</code>.
     *
     * <p>Additionally, the evaluation of composite alarms stops if CloudWatch detects a cycle in the evaluation path.
     *
     * @throws ResourceConflict
     * @throws ResourceNotFound
     */
    DeleteAlarmsOutput deleteAlarms(DeleteAlarmsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Deletes the specified anomaly detection model from your account. For more information about how to delete an
     * anomaly detection model, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Create_Anomaly_Detection_Alarm.html#Delete_Anomaly_Detection_Model">Deleting an anomaly detection model</a> in the <i>CloudWatch User Guide</i>.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     * @throws ResourceNotFoundException
     */
    default DeleteAnomalyDetectorOutput deleteAnomalyDetector(DeleteAnomalyDetectorInput input) {
        return deleteAnomalyDetector(input, null);
    }

    /**
     * Deletes the specified anomaly detection model from your account. For more information about how to delete an
     * anomaly detection model, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Create_Anomaly_Detection_Alarm.html#Delete_Anomaly_Detection_Model">Deleting an anomaly detection model</a> in the <i>CloudWatch User Guide</i>.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     * @throws ResourceNotFoundException
     */
    DeleteAnomalyDetectorOutput deleteAnomalyDetector(DeleteAnomalyDetectorInput input, RequestOverrideConfig overrideConfig);

    /**
     * Deletes all dashboards that you specify. You can specify up to 100 dashboards to delete. If there is an error
     * during this call, the operation attempts to delete as many dashboards as possible.
     *
     * @throws ConflictException
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     */
    default DeleteDashboardsOutput deleteDashboards(DeleteDashboardsInput input) {
        return deleteDashboards(input, null);
    }

    /**
     * Deletes all dashboards that you specify. You can specify up to 100 dashboards to delete. If there is an error
     * during this call, the operation attempts to delete as many dashboards as possible.
     *
     * @throws ConflictException
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     */
    DeleteDashboardsOutput deleteDashboards(DeleteDashboardsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Permanently deletes the specified Contributor Insights rules.
     *
     * <p>If you create a rule, delete it, and then re-create it with the same name, historical data from the first time
     * the rule was created might not be available.
     *
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default DeleteInsightRulesOutput deleteInsightRules(DeleteInsightRulesInput input) {
        return deleteInsightRules(input, null);
    }

    /**
     * Permanently deletes the specified Contributor Insights rules.
     *
     * <p>If you create a rule, delete it, and then re-create it with the same name, historical data from the first time
     * the rule was created might not be available.
     *
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    DeleteInsightRulesOutput deleteInsightRules(DeleteInsightRulesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Permanently deletes the metric stream that you specify.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default DeleteMetricStreamOutput deleteMetricStream(DeleteMetricStreamInput input) {
        return deleteMetricStream(input, null);
    }

    /**
     * Permanently deletes the metric stream that you specify.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    DeleteMetricStreamOutput deleteMetricStream(DeleteMetricStreamInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns the information of the current alarm contributors that are in <code>ALARM</code> state. This operation
     * returns details about the individual time series that contribute to the alarm's state.
     *
     * @throws InvalidNextToken
     * @throws ResourceNotFoundException
     */
    default DescribeAlarmContributorsOutput describeAlarmContributors(DescribeAlarmContributorsInput input) {
        return describeAlarmContributors(input, null);
    }

    /**
     * Returns the information of the current alarm contributors that are in <code>ALARM</code> state. This operation
     * returns details about the individual time series that contribute to the alarm's state.
     *
     * @throws InvalidNextToken
     * @throws ResourceNotFoundException
     */
    DescribeAlarmContributorsOutput describeAlarmContributors(DescribeAlarmContributorsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Retrieves the history for the specified alarm. You can filter the results by date range or item type. If an alarm
     * name is not specified, the histories for either all metric alarms or all composite alarms are returned.
     *
     * <p>CloudWatch retains the history of an alarm even if you delete the alarm.
     *
     * <p>To use this operation and return information about a composite alarm, you must be signed on with the <code>
     * cloudwatch:DescribeAlarmHistory</code> permission that is scoped to <code>&#42;</code>. You can't return information
     * about composite alarms if your <code>cloudwatch:DescribeAlarmHistory</code> permission has a narrower scope.
     *
     * @throws InvalidNextToken
     */
    default DescribeAlarmHistoryOutput describeAlarmHistory(DescribeAlarmHistoryInput input) {
        return describeAlarmHistory(input, null);
    }

    /**
     * Retrieves the history for the specified alarm. You can filter the results by date range or item type. If an alarm
     * name is not specified, the histories for either all metric alarms or all composite alarms are returned.
     *
     * <p>CloudWatch retains the history of an alarm even if you delete the alarm.
     *
     * <p>To use this operation and return information about a composite alarm, you must be signed on with the <code>
     * cloudwatch:DescribeAlarmHistory</code> permission that is scoped to <code>&#42;</code>. You can't return information
     * about composite alarms if your <code>cloudwatch:DescribeAlarmHistory</code> permission has a narrower scope.
     *
     * @throws InvalidNextToken
     */
    DescribeAlarmHistoryOutput describeAlarmHistory(DescribeAlarmHistoryInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #describeAlarmHistory} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<DescribeAlarmHistoryOutput> describeAlarmHistoryPaginator(DescribeAlarmHistoryInput input) {
        return Paginator.paginate(input, DescribeAlarmHistory.instance(), this::describeAlarmHistory);
    }

    /**
     * Retrieves the specified alarms. You can filter the results by specifying a prefix for the alarm name, the alarm
     * state, or a prefix for any action.
     *
     * <p>To use this operation and return information about composite alarms, you must be signed on with the <code>
     * cloudwatch:DescribeAlarms</code> permission that is scoped to <code>&#42;</code>. You can't return information about
     * composite alarms if your <code>cloudwatch:DescribeAlarms</code> permission has a narrower scope.
     *
     * @throws InvalidNextToken
     */
    default DescribeAlarmsOutput describeAlarms(DescribeAlarmsInput input) {
        return describeAlarms(input, null);
    }

    /**
     * Retrieves the specified alarms. You can filter the results by specifying a prefix for the alarm name, the alarm
     * state, or a prefix for any action.
     *
     * <p>To use this operation and return information about composite alarms, you must be signed on with the <code>
     * cloudwatch:DescribeAlarms</code> permission that is scoped to <code>&#42;</code>. You can't return information about
     * composite alarms if your <code>cloudwatch:DescribeAlarms</code> permission has a narrower scope.
     *
     * @throws InvalidNextToken
     */
    DescribeAlarmsOutput describeAlarms(DescribeAlarmsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #describeAlarms} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<DescribeAlarmsOutput> describeAlarmsPaginator(DescribeAlarmsInput input) {
        return Paginator.paginate(input, DescribeAlarms.instance(), this::describeAlarms);
    }

    /**
     * Retrieves the alarms for the specified metric. To filter the results, specify a statistic, period, or unit.
     *
     * <p>This operation retrieves only standard alarms that are based on the specified metric. It does not return
     * alarms based on math expressions that use the specified metric, or composite alarms that use the specified
     * metric.
     */
    default DescribeAlarmsForMetricOutput describeAlarmsForMetric(DescribeAlarmsForMetricInput input) {
        return describeAlarmsForMetric(input, null);
    }

    /**
     * Retrieves the alarms for the specified metric. To filter the results, specify a statistic, period, or unit.
     *
     * <p>This operation retrieves only standard alarms that are based on the specified metric. It does not return
     * alarms based on math expressions that use the specified metric, or composite alarms that use the specified
     * metric.
     */
    DescribeAlarmsForMetricOutput describeAlarmsForMetric(DescribeAlarmsForMetricInput input, RequestOverrideConfig overrideConfig);

    /**
     * Lists the anomaly detection models that you have created in your account. For single metric anomaly detectors,
     * you can list all of the models in your account or filter the results to only the models that are related to a
     * certain namespace, metric name, or metric dimension. For metric math anomaly detectors, you can list them by
     * adding <code>METRIC_MATH</code> to the <code>AnomalyDetectorTypes</code> array. This will return all metric math
     * anomaly detectors in your account.
     *
     * @throws InternalServiceFault
     * @throws InvalidNextToken
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     */
    default DescribeAnomalyDetectorsOutput describeAnomalyDetectors(DescribeAnomalyDetectorsInput input) {
        return describeAnomalyDetectors(input, null);
    }

    /**
     * Lists the anomaly detection models that you have created in your account. For single metric anomaly detectors,
     * you can list all of the models in your account or filter the results to only the models that are related to a
     * certain namespace, metric name, or metric dimension. For metric math anomaly detectors, you can list them by
     * adding <code>METRIC_MATH</code> to the <code>AnomalyDetectorTypes</code> array. This will return all metric math
     * anomaly detectors in your account.
     *
     * @throws InternalServiceFault
     * @throws InvalidNextToken
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     */
    DescribeAnomalyDetectorsOutput describeAnomalyDetectors(DescribeAnomalyDetectorsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #describeAnomalyDetectors} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<DescribeAnomalyDetectorsOutput> describeAnomalyDetectorsPaginator(DescribeAnomalyDetectorsInput input) {
        return Paginator.paginate(input, DescribeAnomalyDetectors.instance(), this::describeAnomalyDetectors);
    }

    /**
     * Returns a list of all the Contributor Insights rules in your account.
     *
     * <p>For more information about Contributor Insights, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights.html">Using Contributor Insights to Analyze High-Cardinality
     * Data</a>.
     *
     * @throws InvalidNextToken
     */
    default DescribeInsightRulesOutput describeInsightRules(DescribeInsightRulesInput input) {
        return describeInsightRules(input, null);
    }

    /**
     * Returns a list of all the Contributor Insights rules in your account.
     *
     * <p>For more information about Contributor Insights, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights.html">Using Contributor Insights to Analyze High-Cardinality
     * Data</a>.
     *
     * @throws InvalidNextToken
     */
    DescribeInsightRulesOutput describeInsightRules(DescribeInsightRulesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #describeInsightRules} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<DescribeInsightRulesOutput> describeInsightRulesPaginator(DescribeInsightRulesInput input) {
        return Paginator.paginate(input, DescribeInsightRules.instance(), this::describeInsightRules);
    }

    /**
     * Disables the actions for the specified alarms. When an alarm's actions are disabled, the alarm actions do not
     * execute when the alarm state changes.
     */
    default DisableAlarmActionsOutput disableAlarmActions(DisableAlarmActionsInput input) {
        return disableAlarmActions(input, null);
    }

    /**
     * Disables the actions for the specified alarms. When an alarm's actions are disabled, the alarm actions do not
     * execute when the alarm state changes.
     */
    DisableAlarmActionsOutput disableAlarmActions(DisableAlarmActionsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Disables the specified Contributor Insights rules. When rules are disabled, they do not analyze log groups and do
     * not incur costs.
     *
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default DisableInsightRulesOutput disableInsightRules(DisableInsightRulesInput input) {
        return disableInsightRules(input, null);
    }

    /**
     * Disables the specified Contributor Insights rules. When rules are disabled, they do not analyze log groups and do
     * not incur costs.
     *
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    DisableInsightRulesOutput disableInsightRules(DisableInsightRulesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Removes the customer managed Amazon Web Services Key Management Service (Amazon Web Services KMS) key association
     * from the specified dataset. After this operation completes, data that you publish to the dataset is encrypted at
     * rest using an Amazon Web Services owned key managed by Amazon CloudWatch.
     *
     * <p>Only the <code>default</code> dataset is supported. To call this operation, the dataset must currently have a
     * customer managed KMS key associated with it. If the dataset has no associated KMS key, the operation fails with <code>
     * ResourceNotFoundException</code>.
     *
     * <p>Amazon CloudWatch performs a dry-run <code>kms:Decrypt</code> call on the currently associated key as part of
     * this operation. The caller must have <code>kms:Decrypt</code> permission on the currently associated key. If the
     * key is accessible but the caller lacks <code>kms:Decrypt</code> permission, the operation fails with <code>
     * AccessDeniedException</code>.
     *
     * <p>If the currently associated key has been deleted, is scheduled for deletion, is pending import, is
     * unavailable, or has been disabled, Amazon CloudWatch does not require <code>kms:Decrypt</code> permission on that
     * key and the disassociation proceeds. If the key was only disabled, consider re-enabling it instead of
     * disassociating, because re-enabling allows Amazon CloudWatch to resume decrypting your existing metric data.
     *
     * <p>Disassociating a KMS key from a dataset does not immediately remove the <code>kms:Decrypt</code> requirement
     * on data plane operations. For up to three hours after disassociation, callers must continue to have <code>
     * kms:Decrypt</code> permission on the previously associated key. Some data might still be encrypted with that key
     * during this window. After this enforcement window elapses, the <code>kms:Decrypt</code> requirement is lifted.
     *
     * <p>For more information about using customer managed keys with Amazon CloudWatch, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cmk-encryption.html">Encryption at rest with
     * customer managed keys</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * @throws ConflictException
     * @throws ResourceNotFoundException
     */
    default DisassociateDatasetKmsKeyOutput disassociateDatasetKmsKey(DisassociateDatasetKmsKeyInput input) {
        return disassociateDatasetKmsKey(input, null);
    }

    /**
     * Removes the customer managed Amazon Web Services Key Management Service (Amazon Web Services KMS) key association
     * from the specified dataset. After this operation completes, data that you publish to the dataset is encrypted at
     * rest using an Amazon Web Services owned key managed by Amazon CloudWatch.
     *
     * <p>Only the <code>default</code> dataset is supported. To call this operation, the dataset must currently have a
     * customer managed KMS key associated with it. If the dataset has no associated KMS key, the operation fails with <code>
     * ResourceNotFoundException</code>.
     *
     * <p>Amazon CloudWatch performs a dry-run <code>kms:Decrypt</code> call on the currently associated key as part of
     * this operation. The caller must have <code>kms:Decrypt</code> permission on the currently associated key. If the
     * key is accessible but the caller lacks <code>kms:Decrypt</code> permission, the operation fails with <code>
     * AccessDeniedException</code>.
     *
     * <p>If the currently associated key has been deleted, is scheduled for deletion, is pending import, is
     * unavailable, or has been disabled, Amazon CloudWatch does not require <code>kms:Decrypt</code> permission on that
     * key and the disassociation proceeds. If the key was only disabled, consider re-enabling it instead of
     * disassociating, because re-enabling allows Amazon CloudWatch to resume decrypting your existing metric data.
     *
     * <p>Disassociating a KMS key from a dataset does not immediately remove the <code>kms:Decrypt</code> requirement
     * on data plane operations. For up to three hours after disassociation, callers must continue to have <code>
     * kms:Decrypt</code> permission on the previously associated key. Some data might still be encrypted with that key
     * during this window. After this enforcement window elapses, the <code>kms:Decrypt</code> requirement is lifted.
     *
     * <p>For more information about using customer managed keys with Amazon CloudWatch, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cmk-encryption.html">Encryption at rest with
     * customer managed keys</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * @throws ConflictException
     * @throws ResourceNotFoundException
     */
    DisassociateDatasetKmsKeyOutput disassociateDatasetKmsKey(DisassociateDatasetKmsKeyInput input, RequestOverrideConfig overrideConfig);

    /**
     * Enables the actions for the specified alarms.
     */
    default EnableAlarmActionsOutput enableAlarmActions(EnableAlarmActionsInput input) {
        return enableAlarmActions(input, null);
    }

    /**
     * Enables the actions for the specified alarms.
     */
    EnableAlarmActionsOutput enableAlarmActions(EnableAlarmActionsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Enables the specified Contributor Insights rules. When rules are enabled, they immediately begin analyzing log
     * data.
     *
     * @throws InvalidParameterValueException
     * @throws LimitExceededException
     * @throws MissingRequiredParameterException
     */
    default EnableInsightRulesOutput enableInsightRules(EnableInsightRulesInput input) {
        return enableInsightRules(input, null);
    }

    /**
     * Enables the specified Contributor Insights rules. When rules are enabled, they immediately begin analyzing log
     * data.
     *
     * @throws InvalidParameterValueException
     * @throws LimitExceededException
     * @throws MissingRequiredParameterException
     */
    EnableInsightRulesOutput enableInsightRules(EnableInsightRulesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Retrieves details for a specific alarm mute rule.
     *
     * <p>This operation returns complete information about the mute rule, including its configuration, status, targeted
     * alarms, and metadata.
     *
     * <p>The returned status indicates the current state of the mute rule:
     *
     * <ul>
     *   <li>
     *     <b>SCHEDULED</b>: The mute rule is configured and will become active in the future
     *   </li>
     *   <li>
     *     <b>ACTIVE</b>: The mute rule is currently muting alarm actions
     *   </li>
     *   <li>
     *     <b>EXPIRED</b>: The mute rule has passed its expiration date and will no longer become active
     *   </li>
     * </ul>
     *
     * <p><b>Permissions</b>
     *
     * <p>To retrieve details for a mute rule, you need the <code>cloudwatch:GetAlarmMuteRule</code> permission on the
     * alarm mute rule resource.
     *
     * @throws ResourceNotFoundException
     */
    default GetAlarmMuteRuleOutput getAlarmMuteRule(GetAlarmMuteRuleInput input) {
        return getAlarmMuteRule(input, null);
    }

    /**
     * Retrieves details for a specific alarm mute rule.
     *
     * <p>This operation returns complete information about the mute rule, including its configuration, status, targeted
     * alarms, and metadata.
     *
     * <p>The returned status indicates the current state of the mute rule:
     *
     * <ul>
     *   <li>
     *     <b>SCHEDULED</b>: The mute rule is configured and will become active in the future
     *   </li>
     *   <li>
     *     <b>ACTIVE</b>: The mute rule is currently muting alarm actions
     *   </li>
     *   <li>
     *     <b>EXPIRED</b>: The mute rule has passed its expiration date and will no longer become active
     *   </li>
     * </ul>
     *
     * <p><b>Permissions</b>
     *
     * <p>To retrieve details for a mute rule, you need the <code>cloudwatch:GetAlarmMuteRule</code> permission on the
     * alarm mute rule resource.
     *
     * @throws ResourceNotFoundException
     */
    GetAlarmMuteRuleOutput getAlarmMuteRule(GetAlarmMuteRuleInput input, RequestOverrideConfig overrideConfig);

    /**
     * Displays the details of the dashboard that you specify.
     *
     * <p>To copy an existing dashboard, use <code>GetDashboard</code>, and then use the data returned within <code>
     * DashboardBody</code> as the template for the new dashboard when you call <code>PutDashboard</code> to create the
     * copy.
     *
     * @throws DashboardNotFoundError
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     */
    default GetDashboardOutput getDashboard(GetDashboardInput input) {
        return getDashboard(input, null);
    }

    /**
     * Displays the details of the dashboard that you specify.
     *
     * <p>To copy an existing dashboard, use <code>GetDashboard</code>, and then use the data returned within <code>
     * DashboardBody</code> as the template for the new dashboard when you call <code>PutDashboard</code> to create the
     * copy.
     *
     * @throws DashboardNotFoundError
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     */
    GetDashboardOutput getDashboard(GetDashboardInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns information about the specified dataset. This includes its identifier, Amazon Resource Name (ARN), and
     * any customer managed Amazon Web Services Key Management Service (Amazon Web Services KMS) key that is currently
     * associated with it.
     *
     * <p>Only the <code>default</code> dataset is supported. The <code>default</code> dataset is implicit for every
     * account in every Region — you can call <code>GetDataset</code> for it without first creating it. If no customer
     * managed KMS key has been associated with the dataset, the response omits the <code>KmsKeyArn</code> field,
     * indicating that data is encrypted at rest using an Amazon Web Services owned key managed by Amazon CloudWatch.
     *
     * <p>To associate a customer managed KMS key with a dataset, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_AssociateDatasetKmsKey.html">AssociateDatasetKmsKey</a>. To remove the
     * association, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DisassociateDatasetKmsKey.html">DisassociateDatasetKmsKey</a>.
     *
     * @throws ResourceNotFoundException
     */
    default GetDatasetOutput getDataset(GetDatasetInput input) {
        return getDataset(input, null);
    }

    /**
     * Returns information about the specified dataset. This includes its identifier, Amazon Resource Name (ARN), and
     * any customer managed Amazon Web Services Key Management Service (Amazon Web Services KMS) key that is currently
     * associated with it.
     *
     * <p>Only the <code>default</code> dataset is supported. The <code>default</code> dataset is implicit for every
     * account in every Region — you can call <code>GetDataset</code> for it without first creating it. If no customer
     * managed KMS key has been associated with the dataset, the response omits the <code>KmsKeyArn</code> field,
     * indicating that data is encrypted at rest using an Amazon Web Services owned key managed by Amazon CloudWatch.
     *
     * <p>To associate a customer managed KMS key with a dataset, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_AssociateDatasetKmsKey.html">AssociateDatasetKmsKey</a>. To remove the
     * association, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DisassociateDatasetKmsKey.html">DisassociateDatasetKmsKey</a>.
     *
     * @throws ResourceNotFoundException
     */
    GetDatasetOutput getDataset(GetDatasetInput input, RequestOverrideConfig overrideConfig);

    /**
     * This operation returns the time series data collected by a Contributor Insights rule. The data includes the
     * identity and number of contributors to the log group.
     *
     * <p>You can also optionally return one or more statistics about each data point in the time series. These
     * statistics can include the following:
     *
     * <ul>
     *   <li>
     *     <code>UniqueContributors</code> -- the number of unique contributors for each data point.
     *   </li>
     *   <li>
     *     <code>MaxContributorValue</code> -- the value of the top contributor for each data point. The identity of
     *     the contributor might change for each data point in the graph.If this rule aggregates by COUNT, the top
     *     contributor for each data point is the contributor with the most occurrences in that period. If the rule
     *     aggregates by SUM, the top contributor is the contributor with the highest sum in the log field specified
     *     by the rule's <code>Value</code>, during that period.
     *   </li>
     *   <li>
     *     <code>SampleCount</code> -- the number of data points matched by the rule.
     *   </li>
     *   <li>
     *     <code>Sum</code> -- the sum of the values from all contributors during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Minimum</code> -- the minimum value from a single observation during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Maximum</code> -- the maximum value from a single observation during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Average</code> -- the average value from all contributors during the time period represented by
     *     that data point.
     *   </li>
     * </ul>
     *
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     * @throws ResourceNotFoundException
     */
    default GetInsightRuleReportOutput getInsightRuleReport(GetInsightRuleReportInput input) {
        return getInsightRuleReport(input, null);
    }

    /**
     * This operation returns the time series data collected by a Contributor Insights rule. The data includes the
     * identity and number of contributors to the log group.
     *
     * <p>You can also optionally return one or more statistics about each data point in the time series. These
     * statistics can include the following:
     *
     * <ul>
     *   <li>
     *     <code>UniqueContributors</code> -- the number of unique contributors for each data point.
     *   </li>
     *   <li>
     *     <code>MaxContributorValue</code> -- the value of the top contributor for each data point. The identity of
     *     the contributor might change for each data point in the graph.If this rule aggregates by COUNT, the top
     *     contributor for each data point is the contributor with the most occurrences in that period. If the rule
     *     aggregates by SUM, the top contributor is the contributor with the highest sum in the log field specified
     *     by the rule's <code>Value</code>, during that period.
     *   </li>
     *   <li>
     *     <code>SampleCount</code> -- the number of data points matched by the rule.
     *   </li>
     *   <li>
     *     <code>Sum</code> -- the sum of the values from all contributors during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Minimum</code> -- the minimum value from a single observation during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Maximum</code> -- the maximum value from a single observation during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Average</code> -- the average value from all contributors during the time period represented by
     *     that data point.
     *   </li>
     * </ul>
     *
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     * @throws ResourceNotFoundException
     */
    GetInsightRuleReportOutput getInsightRuleReport(GetInsightRuleReportInput input, RequestOverrideConfig overrideConfig);

    /**
     * You can use the <code>GetMetricData</code> API to retrieve CloudWatch metric values. The operation can also
     * include a CloudWatch Metrics Insights query, and one or more metric math functions.
     *
     * <p>A <code>GetMetricData</code> operation that does not include a query can retrieve as many as 500 different
     * metrics in a single request, with a total of as many as 100,800 data points. You can also optionally perform
     * metric math expressions on the values of the returned statistics, to create new time series that represent new
     * insights into your data. For example, using Lambda metrics, you could divide the Errors metric by the Invocations
     * metric to get an error rate time series. For more information about metric math expressions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/using-metric-math.html#metric-math-syntax">Metric Math
     * Syntax and Functions</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * <p>If you include a Metrics Insights query, each <code>GetMetricData</code> operation can include only one query.
     * But the same <code>GetMetricData</code> operation can also retrieve other metrics. Metrics Insights queries can
     * query only the most recent three hours of metric data. For more information about Metrics Insights, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/query_with_cloudwatch-metrics-insights.html">Query
     * your metrics with CloudWatch Metrics Insights</a>.
     *
     * <p>Calls to the <code>GetMetricData</code> API have a different pricing structure than calls to <code>
     * GetMetricStatistics</code>. For more information about pricing, see <a href="https://aws.amazon.com/cloudwatch/pricing/">Amazon CloudWatch Pricing</a>.
     *
     * <p>Amazon CloudWatch retains metric data as follows:
     *
     * <ul>
     *   <li>
     *     Data points with a period of less than 60 seconds are available for 3 hours. These data points are
     *     high-resolution metrics and are available only for custom metrics that have been defined with a <code>
     *     StorageResolution</code> of 1.
     *   </li>
     *   <li>
     *     Data points with a period of 60 seconds (1-minute) are available for 15 days.
     *   </li>
     *   <li>
     *     Data points with a period of 300 seconds (5-minute) are available for 63 days.
     *   </li>
     *   <li>
     *     Data points with a period of 3600 seconds (1 hour) are available for 455 days (15 months).
     *   </li>
     * </ul>
     *
     * <p>Data points that are initially published with a shorter period are aggregated together for long-term storage.
     * For example, if you collect data using a period of 1 minute, the data remains available for 15 days with 1-minute
     * resolution. After 15 days, this data is still available, but is aggregated and retrievable only with a resolution
     * of 5 minutes. After 63 days, the data is further aggregated and is available with a resolution of 1 hour.
     *
     * <p>If you omit <code>Unit</code> in your request, all data that was collected with any unit is returned, along
     * with the corresponding units that were specified when the data was reported to CloudWatch. If you specify a unit,
     * the operation returns only data that was collected with that unit specified. If you specify a unit that does not
     * match the data collected, the results of the operation are null. CloudWatch does not perform unit conversions.
     *
     * <p><b>Using Metrics Insights queries with metric math</b>
     *
     * <p>You can't mix a Metric Insights query and metric math syntax in the same expression, but you can reference
     * results from a Metrics Insights query within other Metric math expressions. A Metrics Insights query without a <b>
     * GROUP BY</b> clause returns a single time-series (TS), and can be used as input for a metric math expression that
     * expects a single time series. A Metrics Insights query with a <b>GROUP BY</b> clause returns an array of
     * time-series (TS[]), and can be used as input for a metric math expression that expects an array of time series.
     *
     * @throws InvalidNextToken
     */
    default GetMetricDataOutput getMetricData(GetMetricDataInput input) {
        return getMetricData(input, null);
    }

    /**
     * You can use the <code>GetMetricData</code> API to retrieve CloudWatch metric values. The operation can also
     * include a CloudWatch Metrics Insights query, and one or more metric math functions.
     *
     * <p>A <code>GetMetricData</code> operation that does not include a query can retrieve as many as 500 different
     * metrics in a single request, with a total of as many as 100,800 data points. You can also optionally perform
     * metric math expressions on the values of the returned statistics, to create new time series that represent new
     * insights into your data. For example, using Lambda metrics, you could divide the Errors metric by the Invocations
     * metric to get an error rate time series. For more information about metric math expressions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/using-metric-math.html#metric-math-syntax">Metric Math
     * Syntax and Functions</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * <p>If you include a Metrics Insights query, each <code>GetMetricData</code> operation can include only one query.
     * But the same <code>GetMetricData</code> operation can also retrieve other metrics. Metrics Insights queries can
     * query only the most recent three hours of metric data. For more information about Metrics Insights, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/query_with_cloudwatch-metrics-insights.html">Query
     * your metrics with CloudWatch Metrics Insights</a>.
     *
     * <p>Calls to the <code>GetMetricData</code> API have a different pricing structure than calls to <code>
     * GetMetricStatistics</code>. For more information about pricing, see <a href="https://aws.amazon.com/cloudwatch/pricing/">Amazon CloudWatch Pricing</a>.
     *
     * <p>Amazon CloudWatch retains metric data as follows:
     *
     * <ul>
     *   <li>
     *     Data points with a period of less than 60 seconds are available for 3 hours. These data points are
     *     high-resolution metrics and are available only for custom metrics that have been defined with a <code>
     *     StorageResolution</code> of 1.
     *   </li>
     *   <li>
     *     Data points with a period of 60 seconds (1-minute) are available for 15 days.
     *   </li>
     *   <li>
     *     Data points with a period of 300 seconds (5-minute) are available for 63 days.
     *   </li>
     *   <li>
     *     Data points with a period of 3600 seconds (1 hour) are available for 455 days (15 months).
     *   </li>
     * </ul>
     *
     * <p>Data points that are initially published with a shorter period are aggregated together for long-term storage.
     * For example, if you collect data using a period of 1 minute, the data remains available for 15 days with 1-minute
     * resolution. After 15 days, this data is still available, but is aggregated and retrievable only with a resolution
     * of 5 minutes. After 63 days, the data is further aggregated and is available with a resolution of 1 hour.
     *
     * <p>If you omit <code>Unit</code> in your request, all data that was collected with any unit is returned, along
     * with the corresponding units that were specified when the data was reported to CloudWatch. If you specify a unit,
     * the operation returns only data that was collected with that unit specified. If you specify a unit that does not
     * match the data collected, the results of the operation are null. CloudWatch does not perform unit conversions.
     *
     * <p><b>Using Metrics Insights queries with metric math</b>
     *
     * <p>You can't mix a Metric Insights query and metric math syntax in the same expression, but you can reference
     * results from a Metrics Insights query within other Metric math expressions. A Metrics Insights query without a <b>
     * GROUP BY</b> clause returns a single time-series (TS), and can be used as input for a metric math expression that
     * expects a single time series. A Metrics Insights query with a <b>GROUP BY</b> clause returns an array of
     * time-series (TS[]), and can be used as input for a metric math expression that expects an array of time series.
     *
     * @throws InvalidNextToken
     */
    GetMetricDataOutput getMetricData(GetMetricDataInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #getMetricData} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<GetMetricDataOutput> getMetricDataPaginator(GetMetricDataInput input) {
        return Paginator.paginate(input, GetMetricData.instance(), this::getMetricData);
    }

    /**
     * Gets statistics for the specified metric.
     *
     * <p>The maximum number of data points returned from a single call is 1,440. If you request more than 1,440 data
     * points, CloudWatch returns an error. To reduce the number of data points, you can narrow the specified time range
     * and make multiple requests across adjacent time ranges, or you can increase the specified period. Data points are
     * not returned in chronological order.
     *
     * <p>CloudWatch aggregates data points based on the length of the period that you specify. For example, if you
     * request statistics with a one-hour period, CloudWatch aggregates all data points with time stamps that fall
     * within each one-hour period. Therefore, the number of values aggregated by CloudWatch is larger than the number
     * of data points returned.
     *
     * <p>CloudWatch needs raw data points to calculate percentile statistics. If you publish data using a statistic set
     * instead, you can only retrieve percentile statistics for this data if one of the following conditions is true:
     *
     * <ul>
     *   <li>
     *     The SampleCount value of the statistic set is 1.
     *   </li>
     *   <li>
     *     The Min and the Max values of the statistic set are equal.
     *   </li>
     * </ul>
     *
     * <p>Percentile statistics are not available for metrics when any of the metric values are negative numbers.
     *
     * <p>Amazon CloudWatch retains metric data as follows:
     *
     * <ul>
     *   <li>
     *     Data points with a period of less than 60 seconds are available for 3 hours. These data points are
     *     high-resolution metrics and are available only for custom metrics that have been defined with a <code>
     *     StorageResolution</code> of 1.
     *   </li>
     *   <li>
     *     Data points with a period of 60 seconds (1-minute) are available for 15 days.
     *   </li>
     *   <li>
     *     Data points with a period of 300 seconds (5-minute) are available for 63 days.
     *   </li>
     *   <li>
     *     Data points with a period of 3600 seconds (1 hour) are available for 455 days (15 months).
     *   </li>
     * </ul>
     *
     * <p>Data points that are initially published with a shorter period are aggregated together for long-term storage.
     * For example, if you collect data using a period of 1 minute, the data remains available for 15 days with 1-minute
     * resolution. After 15 days, this data is still available, but is aggregated and retrievable only with a resolution
     * of 5 minutes. After 63 days, the data is further aggregated and is available with a resolution of 1 hour.
     *
     * <p>CloudWatch started retaining 5-minute and 1-hour metric data as of July 9, 2016.
     *
     * <p>For information about metrics and dimensions supported by Amazon Web Services services, see the <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CW_Support_For_AWS.html">Amazon
     * CloudWatch Metrics and Dimensions Reference</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default GetMetricStatisticsOutput getMetricStatistics(GetMetricStatisticsInput input) {
        return getMetricStatistics(input, null);
    }

    /**
     * Gets statistics for the specified metric.
     *
     * <p>The maximum number of data points returned from a single call is 1,440. If you request more than 1,440 data
     * points, CloudWatch returns an error. To reduce the number of data points, you can narrow the specified time range
     * and make multiple requests across adjacent time ranges, or you can increase the specified period. Data points are
     * not returned in chronological order.
     *
     * <p>CloudWatch aggregates data points based on the length of the period that you specify. For example, if you
     * request statistics with a one-hour period, CloudWatch aggregates all data points with time stamps that fall
     * within each one-hour period. Therefore, the number of values aggregated by CloudWatch is larger than the number
     * of data points returned.
     *
     * <p>CloudWatch needs raw data points to calculate percentile statistics. If you publish data using a statistic set
     * instead, you can only retrieve percentile statistics for this data if one of the following conditions is true:
     *
     * <ul>
     *   <li>
     *     The SampleCount value of the statistic set is 1.
     *   </li>
     *   <li>
     *     The Min and the Max values of the statistic set are equal.
     *   </li>
     * </ul>
     *
     * <p>Percentile statistics are not available for metrics when any of the metric values are negative numbers.
     *
     * <p>Amazon CloudWatch retains metric data as follows:
     *
     * <ul>
     *   <li>
     *     Data points with a period of less than 60 seconds are available for 3 hours. These data points are
     *     high-resolution metrics and are available only for custom metrics that have been defined with a <code>
     *     StorageResolution</code> of 1.
     *   </li>
     *   <li>
     *     Data points with a period of 60 seconds (1-minute) are available for 15 days.
     *   </li>
     *   <li>
     *     Data points with a period of 300 seconds (5-minute) are available for 63 days.
     *   </li>
     *   <li>
     *     Data points with a period of 3600 seconds (1 hour) are available for 455 days (15 months).
     *   </li>
     * </ul>
     *
     * <p>Data points that are initially published with a shorter period are aggregated together for long-term storage.
     * For example, if you collect data using a period of 1 minute, the data remains available for 15 days with 1-minute
     * resolution. After 15 days, this data is still available, but is aggregated and retrievable only with a resolution
     * of 5 minutes. After 63 days, the data is further aggregated and is available with a resolution of 1 hour.
     *
     * <p>CloudWatch started retaining 5-minute and 1-hour metric data as of July 9, 2016.
     *
     * <p>For information about metrics and dimensions supported by Amazon Web Services services, see the <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CW_Support_For_AWS.html">Amazon
     * CloudWatch Metrics and Dimensions Reference</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    GetMetricStatisticsOutput getMetricStatistics(GetMetricStatisticsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns information about the metric stream that you specify.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     * @throws ResourceNotFoundException
     */
    default GetMetricStreamOutput getMetricStream(GetMetricStreamInput input) {
        return getMetricStream(input, null);
    }

    /**
     * Returns information about the metric stream that you specify.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     * @throws ResourceNotFoundException
     */
    GetMetricStreamOutput getMetricStream(GetMetricStreamInput input, RequestOverrideConfig overrideConfig);

    /**
     * You can use the <code>GetMetricWidgetImage</code> API to retrieve a snapshot graph of one or more Amazon
     * CloudWatch metrics as a bitmap image. You can then embed this image into your services and products, such as wiki
     * pages, reports, and documents. You could also retrieve images regularly, such as every minute, and create your
     * own custom live dashboard.
     *
     * <p>The graph you retrieve can include all CloudWatch metric graph features, including metric math and horizontal
     * and vertical annotations.
     *
     * <p>There is a limit of 20 transactions per second for this API. Each <code>GetMetricWidgetImage</code> action has
     * the following limits:
     *
     * <ul>
     *   <li>
     *     As many as 100 metrics in the graph.
     *   </li>
     *   <li>
     *     Up to 100 KB uncompressed payload.
     *   </li>
     * </ul>
     */
    default GetMetricWidgetImageOutput getMetricWidgetImage(GetMetricWidgetImageInput input) {
        return getMetricWidgetImage(input, null);
    }

    /**
     * You can use the <code>GetMetricWidgetImage</code> API to retrieve a snapshot graph of one or more Amazon
     * CloudWatch metrics as a bitmap image. You can then embed this image into your services and products, such as wiki
     * pages, reports, and documents. You could also retrieve images regularly, such as every minute, and create your
     * own custom live dashboard.
     *
     * <p>The graph you retrieve can include all CloudWatch metric graph features, including metric math and horizontal
     * and vertical annotations.
     *
     * <p>There is a limit of 20 transactions per second for this API. Each <code>GetMetricWidgetImage</code> action has
     * the following limits:
     *
     * <ul>
     *   <li>
     *     As many as 100 metrics in the graph.
     *   </li>
     *   <li>
     *     Up to 100 KB uncompressed payload.
     *   </li>
     * </ul>
     */
    GetMetricWidgetImageOutput getMetricWidgetImage(GetMetricWidgetImageInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns the current status of vended metric enrichment for the account, including whether CloudWatch vended
     * metrics are enriched with resource ARN and resource tag labels and queryable using PromQL. For the list of
     * supported resources, see <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">Supported
     * Amazon Web Services infrastructure metrics</a>.
     */
    default GetOTelEnrichmentOutput getOTelEnrichment(GetOTelEnrichmentInput input) {
        return getOTelEnrichment(input, null);
    }

    /**
     * Returns the current status of vended metric enrichment for the account, including whether CloudWatch vended
     * metrics are enriched with resource ARN and resource tag labels and queryable using PromQL. For the list of
     * supported resources, see <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">Supported
     * Amazon Web Services infrastructure metrics</a>.
     */
    GetOTelEnrichmentOutput getOTelEnrichment(GetOTelEnrichmentInput input, RequestOverrideConfig overrideConfig);

    /**
     * Lists alarm mute rules in your Amazon Web Services account and region.
     *
     * <p>You can filter the results by alarm name to find all mute rules targeting a specific alarm, or by status to
     * find rules that are scheduled, active, or expired.
     *
     * <p>This operation supports pagination for accounts with many mute rules. Use the <code>MaxRecords</code> and <code>
     * NextToken</code> parameters to retrieve results in multiple calls.
     *
     * <p><b>Permissions</b>
     *
     * <p>To list mute rules, you need the <code>cloudwatch:ListAlarmMuteRules</code> permission.
     *
     * @throws InvalidNextToken
     * @throws ResourceNotFoundException
     */
    default ListAlarmMuteRulesOutput listAlarmMuteRules(ListAlarmMuteRulesInput input) {
        return listAlarmMuteRules(input, null);
    }

    /**
     * Lists alarm mute rules in your Amazon Web Services account and region.
     *
     * <p>You can filter the results by alarm name to find all mute rules targeting a specific alarm, or by status to
     * find rules that are scheduled, active, or expired.
     *
     * <p>This operation supports pagination for accounts with many mute rules. Use the <code>MaxRecords</code> and <code>
     * NextToken</code> parameters to retrieve results in multiple calls.
     *
     * <p><b>Permissions</b>
     *
     * <p>To list mute rules, you need the <code>cloudwatch:ListAlarmMuteRules</code> permission.
     *
     * @throws InvalidNextToken
     * @throws ResourceNotFoundException
     */
    ListAlarmMuteRulesOutput listAlarmMuteRules(ListAlarmMuteRulesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listAlarmMuteRules} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListAlarmMuteRulesOutput> listAlarmMuteRulesPaginator(ListAlarmMuteRulesInput input) {
        return Paginator.paginate(input, ListAlarmMuteRules.instance(), this::listAlarmMuteRules);
    }

    /**
     * Returns a list of the dashboards for your account. If you include <code>DashboardNamePrefix</code>, only those
     * dashboards with names starting with the prefix are listed. Otherwise, all dashboards in your account are listed.
     *
     * <p><code>ListDashboards</code> returns up to 1000 results on one page. If there are more than 1000 dashboards,
     * you can call <code>ListDashboards</code> again and include the value you received for <code>NextToken</code> in
     * the first call, to receive the next 1000 results.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     */
    default ListDashboardsOutput listDashboards(ListDashboardsInput input) {
        return listDashboards(input, null);
    }

    /**
     * Returns a list of the dashboards for your account. If you include <code>DashboardNamePrefix</code>, only those
     * dashboards with names starting with the prefix are listed. Otherwise, all dashboards in your account are listed.
     *
     * <p><code>ListDashboards</code> returns up to 1000 results on one page. If there are more than 1000 dashboards,
     * you can call <code>ListDashboards</code> again and include the value you received for <code>NextToken</code> in
     * the first call, to receive the next 1000 results.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     */
    ListDashboardsOutput listDashboards(ListDashboardsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listDashboards} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListDashboardsOutput> listDashboardsPaginator(ListDashboardsInput input) {
        return Paginator.paginate(input, ListDashboards.instance(), this::listDashboards);
    }

    /**
     * Returns a list that contains the number of managed Contributor Insights rules in your account.
     *
     * <pre>{@code
     *     </p>
     *
     * }</pre>
     *
     * @throws InvalidNextToken
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default ListManagedInsightRulesOutput listManagedInsightRules(ListManagedInsightRulesInput input) {
        return listManagedInsightRules(input, null);
    }

    /**
     * Returns a list that contains the number of managed Contributor Insights rules in your account.
     *
     * <pre>{@code
     *     </p>
     *
     * }</pre>
     *
     * @throws InvalidNextToken
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    ListManagedInsightRulesOutput listManagedInsightRules(ListManagedInsightRulesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listManagedInsightRules} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListManagedInsightRulesOutput> listManagedInsightRulesPaginator(ListManagedInsightRulesInput input) {
        return Paginator.paginate(input, ListManagedInsightRules.instance(), this::listManagedInsightRules);
    }

    /**
     * List the specified metrics. You can use the returned metrics with <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a>
     * to get statistical data.
     *
     * <p>Up to 500 results are returned for any one call. To retrieve additional results, use the returned token with
     * subsequent calls.
     *
     * <p>After you create a metric, allow up to 15 minutes for the metric to appear. To see metric statistics sooner,
     * use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a>.
     *
     * <p>If you are using CloudWatch cross-account observability, you can use this operation in a monitoring account
     * and view metrics from the linked source accounts. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Unified-Cross-Account.html">CloudWatch cross-account
     * observability</a>.
     *
     * <p><code>ListMetrics</code> doesn't return information about metrics if those metrics haven't reported data in
     * the past two weeks. To retrieve those metrics, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a>.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     */
    default ListMetricsOutput listMetrics(ListMetricsInput input) {
        return listMetrics(input, null);
    }

    /**
     * List the specified metrics. You can use the returned metrics with <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a>
     * to get statistical data.
     *
     * <p>Up to 500 results are returned for any one call. To retrieve additional results, use the returned token with
     * subsequent calls.
     *
     * <p>After you create a metric, allow up to 15 minutes for the metric to appear. To see metric statistics sooner,
     * use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a>.
     *
     * <p>If you are using CloudWatch cross-account observability, you can use this operation in a monitoring account
     * and view metrics from the linked source accounts. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Unified-Cross-Account.html">CloudWatch cross-account
     * observability</a>.
     *
     * <p><code>ListMetrics</code> doesn't return information about metrics if those metrics haven't reported data in
     * the past two weeks. To retrieve those metrics, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a>.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     */
    ListMetricsOutput listMetrics(ListMetricsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listMetrics} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListMetricsOutput> listMetricsPaginator(ListMetricsInput input) {
        return Paginator.paginate(input, ListMetrics.instance(), this::listMetrics);
    }

    /**
     * Returns a list of metric streams in this account.
     *
     * @throws InternalServiceFault
     * @throws InvalidNextToken
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default ListMetricStreamsOutput listMetricStreams(ListMetricStreamsInput input) {
        return listMetricStreams(input, null);
    }

    /**
     * Returns a list of metric streams in this account.
     *
     * @throws InternalServiceFault
     * @throws InvalidNextToken
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    ListMetricStreamsOutput listMetricStreams(ListMetricStreamsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Returns a {@link Paginator} for the {@link #listMetricStreams} operation.
     *
     * @param input Input to use as basis for paginated calls.
     * @return Paginator that can be used to retrieval paginated results.
     */
    default Paginator<ListMetricStreamsOutput> listMetricStreamsPaginator(ListMetricStreamsInput input) {
        return Paginator.paginate(input, ListMetricStreams.instance(), this::listMetricStreams);
    }

    /**
     * Displays the tags associated with a CloudWatch resource. Currently, alarms, dashboards, metric streams and
     * Contributor Insights rules support tagging.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws ResourceNotFoundException
     */
    default ListTagsForResourceOutput listTagsForResource(ListTagsForResourceInput input) {
        return listTagsForResource(input, null);
    }

    /**
     * Displays the tags associated with a CloudWatch resource. Currently, alarms, dashboards, metric streams and
     * Contributor Insights rules support tagging.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws ResourceNotFoundException
     */
    ListTagsForResourceOutput listTagsForResource(ListTagsForResourceInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates or updates an alarm mute rule.
     *
     * <p>Alarm mute rules automatically mute alarm actions during predefined time windows. When a mute rule is active,
     * targeted alarms continue to evaluate metrics and transition between states, but their configured actions (such as
     * Amazon SNS notifications or Auto Scaling actions) are muted.
     *
     * <p>You can create mute rules with recurring schedules using <code>cron</code> expressions or one-time mute
     * windows using <code>at</code> expressions. Each mute rule can target up to 100 specific alarms by name.
     *
     * <p>If you specify a rule name that already exists, this operation updates the existing rule with the new
     * configuration.
     *
     * <p><b>Permissions</b>
     *
     * <p>To create or update a mute rule, you must have the <code>cloudwatch:PutAlarmMuteRule</code> permission on two
     * types of resources: the alarm mute rule resource itself, and each alarm that the rule targets.
     *
     * <p>For example, If you want to allow a user to create mute rules that target only specific alarms named
     * "WebServerCPUAlarm" and "DatabaseConnectionAlarm", you would create an IAM policy with one statement granting <code>
     * cloudwatch:PutAlarmMuteRule</code> on the alarm mute rule resource (<code>
     * arn:aws:cloudwatch:[REGION]:123456789012:alarm-mute-rule:&#42;</code>), and another statement granting <code>
     * cloudwatch:PutAlarmMuteRule</code> on the targeted alarm resources (<code>
     * arn:aws:cloudwatch:[REGION]:123456789012:alarm:WebServerCPUAlarm</code> and <code>
     * arn:aws:cloudwatch:[REGION]:123456789012:alarm:DatabaseConnectionAlarm</code>).
     *
     * <p>You can also use IAM policy conditions to allow targeting alarms based on resource tags. For example, you can
     * restrict users to create/update mute rules to only target alarms that have a specific tag key-value pair, such as
     * <code>Team=TeamA</code>.
     *
     * @throws LimitExceededFault
     */
    default PutAlarmMuteRuleOutput putAlarmMuteRule(PutAlarmMuteRuleInput input) {
        return putAlarmMuteRule(input, null);
    }

    /**
     * Creates or updates an alarm mute rule.
     *
     * <p>Alarm mute rules automatically mute alarm actions during predefined time windows. When a mute rule is active,
     * targeted alarms continue to evaluate metrics and transition between states, but their configured actions (such as
     * Amazon SNS notifications or Auto Scaling actions) are muted.
     *
     * <p>You can create mute rules with recurring schedules using <code>cron</code> expressions or one-time mute
     * windows using <code>at</code> expressions. Each mute rule can target up to 100 specific alarms by name.
     *
     * <p>If you specify a rule name that already exists, this operation updates the existing rule with the new
     * configuration.
     *
     * <p><b>Permissions</b>
     *
     * <p>To create or update a mute rule, you must have the <code>cloudwatch:PutAlarmMuteRule</code> permission on two
     * types of resources: the alarm mute rule resource itself, and each alarm that the rule targets.
     *
     * <p>For example, If you want to allow a user to create mute rules that target only specific alarms named
     * "WebServerCPUAlarm" and "DatabaseConnectionAlarm", you would create an IAM policy with one statement granting <code>
     * cloudwatch:PutAlarmMuteRule</code> on the alarm mute rule resource (<code>
     * arn:aws:cloudwatch:[REGION]:123456789012:alarm-mute-rule:&#42;</code>), and another statement granting <code>
     * cloudwatch:PutAlarmMuteRule</code> on the targeted alarm resources (<code>
     * arn:aws:cloudwatch:[REGION]:123456789012:alarm:WebServerCPUAlarm</code> and <code>
     * arn:aws:cloudwatch:[REGION]:123456789012:alarm:DatabaseConnectionAlarm</code>).
     *
     * <p>You can also use IAM policy conditions to allow targeting alarms based on resource tags. For example, you can
     * restrict users to create/update mute rules to only target alarms that have a specific tag key-value pair, such as
     * <code>Team=TeamA</code>.
     *
     * @throws LimitExceededFault
     */
    PutAlarmMuteRuleOutput putAlarmMuteRule(PutAlarmMuteRuleInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates an anomaly detection model for a CloudWatch metric. You can use the model to display a band of expected
     * normal values when the metric is graphed.
     *
     * <p>If you have enabled unified cross-account observability, and this account is a monitoring account, the metric
     * can be in the same account or a source account. You can specify the account ID in the object you specify in the <code>
     * SingleMetricAnomalyDetector</code> parameter.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Anomaly_Detection.html">CloudWatch Anomaly Detection</a>.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws LimitExceededException
     * @throws MissingRequiredParameterException
     */
    default PutAnomalyDetectorOutput putAnomalyDetector(PutAnomalyDetectorInput input) {
        return putAnomalyDetector(input, null);
    }

    /**
     * Creates an anomaly detection model for a CloudWatch metric. You can use the model to display a band of expected
     * normal values when the metric is graphed.
     *
     * <p>If you have enabled unified cross-account observability, and this account is a monitoring account, the metric
     * can be in the same account or a source account. You can specify the account ID in the object you specify in the <code>
     * SingleMetricAnomalyDetector</code> parameter.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Anomaly_Detection.html">CloudWatch Anomaly Detection</a>.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws LimitExceededException
     * @throws MissingRequiredParameterException
     */
    PutAnomalyDetectorOutput putAnomalyDetector(PutAnomalyDetectorInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates or updates a <i>composite alarm</i>. When you create a composite alarm, you specify a rule expression for
     * the alarm that takes into account the alarm states of other alarms that you have created. The composite alarm
     * goes into ALARM state only if all conditions of the rule are met.
     *
     * <p>The alarms specified in a composite alarm's rule expression can include metric alarms and other composite
     * alarms. The rule expression of a composite alarm can include as many as 100 underlying alarms. Any single alarm
     * can be included in the rule expressions of as many as 150 composite alarms.
     *
     * <p>Using composite alarms can reduce alarm noise. You can create multiple metric alarms, and also create a
     * composite alarm and set up alerts only for the composite alarm. For example, you could create a composite alarm
     * that goes into ALARM state only when more than one of the underlying metric alarms are in ALARM state.
     *
     * <p>Composite alarms can take the following actions:
     *
     * <ul>
     *   <li>
     *     Notify Amazon SNS topics.
     *   </li>
     *   <li>
     *     Invoke Lambda functions.
     *   </li>
     *   <li>
     *     Create OpsItems in Systems Manager Ops Center.
     *   </li>
     *   <li>
     *     Create incidents in Systems Manager Incident Manager.
     *   </li>
     * </ul>
     *
     * <p>It is possible to create a loop or cycle of composite alarms, where composite alarm A depends on composite
     * alarm B, and composite alarm B also depends on composite alarm A. In this scenario, you can't delete any
     * composite alarm that is part of the cycle because there is always still a composite alarm that depends on that
     * alarm that you want to delete.
     *
     * <p>To get out of such a situation, you must break the cycle by changing the rule of one of the composite alarms
     * in the cycle to remove a dependency that creates the cycle. The simplest change to make to break a cycle is to
     * change the <code>AlarmRule</code> of one of the alarms to <code>false</code>.
     *
     * <p>Additionally, the evaluation of composite alarms stops if CloudWatch detects a cycle in the evaluation path.
     *
     * <p>When this operation creates an alarm, the alarm state is immediately set to <code>INSUFFICIENT_DATA</code>.
     * The alarm is then evaluated and its state is set appropriately. Any actions associated with the new state are
     * then executed. For a composite alarm, this initial time after creation is the only time that the alarm can be in <code>
     * INSUFFICIENT_DATA</code> state.
     *
     * <p>When you update an existing alarm, its state is left unchanged, but the update completely overwrites the
     * previous configuration of the alarm.
     *
     * <p>To use this operation, you must be signed on with the <code>cloudwatch:PutCompositeAlarm</code> permission
     * that is scoped to <code>&#42;</code>. You can't create a composite alarms if your <code>cloudwatch:PutCompositeAlarm</code>
     * permission has a narrower scope.
     *
     * <p>If you are an IAM user, you must have <code>iam:CreateServiceLinkedRole</code> to create a composite alarm
     * that has Systems Manager OpsItem actions.
     *
     * @throws LimitExceededFault
     */
    default PutCompositeAlarmOutput putCompositeAlarm(PutCompositeAlarmInput input) {
        return putCompositeAlarm(input, null);
    }

    /**
     * Creates or updates a <i>composite alarm</i>. When you create a composite alarm, you specify a rule expression for
     * the alarm that takes into account the alarm states of other alarms that you have created. The composite alarm
     * goes into ALARM state only if all conditions of the rule are met.
     *
     * <p>The alarms specified in a composite alarm's rule expression can include metric alarms and other composite
     * alarms. The rule expression of a composite alarm can include as many as 100 underlying alarms. Any single alarm
     * can be included in the rule expressions of as many as 150 composite alarms.
     *
     * <p>Using composite alarms can reduce alarm noise. You can create multiple metric alarms, and also create a
     * composite alarm and set up alerts only for the composite alarm. For example, you could create a composite alarm
     * that goes into ALARM state only when more than one of the underlying metric alarms are in ALARM state.
     *
     * <p>Composite alarms can take the following actions:
     *
     * <ul>
     *   <li>
     *     Notify Amazon SNS topics.
     *   </li>
     *   <li>
     *     Invoke Lambda functions.
     *   </li>
     *   <li>
     *     Create OpsItems in Systems Manager Ops Center.
     *   </li>
     *   <li>
     *     Create incidents in Systems Manager Incident Manager.
     *   </li>
     * </ul>
     *
     * <p>It is possible to create a loop or cycle of composite alarms, where composite alarm A depends on composite
     * alarm B, and composite alarm B also depends on composite alarm A. In this scenario, you can't delete any
     * composite alarm that is part of the cycle because there is always still a composite alarm that depends on that
     * alarm that you want to delete.
     *
     * <p>To get out of such a situation, you must break the cycle by changing the rule of one of the composite alarms
     * in the cycle to remove a dependency that creates the cycle. The simplest change to make to break a cycle is to
     * change the <code>AlarmRule</code> of one of the alarms to <code>false</code>.
     *
     * <p>Additionally, the evaluation of composite alarms stops if CloudWatch detects a cycle in the evaluation path.
     *
     * <p>When this operation creates an alarm, the alarm state is immediately set to <code>INSUFFICIENT_DATA</code>.
     * The alarm is then evaluated and its state is set appropriately. Any actions associated with the new state are
     * then executed. For a composite alarm, this initial time after creation is the only time that the alarm can be in <code>
     * INSUFFICIENT_DATA</code> state.
     *
     * <p>When you update an existing alarm, its state is left unchanged, but the update completely overwrites the
     * previous configuration of the alarm.
     *
     * <p>To use this operation, you must be signed on with the <code>cloudwatch:PutCompositeAlarm</code> permission
     * that is scoped to <code>&#42;</code>. You can't create a composite alarms if your <code>cloudwatch:PutCompositeAlarm</code>
     * permission has a narrower scope.
     *
     * <p>If you are an IAM user, you must have <code>iam:CreateServiceLinkedRole</code> to create a composite alarm
     * that has Systems Manager OpsItem actions.
     *
     * @throws LimitExceededFault
     */
    PutCompositeAlarmOutput putCompositeAlarm(PutCompositeAlarmInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates a dashboard if it does not already exist, or updates an existing dashboard. If you update a dashboard,
     * the entire contents are replaced with what you specify here.
     *
     * <p>All dashboards in your account are global, not region-specific.
     *
     * <p>A simple way to create a dashboard using <code>PutDashboard</code> is to copy an existing dashboard. To copy
     * an existing dashboard using the console, you can load the dashboard and then use the View/edit source command in
     * the Actions menu to display the JSON block for that dashboard. Another way to copy a dashboard is to use <code>
     * GetDashboard</code>, and then use the data returned within <code>DashboardBody</code> as the template for the new
     * dashboard when you call <code>PutDashboard</code>.
     *
     * <p>When you create a dashboard with <code>PutDashboard</code>, a good practice is to add a text widget at the top
     * of the dashboard with a message that the dashboard was created by script and should not be changed in the
     * console. This message could also point console users to the location of the <code>DashboardBody</code> script or
     * the CloudFormation template used to create the dashboard.
     *
     * @throws ConflictException
     * @throws DashboardInvalidInputError
     * @throws InternalServiceFault
     */
    default PutDashboardOutput putDashboard(PutDashboardInput input) {
        return putDashboard(input, null);
    }

    /**
     * Creates a dashboard if it does not already exist, or updates an existing dashboard. If you update a dashboard,
     * the entire contents are replaced with what you specify here.
     *
     * <p>All dashboards in your account are global, not region-specific.
     *
     * <p>A simple way to create a dashboard using <code>PutDashboard</code> is to copy an existing dashboard. To copy
     * an existing dashboard using the console, you can load the dashboard and then use the View/edit source command in
     * the Actions menu to display the JSON block for that dashboard. Another way to copy a dashboard is to use <code>
     * GetDashboard</code>, and then use the data returned within <code>DashboardBody</code> as the template for the new
     * dashboard when you call <code>PutDashboard</code>.
     *
     * <p>When you create a dashboard with <code>PutDashboard</code>, a good practice is to add a text widget at the top
     * of the dashboard with a message that the dashboard was created by script and should not be changed in the
     * console. This message could also point console users to the location of the <code>DashboardBody</code> script or
     * the CloudFormation template used to create the dashboard.
     *
     * @throws ConflictException
     * @throws DashboardInvalidInputError
     * @throws InternalServiceFault
     */
    PutDashboardOutput putDashboard(PutDashboardInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates a Contributor Insights rule. Rules evaluate log events in a CloudWatch Logs log group, enabling you to
     * find contributor data for the log events in that log group. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights.html">Using Contributor
     * Insights to Analyze High-Cardinality Data</a>.
     *
     * <p>If you create a rule, delete it, and then re-create it with the same name, historical data from the first time
     * the rule was created might not be available.
     *
     * @throws InvalidParameterValueException
     * @throws LimitExceededException
     * @throws MissingRequiredParameterException
     */
    default PutInsightRuleOutput putInsightRule(PutInsightRuleInput input) {
        return putInsightRule(input, null);
    }

    /**
     * Creates a Contributor Insights rule. Rules evaluate log events in a CloudWatch Logs log group, enabling you to
     * find contributor data for the log events in that log group. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights.html">Using Contributor
     * Insights to Analyze High-Cardinality Data</a>.
     *
     * <p>If you create a rule, delete it, and then re-create it with the same name, historical data from the first time
     * the rule was created might not be available.
     *
     * @throws InvalidParameterValueException
     * @throws LimitExceededException
     * @throws MissingRequiredParameterException
     */
    PutInsightRuleOutput putInsightRule(PutInsightRuleInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates or updates a log alarm. A log alarm evaluates the results of a CloudWatch Logs scheduled query against
     * the configured threshold and comparison operator to determine its state.
     *
     * <p>When you create a log alarm, the operation creates a service-managed CloudWatch Logs scheduled query that runs
     * the query string you provide on the schedule you configure. Each scheduled query execution returns one or more
     * aggregated values determined by the <code>AggregationExpression</code>, and each aggregated value is compared
     * against the alarm <code>Threshold</code> to determine the alarm state. The alarm uses M-out-of-N evaluation: if <code>
     * QueryResultsToAlarm</code> out of the most recent <code>QueryResultsToEvaluate</code> query results breach the
     * threshold, the alarm transitions to <code>ALARM</code>.
     *
     * <p>Log alarms support the alarm states (<code>OK</code>, <code>ALARM</code>, <code>INSUFFICIENT_DATA</code>).
     * Configure transition actions using <code>OKActions</code>, <code>AlarmActions</code>, and <code>
     * InsufficientDataActions</code>.
     *
     * <p>If you call this operation with the name of an existing log alarm, the operation replaces the previous
     * configuration of that alarm.
     *
     * <p><b>Permissions</b>
     *
     * <p>To create or update a log alarm, you must have the <code>cloudwatch:PutLogAlarm</code> permission. The IAM
     * role specified in <code>ScheduledQueryRoleARN</code> must grant the CloudWatch Alarms service permission to
     * execute scheduled queries on the specified log groups. If you set <code>ActionLogLineCount</code>, the role
     * specified in <code>ActionLogLineRoleArn</code> must grant permission to retrieve log events for inclusion in
     * alarm notifications.
     *
     * @throws LimitExceededFault
     * @throws ResourceConflict
     */
    default PutLogAlarmOutput putLogAlarm(PutLogAlarmInput input) {
        return putLogAlarm(input, null);
    }

    /**
     * Creates or updates a log alarm. A log alarm evaluates the results of a CloudWatch Logs scheduled query against
     * the configured threshold and comparison operator to determine its state.
     *
     * <p>When you create a log alarm, the operation creates a service-managed CloudWatch Logs scheduled query that runs
     * the query string you provide on the schedule you configure. Each scheduled query execution returns one or more
     * aggregated values determined by the <code>AggregationExpression</code>, and each aggregated value is compared
     * against the alarm <code>Threshold</code> to determine the alarm state. The alarm uses M-out-of-N evaluation: if <code>
     * QueryResultsToAlarm</code> out of the most recent <code>QueryResultsToEvaluate</code> query results breach the
     * threshold, the alarm transitions to <code>ALARM</code>.
     *
     * <p>Log alarms support the alarm states (<code>OK</code>, <code>ALARM</code>, <code>INSUFFICIENT_DATA</code>).
     * Configure transition actions using <code>OKActions</code>, <code>AlarmActions</code>, and <code>
     * InsufficientDataActions</code>.
     *
     * <p>If you call this operation with the name of an existing log alarm, the operation replaces the previous
     * configuration of that alarm.
     *
     * <p><b>Permissions</b>
     *
     * <p>To create or update a log alarm, you must have the <code>cloudwatch:PutLogAlarm</code> permission. The IAM
     * role specified in <code>ScheduledQueryRoleARN</code> must grant the CloudWatch Alarms service permission to
     * execute scheduled queries on the specified log groups. If you set <code>ActionLogLineCount</code>, the role
     * specified in <code>ActionLogLineRoleArn</code> must grant permission to retrieve log events for inclusion in
     * alarm notifications.
     *
     * @throws LimitExceededFault
     * @throws ResourceConflict
     */
    PutLogAlarmOutput putLogAlarm(PutLogAlarmInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates a managed Contributor Insights rule for a specified Amazon Web Services resource. When you enable a
     * managed rule, you create a Contributor Insights rule that collects data from Amazon Web Services services. You
     * cannot edit these rules with <code>PutInsightRule</code>. The rules can be enabled, disabled, and deleted using <code>
     * EnableInsightRules</code>, <code>DisableInsightRules</code>, and <code>DeleteInsightRules</code>. If a previously
     * created managed rule is currently disabled, a subsequent call to this API will re-enable it. Use <code>
     * ListManagedInsightRules</code> to describe all available rules.
     *
     * <pre>{@code
     *     </p>
     *
     * }</pre>
     *
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default PutManagedInsightRulesOutput putManagedInsightRules(PutManagedInsightRulesInput input) {
        return putManagedInsightRules(input, null);
    }

    /**
     * Creates a managed Contributor Insights rule for a specified Amazon Web Services resource. When you enable a
     * managed rule, you create a Contributor Insights rule that collects data from Amazon Web Services services. You
     * cannot edit these rules with <code>PutInsightRule</code>. The rules can be enabled, disabled, and deleted using <code>
     * EnableInsightRules</code>, <code>DisableInsightRules</code>, and <code>DeleteInsightRules</code>. If a previously
     * created managed rule is currently disabled, a subsequent call to this API will re-enable it. Use <code>
     * ListManagedInsightRules</code> to describe all available rules.
     *
     * <pre>{@code
     *     </p>
     *
     * }</pre>
     *
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    PutManagedInsightRulesOutput putManagedInsightRules(PutManagedInsightRulesInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates or updates an alarm and associates it with the specified metric, metric math expression, anomaly
     * detection model, Metrics Insights query, or PromQL query. For more information about using a Metrics Insights
     * query for an alarm, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Create_Metrics_Insights_Alarm.html">Create alarms on Metrics Insights queries</a>.
     *
     * <p>Alarms based on anomaly detection models cannot have Auto Scaling actions.
     *
     * <p>When this operation creates an alarm, the alarm state is immediately set to <code>INSUFFICIENT_DATA</code>.
     * For PromQL alarms, the alarm state is instead immediately set to <code>OK</code>. The alarm is then evaluated and
     * its state is set appropriately. Any actions associated with the new state are then executed.
     *
     * <p>When you update an existing alarm, its state is left unchanged, but the update completely overwrites the
     * previous configuration of the alarm.
     *
     * <p>If you are an IAM user, you must have Amazon EC2 permissions for some alarm operations:
     *
     * <ul>
     *   <li>
     *     The <code>iam:CreateServiceLinkedRole</code> permission for all alarms with EC2 actions
     *   </li>
     *   <li>
     *     The <code>iam:CreateServiceLinkedRole</code> permissions to create an alarm with Systems Manager OpsItem
     *     or response plan actions.
     *   </li>
     * </ul>
     *
     * <p>The first time you create an alarm in the Amazon Web Services Management Console, the CLI, or by using the
     * PutMetricAlarm API, CloudWatch creates the necessary service-linked role for you. The service-linked roles are
     * called <code>AWSServiceRoleForCloudWatchEvents</code> and <code>AWSServiceRoleForCloudWatchAlarms_ActionSSM</code>
     * . For more information, see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_terms-and-concepts.html#iam-term-service-linked-role">Amazon Web Services service-linked role</a>.
     *
     * <p>Each <code>PutMetricAlarm</code> action has a maximum uncompressed payload of 120 KB.
     *
     * <p><b>Cross-account alarms</b>
     *
     * <p>You can set an alarm on metrics in the current account, or in another account. To create a cross-account alarm
     * that watches a metric in a different account, you must have completed the following pre-requisites:
     *
     * <ul>
     *   <li>
     *     The account where the metrics are located (the <i>sharing account</i>) must already have a sharing role
     *     named <b>CloudWatch-CrossAccountSharingRole</b>. If it does not already have this role, you must create
     *     it using the instructions in <b>Set up a sharing account</b> in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Cross-Account-Cross-Region.html#enable-cross-account-cross-Region"> Cross-account cross-Region CloudWatch
     *     console</a>. The policy for that role must grant access to the ID of the account where you are creating
     *     the alarm.
     *   </li>
     *   <li>
     *     The account where you are creating the alarm (the <i>monitoring account</i>) must already have a
     *     service-linked role named <b>AWSServiceRoleForCloudWatchCrossAccount</b> to allow CloudWatch to assume
     *     the sharing role in the sharing account. If it does not, you must create it following the directions in <b>
     *     Set up a monitoring account</b> in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Cross-Account-Cross-Region.html#enable-cross-account-cross-Region"> Cross-account cross-Region CloudWatch console</a>.
     *   </li>
     * </ul>
     *
     * @throws LimitExceededFault
     */
    default PutMetricAlarmOutput putMetricAlarm(PutMetricAlarmInput input) {
        return putMetricAlarm(input, null);
    }

    /**
     * Creates or updates an alarm and associates it with the specified metric, metric math expression, anomaly
     * detection model, Metrics Insights query, or PromQL query. For more information about using a Metrics Insights
     * query for an alarm, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Create_Metrics_Insights_Alarm.html">Create alarms on Metrics Insights queries</a>.
     *
     * <p>Alarms based on anomaly detection models cannot have Auto Scaling actions.
     *
     * <p>When this operation creates an alarm, the alarm state is immediately set to <code>INSUFFICIENT_DATA</code>.
     * For PromQL alarms, the alarm state is instead immediately set to <code>OK</code>. The alarm is then evaluated and
     * its state is set appropriately. Any actions associated with the new state are then executed.
     *
     * <p>When you update an existing alarm, its state is left unchanged, but the update completely overwrites the
     * previous configuration of the alarm.
     *
     * <p>If you are an IAM user, you must have Amazon EC2 permissions for some alarm operations:
     *
     * <ul>
     *   <li>
     *     The <code>iam:CreateServiceLinkedRole</code> permission for all alarms with EC2 actions
     *   </li>
     *   <li>
     *     The <code>iam:CreateServiceLinkedRole</code> permissions to create an alarm with Systems Manager OpsItem
     *     or response plan actions.
     *   </li>
     * </ul>
     *
     * <p>The first time you create an alarm in the Amazon Web Services Management Console, the CLI, or by using the
     * PutMetricAlarm API, CloudWatch creates the necessary service-linked role for you. The service-linked roles are
     * called <code>AWSServiceRoleForCloudWatchEvents</code> and <code>AWSServiceRoleForCloudWatchAlarms_ActionSSM</code>
     * . For more information, see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_terms-and-concepts.html#iam-term-service-linked-role">Amazon Web Services service-linked role</a>.
     *
     * <p>Each <code>PutMetricAlarm</code> action has a maximum uncompressed payload of 120 KB.
     *
     * <p><b>Cross-account alarms</b>
     *
     * <p>You can set an alarm on metrics in the current account, or in another account. To create a cross-account alarm
     * that watches a metric in a different account, you must have completed the following pre-requisites:
     *
     * <ul>
     *   <li>
     *     The account where the metrics are located (the <i>sharing account</i>) must already have a sharing role
     *     named <b>CloudWatch-CrossAccountSharingRole</b>. If it does not already have this role, you must create
     *     it using the instructions in <b>Set up a sharing account</b> in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Cross-Account-Cross-Region.html#enable-cross-account-cross-Region"> Cross-account cross-Region CloudWatch
     *     console</a>. The policy for that role must grant access to the ID of the account where you are creating
     *     the alarm.
     *   </li>
     *   <li>
     *     The account where you are creating the alarm (the <i>monitoring account</i>) must already have a
     *     service-linked role named <b>AWSServiceRoleForCloudWatchCrossAccount</b> to allow CloudWatch to assume
     *     the sharing role in the sharing account. If it does not, you must create it following the directions in <b>
     *     Set up a monitoring account</b> in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Cross-Account-Cross-Region.html#enable-cross-account-cross-Region"> Cross-account cross-Region CloudWatch console</a>.
     *   </li>
     * </ul>
     *
     * @throws LimitExceededFault
     */
    PutMetricAlarmOutput putMetricAlarm(PutMetricAlarmInput input, RequestOverrideConfig overrideConfig);

    /**
     * Publishes metric data to Amazon CloudWatch. CloudWatch associates the data with the specified metric. If the
     * specified metric does not exist, CloudWatch creates the metric. When CloudWatch creates a metric, it can take up
     * to fifteen minutes for the metric to appear in calls to <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_ListMetrics.html">ListMetrics</a>.
     *
     * <p>You can publish metrics with associated entity data (so that related telemetry can be found and viewed
     * together), or publish metric data by itself. To send entity data with your metrics, use the <code>
     * EntityMetricData</code> parameter. To send metrics without entity data, use the <code>MetricData</code>
     * parameter. The <code>EntityMetricData</code> structure includes <code>MetricData</code> structures for the metric
     * data.
     *
     * <p>You can publish either individual values in the <code>Value</code> field, or arrays of values and the number
     * of times each value occurred during the period by using the <code>Values</code> and <code>Counts</code> fields in
     * the <code>MetricData</code> structure. Using the <code>Values</code> and <code>Counts</code> method enables you
     * to publish up to 150 values per metric with one <code>PutMetricData</code> request, and supports retrieving
     * percentile statistics on this data.
     *
     * <p>Each <code>PutMetricData</code> request is limited to 1 MB in size for HTTP POST requests. You can send a
     * payload compressed by gzip. Each request is also limited to no more than 1000 different metrics (across both the <code>
     * MetricData</code> and <code>EntityMetricData</code> properties).
     *
     * <p>Although the <code>Value</code> parameter accepts numbers of type <code>Double</code>, CloudWatch rejects
     * values that are either too small or too large. Values must be in the range of -2^360 to 2^360. In addition,
     * special values (for example, NaN, +Infinity, -Infinity) are not supported.
     *
     * <p>You can use up to 30 dimensions per metric to further clarify what data the metric collects. Each dimension
     * consists of a Name and Value pair. For more information about specifying dimensions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html">Publishing Metrics</a>
     * in the <i>Amazon CloudWatch User Guide</i>.
     *
     * <p>You specify the time stamp to be associated with each data point. You can specify time stamps that are as much
     * as two weeks before the current date, and as much as 2 hours after the current day and time.
     *
     * <p>Data points with time stamps from 24 hours ago or longer can take at least 48 hours to become available for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">
     * GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a> from the time they are submitted. Data points with time stamps
     * between 3 and 24 hours ago can take as much as 2 hours to become available for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">
     * GetMetricStatistics</a>.
     *
     * <p>CloudWatch needs raw data points to calculate percentile statistics. If you publish data using a statistic set
     * instead, you can only retrieve percentile statistics for this data if one of the following conditions is true:
     *
     * <ul>
     *   <li>
     *     The <code>SampleCount</code> value of the statistic set is 1 and <code>Min</code>, <code>Max</code>, and <code>
     *     Sum</code> are all equal.
     *   </li>
     *   <li>
     *     The <code>Min</code> and <code>Max</code> are equal, and <code>Sum</code> is equal to <code>Min</code>
     *     multiplied by <code>SampleCount</code>.
     *   </li>
     * </ul>
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default PutMetricDataOutput putMetricData(PutMetricDataInput input) {
        return putMetricData(input, null);
    }

    /**
     * Publishes metric data to Amazon CloudWatch. CloudWatch associates the data with the specified metric. If the
     * specified metric does not exist, CloudWatch creates the metric. When CloudWatch creates a metric, it can take up
     * to fifteen minutes for the metric to appear in calls to <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_ListMetrics.html">ListMetrics</a>.
     *
     * <p>You can publish metrics with associated entity data (so that related telemetry can be found and viewed
     * together), or publish metric data by itself. To send entity data with your metrics, use the <code>
     * EntityMetricData</code> parameter. To send metrics without entity data, use the <code>MetricData</code>
     * parameter. The <code>EntityMetricData</code> structure includes <code>MetricData</code> structures for the metric
     * data.
     *
     * <p>You can publish either individual values in the <code>Value</code> field, or arrays of values and the number
     * of times each value occurred during the period by using the <code>Values</code> and <code>Counts</code> fields in
     * the <code>MetricData</code> structure. Using the <code>Values</code> and <code>Counts</code> method enables you
     * to publish up to 150 values per metric with one <code>PutMetricData</code> request, and supports retrieving
     * percentile statistics on this data.
     *
     * <p>Each <code>PutMetricData</code> request is limited to 1 MB in size for HTTP POST requests. You can send a
     * payload compressed by gzip. Each request is also limited to no more than 1000 different metrics (across both the <code>
     * MetricData</code> and <code>EntityMetricData</code> properties).
     *
     * <p>Although the <code>Value</code> parameter accepts numbers of type <code>Double</code>, CloudWatch rejects
     * values that are either too small or too large. Values must be in the range of -2^360 to 2^360. In addition,
     * special values (for example, NaN, +Infinity, -Infinity) are not supported.
     *
     * <p>You can use up to 30 dimensions per metric to further clarify what data the metric collects. Each dimension
     * consists of a Name and Value pair. For more information about specifying dimensions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html">Publishing Metrics</a>
     * in the <i>Amazon CloudWatch User Guide</i>.
     *
     * <p>You specify the time stamp to be associated with each data point. You can specify time stamps that are as much
     * as two weeks before the current date, and as much as 2 hours after the current day and time.
     *
     * <p>Data points with time stamps from 24 hours ago or longer can take at least 48 hours to become available for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">
     * GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a> from the time they are submitted. Data points with time stamps
     * between 3 and 24 hours ago can take as much as 2 hours to become available for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">
     * GetMetricStatistics</a>.
     *
     * <p>CloudWatch needs raw data points to calculate percentile statistics. If you publish data using a statistic set
     * instead, you can only retrieve percentile statistics for this data if one of the following conditions is true:
     *
     * <ul>
     *   <li>
     *     The <code>SampleCount</code> value of the statistic set is 1 and <code>Min</code>, <code>Max</code>, and <code>
     *     Sum</code> are all equal.
     *   </li>
     *   <li>
     *     The <code>Min</code> and <code>Max</code> are equal, and <code>Sum</code> is equal to <code>Min</code>
     *     multiplied by <code>SampleCount</code>.
     *   </li>
     * </ul>
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    PutMetricDataOutput putMetricData(PutMetricDataInput input, RequestOverrideConfig overrideConfig);

    /**
     * Creates or updates a metric stream. Metric streams can automatically stream CloudWatch metrics to Amazon Web
     * Services destinations, including Amazon S3, and to many third-party solutions.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Metric-Streams.html"> Using Metric Streams</a>.
     *
     * <p>To create a metric stream, you must be signed in to an account that has the <code>iam:PassRole</code>
     * permission and either the <code>CloudWatchFullAccess</code> policy or the <code>cloudwatch:PutMetricStream</code>
     * permission.
     *
     * <p>When you create or update a metric stream, you choose one of the following:
     *
     * <ul>
     *   <li>
     *     Stream metrics from all metric namespaces in the account.
     *   </li>
     *   <li>
     *     Stream metrics from all metric namespaces in the account, except for the namespaces that you list in <code>
     *     ExcludeFilters</code>.
     *   </li>
     *   <li>
     *     Stream metrics from only the metric namespaces that you list in <code>IncludeFilters</code>.
     *   </li>
     * </ul>
     *
     * <p>By default, a metric stream always sends the <code>MAX</code>, <code>MIN</code>, <code>SUM</code>, and <code>
     * SAMPLECOUNT</code> statistics for each metric that is streamed. You can use the <code>StatisticsConfigurations</code>
     * parameter to have the metric stream send additional statistics in the stream. Streaming additional statistics
     * incurs additional costs. For more information, see <a href="https://aws.amazon.com/cloudwatch/pricing/">Amazon CloudWatch Pricing</a>.
     *
     * <p>When you use <code>PutMetricStream</code> to create a new metric stream, the stream is created in the <code>
     * running</code> state. If you use it to update an existing stream, the state of the stream is not changed.
     *
     * <p>If you are using CloudWatch cross-account observability and you create a metric stream in a monitoring
     * account, you can choose whether to include metrics from source accounts in the stream. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Unified-Cross-Account.html">
     * CloudWatch cross-account observability</a>.
     *
     * @throws ConcurrentModificationException
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default PutMetricStreamOutput putMetricStream(PutMetricStreamInput input) {
        return putMetricStream(input, null);
    }

    /**
     * Creates or updates a metric stream. Metric streams can automatically stream CloudWatch metrics to Amazon Web
     * Services destinations, including Amazon S3, and to many third-party solutions.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Metric-Streams.html"> Using Metric Streams</a>.
     *
     * <p>To create a metric stream, you must be signed in to an account that has the <code>iam:PassRole</code>
     * permission and either the <code>CloudWatchFullAccess</code> policy or the <code>cloudwatch:PutMetricStream</code>
     * permission.
     *
     * <p>When you create or update a metric stream, you choose one of the following:
     *
     * <ul>
     *   <li>
     *     Stream metrics from all metric namespaces in the account.
     *   </li>
     *   <li>
     *     Stream metrics from all metric namespaces in the account, except for the namespaces that you list in <code>
     *     ExcludeFilters</code>.
     *   </li>
     *   <li>
     *     Stream metrics from only the metric namespaces that you list in <code>IncludeFilters</code>.
     *   </li>
     * </ul>
     *
     * <p>By default, a metric stream always sends the <code>MAX</code>, <code>MIN</code>, <code>SUM</code>, and <code>
     * SAMPLECOUNT</code> statistics for each metric that is streamed. You can use the <code>StatisticsConfigurations</code>
     * parameter to have the metric stream send additional statistics in the stream. Streaming additional statistics
     * incurs additional costs. For more information, see <a href="https://aws.amazon.com/cloudwatch/pricing/">Amazon CloudWatch Pricing</a>.
     *
     * <p>When you use <code>PutMetricStream</code> to create a new metric stream, the stream is created in the <code>
     * running</code> state. If you use it to update an existing stream, the state of the stream is not changed.
     *
     * <p>If you are using CloudWatch cross-account observability and you create a metric stream in a monitoring
     * account, you can choose whether to include metrics from source accounts in the stream. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Unified-Cross-Account.html">
     * CloudWatch cross-account observability</a>.
     *
     * @throws ConcurrentModificationException
     * @throws InternalServiceFault
     * @throws InvalidParameterCombinationException
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    PutMetricStreamOutput putMetricStream(PutMetricStreamInput input, RequestOverrideConfig overrideConfig);

    /**
     * Temporarily sets the state of an alarm for testing purposes. When the updated state differs from the previous
     * value, the action configured for the appropriate state is invoked. For example, if your alarm is configured to
     * send an Amazon SNS message when an alarm is triggered, temporarily changing the alarm state to <code>ALARM</code>
     * sends an SNS message.
     *
     * <p>Metric alarms returns to their actual state quickly, often within seconds. Because the metric alarm state
     * change happens quickly, it is typically only visible in the alarm's <b>History</b> tab in the Amazon CloudWatch
     * console or through <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DescribeAlarmHistory.html">DescribeAlarmHistory</a>.
     *
     * <p>If you use <code>SetAlarmState</code> on a composite alarm, the composite alarm is not guaranteed to return to
     * its actual state. It returns to its actual state only once any of its children alarms change state. It is also
     * reevaluated if you update its configuration.
     *
     * <p>If an alarm triggers EC2 Auto Scaling policies or application Auto Scaling policies, you must include
     * information in the <code>StateReasonData</code> parameter to enable the policy to take the correct action.
     *
     * @throws InvalidFormatFault
     * @throws ResourceNotFound
     */
    default SetAlarmStateOutput setAlarmState(SetAlarmStateInput input) {
        return setAlarmState(input, null);
    }

    /**
     * Temporarily sets the state of an alarm for testing purposes. When the updated state differs from the previous
     * value, the action configured for the appropriate state is invoked. For example, if your alarm is configured to
     * send an Amazon SNS message when an alarm is triggered, temporarily changing the alarm state to <code>ALARM</code>
     * sends an SNS message.
     *
     * <p>Metric alarms returns to their actual state quickly, often within seconds. Because the metric alarm state
     * change happens quickly, it is typically only visible in the alarm's <b>History</b> tab in the Amazon CloudWatch
     * console or through <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DescribeAlarmHistory.html">DescribeAlarmHistory</a>.
     *
     * <p>If you use <code>SetAlarmState</code> on a composite alarm, the composite alarm is not guaranteed to return to
     * its actual state. It returns to its actual state only once any of its children alarms change state. It is also
     * reevaluated if you update its configuration.
     *
     * <p>If an alarm triggers EC2 Auto Scaling policies or application Auto Scaling policies, you must include
     * information in the <code>StateReasonData</code> parameter to enable the policy to take the correct action.
     *
     * @throws InvalidFormatFault
     * @throws ResourceNotFound
     */
    SetAlarmStateOutput setAlarmState(SetAlarmStateInput input, RequestOverrideConfig overrideConfig);

    /**
     * Starts the streaming of metrics for one or more of your metric streams.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default StartMetricStreamsOutput startMetricStreams(StartMetricStreamsInput input) {
        return startMetricStreams(input, null);
    }

    /**
     * Starts the streaming of metrics for one or more of your metric streams.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    StartMetricStreamsOutput startMetricStreams(StartMetricStreamsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Enables enrichment and PromQL access for CloudWatch vended metrics for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">supported Amazon Web Services resources</a>
     * in the account. Once enabled, metrics that contain a resource identifier dimension (for example, EC2 <code>
     * CPUUtilization</code> with an <code>InstanceId</code> dimension) are enriched with resource ARN and resource tag
     * labels and become queryable using PromQL.
     *
     * <p>Before calling this operation, you must enable resource tags on telemetry for your account. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/EnableResourceTagsOnTelemetry.html">Enable resource tags on telemetry</a>.
     */
    default StartOTelEnrichmentOutput startOTelEnrichment(StartOTelEnrichmentInput input) {
        return startOTelEnrichment(input, null);
    }

    /**
     * Enables enrichment and PromQL access for CloudWatch vended metrics for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">supported Amazon Web Services resources</a>
     * in the account. Once enabled, metrics that contain a resource identifier dimension (for example, EC2 <code>
     * CPUUtilization</code> with an <code>InstanceId</code> dimension) are enriched with resource ARN and resource tag
     * labels and become queryable using PromQL.
     *
     * <p>Before calling this operation, you must enable resource tags on telemetry for your account. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/EnableResourceTagsOnTelemetry.html">Enable resource tags on telemetry</a>.
     */
    StartOTelEnrichmentOutput startOTelEnrichment(StartOTelEnrichmentInput input, RequestOverrideConfig overrideConfig);

    /**
     * Stops the streaming of metrics for one or more of your metric streams.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    default StopMetricStreamsOutput stopMetricStreams(StopMetricStreamsInput input) {
        return stopMetricStreams(input, null);
    }

    /**
     * Stops the streaming of metrics for one or more of your metric streams.
     *
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws MissingRequiredParameterException
     */
    StopMetricStreamsOutput stopMetricStreams(StopMetricStreamsInput input, RequestOverrideConfig overrideConfig);

    /**
     * Disables enrichment and PromQL access for CloudWatch vended metrics for <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">supported
     * Amazon Web Services resources</a> in the account. After disabling, these metrics are no longer enriched with
     * resource ARN and resource tag labels, and cannot be queried using PromQL.
     */
    default StopOTelEnrichmentOutput stopOTelEnrichment(StopOTelEnrichmentInput input) {
        return stopOTelEnrichment(input, null);
    }

    /**
     * Disables enrichment and PromQL access for CloudWatch vended metrics for <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">supported
     * Amazon Web Services resources</a> in the account. After disabling, these metrics are no longer enriched with
     * resource ARN and resource tag labels, and cannot be queried using PromQL.
     */
    StopOTelEnrichmentOutput stopOTelEnrichment(StopOTelEnrichmentInput input, RequestOverrideConfig overrideConfig);

    /**
     * Assigns one or more tags (key-value pairs) to the specified CloudWatch resource. Currently, the only CloudWatch
     * resources that can be tagged are alarms, dashboards, metric streams and Contributor Insights rules.
     *
     * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions by
     * granting a user permission to access or change only resources with certain tag values.
     *
     * <p>Tags don't have any semantic meaning to Amazon Web Services and are interpreted strictly as strings of
     * characters.
     *
     * <p>You can use the <code>TagResource</code> action with an alarm that already has tags. If you specify a new tag
     * key for the alarm, this tag is appended to the list of tags associated with the alarm. If you specify a tag key
     * that is already associated with the alarm, the new tag value that you specify replaces the previous value for
     * that tag.
     *
     * <p>You can associate as many as 50 tags with a CloudWatch resource.
     *
     * @throws ConcurrentModificationException
     * @throws ConflictException
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws ResourceNotFoundException
     */
    default TagResourceOutput tagResource(TagResourceInput input) {
        return tagResource(input, null);
    }

    /**
     * Assigns one or more tags (key-value pairs) to the specified CloudWatch resource. Currently, the only CloudWatch
     * resources that can be tagged are alarms, dashboards, metric streams and Contributor Insights rules.
     *
     * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions by
     * granting a user permission to access or change only resources with certain tag values.
     *
     * <p>Tags don't have any semantic meaning to Amazon Web Services and are interpreted strictly as strings of
     * characters.
     *
     * <p>You can use the <code>TagResource</code> action with an alarm that already has tags. If you specify a new tag
     * key for the alarm, this tag is appended to the list of tags associated with the alarm. If you specify a tag key
     * that is already associated with the alarm, the new tag value that you specify replaces the previous value for
     * that tag.
     *
     * <p>You can associate as many as 50 tags with a CloudWatch resource.
     *
     * @throws ConcurrentModificationException
     * @throws ConflictException
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws ResourceNotFoundException
     */
    TagResourceOutput tagResource(TagResourceInput input, RequestOverrideConfig overrideConfig);

    /**
     * Removes one or more tags from the specified resource. Currently, alarms, dashboards, metric streams and
     * Contributor Insights rules support tagging.
     *
     * @throws ConcurrentModificationException
     * @throws ConflictException
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws ResourceNotFoundException
     */
    default UntagResourceOutput untagResource(UntagResourceInput input) {
        return untagResource(input, null);
    }

    /**
     * Removes one or more tags from the specified resource. Currently, alarms, dashboards, metric streams and
     * Contributor Insights rules support tagging.
     *
     * @throws ConcurrentModificationException
     * @throws ConflictException
     * @throws InternalServiceFault
     * @throws InvalidParameterValueException
     * @throws ResourceNotFoundException
     */
    UntagResourceOutput untagResource(UntagResourceInput input, RequestOverrideConfig overrideConfig);

    /**
     * Create a new {@link CloudWatchWaiter} instance that uses this client for polling.
     *
     * @return new {@link CloudWatchWaiter} instance.
     */
    CloudWatchWaiter waiter();

    /**
     * @return Configuration in use by client.
     */
    ClientConfig config();

    /**
     * Create a Builder for {@link CloudWatchClient}.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create a {@link RequestOverrideConfig} builder for this client.
     */
    static RequestOverrideBuilder requestOverrideBuilder() {
        return new RequestOverrideBuilder();
    }

    /**
     * Builder for {@link CloudWatchClient}.
     */
    final class Builder extends Client.Builder<CloudWatchClient, Builder> {
        private final AwsCredentialChainPlugin awsCredentialChainPlugin = new AwsCredentialChainPlugin();
        private final List<ClientPlugin> defaultPlugins = List.of(awsCredentialChainPlugin);

        private static final ProtocolSettings protocolSettings = ProtocolSettings.builder()
                .service(ShapeId.from("com.amazonaws.cloudwatch#GraniteServiceVersion20100801"))
                .serviceVersion("2010-08-01")
                .serviceSchema(CloudWatchApiService.instance().schema())
                .build();
        private static final AwsQueryTrait protocolTrait = new AwsQueryTrait();

        private static final SigV4Trait sigv4Scheme = (SigV4Trait) new SigV4Trait.Provider().createTrait(
            ShapeId.from("aws.auth#sigv4"),
            Node.objectNodeBuilder()
                .withMember("name", "monitoring")
                .build()
        );
        private static final AuthSchemeFactory<SigV4Trait> sigv4SchemeFactory = new SigV4AuthScheme.Factory();

        private Builder() {
            configBuilder()
                .putSupportedAuthSchemes(sigv4SchemeFactory.createAuthScheme(sigv4Scheme))
                .service(CloudWatchApiService.instance());
        }

        @Override
        public CloudWatchClient build() {
            for (var plugin : defaultPlugins) {
                addPlugin(plugin);
            }
            if (configBuilder().protocol() == null) {
                configBuilder().protocol(new AwsQueryClientProtocol.Factory().createProtocol(protocolSettings, protocolTrait));
            }
            if (configBuilder().transport() == null) {
                configBuilder().transport(new JavaHttpClientTransport.Factory().createTransport(Document.EMPTY_MAP, Document.EMPTY_MAP));
            }
            try (var stream = getClass().getResourceAsStream("/META-INF/endpoints/GraniteServiceVersion20100801.bdd")) {
                var bytecode = new RulesEngineBuilder().load(stream.readAllBytes());
                putConfig(RulesEngineSettings.BYTECODE, bytecode);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load BDD bytecode binary file", e);
            }

            return new CloudWatchClientImpl(this);
        }
    }

    /**
     * Builder used to create a {@link RequestOverrideConfig} for {@link CloudWatchClient} operations.
     */
    final class RequestOverrideBuilder extends RequestOverrideConfig.OverrideBuilder<RequestOverrideBuilder> {}
}
