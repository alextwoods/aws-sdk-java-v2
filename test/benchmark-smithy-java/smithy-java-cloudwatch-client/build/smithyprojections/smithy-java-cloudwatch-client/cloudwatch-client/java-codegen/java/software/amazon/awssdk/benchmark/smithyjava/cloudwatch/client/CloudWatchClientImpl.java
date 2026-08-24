package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.client;

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.AssociateDatasetKmsKey;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.AssociateDatasetKmsKeyInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.AssociateDatasetKmsKeyOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmMuteRule;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmMuteRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmMuteRuleOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarms;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAlarmsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAnomalyDetector;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAnomalyDetectorInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteAnomalyDetectorOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteDashboards;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteDashboardsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteDashboardsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteInsightRules;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteMetricStream;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteMetricStreamInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DeleteMetricStreamOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmContributors;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmContributorsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmContributorsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmHistory;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmHistoryInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmHistoryOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarms;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmsForMetric;
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
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableAlarmActions;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableAlarmActionsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableAlarmActionsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableInsightRules;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisableInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisassociateDatasetKmsKey;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisassociateDatasetKmsKeyInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DisassociateDatasetKmsKeyOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableAlarmActions;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableAlarmActionsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableAlarmActionsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableInsightRules;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.EnableInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetAlarmMuteRule;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetAlarmMuteRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetAlarmMuteRuleOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDashboard;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDashboardInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDashboardOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDataset;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDatasetInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetDatasetOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetInsightRuleReport;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetInsightRuleReportInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetInsightRuleReportOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricData;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricDataInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricDataOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStatistics;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStatisticsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStatisticsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStream;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStreamInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricStreamOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricWidgetImage;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricWidgetImageInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricWidgetImageOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetOTelEnrichment;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetOTelEnrichmentInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetOTelEnrichmentOutput;
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
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListTagsForResource;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListTagsForResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.ListTagsForResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAlarmMuteRule;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAlarmMuteRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAlarmMuteRuleOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAnomalyDetector;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAnomalyDetectorInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutAnomalyDetectorOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutCompositeAlarm;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutCompositeAlarmInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutCompositeAlarmOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutDashboard;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutDashboardInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutDashboardOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutInsightRule;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutInsightRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutInsightRuleOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutLogAlarm;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutLogAlarmInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutLogAlarmOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutManagedInsightRules;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutManagedInsightRulesInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutManagedInsightRulesOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricAlarm;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricAlarmInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricAlarmOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricData;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricDataInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricDataOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricStream;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricStreamInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricStreamOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.SetAlarmState;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.SetAlarmStateInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.SetAlarmStateOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartMetricStreams;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartMetricStreamsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartMetricStreamsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartOTelEnrichment;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartOTelEnrichmentInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StartOTelEnrichmentOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopMetricStreams;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopMetricStreamsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopMetricStreamsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopOTelEnrichment;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopOTelEnrichmentInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StopOTelEnrichmentOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.TagResource;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.TagResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.TagResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.UntagResource;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.UntagResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.UntagResourceOutput;
import software.amazon.smithy.java.client.core.Client;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.core.VersionCheck;
import software.amazon.smithy.java.versionspi.ModuleVersion;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
final class CloudWatchClientImpl extends Client implements CloudWatchClient {

    private static final ModuleVersion CODEGEN_VERSION = new ModuleVersion("codegen", 1, 5, 1);

    CloudWatchClientImpl(CloudWatchClient.Builder builder) {
        super(builder);
        VersionCheck.check(CODEGEN_VERSION);
    }

    @Override
    public AssociateDatasetKmsKeyOutput associateDatasetKmsKey(AssociateDatasetKmsKeyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, AssociateDatasetKmsKey.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteAlarmMuteRuleOutput deleteAlarmMuteRule(DeleteAlarmMuteRuleInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteAlarmMuteRule.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteAlarmsOutput deleteAlarms(DeleteAlarmsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteAlarms.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteAnomalyDetectorOutput deleteAnomalyDetector(DeleteAnomalyDetectorInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteAnomalyDetector.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteDashboardsOutput deleteDashboards(DeleteDashboardsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteDashboards.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteInsightRulesOutput deleteInsightRules(DeleteInsightRulesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteInsightRules.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteMetricStreamOutput deleteMetricStream(DeleteMetricStreamInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteMetricStream.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeAlarmContributorsOutput describeAlarmContributors(DescribeAlarmContributorsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeAlarmContributors.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeAlarmHistoryOutput describeAlarmHistory(DescribeAlarmHistoryInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeAlarmHistory.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeAlarmsOutput describeAlarms(DescribeAlarmsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeAlarms.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeAlarmsForMetricOutput describeAlarmsForMetric(DescribeAlarmsForMetricInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeAlarmsForMetric.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeAnomalyDetectorsOutput describeAnomalyDetectors(DescribeAnomalyDetectorsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeAnomalyDetectors.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeInsightRulesOutput describeInsightRules(DescribeInsightRulesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeInsightRules.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DisableAlarmActionsOutput disableAlarmActions(DisableAlarmActionsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DisableAlarmActions.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DisableInsightRulesOutput disableInsightRules(DisableInsightRulesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DisableInsightRules.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DisassociateDatasetKmsKeyOutput disassociateDatasetKmsKey(DisassociateDatasetKmsKeyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DisassociateDatasetKmsKey.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public EnableAlarmActionsOutput enableAlarmActions(EnableAlarmActionsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, EnableAlarmActions.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public EnableInsightRulesOutput enableInsightRules(EnableInsightRulesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, EnableInsightRules.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetAlarmMuteRuleOutput getAlarmMuteRule(GetAlarmMuteRuleInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetAlarmMuteRule.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetDashboardOutput getDashboard(GetDashboardInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetDashboard.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetDatasetOutput getDataset(GetDatasetInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetDataset.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetInsightRuleReportOutput getInsightRuleReport(GetInsightRuleReportInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetInsightRuleReport.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetMetricDataOutput getMetricData(GetMetricDataInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetMetricData.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetMetricStatisticsOutput getMetricStatistics(GetMetricStatisticsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetMetricStatistics.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetMetricStreamOutput getMetricStream(GetMetricStreamInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetMetricStream.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetMetricWidgetImageOutput getMetricWidgetImage(GetMetricWidgetImageInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetMetricWidgetImage.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetOTelEnrichmentOutput getOTelEnrichment(GetOTelEnrichmentInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetOTelEnrichment.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListAlarmMuteRulesOutput listAlarmMuteRules(ListAlarmMuteRulesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListAlarmMuteRules.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListDashboardsOutput listDashboards(ListDashboardsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListDashboards.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListManagedInsightRulesOutput listManagedInsightRules(ListManagedInsightRulesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListManagedInsightRules.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListMetricsOutput listMetrics(ListMetricsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListMetrics.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListMetricStreamsOutput listMetricStreams(ListMetricStreamsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListMetricStreams.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListTagsForResourceOutput listTagsForResource(ListTagsForResourceInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListTagsForResource.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutAlarmMuteRuleOutput putAlarmMuteRule(PutAlarmMuteRuleInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutAlarmMuteRule.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutAnomalyDetectorOutput putAnomalyDetector(PutAnomalyDetectorInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutAnomalyDetector.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutCompositeAlarmOutput putCompositeAlarm(PutCompositeAlarmInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutCompositeAlarm.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutDashboardOutput putDashboard(PutDashboardInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutDashboard.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutInsightRuleOutput putInsightRule(PutInsightRuleInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutInsightRule.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutLogAlarmOutput putLogAlarm(PutLogAlarmInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutLogAlarm.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutManagedInsightRulesOutput putManagedInsightRules(PutManagedInsightRulesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutManagedInsightRules.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutMetricAlarmOutput putMetricAlarm(PutMetricAlarmInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutMetricAlarm.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutMetricDataOutput putMetricData(PutMetricDataInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutMetricData.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutMetricStreamOutput putMetricStream(PutMetricStreamInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutMetricStream.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public SetAlarmStateOutput setAlarmState(SetAlarmStateInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, SetAlarmState.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public StartMetricStreamsOutput startMetricStreams(StartMetricStreamsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, StartMetricStreams.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public StartOTelEnrichmentOutput startOTelEnrichment(StartOTelEnrichmentInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, StartOTelEnrichment.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public StopMetricStreamsOutput stopMetricStreams(StopMetricStreamsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, StopMetricStreams.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public StopOTelEnrichmentOutput stopOTelEnrichment(StopOTelEnrichmentInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, StopOTelEnrichment.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public TagResourceOutput tagResource(TagResourceInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, TagResource.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UntagResourceOutput untagResource(UntagResourceInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UntagResource.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CloudWatchWaiter waiter() {
        return new CloudWatchWaiter(this);
    }

}
