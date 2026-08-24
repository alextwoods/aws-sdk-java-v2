package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.function.BiConsumer;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;


/**
 * Defines shared serialization and deserialization methods for map and list shapes.
 */
final class SharedSerde {

    static final class TagKeyListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final TagKeyListSerializer INSTANCE = new TagKeyListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.TAG_KEY_LIST.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeTagKeyList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TagKeyList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TagKeyList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final TagKeyList$MemberDeserializer INSTANCE = new TagKeyList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.TAG_KEY_LIST.listMember()));
        }
    }

    static final class MetricStreamNamesSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final MetricStreamNamesSerializer INSTANCE = new MetricStreamNamesSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_STREAM_NAMES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeMetricStreamNames(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricStreamNames$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricStreamNames$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final MetricStreamNames$MemberDeserializer INSTANCE = new MetricStreamNames$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.METRIC_STREAM_NAMES.listMember()));
        }
    }

    static final class ManagedRulesSerializer implements BiConsumer<List<ManagedRule>, ShapeSerializer> {
        static final ManagedRulesSerializer INSTANCE = new ManagedRulesSerializer();

        @Override
        public void accept(List<ManagedRule> values, ShapeSerializer serializer) {
            var $m = Schemas.MANAGED_RULES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<ManagedRule> deserializeManagedRules(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ManagedRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ManagedRules$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ManagedRules$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ManagedRule>> {
        static final ManagedRules$MemberDeserializer INSTANCE = new ManagedRules$MemberDeserializer();

        @Override
        public void accept(List<ManagedRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ManagedRule.builder().deserializeMember(deserializer, Schemas.MANAGED_RULES.listMember()).build());
        }
    }

    static final class MetricStreamEntriesSerializer implements BiConsumer<List<MetricStreamEntry>, ShapeSerializer> {
        static final MetricStreamEntriesSerializer INSTANCE = new MetricStreamEntriesSerializer();

        @Override
        public void accept(List<MetricStreamEntry> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_STREAM_ENTRIES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MetricStreamEntry> deserializeMetricStreamEntries(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricStreamEntry> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricStreamEntries$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricStreamEntries$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricStreamEntry>> {
        static final MetricStreamEntries$MemberDeserializer INSTANCE = new MetricStreamEntries$MemberDeserializer();

        @Override
        public void accept(List<MetricStreamEntry> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricStreamEntry.builder().deserializeMember(deserializer, Schemas.METRIC_STREAM_ENTRIES.listMember()).build());
        }
    }

    static final class OwningAccountsSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final OwningAccountsSerializer INSTANCE = new OwningAccountsSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.OWNING_ACCOUNTS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeOwningAccounts(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, OwningAccounts$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class OwningAccounts$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final OwningAccounts$MemberDeserializer INSTANCE = new OwningAccounts$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.OWNING_ACCOUNTS.listMember()));
        }
    }

    static final class MetricsSerializer implements BiConsumer<List<Metric>, ShapeSerializer> {
        static final MetricsSerializer INSTANCE = new MetricsSerializer();

        @Override
        public void accept(List<Metric> values, ShapeSerializer serializer) {
            var $m = Schemas.METRICS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<Metric> deserializeMetrics(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Metric> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Metrics$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Metrics$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Metric>> {
        static final Metrics$MemberDeserializer INSTANCE = new Metrics$MemberDeserializer();

        @Override
        public void accept(List<Metric> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Metric.builder().deserializeMember(deserializer, Schemas.METRICS.listMember()).build());
        }
    }

    static final class ManagedRuleDescriptionsSerializer implements BiConsumer<List<ManagedRuleDescription>, ShapeSerializer> {
        static final ManagedRuleDescriptionsSerializer INSTANCE = new ManagedRuleDescriptionsSerializer();

        @Override
        public void accept(List<ManagedRuleDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.MANAGED_RULE_DESCRIPTIONS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<ManagedRuleDescription> deserializeManagedRuleDescriptions(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ManagedRuleDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ManagedRuleDescriptions$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ManagedRuleDescriptions$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ManagedRuleDescription>> {
        static final ManagedRuleDescriptions$MemberDeserializer INSTANCE = new ManagedRuleDescriptions$MemberDeserializer();

        @Override
        public void accept(List<ManagedRuleDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ManagedRuleDescription.builder().deserializeMember(deserializer, Schemas.MANAGED_RULE_DESCRIPTIONS.listMember()).build());
        }
    }

    static final class MetricStreamStatisticsConfigurationsSerializer implements BiConsumer<List<MetricStreamStatisticsConfiguration>, ShapeSerializer> {
        static final MetricStreamStatisticsConfigurationsSerializer INSTANCE = new MetricStreamStatisticsConfigurationsSerializer();

        @Override
        public void accept(List<MetricStreamStatisticsConfiguration> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_STREAM_STATISTICS_CONFIGURATIONS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MetricStreamStatisticsConfiguration> deserializeMetricStreamStatisticsConfigurations(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricStreamStatisticsConfiguration> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricStreamStatisticsConfigurations$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricStreamStatisticsConfigurations$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricStreamStatisticsConfiguration>> {
        static final MetricStreamStatisticsConfigurations$MemberDeserializer INSTANCE = new MetricStreamStatisticsConfigurations$MemberDeserializer();

        @Override
        public void accept(List<MetricStreamStatisticsConfiguration> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricStreamStatisticsConfiguration.builder().deserializeMember(deserializer, Schemas.METRIC_STREAM_STATISTICS_CONFIGURATIONS.listMember()).build());
        }
    }

    static final class MetricStreamStatisticsIncludeMetricsSerializer implements BiConsumer<List<MetricStreamStatisticsMetric>, ShapeSerializer> {
        static final MetricStreamStatisticsIncludeMetricsSerializer INSTANCE = new MetricStreamStatisticsIncludeMetricsSerializer();

        @Override
        public void accept(List<MetricStreamStatisticsMetric> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_STREAM_STATISTICS_INCLUDE_METRICS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MetricStreamStatisticsMetric> deserializeMetricStreamStatisticsIncludeMetrics(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricStreamStatisticsMetric> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricStreamStatisticsIncludeMetrics$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricStreamStatisticsIncludeMetrics$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricStreamStatisticsMetric>> {
        static final MetricStreamStatisticsIncludeMetrics$MemberDeserializer INSTANCE = new MetricStreamStatisticsIncludeMetrics$MemberDeserializer();

        @Override
        public void accept(List<MetricStreamStatisticsMetric> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricStreamStatisticsMetric.builder().deserializeMember(deserializer, Schemas.METRIC_STREAM_STATISTICS_INCLUDE_METRICS.listMember()).build());
        }
    }

    static final class MetricStreamStatisticsAdditionalStatisticsSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final MetricStreamStatisticsAdditionalStatisticsSerializer INSTANCE = new MetricStreamStatisticsAdditionalStatisticsSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_STREAM_STATISTICS_ADDITIONAL_STATISTICS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeMetricStreamStatisticsAdditionalStatistics(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricStreamStatisticsAdditionalStatistics$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricStreamStatisticsAdditionalStatistics$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final MetricStreamStatisticsAdditionalStatistics$MemberDeserializer INSTANCE = new MetricStreamStatisticsAdditionalStatistics$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.METRIC_STREAM_STATISTICS_ADDITIONAL_STATISTICS.listMember()));
        }
    }

    static final class MetricStreamFiltersSerializer implements BiConsumer<List<MetricStreamFilter>, ShapeSerializer> {
        static final MetricStreamFiltersSerializer INSTANCE = new MetricStreamFiltersSerializer();

        @Override
        public void accept(List<MetricStreamFilter> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_STREAM_FILTERS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MetricStreamFilter> deserializeMetricStreamFilters(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricStreamFilter> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricStreamFilters$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricStreamFilters$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricStreamFilter>> {
        static final MetricStreamFilters$MemberDeserializer INSTANCE = new MetricStreamFilters$MemberDeserializer();

        @Override
        public void accept(List<MetricStreamFilter> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricStreamFilter.builder().deserializeMember(deserializer, Schemas.METRIC_STREAM_FILTERS.listMember()).build());
        }
    }

    static final class MetricStreamFilterMetricNamesSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final MetricStreamFilterMetricNamesSerializer INSTANCE = new MetricStreamFilterMetricNamesSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_STREAM_FILTER_METRIC_NAMES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeMetricStreamFilterMetricNames(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricStreamFilterMetricNames$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricStreamFilterMetricNames$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final MetricStreamFilterMetricNames$MemberDeserializer INSTANCE = new MetricStreamFilterMetricNames$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.METRIC_STREAM_FILTER_METRIC_NAMES.listMember()));
        }
    }

    static final class StatisticsSerializer implements BiConsumer<List<Statistic>, ShapeSerializer> {
        static final StatisticsSerializer INSTANCE = new StatisticsSerializer();

        @Override
        public void accept(List<Statistic> values, ShapeSerializer serializer) {
            var $m = Schemas.STATISTICS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value.getValue());
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value.getValue());
                }
            }
        }
    }

    static List<Statistic> deserializeStatistics(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Statistic> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Statistics$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Statistics$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Statistic>> {
        static final Statistics$MemberDeserializer INSTANCE = new Statistics$MemberDeserializer();

        @Override
        public void accept(List<Statistic> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Statistic.builder().deserializeMember(deserializer, Schemas.STATISTICS.listMember()).build());
        }
    }

    static final class MetricDataResultsSerializer implements BiConsumer<List<MetricDataResult>, ShapeSerializer> {
        static final MetricDataResultsSerializer INSTANCE = new MetricDataResultsSerializer();

        @Override
        public void accept(List<MetricDataResult> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_DATA_RESULTS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MetricDataResult> deserializeMetricDataResults(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricDataResult> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricDataResults$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricDataResults$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricDataResult>> {
        static final MetricDataResults$MemberDeserializer INSTANCE = new MetricDataResults$MemberDeserializer();

        @Override
        public void accept(List<MetricDataResult> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricDataResult.builder().deserializeMember(deserializer, Schemas.METRIC_DATA_RESULTS.listMember()).build());
        }
    }

    static final class TimestampsSerializer implements BiConsumer<List<Instant>, ShapeSerializer> {
        static final TimestampsSerializer INSTANCE = new TimestampsSerializer();

        @Override
        public void accept(List<Instant> values, ShapeSerializer serializer) {
            var $m = Schemas.TIMESTAMPS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeTimestamp($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeTimestamp($m, value);
                }
            }
        }
    }

    static List<Instant> deserializeTimestamps(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Instant> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Timestamps$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Timestamps$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Instant>> {
        static final Timestamps$MemberDeserializer INSTANCE = new Timestamps$MemberDeserializer();

        @Override
        public void accept(List<Instant> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readTimestamp(Schemas.TIMESTAMPS.listMember()));
        }
    }

    static final class MetricDataResultMessagesSerializer implements BiConsumer<List<MessageData>, ShapeSerializer> {
        static final MetricDataResultMessagesSerializer INSTANCE = new MetricDataResultMessagesSerializer();

        @Override
        public void accept(List<MessageData> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_DATA_RESULT_MESSAGES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MessageData> deserializeMetricDataResultMessages(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MessageData> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricDataResultMessages$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricDataResultMessages$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MessageData>> {
        static final MetricDataResultMessages$MemberDeserializer INSTANCE = new MetricDataResultMessages$MemberDeserializer();

        @Override
        public void accept(List<MessageData> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MessageData.builder().deserializeMember(deserializer, Schemas.METRIC_DATA_RESULT_MESSAGES.listMember()).build());
        }
    }

    static final class InsightRuleMetricDatapointsSerializer implements BiConsumer<List<InsightRuleMetricDatapoint>, ShapeSerializer> {
        static final InsightRuleMetricDatapointsSerializer INSTANCE = new InsightRuleMetricDatapointsSerializer();

        @Override
        public void accept(List<InsightRuleMetricDatapoint> values, ShapeSerializer serializer) {
            var $m = Schemas.INSIGHT_RULE_METRIC_DATAPOINTS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<InsightRuleMetricDatapoint> deserializeInsightRuleMetricDatapoints(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<InsightRuleMetricDatapoint> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InsightRuleMetricDatapoints$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InsightRuleMetricDatapoints$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<InsightRuleMetricDatapoint>> {
        static final InsightRuleMetricDatapoints$MemberDeserializer INSTANCE = new InsightRuleMetricDatapoints$MemberDeserializer();

        @Override
        public void accept(List<InsightRuleMetricDatapoint> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(InsightRuleMetricDatapoint.builder().deserializeMember(deserializer, Schemas.INSIGHT_RULE_METRIC_DATAPOINTS.listMember()).build());
        }
    }

    static final class InsightRuleContributorKeyLabelsSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final InsightRuleContributorKeyLabelsSerializer INSTANCE = new InsightRuleContributorKeyLabelsSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.INSIGHT_RULE_CONTRIBUTOR_KEY_LABELS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeInsightRuleContributorKeyLabels(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InsightRuleContributorKeyLabels$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InsightRuleContributorKeyLabels$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final InsightRuleContributorKeyLabels$MemberDeserializer INSTANCE = new InsightRuleContributorKeyLabels$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.INSIGHT_RULE_CONTRIBUTOR_KEY_LABELS.listMember()));
        }
    }

    static final class InsightRuleContributorsSerializer implements BiConsumer<List<InsightRuleContributor>, ShapeSerializer> {
        static final InsightRuleContributorsSerializer INSTANCE = new InsightRuleContributorsSerializer();

        @Override
        public void accept(List<InsightRuleContributor> values, ShapeSerializer serializer) {
            var $m = Schemas.INSIGHT_RULE_CONTRIBUTORS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<InsightRuleContributor> deserializeInsightRuleContributors(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<InsightRuleContributor> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InsightRuleContributors$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InsightRuleContributors$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<InsightRuleContributor>> {
        static final InsightRuleContributors$MemberDeserializer INSTANCE = new InsightRuleContributors$MemberDeserializer();

        @Override
        public void accept(List<InsightRuleContributor> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(InsightRuleContributor.builder().deserializeMember(deserializer, Schemas.INSIGHT_RULE_CONTRIBUTORS.listMember()).build());
        }
    }

    static final class InsightRuleContributorKeysSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final InsightRuleContributorKeysSerializer INSTANCE = new InsightRuleContributorKeysSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.INSIGHT_RULE_CONTRIBUTOR_KEYS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeInsightRuleContributorKeys(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InsightRuleContributorKeys$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InsightRuleContributorKeys$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final InsightRuleContributorKeys$MemberDeserializer INSTANCE = new InsightRuleContributorKeys$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.INSIGHT_RULE_CONTRIBUTOR_KEYS.listMember()));
        }
    }

    static final class InsightRuleContributorDatapointsSerializer implements BiConsumer<List<InsightRuleContributorDatapoint>, ShapeSerializer> {
        static final InsightRuleContributorDatapointsSerializer INSTANCE = new InsightRuleContributorDatapointsSerializer();

        @Override
        public void accept(List<InsightRuleContributorDatapoint> values, ShapeSerializer serializer) {
            var $m = Schemas.INSIGHT_RULE_CONTRIBUTOR_DATAPOINTS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<InsightRuleContributorDatapoint> deserializeInsightRuleContributorDatapoints(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<InsightRuleContributorDatapoint> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InsightRuleContributorDatapoints$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InsightRuleContributorDatapoints$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<InsightRuleContributorDatapoint>> {
        static final InsightRuleContributorDatapoints$MemberDeserializer INSTANCE = new InsightRuleContributorDatapoints$MemberDeserializer();

        @Override
        public void accept(List<InsightRuleContributorDatapoint> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(InsightRuleContributorDatapoint.builder().deserializeMember(deserializer, Schemas.INSIGHT_RULE_CONTRIBUTOR_DATAPOINTS.listMember()).build());
        }
    }

    static final class InsightRuleMetricListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final InsightRuleMetricListSerializer INSTANCE = new InsightRuleMetricListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.INSIGHT_RULE_METRIC_LIST.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeInsightRuleMetricList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InsightRuleMetricList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InsightRuleMetricList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final InsightRuleMetricList$MemberDeserializer INSTANCE = new InsightRuleMetricList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.INSIGHT_RULE_METRIC_LIST.listMember()));
        }
    }

    static final class MuteTargetAlarmNameListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final MuteTargetAlarmNameListSerializer INSTANCE = new MuteTargetAlarmNameListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.MUTE_TARGET_ALARM_NAME_LIST.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeMuteTargetAlarmNameList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MuteTargetAlarmNameList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MuteTargetAlarmNameList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final MuteTargetAlarmNameList$MemberDeserializer INSTANCE = new MuteTargetAlarmNameList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.MUTE_TARGET_ALARM_NAME_LIST.listMember()));
        }
    }

    static final class ExtendedStatisticsSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final ExtendedStatisticsSerializer INSTANCE = new ExtendedStatisticsSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.EXTENDED_STATISTICS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeExtendedStatistics(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ExtendedStatistics$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ExtendedStatistics$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final ExtendedStatistics$MemberDeserializer INSTANCE = new ExtendedStatistics$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.EXTENDED_STATISTICS.listMember()));
        }
    }

    static final class EntityMetricDataListSerializer implements BiConsumer<List<EntityMetricData>, ShapeSerializer> {
        static final EntityMetricDataListSerializer INSTANCE = new EntityMetricDataListSerializer();

        @Override
        public void accept(List<EntityMetricData> values, ShapeSerializer serializer) {
            var $m = Schemas.ENTITY_METRIC_DATA_LIST.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<EntityMetricData> deserializeEntityMetricDataList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<EntityMetricData> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, EntityMetricDataList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class EntityMetricDataList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<EntityMetricData>> {
        static final EntityMetricDataList$MemberDeserializer INSTANCE = new EntityMetricDataList$MemberDeserializer();

        @Override
        public void accept(List<EntityMetricData> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(EntityMetricData.builder().deserializeMember(deserializer, Schemas.ENTITY_METRIC_DATA_LIST.listMember()).build());
        }
    }

    static final class MetricDataSerializer implements BiConsumer<List<MetricDatum>, ShapeSerializer> {
        static final MetricDataSerializer INSTANCE = new MetricDataSerializer();

        @Override
        public void accept(List<MetricDatum> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_DATA.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MetricDatum> deserializeMetricData(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricDatum> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricData$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricData$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricDatum>> {
        static final MetricData$MemberDeserializer INSTANCE = new MetricData$MemberDeserializer();

        @Override
        public void accept(List<MetricDatum> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricDatum.builder().deserializeMember(deserializer, Schemas.METRIC_DATA.listMember()).build());
        }
    }

    static final class ValuesSerializer implements BiConsumer<List<Double>, ShapeSerializer> {
        static final ValuesSerializer INSTANCE = new ValuesSerializer();

        @Override
        public void accept(List<Double> values, ShapeSerializer serializer) {
            var $m = Schemas.VALUES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeDouble($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeDouble($m, value);
                }
            }
        }
    }

    static List<Double> deserializeValues(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Double> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Values$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Values$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Double>> {
        static final Values$MemberDeserializer INSTANCE = new Values$MemberDeserializer();

        @Override
        public void accept(List<Double> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readDouble(Schemas.VALUES.listMember()));
        }
    }

    static final class EntityKeyAttributesMapSerializer implements BiConsumer<Map<String, String>, MapSerializer> {
        static final EntityKeyAttributesMapSerializer INSTANCE = new EntityKeyAttributesMapSerializer();

        @Override
        public void accept(Map<String, String> values, MapSerializer serializer) {
            var $k = Schemas.ENTITY_KEY_ATTRIBUTES_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    EntityKeyAttributesMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class EntityKeyAttributesMap$ValueSerializer implements BiConsumer<String, ShapeSerializer> {
        private static final EntityKeyAttributesMap$ValueSerializer INSTANCE = new EntityKeyAttributesMap$ValueSerializer();

        @Override
        public void accept(String values, ShapeSerializer serializer) {
            serializer.writeString(Schemas.ENTITY_KEY_ATTRIBUTES_MAP.mapValueMember(), values);
        }
    }

    static Map<String, String> deserializeEntityKeyAttributesMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, String> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, EntityKeyAttributesMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class EntityKeyAttributesMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, String>> {
        static final EntityKeyAttributesMap$ValueDeserializer INSTANCE = new EntityKeyAttributesMap$ValueDeserializer();

        @Override
        public void accept(Map<String, String> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, deserializer.readString(Schemas.ENTITY_KEY_ATTRIBUTES_MAP.mapValueMember()));
        }
    }

    static final class EntityAttributesMapSerializer implements BiConsumer<Map<String, String>, MapSerializer> {
        static final EntityAttributesMapSerializer INSTANCE = new EntityAttributesMapSerializer();

        @Override
        public void accept(Map<String, String> values, MapSerializer serializer) {
            var $k = Schemas.ENTITY_ATTRIBUTES_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    EntityAttributesMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class EntityAttributesMap$ValueSerializer implements BiConsumer<String, ShapeSerializer> {
        private static final EntityAttributesMap$ValueSerializer INSTANCE = new EntityAttributesMap$ValueSerializer();

        @Override
        public void accept(String values, ShapeSerializer serializer) {
            serializer.writeString(Schemas.ENTITY_ATTRIBUTES_MAP.mapValueMember(), values);
        }
    }

    static Map<String, String> deserializeEntityAttributesMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, String> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, EntityAttributesMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class EntityAttributesMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, String>> {
        static final EntityAttributesMap$ValueDeserializer INSTANCE = new EntityAttributesMap$ValueDeserializer();

        @Override
        public void accept(Map<String, String> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, deserializer.readString(Schemas.ENTITY_ATTRIBUTES_MAP.mapValueMember()));
        }
    }

    static final class DimensionFiltersSerializer implements BiConsumer<List<DimensionFilter>, ShapeSerializer> {
        static final DimensionFiltersSerializer INSTANCE = new DimensionFiltersSerializer();

        @Override
        public void accept(List<DimensionFilter> values, ShapeSerializer serializer) {
            var $m = Schemas.DIMENSION_FILTERS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<DimensionFilter> deserializeDimensionFilters(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<DimensionFilter> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, DimensionFilters$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class DimensionFilters$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<DimensionFilter>> {
        static final DimensionFilters$MemberDeserializer INSTANCE = new DimensionFilters$MemberDeserializer();

        @Override
        public void accept(List<DimensionFilter> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(DimensionFilter.builder().deserializeMember(deserializer, Schemas.DIMENSION_FILTERS.listMember()).build());
        }
    }

    static final class InsightRulesSerializer implements BiConsumer<List<InsightRule>, ShapeSerializer> {
        static final InsightRulesSerializer INSTANCE = new InsightRulesSerializer();

        @Override
        public void accept(List<InsightRule> values, ShapeSerializer serializer) {
            var $m = Schemas.INSIGHT_RULES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<InsightRule> deserializeInsightRules(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<InsightRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InsightRules$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InsightRules$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<InsightRule>> {
        static final InsightRules$MemberDeserializer INSTANCE = new InsightRules$MemberDeserializer();

        @Override
        public void accept(List<InsightRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(InsightRule.builder().deserializeMember(deserializer, Schemas.INSIGHT_RULES.listMember()).build());
        }
    }

    static final class MetricAlarmsSerializer implements BiConsumer<List<MetricAlarm>, ShapeSerializer> {
        static final MetricAlarmsSerializer INSTANCE = new MetricAlarmsSerializer();

        @Override
        public void accept(List<MetricAlarm> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_ALARMS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MetricAlarm> deserializeMetricAlarms(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricAlarm> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricAlarms$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricAlarms$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricAlarm>> {
        static final MetricAlarms$MemberDeserializer INSTANCE = new MetricAlarms$MemberDeserializer();

        @Override
        public void accept(List<MetricAlarm> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricAlarm.builder().deserializeMember(deserializer, Schemas.METRIC_ALARMS.listMember()).build());
        }
    }

    static final class LogAlarmsSerializer implements BiConsumer<List<LogAlarm>, ShapeSerializer> {
        static final LogAlarmsSerializer INSTANCE = new LogAlarmsSerializer();

        @Override
        public void accept(List<LogAlarm> values, ShapeSerializer serializer) {
            var $m = Schemas.LOG_ALARMS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<LogAlarm> deserializeLogAlarms(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<LogAlarm> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, LogAlarms$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class LogAlarms$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<LogAlarm>> {
        static final LogAlarms$MemberDeserializer INSTANCE = new LogAlarms$MemberDeserializer();

        @Override
        public void accept(List<LogAlarm> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(LogAlarm.builder().deserializeMember(deserializer, Schemas.LOG_ALARMS.listMember()).build());
        }
    }

    static final class TagListSerializer implements BiConsumer<List<Tag>, ShapeSerializer> {
        static final TagListSerializer INSTANCE = new TagListSerializer();

        @Override
        public void accept(List<Tag> values, ShapeSerializer serializer) {
            var $m = Schemas.TAG_LIST.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<Tag> deserializeTagList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Tag> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TagList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TagList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Tag>> {
        static final TagList$MemberDeserializer INSTANCE = new TagList$MemberDeserializer();

        @Override
        public void accept(List<Tag> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Tag.builder().deserializeMember(deserializer, Schemas.TAG_LIST.listMember()).build());
        }
    }

    static final class LogGroupIdentifiersSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final LogGroupIdentifiersSerializer INSTANCE = new LogGroupIdentifiersSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.LOG_GROUP_IDENTIFIERS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeLogGroupIdentifiers(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, LogGroupIdentifiers$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class LogGroupIdentifiers$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final LogGroupIdentifiers$MemberDeserializer INSTANCE = new LogGroupIdentifiers$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.LOG_GROUP_IDENTIFIERS.listMember()));
        }
    }

    static final class InsightRuleNamesSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final InsightRuleNamesSerializer INSTANCE = new InsightRuleNamesSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.INSIGHT_RULE_NAMES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeInsightRuleNames(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InsightRuleNames$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InsightRuleNames$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final InsightRuleNames$MemberDeserializer INSTANCE = new InsightRuleNames$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.INSIGHT_RULE_NAMES.listMember()));
        }
    }

    static final class DatapointValuesSerializer implements BiConsumer<List<Double>, ShapeSerializer> {
        static final DatapointValuesSerializer INSTANCE = new DatapointValuesSerializer();

        @Override
        public void accept(List<Double> values, ShapeSerializer serializer) {
            var $m = Schemas.DATAPOINT_VALUES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeDouble($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeDouble($m, value);
                }
            }
        }
    }

    static List<Double> deserializeDatapointValues(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Double> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, DatapointValues$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class DatapointValues$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Double>> {
        static final DatapointValues$MemberDeserializer INSTANCE = new DatapointValues$MemberDeserializer();

        @Override
        public void accept(List<Double> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readDouble(Schemas.DATAPOINT_VALUES.listMember()));
        }
    }

    static final class DatapointsSerializer implements BiConsumer<List<Datapoint>, ShapeSerializer> {
        static final DatapointsSerializer INSTANCE = new DatapointsSerializer();

        @Override
        public void accept(List<Datapoint> values, ShapeSerializer serializer) {
            var $m = Schemas.DATAPOINTS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<Datapoint> deserializeDatapoints(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Datapoint> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Datapoints$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Datapoints$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Datapoint>> {
        static final Datapoints$MemberDeserializer INSTANCE = new Datapoints$MemberDeserializer();

        @Override
        public void accept(List<Datapoint> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Datapoint.builder().deserializeMember(deserializer, Schemas.DATAPOINTS.listMember()).build());
        }
    }

    static final class DatapointValueMapSerializer implements BiConsumer<Map<String, Double>, MapSerializer> {
        static final DatapointValueMapSerializer INSTANCE = new DatapointValueMapSerializer();

        @Override
        public void accept(Map<String, Double> values, MapSerializer serializer) {
            var $k = Schemas.DATAPOINT_VALUE_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    DatapointValueMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class DatapointValueMap$ValueSerializer implements BiConsumer<Double, ShapeSerializer> {
        private static final DatapointValueMap$ValueSerializer INSTANCE = new DatapointValueMap$ValueSerializer();

        @Override
        public void accept(Double values, ShapeSerializer serializer) {
            serializer.writeDouble(Schemas.DATAPOINT_VALUE_MAP.mapValueMember(), values);
        }
    }

    static Map<String, Double> deserializeDatapointValueMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, Double> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, DatapointValueMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class DatapointValueMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, Double>> {
        static final DatapointValueMap$ValueDeserializer INSTANCE = new DatapointValueMap$ValueDeserializer();

        @Override
        public void accept(Map<String, Double> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, deserializer.readDouble(Schemas.DATAPOINT_VALUE_MAP.mapValueMember()));
        }
    }

    static final class DashboardNamesSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final DashboardNamesSerializer INSTANCE = new DashboardNamesSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.DASHBOARD_NAMES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeDashboardNames(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, DashboardNames$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class DashboardNames$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final DashboardNames$MemberDeserializer INSTANCE = new DashboardNames$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.DASHBOARD_NAMES.listMember()));
        }
    }

    static final class DashboardValidationMessagesSerializer implements BiConsumer<List<DashboardValidationMessage>, ShapeSerializer> {
        static final DashboardValidationMessagesSerializer INSTANCE = new DashboardValidationMessagesSerializer();

        @Override
        public void accept(List<DashboardValidationMessage> values, ShapeSerializer serializer) {
            var $m = Schemas.DASHBOARD_VALIDATION_MESSAGES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<DashboardValidationMessage> deserializeDashboardValidationMessages(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<DashboardValidationMessage> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, DashboardValidationMessages$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class DashboardValidationMessages$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<DashboardValidationMessage>> {
        static final DashboardValidationMessages$MemberDeserializer INSTANCE = new DashboardValidationMessages$MemberDeserializer();

        @Override
        public void accept(List<DashboardValidationMessage> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(DashboardValidationMessage.builder().deserializeMember(deserializer, Schemas.DASHBOARD_VALIDATION_MESSAGES.listMember()).build());
        }
    }

    static final class DashboardEntriesSerializer implements BiConsumer<List<DashboardEntry>, ShapeSerializer> {
        static final DashboardEntriesSerializer INSTANCE = new DashboardEntriesSerializer();

        @Override
        public void accept(List<DashboardEntry> values, ShapeSerializer serializer) {
            var $m = Schemas.DASHBOARD_ENTRIES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<DashboardEntry> deserializeDashboardEntries(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<DashboardEntry> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, DashboardEntries$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class DashboardEntries$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<DashboardEntry>> {
        static final DashboardEntries$MemberDeserializer INSTANCE = new DashboardEntries$MemberDeserializer();

        @Override
        public void accept(List<DashboardEntry> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(DashboardEntry.builder().deserializeMember(deserializer, Schemas.DASHBOARD_ENTRIES.listMember()).build());
        }
    }

    static final class CountsSerializer implements BiConsumer<List<Double>, ShapeSerializer> {
        static final CountsSerializer INSTANCE = new CountsSerializer();

        @Override
        public void accept(List<Double> values, ShapeSerializer serializer) {
            var $m = Schemas.COUNTS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeDouble($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeDouble($m, value);
                }
            }
        }
    }

    static List<Double> deserializeCounts(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Double> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Counts$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Counts$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Double>> {
        static final Counts$MemberDeserializer INSTANCE = new Counts$MemberDeserializer();

        @Override
        public void accept(List<Double> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readDouble(Schemas.COUNTS.listMember()));
        }
    }

    static final class CompositeAlarmsSerializer implements BiConsumer<List<CompositeAlarm>, ShapeSerializer> {
        static final CompositeAlarmsSerializer INSTANCE = new CompositeAlarmsSerializer();

        @Override
        public void accept(List<CompositeAlarm> values, ShapeSerializer serializer) {
            var $m = Schemas.COMPOSITE_ALARMS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<CompositeAlarm> deserializeCompositeAlarms(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<CompositeAlarm> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, CompositeAlarms$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class CompositeAlarms$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<CompositeAlarm>> {
        static final CompositeAlarms$MemberDeserializer INSTANCE = new CompositeAlarms$MemberDeserializer();

        @Override
        public void accept(List<CompositeAlarm> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(CompositeAlarm.builder().deserializeMember(deserializer, Schemas.COMPOSITE_ALARMS.listMember()).build());
        }
    }

    static final class ResourceListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final ResourceListSerializer INSTANCE = new ResourceListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.RESOURCE_LIST.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeResourceList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ResourceList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ResourceList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final ResourceList$MemberDeserializer INSTANCE = new ResourceList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.RESOURCE_LIST.listMember()));
        }
    }

    static final class BatchFailuresSerializer implements BiConsumer<List<PartialFailure>, ShapeSerializer> {
        static final BatchFailuresSerializer INSTANCE = new BatchFailuresSerializer();

        @Override
        public void accept(List<PartialFailure> values, ShapeSerializer serializer) {
            var $m = Schemas.BATCH_FAILURES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<PartialFailure> deserializeBatchFailures(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<PartialFailure> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, BatchFailures$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class BatchFailures$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<PartialFailure>> {
        static final BatchFailures$MemberDeserializer INSTANCE = new BatchFailures$MemberDeserializer();

        @Override
        public void accept(List<PartialFailure> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(PartialFailure.builder().deserializeMember(deserializer, Schemas.BATCH_FAILURES.listMember()).build());
        }
    }

    static final class AnomalyDetectorTypesSerializer implements BiConsumer<List<AnomalyDetectorType>, ShapeSerializer> {
        static final AnomalyDetectorTypesSerializer INSTANCE = new AnomalyDetectorTypesSerializer();

        @Override
        public void accept(List<AnomalyDetectorType> values, ShapeSerializer serializer) {
            var $m = Schemas.ANOMALY_DETECTOR_TYPES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value.getValue());
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value.getValue());
                }
            }
        }
    }

    static List<AnomalyDetectorType> deserializeAnomalyDetectorTypes(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AnomalyDetectorType> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AnomalyDetectorTypes$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AnomalyDetectorTypes$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AnomalyDetectorType>> {
        static final AnomalyDetectorTypes$MemberDeserializer INSTANCE = new AnomalyDetectorTypes$MemberDeserializer();

        @Override
        public void accept(List<AnomalyDetectorType> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AnomalyDetectorType.builder().deserializeMember(deserializer, Schemas.ANOMALY_DETECTOR_TYPES.listMember()).build());
        }
    }

    static final class AnomalyDetectorsSerializer implements BiConsumer<List<AnomalyDetector>, ShapeSerializer> {
        static final AnomalyDetectorsSerializer INSTANCE = new AnomalyDetectorsSerializer();

        @Override
        public void accept(List<AnomalyDetector> values, ShapeSerializer serializer) {
            var $m = Schemas.ANOMALY_DETECTORS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<AnomalyDetector> deserializeAnomalyDetectors(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AnomalyDetector> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AnomalyDetectors$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AnomalyDetectors$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AnomalyDetector>> {
        static final AnomalyDetectors$MemberDeserializer INSTANCE = new AnomalyDetectors$MemberDeserializer();

        @Override
        public void accept(List<AnomalyDetector> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AnomalyDetector.builder().deserializeMember(deserializer, Schemas.ANOMALY_DETECTORS.listMember()).build());
        }
    }

    static final class AnomalyDetectorIdsSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final AnomalyDetectorIdsSerializer INSTANCE = new AnomalyDetectorIdsSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.ANOMALY_DETECTOR_IDS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeAnomalyDetectorIds(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AnomalyDetectorIds$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AnomalyDetectorIds$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final AnomalyDetectorIds$MemberDeserializer INSTANCE = new AnomalyDetectorIds$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.ANOMALY_DETECTOR_IDS.listMember()));
        }
    }

    static final class MetricDataQueriesSerializer implements BiConsumer<List<MetricDataQuery>, ShapeSerializer> {
        static final MetricDataQueriesSerializer INSTANCE = new MetricDataQueriesSerializer();

        @Override
        public void accept(List<MetricDataQuery> values, ShapeSerializer serializer) {
            var $m = Schemas.METRIC_DATA_QUERIES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<MetricDataQuery> deserializeMetricDataQueries(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricDataQuery> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricDataQueries$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricDataQueries$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricDataQuery>> {
        static final MetricDataQueries$MemberDeserializer INSTANCE = new MetricDataQueries$MemberDeserializer();

        @Override
        public void accept(List<MetricDataQuery> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricDataQuery.builder().deserializeMember(deserializer, Schemas.METRIC_DATA_QUERIES.listMember()).build());
        }
    }

    static final class DimensionsSerializer implements BiConsumer<List<Dimension>, ShapeSerializer> {
        static final DimensionsSerializer INSTANCE = new DimensionsSerializer();

        @Override
        public void accept(List<Dimension> values, ShapeSerializer serializer) {
            var $m = Schemas.DIMENSIONS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<Dimension> deserializeDimensions(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Dimension> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Dimensions$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Dimensions$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Dimension>> {
        static final Dimensions$MemberDeserializer INSTANCE = new Dimensions$MemberDeserializer();

        @Override
        public void accept(List<Dimension> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Dimension.builder().deserializeMember(deserializer, Schemas.DIMENSIONS.listMember()).build());
        }
    }

    static final class AnomalyDetectorExcludedTimeRangesSerializer implements BiConsumer<List<Range>, ShapeSerializer> {
        static final AnomalyDetectorExcludedTimeRangesSerializer INSTANCE = new AnomalyDetectorExcludedTimeRangesSerializer();

        @Override
        public void accept(List<Range> values, ShapeSerializer serializer) {
            var $m = Schemas.ANOMALY_DETECTOR_EXCLUDED_TIME_RANGES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<Range> deserializeAnomalyDetectorExcludedTimeRanges(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Range> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AnomalyDetectorExcludedTimeRanges$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AnomalyDetectorExcludedTimeRanges$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Range>> {
        static final AnomalyDetectorExcludedTimeRanges$MemberDeserializer INSTANCE = new AnomalyDetectorExcludedTimeRanges$MemberDeserializer();

        @Override
        public void accept(List<Range> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Range.builder().deserializeMember(deserializer, Schemas.ANOMALY_DETECTOR_EXCLUDED_TIME_RANGES.listMember()).build());
        }
    }

    static final class AlarmTypesSerializer implements BiConsumer<List<AlarmType>, ShapeSerializer> {
        static final AlarmTypesSerializer INSTANCE = new AlarmTypesSerializer();

        @Override
        public void accept(List<AlarmType> values, ShapeSerializer serializer) {
            var $m = Schemas.ALARM_TYPES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value.getValue());
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value.getValue());
                }
            }
        }
    }

    static List<AlarmType> deserializeAlarmTypes(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AlarmType> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AlarmTypes$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AlarmTypes$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AlarmType>> {
        static final AlarmTypes$MemberDeserializer INSTANCE = new AlarmTypes$MemberDeserializer();

        @Override
        public void accept(List<AlarmType> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AlarmType.builder().deserializeMember(deserializer, Schemas.ALARM_TYPES.listMember()).build());
        }
    }

    static final class AlarmNamesSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final AlarmNamesSerializer INSTANCE = new AlarmNamesSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.ALARM_NAMES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value);
                }
            }
        }
    }

    static List<String> deserializeAlarmNames(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AlarmNames$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AlarmNames$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final AlarmNames$MemberDeserializer INSTANCE = new AlarmNames$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.ALARM_NAMES.listMember()));
        }
    }

    static final class AlarmMuteRuleSummariesSerializer implements BiConsumer<List<AlarmMuteRuleSummary>, ShapeSerializer> {
        static final AlarmMuteRuleSummariesSerializer INSTANCE = new AlarmMuteRuleSummariesSerializer();

        @Override
        public void accept(List<AlarmMuteRuleSummary> values, ShapeSerializer serializer) {
            var $m = Schemas.ALARM_MUTE_RULE_SUMMARIES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<AlarmMuteRuleSummary> deserializeAlarmMuteRuleSummaries(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AlarmMuteRuleSummary> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AlarmMuteRuleSummaries$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AlarmMuteRuleSummaries$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AlarmMuteRuleSummary>> {
        static final AlarmMuteRuleSummaries$MemberDeserializer INSTANCE = new AlarmMuteRuleSummaries$MemberDeserializer();

        @Override
        public void accept(List<AlarmMuteRuleSummary> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AlarmMuteRuleSummary.builder().deserializeMember(deserializer, Schemas.ALARM_MUTE_RULE_SUMMARIES.listMember()).build());
        }
    }

    static final class AlarmMuteRuleStatusesSerializer implements BiConsumer<List<AlarmMuteRuleStatus>, ShapeSerializer> {
        static final AlarmMuteRuleStatusesSerializer INSTANCE = new AlarmMuteRuleStatusesSerializer();

        @Override
        public void accept(List<AlarmMuteRuleStatus> values, ShapeSerializer serializer) {
            var $m = Schemas.ALARM_MUTE_RULE_STATUSES.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeString($m, value.getValue());
                }
            } else {
                for (var value : values) {
                    serializer.writeString($m, value.getValue());
                }
            }
        }
    }

    static List<AlarmMuteRuleStatus> deserializeAlarmMuteRuleStatuses(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AlarmMuteRuleStatus> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AlarmMuteRuleStatuses$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AlarmMuteRuleStatuses$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AlarmMuteRuleStatus>> {
        static final AlarmMuteRuleStatuses$MemberDeserializer INSTANCE = new AlarmMuteRuleStatuses$MemberDeserializer();

        @Override
        public void accept(List<AlarmMuteRuleStatus> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AlarmMuteRuleStatus.builder().deserializeMember(deserializer, Schemas.ALARM_MUTE_RULE_STATUSES.listMember()).build());
        }
    }

    static final class AlarmHistoryItemsSerializer implements BiConsumer<List<AlarmHistoryItem>, ShapeSerializer> {
        static final AlarmHistoryItemsSerializer INSTANCE = new AlarmHistoryItemsSerializer();

        @Override
        public void accept(List<AlarmHistoryItem> values, ShapeSerializer serializer) {
            var $m = Schemas.ALARM_HISTORY_ITEMS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<AlarmHistoryItem> deserializeAlarmHistoryItems(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AlarmHistoryItem> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AlarmHistoryItems$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AlarmHistoryItems$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AlarmHistoryItem>> {
        static final AlarmHistoryItems$MemberDeserializer INSTANCE = new AlarmHistoryItems$MemberDeserializer();

        @Override
        public void accept(List<AlarmHistoryItem> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AlarmHistoryItem.builder().deserializeMember(deserializer, Schemas.ALARM_HISTORY_ITEMS.listMember()).build());
        }
    }

    static final class AlarmContributorsSerializer implements BiConsumer<List<AlarmContributor>, ShapeSerializer> {
        static final AlarmContributorsSerializer INSTANCE = new AlarmContributorsSerializer();

        @Override
        public void accept(List<AlarmContributor> values, ShapeSerializer serializer) {
            var $m = Schemas.ALARM_CONTRIBUTORS.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeStruct($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeStruct($m, value);
                }
            }
        }
    }

    static List<AlarmContributor> deserializeAlarmContributors(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AlarmContributor> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AlarmContributors$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AlarmContributors$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AlarmContributor>> {
        static final AlarmContributors$MemberDeserializer INSTANCE = new AlarmContributors$MemberDeserializer();

        @Override
        public void accept(List<AlarmContributor> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AlarmContributor.builder().deserializeMember(deserializer, Schemas.ALARM_CONTRIBUTORS.listMember()).build());
        }
    }

    static final class ContributorAttributesSerializer implements BiConsumer<Map<String, String>, MapSerializer> {
        static final ContributorAttributesSerializer INSTANCE = new ContributorAttributesSerializer();

        @Override
        public void accept(Map<String, String> values, MapSerializer serializer) {
            var $k = Schemas.CONTRIBUTOR_ATTRIBUTES.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    ContributorAttributes$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class ContributorAttributes$ValueSerializer implements BiConsumer<String, ShapeSerializer> {
        private static final ContributorAttributes$ValueSerializer INSTANCE = new ContributorAttributes$ValueSerializer();

        @Override
        public void accept(String values, ShapeSerializer serializer) {
            serializer.writeString(Schemas.CONTRIBUTOR_ATTRIBUTES.mapValueMember(), values);
        }
    }

    static Map<String, String> deserializeContributorAttributes(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, String> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, ContributorAttributes$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class ContributorAttributes$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, String>> {
        static final ContributorAttributes$ValueDeserializer INSTANCE = new ContributorAttributes$ValueDeserializer();

        @Override
        public void accept(Map<String, String> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, deserializer.readString(Schemas.CONTRIBUTOR_ATTRIBUTES.mapValueMember()));
        }
    }

    private SharedSerde() {}
}
