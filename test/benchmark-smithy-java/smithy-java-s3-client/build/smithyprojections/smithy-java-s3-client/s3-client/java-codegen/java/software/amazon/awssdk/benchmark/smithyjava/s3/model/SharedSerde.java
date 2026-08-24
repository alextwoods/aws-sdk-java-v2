package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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

    static final class UserMetadataSerializer implements BiConsumer<List<MetadataEntry>, ShapeSerializer> {
        static final UserMetadataSerializer INSTANCE = new UserMetadataSerializer();

        @Override
        public void accept(List<MetadataEntry> values, ShapeSerializer serializer) {
            var $m = Schemas6.USER_METADATA.listMember();
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

    static List<MetadataEntry> deserializeUserMetadata(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetadataEntry> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, UserMetadata$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class UserMetadata$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetadataEntry>> {
        static final UserMetadata$MemberDeserializer INSTANCE = new UserMetadata$MemberDeserializer();

        @Override
        public void accept(List<MetadataEntry> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetadataEntry.builder().deserializeMember(deserializer, Schemas6.USER_METADATA.listMember()).build());
        }
    }

    static final class PartsSerializer implements BiConsumer<List<Part>, ShapeSerializer> {
        static final PartsSerializer INSTANCE = new PartsSerializer();

        @Override
        public void accept(List<Part> values, ShapeSerializer serializer) {
            var $m = Schemas5.PARTS.listMember();
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

    static List<Part> deserializeParts(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Part> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Parts$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Parts$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Part>> {
        static final Parts$MemberDeserializer INSTANCE = new Parts$MemberDeserializer();

        @Override
        public void accept(List<Part> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Part.builder().deserializeMember(deserializer, Schemas5.PARTS.listMember()).build());
        }
    }

    static final class ObjectVersionListSerializer implements BiConsumer<List<ObjectVersion>, ShapeSerializer> {
        static final ObjectVersionListSerializer INSTANCE = new ObjectVersionListSerializer();

        @Override
        public void accept(List<ObjectVersion> values, ShapeSerializer serializer) {
            var $m = Schemas4.OBJECT_VERSION_LIST.listMember();
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

    static List<ObjectVersion> deserializeObjectVersionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ObjectVersion> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ObjectVersionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ObjectVersionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ObjectVersion>> {
        static final ObjectVersionList$MemberDeserializer INSTANCE = new ObjectVersionList$MemberDeserializer();

        @Override
        public void accept(List<ObjectVersion> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ObjectVersion.builder().deserializeMember(deserializer, Schemas4.OBJECT_VERSION_LIST.listMember()).build());
        }
    }

    static final class DeleteMarkersSerializer implements BiConsumer<List<DeleteMarkerEntry>, ShapeSerializer> {
        static final DeleteMarkersSerializer INSTANCE = new DeleteMarkersSerializer();

        @Override
        public void accept(List<DeleteMarkerEntry> values, ShapeSerializer serializer) {
            var $m = Schemas4.DELETE_MARKERS.listMember();
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

    static List<DeleteMarkerEntry> deserializeDeleteMarkers(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<DeleteMarkerEntry> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, DeleteMarkers$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class DeleteMarkers$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<DeleteMarkerEntry>> {
        static final DeleteMarkers$MemberDeserializer INSTANCE = new DeleteMarkers$MemberDeserializer();

        @Override
        public void accept(List<DeleteMarkerEntry> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(DeleteMarkerEntry.builder().deserializeMember(deserializer, Schemas4.DELETE_MARKERS.listMember()).build());
        }
    }

    static final class ObjectListSerializer implements BiConsumer<List<ObjectShape>, ShapeSerializer> {
        static final ObjectListSerializer INSTANCE = new ObjectListSerializer();

        @Override
        public void accept(List<ObjectShape> values, ShapeSerializer serializer) {
            var $m = Schemas4.OBJECT_LIST.listMember();
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

    static List<ObjectShape> deserializeObjectList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ObjectShape> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ObjectList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ObjectList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ObjectShape>> {
        static final ObjectList$MemberDeserializer INSTANCE = new ObjectList$MemberDeserializer();

        @Override
        public void accept(List<ObjectShape> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ObjectShape.builder().deserializeMember(deserializer, Schemas4.OBJECT_LIST.listMember()).build());
        }
    }

    static final class OptionalObjectAttributesListSerializer implements BiConsumer<List<OptionalObjectAttributes>, ShapeSerializer> {
        static final OptionalObjectAttributesListSerializer INSTANCE = new OptionalObjectAttributesListSerializer();

        @Override
        public void accept(List<OptionalObjectAttributes> values, ShapeSerializer serializer) {
            var $m = Schemas4.OPTIONAL_OBJECT_ATTRIBUTES_LIST.listMember();
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

    static List<OptionalObjectAttributes> deserializeOptionalObjectAttributesList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<OptionalObjectAttributes> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, OptionalObjectAttributesList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class OptionalObjectAttributesList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<OptionalObjectAttributes>> {
        static final OptionalObjectAttributesList$MemberDeserializer INSTANCE = new OptionalObjectAttributesList$MemberDeserializer();

        @Override
        public void accept(List<OptionalObjectAttributes> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(OptionalObjectAttributes.builder().deserializeMember(deserializer, Schemas4.OPTIONAL_OBJECT_ATTRIBUTES_LIST.listMember()).build());
        }
    }

    static final class AnnotationListSerializer implements BiConsumer<List<AnnotationEntry>, ShapeSerializer> {
        static final AnnotationListSerializer INSTANCE = new AnnotationListSerializer();

        @Override
        public void accept(List<AnnotationEntry> values, ShapeSerializer serializer) {
            var $m = Schemas4.ANNOTATION_LIST.listMember();
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

    static List<AnnotationEntry> deserializeAnnotationList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AnnotationEntry> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AnnotationList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AnnotationList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AnnotationEntry>> {
        static final AnnotationList$MemberDeserializer INSTANCE = new AnnotationList$MemberDeserializer();

        @Override
        public void accept(List<AnnotationEntry> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AnnotationEntry.builder().deserializeMember(deserializer, Schemas4.ANNOTATION_LIST.listMember()).build());
        }
    }

    static final class ChecksumAlgorithmListSerializer implements BiConsumer<List<ChecksumAlgorithm>, ShapeSerializer> {
        static final ChecksumAlgorithmListSerializer INSTANCE = new ChecksumAlgorithmListSerializer();

        @Override
        public void accept(List<ChecksumAlgorithm> values, ShapeSerializer serializer) {
            var $m = Schemas4.CHECKSUM_ALGORITHM_LIST.listMember();
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

    static List<ChecksumAlgorithm> deserializeChecksumAlgorithmList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ChecksumAlgorithm> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ChecksumAlgorithmList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ChecksumAlgorithmList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ChecksumAlgorithm>> {
        static final ChecksumAlgorithmList$MemberDeserializer INSTANCE = new ChecksumAlgorithmList$MemberDeserializer();

        @Override
        public void accept(List<ChecksumAlgorithm> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ChecksumAlgorithm.builder().deserializeMember(deserializer, Schemas4.CHECKSUM_ALGORITHM_LIST.listMember()).build());
        }
    }

    static final class MultipartUploadListSerializer implements BiConsumer<List<MultipartUpload>, ShapeSerializer> {
        static final MultipartUploadListSerializer INSTANCE = new MultipartUploadListSerializer();

        @Override
        public void accept(List<MultipartUpload> values, ShapeSerializer serializer) {
            var $m = Schemas4.MULTIPART_UPLOAD_LIST.listMember();
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

    static List<MultipartUpload> deserializeMultipartUploadList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MultipartUpload> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MultipartUploadList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MultipartUploadList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MultipartUpload>> {
        static final MultipartUploadList$MemberDeserializer INSTANCE = new MultipartUploadList$MemberDeserializer();

        @Override
        public void accept(List<MultipartUpload> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MultipartUpload.builder().deserializeMember(deserializer, Schemas4.MULTIPART_UPLOAD_LIST.listMember()).build());
        }
    }

    static final class CommonPrefixListSerializer implements BiConsumer<List<CommonPrefix>, ShapeSerializer> {
        static final CommonPrefixListSerializer INSTANCE = new CommonPrefixListSerializer();

        @Override
        public void accept(List<CommonPrefix> values, ShapeSerializer serializer) {
            var $m = Schemas4.COMMON_PREFIX_LIST.listMember();
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

    static List<CommonPrefix> deserializeCommonPrefixList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<CommonPrefix> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, CommonPrefixList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class CommonPrefixList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<CommonPrefix>> {
        static final CommonPrefixList$MemberDeserializer INSTANCE = new CommonPrefixList$MemberDeserializer();

        @Override
        public void accept(List<CommonPrefix> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(CommonPrefix.builder().deserializeMember(deserializer, Schemas4.COMMON_PREFIX_LIST.listMember()).build());
        }
    }

    static final class BucketsSerializer implements BiConsumer<List<Bucket>, ShapeSerializer> {
        static final BucketsSerializer INSTANCE = new BucketsSerializer();

        @Override
        public void accept(List<Bucket> values, ShapeSerializer serializer) {
            var $m = Schemas4.BUCKETS.listMember();
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

    static List<Bucket> deserializeBuckets(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Bucket> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Buckets$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Buckets$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Bucket>> {
        static final Buckets$MemberDeserializer INSTANCE = new Buckets$MemberDeserializer();

        @Override
        public void accept(List<Bucket> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Bucket.builder().deserializeMember(deserializer, Schemas4.BUCKETS.listMember()).build());
        }
    }

    static final class MetricsConfigurationListSerializer implements BiConsumer<List<MetricsConfiguration>, ShapeSerializer> {
        static final MetricsConfigurationListSerializer INSTANCE = new MetricsConfigurationListSerializer();

        @Override
        public void accept(List<MetricsConfiguration> values, ShapeSerializer serializer) {
            var $m = Schemas4.METRICS_CONFIGURATION_LIST.listMember();
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

    static List<MetricsConfiguration> deserializeMetricsConfigurationList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<MetricsConfiguration> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, MetricsConfigurationList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class MetricsConfigurationList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<MetricsConfiguration>> {
        static final MetricsConfigurationList$MemberDeserializer INSTANCE = new MetricsConfigurationList$MemberDeserializer();

        @Override
        public void accept(List<MetricsConfiguration> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(MetricsConfiguration.builder().deserializeMember(deserializer, Schemas4.METRICS_CONFIGURATION_LIST.listMember()).build());
        }
    }

    static final class InventoryConfigurationListSerializer implements BiConsumer<List<InventoryConfiguration>, ShapeSerializer> {
        static final InventoryConfigurationListSerializer INSTANCE = new InventoryConfigurationListSerializer();

        @Override
        public void accept(List<InventoryConfiguration> values, ShapeSerializer serializer) {
            var $m = Schemas4.INVENTORY_CONFIGURATION_LIST.listMember();
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

    static List<InventoryConfiguration> deserializeInventoryConfigurationList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<InventoryConfiguration> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InventoryConfigurationList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InventoryConfigurationList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<InventoryConfiguration>> {
        static final InventoryConfigurationList$MemberDeserializer INSTANCE = new InventoryConfigurationList$MemberDeserializer();

        @Override
        public void accept(List<InventoryConfiguration> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(InventoryConfiguration.builder().deserializeMember(deserializer, Schemas4.INVENTORY_CONFIGURATION_LIST.listMember()).build());
        }
    }

    static final class IntelligentTieringConfigurationListSerializer implements BiConsumer<List<IntelligentTieringConfiguration>, ShapeSerializer> {
        static final IntelligentTieringConfigurationListSerializer INSTANCE = new IntelligentTieringConfigurationListSerializer();

        @Override
        public void accept(List<IntelligentTieringConfiguration> values, ShapeSerializer serializer) {
            var $m = Schemas4.INTELLIGENT_TIERING_CONFIGURATION_LIST.listMember();
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

    static List<IntelligentTieringConfiguration> deserializeIntelligentTieringConfigurationList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<IntelligentTieringConfiguration> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, IntelligentTieringConfigurationList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class IntelligentTieringConfigurationList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<IntelligentTieringConfiguration>> {
        static final IntelligentTieringConfigurationList$MemberDeserializer INSTANCE = new IntelligentTieringConfigurationList$MemberDeserializer();

        @Override
        public void accept(List<IntelligentTieringConfiguration> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(IntelligentTieringConfiguration.builder().deserializeMember(deserializer, Schemas4.INTELLIGENT_TIERING_CONFIGURATION_LIST.listMember()).build());
        }
    }

    static final class AnalyticsConfigurationListSerializer implements BiConsumer<List<AnalyticsConfiguration>, ShapeSerializer> {
        static final AnalyticsConfigurationListSerializer INSTANCE = new AnalyticsConfigurationListSerializer();

        @Override
        public void accept(List<AnalyticsConfiguration> values, ShapeSerializer serializer) {
            var $m = Schemas4.ANALYTICS_CONFIGURATION_LIST.listMember();
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

    static List<AnalyticsConfiguration> deserializeAnalyticsConfigurationList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AnalyticsConfiguration> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AnalyticsConfigurationList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AnalyticsConfigurationList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AnalyticsConfiguration>> {
        static final AnalyticsConfigurationList$MemberDeserializer INSTANCE = new AnalyticsConfigurationList$MemberDeserializer();

        @Override
        public void accept(List<AnalyticsConfiguration> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AnalyticsConfiguration.builder().deserializeMember(deserializer, Schemas4.ANALYTICS_CONFIGURATION_LIST.listMember()).build());
        }
    }

    static final class PartsListSerializer implements BiConsumer<List<ObjectPart>, ShapeSerializer> {
        static final PartsListSerializer INSTANCE = new PartsListSerializer();

        @Override
        public void accept(List<ObjectPart> values, ShapeSerializer serializer) {
            var $m = Schemas3.PARTS_LIST.listMember();
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

    static List<ObjectPart> deserializePartsList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ObjectPart> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, PartsList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class PartsList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ObjectPart>> {
        static final PartsList$MemberDeserializer INSTANCE = new PartsList$MemberDeserializer();

        @Override
        public void accept(List<ObjectPart> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ObjectPart.builder().deserializeMember(deserializer, Schemas3.PARTS_LIST.listMember()).build());
        }
    }

    static final class ObjectAttributesListSerializer implements BiConsumer<List<ObjectAttributes>, ShapeSerializer> {
        static final ObjectAttributesListSerializer INSTANCE = new ObjectAttributesListSerializer();

        @Override
        public void accept(List<ObjectAttributes> values, ShapeSerializer serializer) {
            var $m = Schemas3.OBJECT_ATTRIBUTES_LIST.listMember();
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

    static List<ObjectAttributes> deserializeObjectAttributesList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ObjectAttributes> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ObjectAttributesList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ObjectAttributesList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ObjectAttributes>> {
        static final ObjectAttributesList$MemberDeserializer INSTANCE = new ObjectAttributesList$MemberDeserializer();

        @Override
        public void accept(List<ObjectAttributes> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ObjectAttributes.builder().deserializeMember(deserializer, Schemas3.OBJECT_ATTRIBUTES_LIST.listMember()).build());
        }
    }

    static final class RoutingRulesSerializer implements BiConsumer<List<RoutingRule>, ShapeSerializer> {
        static final RoutingRulesSerializer INSTANCE = new RoutingRulesSerializer();

        @Override
        public void accept(List<RoutingRule> values, ShapeSerializer serializer) {
            var $m = Schemas3.ROUTING_RULES.listMember();
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

    static List<RoutingRule> deserializeRoutingRules(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<RoutingRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, RoutingRules$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class RoutingRules$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<RoutingRule>> {
        static final RoutingRules$MemberDeserializer INSTANCE = new RoutingRules$MemberDeserializer();

        @Override
        public void accept(List<RoutingRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(RoutingRule.builder().deserializeMember(deserializer, Schemas3.ROUTING_RULES.listMember()).build());
        }
    }

    static final class ReplicationRulesSerializer implements BiConsumer<List<ReplicationRule>, ShapeSerializer> {
        static final ReplicationRulesSerializer INSTANCE = new ReplicationRulesSerializer();

        @Override
        public void accept(List<ReplicationRule> values, ShapeSerializer serializer) {
            var $m = Schemas3.REPLICATION_RULES.listMember();
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

    static List<ReplicationRule> deserializeReplicationRules(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicationRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicationRules$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicationRules$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicationRule>> {
        static final ReplicationRules$MemberDeserializer INSTANCE = new ReplicationRules$MemberDeserializer();

        @Override
        public void accept(List<ReplicationRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicationRule.builder().deserializeMember(deserializer, Schemas3.REPLICATION_RULES.listMember()).build());
        }
    }

    static final class OwnershipControlsRulesSerializer implements BiConsumer<List<OwnershipControlsRule>, ShapeSerializer> {
        static final OwnershipControlsRulesSerializer INSTANCE = new OwnershipControlsRulesSerializer();

        @Override
        public void accept(List<OwnershipControlsRule> values, ShapeSerializer serializer) {
            var $m = Schemas2.OWNERSHIP_CONTROLS_RULES.listMember();
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

    static List<OwnershipControlsRule> deserializeOwnershipControlsRules(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<OwnershipControlsRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, OwnershipControlsRules$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class OwnershipControlsRules$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<OwnershipControlsRule>> {
        static final OwnershipControlsRules$MemberDeserializer INSTANCE = new OwnershipControlsRules$MemberDeserializer();

        @Override
        public void accept(List<OwnershipControlsRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(OwnershipControlsRule.builder().deserializeMember(deserializer, Schemas2.OWNERSHIP_CONTROLS_RULES.listMember()).build());
        }
    }

    static final class TopicConfigurationListSerializer implements BiConsumer<List<TopicConfiguration>, ShapeSerializer> {
        static final TopicConfigurationListSerializer INSTANCE = new TopicConfigurationListSerializer();

        @Override
        public void accept(List<TopicConfiguration> values, ShapeSerializer serializer) {
            var $m = Schemas2.TOPIC_CONFIGURATION_LIST.listMember();
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

    static List<TopicConfiguration> deserializeTopicConfigurationList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<TopicConfiguration> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TopicConfigurationList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TopicConfigurationList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<TopicConfiguration>> {
        static final TopicConfigurationList$MemberDeserializer INSTANCE = new TopicConfigurationList$MemberDeserializer();

        @Override
        public void accept(List<TopicConfiguration> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(TopicConfiguration.builder().deserializeMember(deserializer, Schemas2.TOPIC_CONFIGURATION_LIST.listMember()).build());
        }
    }

    static final class QueueConfigurationListSerializer implements BiConsumer<List<QueueConfiguration>, ShapeSerializer> {
        static final QueueConfigurationListSerializer INSTANCE = new QueueConfigurationListSerializer();

        @Override
        public void accept(List<QueueConfiguration> values, ShapeSerializer serializer) {
            var $m = Schemas2.QUEUE_CONFIGURATION_LIST.listMember();
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

    static List<QueueConfiguration> deserializeQueueConfigurationList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<QueueConfiguration> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, QueueConfigurationList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class QueueConfigurationList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<QueueConfiguration>> {
        static final QueueConfigurationList$MemberDeserializer INSTANCE = new QueueConfigurationList$MemberDeserializer();

        @Override
        public void accept(List<QueueConfiguration> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(QueueConfiguration.builder().deserializeMember(deserializer, Schemas2.QUEUE_CONFIGURATION_LIST.listMember()).build());
        }
    }

    static final class LambdaFunctionConfigurationListSerializer implements BiConsumer<List<LambdaFunctionConfiguration>, ShapeSerializer> {
        static final LambdaFunctionConfigurationListSerializer INSTANCE = new LambdaFunctionConfigurationListSerializer();

        @Override
        public void accept(List<LambdaFunctionConfiguration> values, ShapeSerializer serializer) {
            var $m = Schemas2.LAMBDA_FUNCTION_CONFIGURATION_LIST.listMember();
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

    static List<LambdaFunctionConfiguration> deserializeLambdaFunctionConfigurationList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<LambdaFunctionConfiguration> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, LambdaFunctionConfigurationList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class LambdaFunctionConfigurationList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<LambdaFunctionConfiguration>> {
        static final LambdaFunctionConfigurationList$MemberDeserializer INSTANCE = new LambdaFunctionConfigurationList$MemberDeserializer();

        @Override
        public void accept(List<LambdaFunctionConfiguration> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(LambdaFunctionConfiguration.builder().deserializeMember(deserializer, Schemas2.LAMBDA_FUNCTION_CONFIGURATION_LIST.listMember()).build());
        }
    }

    static final class FilterRuleListSerializer implements BiConsumer<List<FilterRule>, ShapeSerializer> {
        static final FilterRuleListSerializer INSTANCE = new FilterRuleListSerializer();

        @Override
        public void accept(List<FilterRule> values, ShapeSerializer serializer) {
            var $m = Schemas2.FILTER_RULE_LIST.listMember();
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

    static List<FilterRule> deserializeFilterRuleList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<FilterRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, FilterRuleList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class FilterRuleList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<FilterRule>> {
        static final FilterRuleList$MemberDeserializer INSTANCE = new FilterRuleList$MemberDeserializer();

        @Override
        public void accept(List<FilterRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(FilterRule.builder().deserializeMember(deserializer, Schemas2.FILTER_RULE_LIST.listMember()).build());
        }
    }

    static final class EventListSerializer implements BiConsumer<List<Event>, ShapeSerializer> {
        static final EventListSerializer INSTANCE = new EventListSerializer();

        @Override
        public void accept(List<Event> values, ShapeSerializer serializer) {
            var $m = Schemas2.EVENT_LIST.listMember();
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

    static List<Event> deserializeEventList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Event> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, EventList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class EventList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Event>> {
        static final EventList$MemberDeserializer INSTANCE = new EventList$MemberDeserializer();

        @Override
        public void accept(List<Event> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Event.builder().deserializeMember(deserializer, Schemas2.EVENT_LIST.listMember()).build());
        }
    }

    static final class TargetGrantsSerializer implements BiConsumer<List<TargetGrant>, ShapeSerializer> {
        static final TargetGrantsSerializer INSTANCE = new TargetGrantsSerializer();

        @Override
        public void accept(List<TargetGrant> values, ShapeSerializer serializer) {
            var $m = Schemas2.TARGET_GRANTS.listMember();
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

    static List<TargetGrant> deserializeTargetGrants(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<TargetGrant> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TargetGrants$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TargetGrants$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<TargetGrant>> {
        static final TargetGrants$MemberDeserializer INSTANCE = new TargetGrants$MemberDeserializer();

        @Override
        public void accept(List<TargetGrant> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(TargetGrant.builder().deserializeMember(deserializer, Schemas2.TARGET_GRANTS.listMember()).build());
        }
    }

    static final class LifecycleRulesSerializer implements BiConsumer<List<LifecycleRule>, ShapeSerializer> {
        static final LifecycleRulesSerializer INSTANCE = new LifecycleRulesSerializer();

        @Override
        public void accept(List<LifecycleRule> values, ShapeSerializer serializer) {
            var $m = Schemas2.LIFECYCLE_RULES.listMember();
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

    static List<LifecycleRule> deserializeLifecycleRules(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<LifecycleRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, LifecycleRules$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class LifecycleRules$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<LifecycleRule>> {
        static final LifecycleRules$MemberDeserializer INSTANCE = new LifecycleRules$MemberDeserializer();

        @Override
        public void accept(List<LifecycleRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(LifecycleRule.builder().deserializeMember(deserializer, Schemas2.LIFECYCLE_RULES.listMember()).build());
        }
    }

    static final class TransitionListSerializer implements BiConsumer<List<Transition>, ShapeSerializer> {
        static final TransitionListSerializer INSTANCE = new TransitionListSerializer();

        @Override
        public void accept(List<Transition> values, ShapeSerializer serializer) {
            var $m = Schemas2.TRANSITION_LIST.listMember();
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

    static List<Transition> deserializeTransitionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Transition> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TransitionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TransitionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Transition>> {
        static final TransitionList$MemberDeserializer INSTANCE = new TransitionList$MemberDeserializer();

        @Override
        public void accept(List<Transition> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Transition.builder().deserializeMember(deserializer, Schemas2.TRANSITION_LIST.listMember()).build());
        }
    }

    static final class NoncurrentVersionTransitionListSerializer implements BiConsumer<List<NoncurrentVersionTransition>, ShapeSerializer> {
        static final NoncurrentVersionTransitionListSerializer INSTANCE = new NoncurrentVersionTransitionListSerializer();

        @Override
        public void accept(List<NoncurrentVersionTransition> values, ShapeSerializer serializer) {
            var $m = Schemas2.NONCURRENT_VERSION_TRANSITION_LIST.listMember();
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

    static List<NoncurrentVersionTransition> deserializeNoncurrentVersionTransitionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<NoncurrentVersionTransition> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, NoncurrentVersionTransitionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class NoncurrentVersionTransitionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<NoncurrentVersionTransition>> {
        static final NoncurrentVersionTransitionList$MemberDeserializer INSTANCE = new NoncurrentVersionTransitionList$MemberDeserializer();

        @Override
        public void accept(List<NoncurrentVersionTransition> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(NoncurrentVersionTransition.builder().deserializeMember(deserializer, Schemas2.NONCURRENT_VERSION_TRANSITION_LIST.listMember()).build());
        }
    }

    static final class InventoryOptionalFieldsSerializer implements BiConsumer<List<InventoryOptionalField>, ShapeSerializer> {
        static final InventoryOptionalFieldsSerializer INSTANCE = new InventoryOptionalFieldsSerializer();

        @Override
        public void accept(List<InventoryOptionalField> values, ShapeSerializer serializer) {
            var $m = Schemas2.INVENTORY_OPTIONAL_FIELDS.listMember();
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

    static List<InventoryOptionalField> deserializeInventoryOptionalFields(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<InventoryOptionalField> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, InventoryOptionalFields$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class InventoryOptionalFields$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<InventoryOptionalField>> {
        static final InventoryOptionalFields$MemberDeserializer INSTANCE = new InventoryOptionalFields$MemberDeserializer();

        @Override
        public void accept(List<InventoryOptionalField> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(InventoryOptionalField.builder().deserializeMember(deserializer, Schemas2.INVENTORY_OPTIONAL_FIELDS.listMember()).build());
        }
    }

    static final class TieringListSerializer implements BiConsumer<List<Tiering>, ShapeSerializer> {
        static final TieringListSerializer INSTANCE = new TieringListSerializer();

        @Override
        public void accept(List<Tiering> values, ShapeSerializer serializer) {
            var $m = Schemas2.TIERING_LIST.listMember();
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

    static List<Tiering> deserializeTieringList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Tiering> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TieringList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TieringList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Tiering>> {
        static final TieringList$MemberDeserializer INSTANCE = new TieringList$MemberDeserializer();

        @Override
        public void accept(List<Tiering> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Tiering.builder().deserializeMember(deserializer, Schemas2.TIERING_LIST.listMember()).build());
        }
    }

    static final class ServerSideEncryptionRulesSerializer implements BiConsumer<List<ServerSideEncryptionRule>, ShapeSerializer> {
        static final ServerSideEncryptionRulesSerializer INSTANCE = new ServerSideEncryptionRulesSerializer();

        @Override
        public void accept(List<ServerSideEncryptionRule> values, ShapeSerializer serializer) {
            var $m = Schemas2.SERVER_SIDE_ENCRYPTION_RULES.listMember();
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

    static List<ServerSideEncryptionRule> deserializeServerSideEncryptionRules(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ServerSideEncryptionRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ServerSideEncryptionRules$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ServerSideEncryptionRules$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ServerSideEncryptionRule>> {
        static final ServerSideEncryptionRules$MemberDeserializer INSTANCE = new ServerSideEncryptionRules$MemberDeserializer();

        @Override
        public void accept(List<ServerSideEncryptionRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ServerSideEncryptionRule.builder().deserializeMember(deserializer, Schemas2.SERVER_SIDE_ENCRYPTION_RULES.listMember()).build());
        }
    }

    static final class EncryptionTypeListSerializer implements BiConsumer<List<EncryptionType>, ShapeSerializer> {
        static final EncryptionTypeListSerializer INSTANCE = new EncryptionTypeListSerializer();

        @Override
        public void accept(List<EncryptionType> values, ShapeSerializer serializer) {
            var $m = Schemas2.ENCRYPTION_TYPE_LIST.listMember();
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

    static List<EncryptionType> deserializeEncryptionTypeList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<EncryptionType> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, EncryptionTypeList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class EncryptionTypeList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<EncryptionType>> {
        static final EncryptionTypeList$MemberDeserializer INSTANCE = new EncryptionTypeList$MemberDeserializer();

        @Override
        public void accept(List<EncryptionType> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(EncryptionType.builder().deserializeMember(deserializer, Schemas2.ENCRYPTION_TYPE_LIST.listMember()).build());
        }
    }

    static final class CORSRulesSerializer implements BiConsumer<List<CORSRule>, ShapeSerializer> {
        static final CORSRulesSerializer INSTANCE = new CORSRulesSerializer();

        @Override
        public void accept(List<CORSRule> values, ShapeSerializer serializer) {
            var $m = Schemas2.CORS_RULES.listMember();
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

    static List<CORSRule> deserializeCORSRules(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<CORSRule> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, CORSRules$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class CORSRules$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<CORSRule>> {
        static final CORSRules$MemberDeserializer INSTANCE = new CORSRules$MemberDeserializer();

        @Override
        public void accept(List<CORSRule> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(CORSRule.builder().deserializeMember(deserializer, Schemas2.CORS_RULES.listMember()).build());
        }
    }

    static final class ExposeHeadersSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final ExposeHeadersSerializer INSTANCE = new ExposeHeadersSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas2.EXPOSE_HEADERS.listMember();
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

    static List<String> deserializeExposeHeaders(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ExposeHeaders$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ExposeHeaders$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final ExposeHeaders$MemberDeserializer INSTANCE = new ExposeHeaders$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas2.EXPOSE_HEADERS.listMember()));
        }
    }

    static final class ErrorsSerializer implements BiConsumer<List<Error>, ShapeSerializer> {
        static final ErrorsSerializer INSTANCE = new ErrorsSerializer();

        @Override
        public void accept(List<Error> values, ShapeSerializer serializer) {
            var $m = Schemas1.ERRORS.listMember();
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

    static List<Error> deserializeErrors(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Error> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Errors$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Errors$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Error>> {
        static final Errors$MemberDeserializer INSTANCE = new Errors$MemberDeserializer();

        @Override
        public void accept(List<Error> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Error.builder().deserializeMember(deserializer, Schemas1.ERRORS.listMember()).build());
        }
    }

    static final class DeletedObjectsSerializer implements BiConsumer<List<DeletedObject>, ShapeSerializer> {
        static final DeletedObjectsSerializer INSTANCE = new DeletedObjectsSerializer();

        @Override
        public void accept(List<DeletedObject> values, ShapeSerializer serializer) {
            var $m = Schemas1.DELETED_OBJECTS.listMember();
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

    static List<DeletedObject> deserializeDeletedObjects(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<DeletedObject> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, DeletedObjects$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class DeletedObjects$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<DeletedObject>> {
        static final DeletedObjects$MemberDeserializer INSTANCE = new DeletedObjects$MemberDeserializer();

        @Override
        public void accept(List<DeletedObject> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(DeletedObject.builder().deserializeMember(deserializer, Schemas1.DELETED_OBJECTS.listMember()).build());
        }
    }

    static final class ObjectIdentifierListSerializer implements BiConsumer<List<ObjectIdentifier>, ShapeSerializer> {
        static final ObjectIdentifierListSerializer INSTANCE = new ObjectIdentifierListSerializer();

        @Override
        public void accept(List<ObjectIdentifier> values, ShapeSerializer serializer) {
            var $m = Schemas1.OBJECT_IDENTIFIER_LIST.listMember();
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

    static List<ObjectIdentifier> deserializeObjectIdentifierList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ObjectIdentifier> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ObjectIdentifierList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ObjectIdentifierList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ObjectIdentifier>> {
        static final ObjectIdentifierList$MemberDeserializer INSTANCE = new ObjectIdentifierList$MemberDeserializer();

        @Override
        public void accept(List<ObjectIdentifier> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ObjectIdentifier.builder().deserializeMember(deserializer, Schemas1.OBJECT_IDENTIFIER_LIST.listMember()).build());
        }
    }

    static final class TagSetSerializer implements BiConsumer<List<Tag>, ShapeSerializer> {
        static final TagSetSerializer INSTANCE = new TagSetSerializer();

        @Override
        public void accept(List<Tag> values, ShapeSerializer serializer) {
            var $m = Schemas.TAG_SET.listMember();
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

    static List<Tag> deserializeTagSet(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Tag> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TagSet$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TagSet$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Tag>> {
        static final TagSet$MemberDeserializer INSTANCE = new TagSet$MemberDeserializer();

        @Override
        public void accept(List<Tag> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Tag.builder().deserializeMember(deserializer, Schemas.TAG_SET.listMember()).build());
        }
    }

    static final class MetadataSerializer implements BiConsumer<Map<String, String>, MapSerializer> {
        static final MetadataSerializer INSTANCE = new MetadataSerializer();

        @Override
        public void accept(Map<String, String> values, MapSerializer serializer) {
            var $k = Schemas.METADATA.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    Metadata$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class Metadata$ValueSerializer implements BiConsumer<String, ShapeSerializer> {
        private static final Metadata$ValueSerializer INSTANCE = new Metadata$ValueSerializer();

        @Override
        public void accept(String values, ShapeSerializer serializer) {
            serializer.writeString(Schemas.METADATA.mapValueMember(), values);
        }
    }

    static Map<String, String> deserializeMetadata(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, String> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, Metadata$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class Metadata$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, String>> {
        static final Metadata$ValueDeserializer INSTANCE = new Metadata$ValueDeserializer();

        @Override
        public void accept(Map<String, String> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, deserializer.readString(Schemas.METADATA.mapValueMember()));
        }
    }

    static final class CompletedPartListSerializer implements BiConsumer<List<CompletedPart>, ShapeSerializer> {
        static final CompletedPartListSerializer INSTANCE = new CompletedPartListSerializer();

        @Override
        public void accept(List<CompletedPart> values, ShapeSerializer serializer) {
            var $m = Schemas.COMPLETED_PART_LIST.listMember();
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

    static List<CompletedPart> deserializeCompletedPartList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<CompletedPart> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, CompletedPartList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class CompletedPartList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<CompletedPart>> {
        static final CompletedPartList$MemberDeserializer INSTANCE = new CompletedPartList$MemberDeserializer();

        @Override
        public void accept(List<CompletedPart> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(CompletedPart.builder().deserializeMember(deserializer, Schemas.COMPLETED_PART_LIST.listMember()).build());
        }
    }

    static final class AllowedOriginsSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final AllowedOriginsSerializer INSTANCE = new AllowedOriginsSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.ALLOWED_ORIGINS.listMember();
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

    static List<String> deserializeAllowedOrigins(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AllowedOrigins$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AllowedOrigins$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final AllowedOrigins$MemberDeserializer INSTANCE = new AllowedOrigins$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.ALLOWED_ORIGINS.listMember()));
        }
    }

    static final class AllowedMethodsSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final AllowedMethodsSerializer INSTANCE = new AllowedMethodsSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.ALLOWED_METHODS.listMember();
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

    static List<String> deserializeAllowedMethods(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AllowedMethods$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AllowedMethods$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final AllowedMethods$MemberDeserializer INSTANCE = new AllowedMethods$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.ALLOWED_METHODS.listMember()));
        }
    }

    static final class AllowedHeadersSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final AllowedHeadersSerializer INSTANCE = new AllowedHeadersSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.ALLOWED_HEADERS.listMember();
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

    static List<String> deserializeAllowedHeaders(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AllowedHeaders$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AllowedHeaders$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final AllowedHeaders$MemberDeserializer INSTANCE = new AllowedHeaders$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.ALLOWED_HEADERS.listMember()));
        }
    }

    static final class GrantsSerializer implements BiConsumer<List<Grant>, ShapeSerializer> {
        static final GrantsSerializer INSTANCE = new GrantsSerializer();

        @Override
        public void accept(List<Grant> values, ShapeSerializer serializer) {
            var $m = Schemas.GRANTS.listMember();
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

    static List<Grant> deserializeGrants(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Grant> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Grants$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Grants$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Grant>> {
        static final Grants$MemberDeserializer INSTANCE = new Grants$MemberDeserializer();

        @Override
        public void accept(List<Grant> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Grant.builder().deserializeMember(deserializer, Schemas.GRANTS.listMember()).build());
        }
    }

    private SharedSerde() {}
}
