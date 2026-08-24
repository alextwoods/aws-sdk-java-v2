package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.nio.ByteBuffer;
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

    static final class TransactWriteItemListSerializer implements BiConsumer<List<TransactWriteItem>, ShapeSerializer> {
        static final TransactWriteItemListSerializer INSTANCE = new TransactWriteItemListSerializer();

        @Override
        public void accept(List<TransactWriteItem> values, ShapeSerializer serializer) {
            var $m = Schemas.TRANSACT_WRITE_ITEM_LIST.listMember();
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

    static List<TransactWriteItem> deserializeTransactWriteItemList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<TransactWriteItem> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TransactWriteItemList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TransactWriteItemList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<TransactWriteItem>> {
        static final TransactWriteItemList$MemberDeserializer INSTANCE = new TransactWriteItemList$MemberDeserializer();

        @Override
        public void accept(List<TransactWriteItem> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(TransactWriteItem.builder().deserializeMember(deserializer, Schemas.TRANSACT_WRITE_ITEM_LIST.listMember()).build());
        }
    }

    static final class BatchWriteItemRequestMapSerializer implements BiConsumer<Map<String, List<WriteRequest>>, MapSerializer> {
        static final BatchWriteItemRequestMapSerializer INSTANCE = new BatchWriteItemRequestMapSerializer();

        @Override
        public void accept(Map<String, List<WriteRequest>> values, MapSerializer serializer) {
            var $k = Schemas.BATCH_WRITE_ITEM_REQUEST_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    BatchWriteItemRequestMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class BatchWriteItemRequestMap$ValueSerializer implements BiConsumer<List<WriteRequest>, ShapeSerializer> {
        private static final BatchWriteItemRequestMap$ValueSerializer INSTANCE = new BatchWriteItemRequestMap$ValueSerializer();

        @Override
        public void accept(List<WriteRequest> values, ShapeSerializer serializer) {
            serializer.writeList(Schemas.BATCH_WRITE_ITEM_REQUEST_MAP.mapValueMember(), values, values.size(), SharedSerde.WriteRequestsSerializer.INSTANCE);
        }
    }

    static Map<String, List<WriteRequest>> deserializeBatchWriteItemRequestMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, List<WriteRequest>> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, BatchWriteItemRequestMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class BatchWriteItemRequestMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, List<WriteRequest>>> {
        static final BatchWriteItemRequestMap$ValueDeserializer INSTANCE = new BatchWriteItemRequestMap$ValueDeserializer();

        @Override
        public void accept(Map<String, List<WriteRequest>> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, SharedSerde.deserializeWriteRequests(Schemas.BATCH_WRITE_ITEM_REQUEST_MAP.mapValueMember(), deserializer));
        }
    }

    static final class WriteRequestsSerializer implements BiConsumer<List<WriteRequest>, ShapeSerializer> {
        static final WriteRequestsSerializer INSTANCE = new WriteRequestsSerializer();

        @Override
        public void accept(List<WriteRequest> values, ShapeSerializer serializer) {
            var $m = Schemas.WRITE_REQUESTS.listMember();
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

    static List<WriteRequest> deserializeWriteRequests(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<WriteRequest> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, WriteRequests$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class WriteRequests$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<WriteRequest>> {
        static final WriteRequests$MemberDeserializer INSTANCE = new WriteRequests$MemberDeserializer();

        @Override
        public void accept(List<WriteRequest> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(WriteRequest.builder().deserializeMember(deserializer, Schemas.WRITE_REQUESTS.listMember()).build());
        }
    }

    static final class PartiQLBatchResponseSerializer implements BiConsumer<List<BatchStatementResponse>, ShapeSerializer> {
        static final PartiQLBatchResponseSerializer INSTANCE = new PartiQLBatchResponseSerializer();

        @Override
        public void accept(List<BatchStatementResponse> values, ShapeSerializer serializer) {
            var $m = Schemas.PARTI_QL_BATCH_RESPONSE.listMember();
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

    static List<BatchStatementResponse> deserializePartiQLBatchResponse(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<BatchStatementResponse> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, PartiQLBatchResponse$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class PartiQLBatchResponse$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<BatchStatementResponse>> {
        static final PartiQLBatchResponse$MemberDeserializer INSTANCE = new PartiQLBatchResponse$MemberDeserializer();

        @Override
        public void accept(List<BatchStatementResponse> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(BatchStatementResponse.builder().deserializeMember(deserializer, Schemas.PARTI_QL_BATCH_RESPONSE.listMember()).build());
        }
    }

    static final class ExpectedAttributeMapSerializer implements BiConsumer<Map<String, ExpectedAttributeValue>, MapSerializer> {
        static final ExpectedAttributeMapSerializer INSTANCE = new ExpectedAttributeMapSerializer();

        @Override
        public void accept(Map<String, ExpectedAttributeValue> values, MapSerializer serializer) {
            var $k = Schemas.EXPECTED_ATTRIBUTE_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    ExpectedAttributeMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class ExpectedAttributeMap$ValueSerializer implements BiConsumer<ExpectedAttributeValue, ShapeSerializer> {
        private static final ExpectedAttributeMap$ValueSerializer INSTANCE = new ExpectedAttributeMap$ValueSerializer();

        @Override
        public void accept(ExpectedAttributeValue values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.EXPECTED_ATTRIBUTE_MAP.mapValueMember(), values);
        }
    }

    static Map<String, ExpectedAttributeValue> deserializeExpectedAttributeMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, ExpectedAttributeValue> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, ExpectedAttributeMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class ExpectedAttributeMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, ExpectedAttributeValue>> {
        static final ExpectedAttributeMap$ValueDeserializer INSTANCE = new ExpectedAttributeMap$ValueDeserializer();

        @Override
        public void accept(Map<String, ExpectedAttributeValue> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, ExpectedAttributeValue.builder().deserializeMember(deserializer, Schemas.EXPECTED_ATTRIBUTE_MAP.mapValueMember()).build());
        }
    }

    static final class TransactGetItemListSerializer implements BiConsumer<List<TransactGetItem>, ShapeSerializer> {
        static final TransactGetItemListSerializer INSTANCE = new TransactGetItemListSerializer();

        @Override
        public void accept(List<TransactGetItem> values, ShapeSerializer serializer) {
            var $m = Schemas.TRANSACT_GET_ITEM_LIST.listMember();
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

    static List<TransactGetItem> deserializeTransactGetItemList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<TransactGetItem> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TransactGetItemList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TransactGetItemList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<TransactGetItem>> {
        static final TransactGetItemList$MemberDeserializer INSTANCE = new TransactGetItemList$MemberDeserializer();

        @Override
        public void accept(List<TransactGetItem> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(TransactGetItem.builder().deserializeMember(deserializer, Schemas.TRANSACT_GET_ITEM_LIST.listMember()).build());
        }
    }

    static final class ItemCollectionMetricsPerTableSerializer implements BiConsumer<Map<String, List<ItemCollectionMetrics>>, MapSerializer> {
        static final ItemCollectionMetricsPerTableSerializer INSTANCE = new ItemCollectionMetricsPerTableSerializer();

        @Override
        public void accept(Map<String, List<ItemCollectionMetrics>> values, MapSerializer serializer) {
            var $k = Schemas.ITEM_COLLECTION_METRICS_PER_TABLE.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    ItemCollectionMetricsPerTable$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class ItemCollectionMetricsPerTable$ValueSerializer implements BiConsumer<List<ItemCollectionMetrics>, ShapeSerializer> {
        private static final ItemCollectionMetricsPerTable$ValueSerializer INSTANCE = new ItemCollectionMetricsPerTable$ValueSerializer();

        @Override
        public void accept(List<ItemCollectionMetrics> values, ShapeSerializer serializer) {
            serializer.writeList(Schemas.ITEM_COLLECTION_METRICS_PER_TABLE.mapValueMember(), values, values.size(), SharedSerde.ItemCollectionMetricsMultipleSerializer.INSTANCE);
        }
    }

    static Map<String, List<ItemCollectionMetrics>> deserializeItemCollectionMetricsPerTable(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, List<ItemCollectionMetrics>> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, ItemCollectionMetricsPerTable$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class ItemCollectionMetricsPerTable$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, List<ItemCollectionMetrics>>> {
        static final ItemCollectionMetricsPerTable$ValueDeserializer INSTANCE = new ItemCollectionMetricsPerTable$ValueDeserializer();

        @Override
        public void accept(Map<String, List<ItemCollectionMetrics>> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, SharedSerde.deserializeItemCollectionMetricsMultiple(Schemas.ITEM_COLLECTION_METRICS_PER_TABLE.mapValueMember(), deserializer));
        }
    }

    static final class BatchGetRequestMapSerializer implements BiConsumer<Map<String, KeysAndAttributes>, MapSerializer> {
        static final BatchGetRequestMapSerializer INSTANCE = new BatchGetRequestMapSerializer();

        @Override
        public void accept(Map<String, KeysAndAttributes> values, MapSerializer serializer) {
            var $k = Schemas.BATCH_GET_REQUEST_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    BatchGetRequestMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class BatchGetRequestMap$ValueSerializer implements BiConsumer<KeysAndAttributes, ShapeSerializer> {
        private static final BatchGetRequestMap$ValueSerializer INSTANCE = new BatchGetRequestMap$ValueSerializer();

        @Override
        public void accept(KeysAndAttributes values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.BATCH_GET_REQUEST_MAP.mapValueMember(), values);
        }
    }

    static Map<String, KeysAndAttributes> deserializeBatchGetRequestMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, KeysAndAttributes> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, BatchGetRequestMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class BatchGetRequestMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, KeysAndAttributes>> {
        static final BatchGetRequestMap$ValueDeserializer INSTANCE = new BatchGetRequestMap$ValueDeserializer();

        @Override
        public void accept(Map<String, KeysAndAttributes> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, KeysAndAttributes.builder().deserializeMember(deserializer, Schemas.BATCH_GET_REQUEST_MAP.mapValueMember()).build());
        }
    }

    static final class SearchResultListSerializer implements BiConsumer<List<SearchResultItem>, ShapeSerializer> {
        static final SearchResultListSerializer INSTANCE = new SearchResultListSerializer();

        @Override
        public void accept(List<SearchResultItem> values, ShapeSerializer serializer) {
            var $m = Schemas.SEARCH_RESULT_LIST.listMember();
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

    static List<SearchResultItem> deserializeSearchResultList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<SearchResultItem> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, SearchResultList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class SearchResultList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<SearchResultItem>> {
        static final SearchResultList$MemberDeserializer INSTANCE = new SearchResultList$MemberDeserializer();

        @Override
        public void accept(List<SearchResultItem> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(SearchResultItem.builder().deserializeMember(deserializer, Schemas.SEARCH_RESULT_LIST.listMember()).build());
        }
    }

    static final class PartiQLBatchRequestSerializer implements BiConsumer<List<BatchStatementRequest>, ShapeSerializer> {
        static final PartiQLBatchRequestSerializer INSTANCE = new PartiQLBatchRequestSerializer();

        @Override
        public void accept(List<BatchStatementRequest> values, ShapeSerializer serializer) {
            var $m = Schemas.PARTI_QL_BATCH_REQUEST.listMember();
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

    static List<BatchStatementRequest> deserializePartiQLBatchRequest(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<BatchStatementRequest> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, PartiQLBatchRequest$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class PartiQLBatchRequest$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<BatchStatementRequest>> {
        static final PartiQLBatchRequest$MemberDeserializer INSTANCE = new PartiQLBatchRequest$MemberDeserializer();

        @Override
        public void accept(List<BatchStatementRequest> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(BatchStatementRequest.builder().deserializeMember(deserializer, Schemas.PARTI_QL_BATCH_REQUEST.listMember()).build());
        }
    }

    static final class ParameterizedStatementsSerializer implements BiConsumer<List<ParameterizedStatement>, ShapeSerializer> {
        static final ParameterizedStatementsSerializer INSTANCE = new ParameterizedStatementsSerializer();

        @Override
        public void accept(List<ParameterizedStatement> values, ShapeSerializer serializer) {
            var $m = Schemas.PARAMETERIZED_STATEMENTS.listMember();
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

    static List<ParameterizedStatement> deserializeParameterizedStatements(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ParameterizedStatement> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ParameterizedStatements$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ParameterizedStatements$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ParameterizedStatement>> {
        static final ParameterizedStatements$MemberDeserializer INSTANCE = new ParameterizedStatements$MemberDeserializer();

        @Override
        public void accept(List<ParameterizedStatement> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ParameterizedStatement.builder().deserializeMember(deserializer, Schemas.PARAMETERIZED_STATEMENTS.listMember()).build());
        }
    }

    static final class KeyConditionsSerializer implements BiConsumer<Map<String, Condition>, MapSerializer> {
        static final KeyConditionsSerializer INSTANCE = new KeyConditionsSerializer();

        @Override
        public void accept(Map<String, Condition> values, MapSerializer serializer) {
            var $k = Schemas.KEY_CONDITIONS.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    KeyConditions$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class KeyConditions$ValueSerializer implements BiConsumer<Condition, ShapeSerializer> {
        private static final KeyConditions$ValueSerializer INSTANCE = new KeyConditions$ValueSerializer();

        @Override
        public void accept(Condition values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.KEY_CONDITIONS.mapValueMember(), values);
        }
    }

    static Map<String, Condition> deserializeKeyConditions(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, Condition> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, KeyConditions$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class KeyConditions$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, Condition>> {
        static final KeyConditions$ValueDeserializer INSTANCE = new KeyConditions$ValueDeserializer();

        @Override
        public void accept(Map<String, Condition> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, Condition.builder().deserializeMember(deserializer, Schemas.KEY_CONDITIONS.mapValueMember()).build());
        }
    }

    static final class ItemResponseListSerializer implements BiConsumer<List<ItemResponse>, ShapeSerializer> {
        static final ItemResponseListSerializer INSTANCE = new ItemResponseListSerializer();

        @Override
        public void accept(List<ItemResponse> values, ShapeSerializer serializer) {
            var $m = Schemas.ITEM_RESPONSE_LIST.listMember();
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

    static List<ItemResponse> deserializeItemResponseList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ItemResponse> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ItemResponseList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ItemResponseList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ItemResponse>> {
        static final ItemResponseList$MemberDeserializer INSTANCE = new ItemResponseList$MemberDeserializer();

        @Override
        public void accept(List<ItemResponse> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ItemResponse.builder().deserializeMember(deserializer, Schemas.ITEM_RESPONSE_LIST.listMember()).build());
        }
    }

    static final class ItemCollectionMetricsMultipleSerializer implements BiConsumer<List<ItemCollectionMetrics>, ShapeSerializer> {
        static final ItemCollectionMetricsMultipleSerializer INSTANCE = new ItemCollectionMetricsMultipleSerializer();

        @Override
        public void accept(List<ItemCollectionMetrics> values, ShapeSerializer serializer) {
            var $m = Schemas.ITEM_COLLECTION_METRICS_MULTIPLE.listMember();
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

    static List<ItemCollectionMetrics> deserializeItemCollectionMetricsMultiple(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ItemCollectionMetrics> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ItemCollectionMetricsMultiple$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ItemCollectionMetricsMultiple$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ItemCollectionMetrics>> {
        static final ItemCollectionMetricsMultiple$MemberDeserializer INSTANCE = new ItemCollectionMetricsMultiple$MemberDeserializer();

        @Override
        public void accept(List<ItemCollectionMetrics> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ItemCollectionMetrics.builder().deserializeMember(deserializer, Schemas.ITEM_COLLECTION_METRICS_MULTIPLE.listMember()).build());
        }
    }

    static final class FilterConditionMapSerializer implements BiConsumer<Map<String, Condition>, MapSerializer> {
        static final FilterConditionMapSerializer INSTANCE = new FilterConditionMapSerializer();

        @Override
        public void accept(Map<String, Condition> values, MapSerializer serializer) {
            var $k = Schemas.FILTER_CONDITION_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    FilterConditionMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class FilterConditionMap$ValueSerializer implements BiConsumer<Condition, ShapeSerializer> {
        private static final FilterConditionMap$ValueSerializer INSTANCE = new FilterConditionMap$ValueSerializer();

        @Override
        public void accept(Condition values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.FILTER_CONDITION_MAP.mapValueMember(), values);
        }
    }

    static Map<String, Condition> deserializeFilterConditionMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, Condition> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, FilterConditionMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class FilterConditionMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, Condition>> {
        static final FilterConditionMap$ValueDeserializer INSTANCE = new FilterConditionMap$ValueDeserializer();

        @Override
        public void accept(Map<String, Condition> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, Condition.builder().deserializeMember(deserializer, Schemas.FILTER_CONDITION_MAP.mapValueMember()).build());
        }
    }

    static final class CancellationReasonListSerializer implements BiConsumer<List<CancellationReason>, ShapeSerializer> {
        static final CancellationReasonListSerializer INSTANCE = new CancellationReasonListSerializer();

        @Override
        public void accept(List<CancellationReason> values, ShapeSerializer serializer) {
            var $m = Schemas.CANCELLATION_REASON_LIST.listMember();
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

    static List<CancellationReason> deserializeCancellationReasonList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<CancellationReason> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, CancellationReasonList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class CancellationReasonList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<CancellationReason>> {
        static final CancellationReasonList$MemberDeserializer INSTANCE = new CancellationReasonList$MemberDeserializer();

        @Override
        public void accept(List<CancellationReason> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(CancellationReason.builder().deserializeMember(deserializer, Schemas.CANCELLATION_REASON_LIST.listMember()).build());
        }
    }

    static final class BatchGetResponseMapSerializer implements BiConsumer<Map<String, List<Map<String, AttributeValue>>>, MapSerializer> {
        static final BatchGetResponseMapSerializer INSTANCE = new BatchGetResponseMapSerializer();

        @Override
        public void accept(Map<String, List<Map<String, AttributeValue>>> values, MapSerializer serializer) {
            var $k = Schemas.BATCH_GET_RESPONSE_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    BatchGetResponseMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class BatchGetResponseMap$ValueSerializer implements BiConsumer<List<Map<String, AttributeValue>>, ShapeSerializer> {
        private static final BatchGetResponseMap$ValueSerializer INSTANCE = new BatchGetResponseMap$ValueSerializer();

        @Override
        public void accept(List<Map<String, AttributeValue>> values, ShapeSerializer serializer) {
            serializer.writeList(Schemas.BATCH_GET_RESPONSE_MAP.mapValueMember(), values, values.size(), SharedSerde.ItemListSerializer.INSTANCE);
        }
    }

    static Map<String, List<Map<String, AttributeValue>>> deserializeBatchGetResponseMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, List<Map<String, AttributeValue>>> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, BatchGetResponseMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class BatchGetResponseMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, List<Map<String, AttributeValue>>>> {
        static final BatchGetResponseMap$ValueDeserializer INSTANCE = new BatchGetResponseMap$ValueDeserializer();

        @Override
        public void accept(Map<String, List<Map<String, AttributeValue>>> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, SharedSerde.deserializeItemList(Schemas.BATCH_GET_RESPONSE_MAP.mapValueMember(), deserializer));
        }
    }

    static final class KeyListSerializer implements BiConsumer<List<Map<String, AttributeValue>>, ShapeSerializer> {
        static final KeyListSerializer INSTANCE = new KeyListSerializer();

        @Override
        public void accept(List<Map<String, AttributeValue>> values, ShapeSerializer serializer) {
            var $m = Schemas.KEY_LIST.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeMap($m, value, value.size(), SharedSerde.KeySerializer.INSTANCE);
                }
            } else {
                for (var value : values) {
                    serializer.writeMap($m, value, value.size(), SharedSerde.KeySerializer.INSTANCE);
                }
            }
        }
    }

    static List<Map<String, AttributeValue>> deserializeKeyList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Map<String, AttributeValue>> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, KeyList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class KeyList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Map<String, AttributeValue>>> {
        static final KeyList$MemberDeserializer INSTANCE = new KeyList$MemberDeserializer();

        @Override
        public void accept(List<Map<String, AttributeValue>> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(SharedSerde.deserializeKey(Schemas.KEY_LIST.listMember(), deserializer));
        }
    }

    static final class ItemListSerializer implements BiConsumer<List<Map<String, AttributeValue>>, ShapeSerializer> {
        static final ItemListSerializer INSTANCE = new ItemListSerializer();

        @Override
        public void accept(List<Map<String, AttributeValue>> values, ShapeSerializer serializer) {
            var $m = Schemas.ITEM_LIST.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeMap($m, value, value.size(), SharedSerde.AttributeMapSerializer.INSTANCE);
                }
            } else {
                for (var value : values) {
                    serializer.writeMap($m, value, value.size(), SharedSerde.AttributeMapSerializer.INSTANCE);
                }
            }
        }
    }

    static List<Map<String, AttributeValue>> deserializeItemList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Map<String, AttributeValue>> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ItemList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ItemList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Map<String, AttributeValue>>> {
        static final ItemList$MemberDeserializer INSTANCE = new ItemList$MemberDeserializer();

        @Override
        public void accept(List<Map<String, AttributeValue>> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(SharedSerde.deserializeAttributeMap(Schemas.ITEM_LIST.listMember(), deserializer));
        }
    }

    static final class AttributeUpdatesSerializer implements BiConsumer<Map<String, AttributeValueUpdate>, MapSerializer> {
        static final AttributeUpdatesSerializer INSTANCE = new AttributeUpdatesSerializer();

        @Override
        public void accept(Map<String, AttributeValueUpdate> values, MapSerializer serializer) {
            var $k = Schemas.ATTRIBUTE_UPDATES.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    AttributeUpdates$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class AttributeUpdates$ValueSerializer implements BiConsumer<AttributeValueUpdate, ShapeSerializer> {
        private static final AttributeUpdates$ValueSerializer INSTANCE = new AttributeUpdates$ValueSerializer();

        @Override
        public void accept(AttributeValueUpdate values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.ATTRIBUTE_UPDATES.mapValueMember(), values);
        }
    }

    static Map<String, AttributeValueUpdate> deserializeAttributeUpdates(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, AttributeValueUpdate> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, AttributeUpdates$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class AttributeUpdates$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, AttributeValueUpdate>> {
        static final AttributeUpdates$ValueDeserializer INSTANCE = new AttributeUpdates$ValueDeserializer();

        @Override
        public void accept(Map<String, AttributeValueUpdate> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, AttributeValueUpdate.builder().deserializeMember(deserializer, Schemas.ATTRIBUTE_UPDATES.mapValueMember()).build());
        }
    }

    static final class SearchVectorListSerializer implements BiConsumer<List<AttributeValue>, ShapeSerializer> {
        static final SearchVectorListSerializer INSTANCE = new SearchVectorListSerializer();

        @Override
        public void accept(List<AttributeValue> values, ShapeSerializer serializer) {
            var $m = Schemas.SEARCH_VECTOR_LIST.listMember();
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

    static List<AttributeValue> deserializeSearchVectorList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AttributeValue> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, SearchVectorList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class SearchVectorList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AttributeValue>> {
        static final SearchVectorList$MemberDeserializer INSTANCE = new SearchVectorList$MemberDeserializer();

        @Override
        public void accept(List<AttributeValue> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AttributeValue.builder().deserializeMember(deserializer, Schemas.SEARCH_VECTOR_LIST.listMember()).build());
        }
    }

    static final class PutItemInputAttributeMapSerializer implements BiConsumer<Map<String, AttributeValue>, MapSerializer> {
        static final PutItemInputAttributeMapSerializer INSTANCE = new PutItemInputAttributeMapSerializer();

        @Override
        public void accept(Map<String, AttributeValue> values, MapSerializer serializer) {
            var $k = Schemas.PUT_ITEM_INPUT_ATTRIBUTE_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    PutItemInputAttributeMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class PutItemInputAttributeMap$ValueSerializer implements BiConsumer<AttributeValue, ShapeSerializer> {
        private static final PutItemInputAttributeMap$ValueSerializer INSTANCE = new PutItemInputAttributeMap$ValueSerializer();

        @Override
        public void accept(AttributeValue values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.PUT_ITEM_INPUT_ATTRIBUTE_MAP.mapValueMember(), values);
        }
    }

    static Map<String, AttributeValue> deserializePutItemInputAttributeMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, AttributeValue> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, PutItemInputAttributeMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class PutItemInputAttributeMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, AttributeValue>> {
        static final PutItemInputAttributeMap$ValueDeserializer INSTANCE = new PutItemInputAttributeMap$ValueDeserializer();

        @Override
        public void accept(Map<String, AttributeValue> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, AttributeValue.builder().deserializeMember(deserializer, Schemas.PUT_ITEM_INPUT_ATTRIBUTE_MAP.mapValueMember()).build());
        }
    }

    static final class PreparedStatementParametersSerializer implements BiConsumer<List<AttributeValue>, ShapeSerializer> {
        static final PreparedStatementParametersSerializer INSTANCE = new PreparedStatementParametersSerializer();

        @Override
        public void accept(List<AttributeValue> values, ShapeSerializer serializer) {
            var $m = Schemas.PREPARED_STATEMENT_PARAMETERS.listMember();
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

    static List<AttributeValue> deserializePreparedStatementParameters(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AttributeValue> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, PreparedStatementParameters$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class PreparedStatementParameters$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AttributeValue>> {
        static final PreparedStatementParameters$MemberDeserializer INSTANCE = new PreparedStatementParameters$MemberDeserializer();

        @Override
        public void accept(List<AttributeValue> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AttributeValue.builder().deserializeMember(deserializer, Schemas.PREPARED_STATEMENT_PARAMETERS.listMember()).build());
        }
    }

    static final class KeySerializer implements BiConsumer<Map<String, AttributeValue>, MapSerializer> {
        static final KeySerializer INSTANCE = new KeySerializer();

        @Override
        public void accept(Map<String, AttributeValue> values, MapSerializer serializer) {
            var $k = Schemas.KEY.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    Key$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class Key$ValueSerializer implements BiConsumer<AttributeValue, ShapeSerializer> {
        private static final Key$ValueSerializer INSTANCE = new Key$ValueSerializer();

        @Override
        public void accept(AttributeValue values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.KEY.mapValueMember(), values);
        }
    }

    static Map<String, AttributeValue> deserializeKey(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, AttributeValue> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, Key$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class Key$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, AttributeValue>> {
        static final Key$ValueDeserializer INSTANCE = new Key$ValueDeserializer();

        @Override
        public void accept(Map<String, AttributeValue> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, AttributeValue.builder().deserializeMember(deserializer, Schemas.KEY.mapValueMember()).build());
        }
    }

    static final class ItemCollectionKeyAttributeMapSerializer implements BiConsumer<Map<String, AttributeValue>, MapSerializer> {
        static final ItemCollectionKeyAttributeMapSerializer INSTANCE = new ItemCollectionKeyAttributeMapSerializer();

        @Override
        public void accept(Map<String, AttributeValue> values, MapSerializer serializer) {
            var $k = Schemas.ITEM_COLLECTION_KEY_ATTRIBUTE_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    ItemCollectionKeyAttributeMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class ItemCollectionKeyAttributeMap$ValueSerializer implements BiConsumer<AttributeValue, ShapeSerializer> {
        private static final ItemCollectionKeyAttributeMap$ValueSerializer INSTANCE = new ItemCollectionKeyAttributeMap$ValueSerializer();

        @Override
        public void accept(AttributeValue values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.ITEM_COLLECTION_KEY_ATTRIBUTE_MAP.mapValueMember(), values);
        }
    }

    static Map<String, AttributeValue> deserializeItemCollectionKeyAttributeMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, AttributeValue> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, ItemCollectionKeyAttributeMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class ItemCollectionKeyAttributeMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, AttributeValue>> {
        static final ItemCollectionKeyAttributeMap$ValueDeserializer INSTANCE = new ItemCollectionKeyAttributeMap$ValueDeserializer();

        @Override
        public void accept(Map<String, AttributeValue> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, AttributeValue.builder().deserializeMember(deserializer, Schemas.ITEM_COLLECTION_KEY_ATTRIBUTE_MAP.mapValueMember()).build());
        }
    }

    static final class ExpressionAttributeValueMapSerializer implements BiConsumer<Map<String, AttributeValue>, MapSerializer> {
        static final ExpressionAttributeValueMapSerializer INSTANCE = new ExpressionAttributeValueMapSerializer();

        @Override
        public void accept(Map<String, AttributeValue> values, MapSerializer serializer) {
            var $k = Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    ExpressionAttributeValueMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class ExpressionAttributeValueMap$ValueSerializer implements BiConsumer<AttributeValue, ShapeSerializer> {
        private static final ExpressionAttributeValueMap$ValueSerializer INSTANCE = new ExpressionAttributeValueMap$ValueSerializer();

        @Override
        public void accept(AttributeValue values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP.mapValueMember(), values);
        }
    }

    static Map<String, AttributeValue> deserializeExpressionAttributeValueMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, AttributeValue> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, ExpressionAttributeValueMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class ExpressionAttributeValueMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, AttributeValue>> {
        static final ExpressionAttributeValueMap$ValueDeserializer INSTANCE = new ExpressionAttributeValueMap$ValueDeserializer();

        @Override
        public void accept(Map<String, AttributeValue> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, AttributeValue.builder().deserializeMember(deserializer, Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP.mapValueMember()).build());
        }
    }

    static final class AttributeValueListSerializer implements BiConsumer<List<AttributeValue>, ShapeSerializer> {
        static final AttributeValueListSerializer INSTANCE = new AttributeValueListSerializer();

        @Override
        public void accept(List<AttributeValue> values, ShapeSerializer serializer) {
            var $m = Schemas.ATTRIBUTE_VALUE_LIST.listMember();
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

    static List<AttributeValue> deserializeAttributeValueList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AttributeValue> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AttributeValueList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AttributeValueList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AttributeValue>> {
        static final AttributeValueList$MemberDeserializer INSTANCE = new AttributeValueList$MemberDeserializer();

        @Override
        public void accept(List<AttributeValue> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AttributeValue.builder().deserializeMember(deserializer, Schemas.ATTRIBUTE_VALUE_LIST.listMember()).build());
        }
    }

    static final class AttributeMapSerializer implements BiConsumer<Map<String, AttributeValue>, MapSerializer> {
        static final AttributeMapSerializer INSTANCE = new AttributeMapSerializer();

        @Override
        public void accept(Map<String, AttributeValue> values, MapSerializer serializer) {
            var $k = Schemas.ATTRIBUTE_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    AttributeMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class AttributeMap$ValueSerializer implements BiConsumer<AttributeValue, ShapeSerializer> {
        private static final AttributeMap$ValueSerializer INSTANCE = new AttributeMap$ValueSerializer();

        @Override
        public void accept(AttributeValue values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.ATTRIBUTE_MAP.mapValueMember(), values);
        }
    }

    static Map<String, AttributeValue> deserializeAttributeMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, AttributeValue> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, AttributeMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class AttributeMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, AttributeValue>> {
        static final AttributeMap$ValueDeserializer INSTANCE = new AttributeMap$ValueDeserializer();

        @Override
        public void accept(Map<String, AttributeValue> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, AttributeValue.builder().deserializeMember(deserializer, Schemas.ATTRIBUTE_MAP.mapValueMember()).build());
        }
    }

    static final class MapAttributeValueSerializer implements BiConsumer<Map<String, AttributeValue>, MapSerializer> {
        static final MapAttributeValueSerializer INSTANCE = new MapAttributeValueSerializer();

        @Override
        public void accept(Map<String, AttributeValue> values, MapSerializer serializer) {
            var $k = Schemas.MAP_ATTRIBUTE_VALUE.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    MapAttributeValue$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class MapAttributeValue$ValueSerializer implements BiConsumer<AttributeValue, ShapeSerializer> {
        private static final MapAttributeValue$ValueSerializer INSTANCE = new MapAttributeValue$ValueSerializer();

        @Override
        public void accept(AttributeValue values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.MAP_ATTRIBUTE_VALUE.mapValueMember(), values);
        }
    }

    static Map<String, AttributeValue> deserializeMapAttributeValue(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, AttributeValue> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, MapAttributeValue$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class MapAttributeValue$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, AttributeValue>> {
        static final MapAttributeValue$ValueDeserializer INSTANCE = new MapAttributeValue$ValueDeserializer();

        @Override
        public void accept(Map<String, AttributeValue> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, AttributeValue.builder().deserializeMember(deserializer, Schemas.MAP_ATTRIBUTE_VALUE.mapValueMember()).build());
        }
    }

    static final class ListAttributeValueSerializer implements BiConsumer<List<AttributeValue>, ShapeSerializer> {
        static final ListAttributeValueSerializer INSTANCE = new ListAttributeValueSerializer();

        @Override
        public void accept(List<AttributeValue> values, ShapeSerializer serializer) {
            var $m = Schemas.LIST_ATTRIBUTE_VALUE.listMember();
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

    static List<AttributeValue> deserializeListAttributeValue(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AttributeValue> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ListAttributeValue$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ListAttributeValue$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AttributeValue>> {
        static final ListAttributeValue$MemberDeserializer INSTANCE = new ListAttributeValue$MemberDeserializer();

        @Override
        public void accept(List<AttributeValue> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AttributeValue.builder().deserializeMember(deserializer, Schemas.LIST_ATTRIBUTE_VALUE.listMember()).build());
        }
    }

    static final class ReplicaAutoScalingUpdateListSerializer implements BiConsumer<List<ReplicaAutoScalingUpdate>, ShapeSerializer> {
        static final ReplicaAutoScalingUpdateListSerializer INSTANCE = new ReplicaAutoScalingUpdateListSerializer();

        @Override
        public void accept(List<ReplicaAutoScalingUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_AUTO_SCALING_UPDATE_LIST.listMember();
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

    static List<ReplicaAutoScalingUpdate> deserializeReplicaAutoScalingUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaAutoScalingUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaAutoScalingUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaAutoScalingUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaAutoScalingUpdate>> {
        static final ReplicaAutoScalingUpdateList$MemberDeserializer INSTANCE = new ReplicaAutoScalingUpdateList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaAutoScalingUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaAutoScalingUpdate.builder().deserializeMember(deserializer, Schemas.REPLICA_AUTO_SCALING_UPDATE_LIST.listMember()).build());
        }
    }

    static final class ReplicaGlobalSecondaryIndexAutoScalingUpdateListSerializer implements BiConsumer<List<ReplicaGlobalSecondaryIndexAutoScalingUpdate>, ShapeSerializer> {
        static final ReplicaGlobalSecondaryIndexAutoScalingUpdateListSerializer INSTANCE = new ReplicaGlobalSecondaryIndexAutoScalingUpdateListSerializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexAutoScalingUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE_LIST.listMember();
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

    static List<ReplicaGlobalSecondaryIndexAutoScalingUpdate> deserializeReplicaGlobalSecondaryIndexAutoScalingUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaGlobalSecondaryIndexAutoScalingUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaGlobalSecondaryIndexAutoScalingUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaGlobalSecondaryIndexAutoScalingUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaGlobalSecondaryIndexAutoScalingUpdate>> {
        static final ReplicaGlobalSecondaryIndexAutoScalingUpdateList$MemberDeserializer INSTANCE = new ReplicaGlobalSecondaryIndexAutoScalingUpdateList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexAutoScalingUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaGlobalSecondaryIndexAutoScalingUpdate.builder().deserializeMember(deserializer, Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE_LIST.listMember()).build());
        }
    }

    static final class GlobalSecondaryIndexAutoScalingUpdateListSerializer implements BiConsumer<List<GlobalSecondaryIndexAutoScalingUpdate>, ShapeSerializer> {
        static final GlobalSecondaryIndexAutoScalingUpdateListSerializer INSTANCE = new GlobalSecondaryIndexAutoScalingUpdateListSerializer();

        @Override
        public void accept(List<GlobalSecondaryIndexAutoScalingUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE_LIST.listMember();
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

    static List<GlobalSecondaryIndexAutoScalingUpdate> deserializeGlobalSecondaryIndexAutoScalingUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalSecondaryIndexAutoScalingUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalSecondaryIndexAutoScalingUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalSecondaryIndexAutoScalingUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalSecondaryIndexAutoScalingUpdate>> {
        static final GlobalSecondaryIndexAutoScalingUpdateList$MemberDeserializer INSTANCE = new GlobalSecondaryIndexAutoScalingUpdateList$MemberDeserializer();

        @Override
        public void accept(List<GlobalSecondaryIndexAutoScalingUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalSecondaryIndexAutoScalingUpdate.builder().deserializeMember(deserializer, Schemas.GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE_LIST.listMember()).build());
        }
    }

    static final class VectorIndexUpdateListSerializer implements BiConsumer<List<VectorIndexUpdate>, ShapeSerializer> {
        static final VectorIndexUpdateListSerializer INSTANCE = new VectorIndexUpdateListSerializer();

        @Override
        public void accept(List<VectorIndexUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.VECTOR_INDEX_UPDATE_LIST.listMember();
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

    static List<VectorIndexUpdate> deserializeVectorIndexUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<VectorIndexUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, VectorIndexUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class VectorIndexUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<VectorIndexUpdate>> {
        static final VectorIndexUpdateList$MemberDeserializer INSTANCE = new VectorIndexUpdateList$MemberDeserializer();

        @Override
        public void accept(List<VectorIndexUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(VectorIndexUpdate.builder().deserializeMember(deserializer, Schemas.VECTOR_INDEX_UPDATE_LIST.listMember()).build());
        }
    }

    static final class ReplicationGroupUpdateListSerializer implements BiConsumer<List<ReplicationGroupUpdate>, ShapeSerializer> {
        static final ReplicationGroupUpdateListSerializer INSTANCE = new ReplicationGroupUpdateListSerializer();

        @Override
        public void accept(List<ReplicationGroupUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICATION_GROUP_UPDATE_LIST.listMember();
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

    static List<ReplicationGroupUpdate> deserializeReplicationGroupUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicationGroupUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicationGroupUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicationGroupUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicationGroupUpdate>> {
        static final ReplicationGroupUpdateList$MemberDeserializer INSTANCE = new ReplicationGroupUpdateList$MemberDeserializer();

        @Override
        public void accept(List<ReplicationGroupUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicationGroupUpdate.builder().deserializeMember(deserializer, Schemas.REPLICATION_GROUP_UPDATE_LIST.listMember()).build());
        }
    }

    static final class GlobalTableWitnessGroupUpdateListSerializer implements BiConsumer<List<GlobalTableWitnessGroupUpdate>, ShapeSerializer> {
        static final GlobalTableWitnessGroupUpdateListSerializer INSTANCE = new GlobalTableWitnessGroupUpdateListSerializer();

        @Override
        public void accept(List<GlobalTableWitnessGroupUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_TABLE_WITNESS_GROUP_UPDATE_LIST.listMember();
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

    static List<GlobalTableWitnessGroupUpdate> deserializeGlobalTableWitnessGroupUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalTableWitnessGroupUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalTableWitnessGroupUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalTableWitnessGroupUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalTableWitnessGroupUpdate>> {
        static final GlobalTableWitnessGroupUpdateList$MemberDeserializer INSTANCE = new GlobalTableWitnessGroupUpdateList$MemberDeserializer();

        @Override
        public void accept(List<GlobalTableWitnessGroupUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalTableWitnessGroupUpdate.builder().deserializeMember(deserializer, Schemas.GLOBAL_TABLE_WITNESS_GROUP_UPDATE_LIST.listMember()).build());
        }
    }

    static final class GlobalSecondaryIndexUpdateListSerializer implements BiConsumer<List<GlobalSecondaryIndexUpdate>, ShapeSerializer> {
        static final GlobalSecondaryIndexUpdateListSerializer INSTANCE = new GlobalSecondaryIndexUpdateListSerializer();

        @Override
        public void accept(List<GlobalSecondaryIndexUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_SECONDARY_INDEX_UPDATE_LIST.listMember();
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

    static List<GlobalSecondaryIndexUpdate> deserializeGlobalSecondaryIndexUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalSecondaryIndexUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalSecondaryIndexUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalSecondaryIndexUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalSecondaryIndexUpdate>> {
        static final GlobalSecondaryIndexUpdateList$MemberDeserializer INSTANCE = new GlobalSecondaryIndexUpdateList$MemberDeserializer();

        @Override
        public void accept(List<GlobalSecondaryIndexUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalSecondaryIndexUpdate.builder().deserializeMember(deserializer, Schemas.GLOBAL_SECONDARY_INDEX_UPDATE_LIST.listMember()).build());
        }
    }

    static final class ReplicaSettingsUpdateListSerializer implements BiConsumer<List<ReplicaSettingsUpdate>, ShapeSerializer> {
        static final ReplicaSettingsUpdateListSerializer INSTANCE = new ReplicaSettingsUpdateListSerializer();

        @Override
        public void accept(List<ReplicaSettingsUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_SETTINGS_UPDATE_LIST.listMember();
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

    static List<ReplicaSettingsUpdate> deserializeReplicaSettingsUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaSettingsUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaSettingsUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaSettingsUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaSettingsUpdate>> {
        static final ReplicaSettingsUpdateList$MemberDeserializer INSTANCE = new ReplicaSettingsUpdateList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaSettingsUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaSettingsUpdate.builder().deserializeMember(deserializer, Schemas.REPLICA_SETTINGS_UPDATE_LIST.listMember()).build());
        }
    }

    static final class ReplicaGlobalSecondaryIndexSettingsUpdateListSerializer implements BiConsumer<List<ReplicaGlobalSecondaryIndexSettingsUpdate>, ShapeSerializer> {
        static final ReplicaGlobalSecondaryIndexSettingsUpdateListSerializer INSTANCE = new ReplicaGlobalSecondaryIndexSettingsUpdateListSerializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexSettingsUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE_LIST.listMember();
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

    static List<ReplicaGlobalSecondaryIndexSettingsUpdate> deserializeReplicaGlobalSecondaryIndexSettingsUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaGlobalSecondaryIndexSettingsUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaGlobalSecondaryIndexSettingsUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaGlobalSecondaryIndexSettingsUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaGlobalSecondaryIndexSettingsUpdate>> {
        static final ReplicaGlobalSecondaryIndexSettingsUpdateList$MemberDeserializer INSTANCE = new ReplicaGlobalSecondaryIndexSettingsUpdateList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexSettingsUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaGlobalSecondaryIndexSettingsUpdate.builder().deserializeMember(deserializer, Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE_LIST.listMember()).build());
        }
    }

    static final class GlobalTableGlobalSecondaryIndexSettingsUpdateListSerializer implements BiConsumer<List<GlobalTableGlobalSecondaryIndexSettingsUpdate>, ShapeSerializer> {
        static final GlobalTableGlobalSecondaryIndexSettingsUpdateListSerializer INSTANCE = new GlobalTableGlobalSecondaryIndexSettingsUpdateListSerializer();

        @Override
        public void accept(List<GlobalTableGlobalSecondaryIndexSettingsUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE_LIST.listMember();
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

    static List<GlobalTableGlobalSecondaryIndexSettingsUpdate> deserializeGlobalTableGlobalSecondaryIndexSettingsUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalTableGlobalSecondaryIndexSettingsUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalTableGlobalSecondaryIndexSettingsUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalTableGlobalSecondaryIndexSettingsUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalTableGlobalSecondaryIndexSettingsUpdate>> {
        static final GlobalTableGlobalSecondaryIndexSettingsUpdateList$MemberDeserializer INSTANCE = new GlobalTableGlobalSecondaryIndexSettingsUpdateList$MemberDeserializer();

        @Override
        public void accept(List<GlobalTableGlobalSecondaryIndexSettingsUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalTableGlobalSecondaryIndexSettingsUpdate.builder().deserializeMember(deserializer, Schemas.GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE_LIST.listMember()).build());
        }
    }

    static final class ReplicaUpdateListSerializer implements BiConsumer<List<ReplicaUpdate>, ShapeSerializer> {
        static final ReplicaUpdateListSerializer INSTANCE = new ReplicaUpdateListSerializer();

        @Override
        public void accept(List<ReplicaUpdate> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_UPDATE_LIST.listMember();
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

    static List<ReplicaUpdate> deserializeReplicaUpdateList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaUpdate> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaUpdateList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaUpdateList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaUpdate>> {
        static final ReplicaUpdateList$MemberDeserializer INSTANCE = new ReplicaUpdateList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaUpdate> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaUpdate.builder().deserializeMember(deserializer, Schemas.REPLICA_UPDATE_LIST.listMember()).build());
        }
    }

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

    static final class TableNameListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final TableNameListSerializer INSTANCE = new TableNameListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.TABLE_NAME_LIST.listMember();
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

    static List<String> deserializeTableNameList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, TableNameList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class TableNameList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final TableNameList$MemberDeserializer INSTANCE = new TableNameList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.TABLE_NAME_LIST.listMember()));
        }
    }

    static final class ImportSummaryListSerializer implements BiConsumer<List<ImportSummary>, ShapeSerializer> {
        static final ImportSummaryListSerializer INSTANCE = new ImportSummaryListSerializer();

        @Override
        public void accept(List<ImportSummary> values, ShapeSerializer serializer) {
            var $m = Schemas.IMPORT_SUMMARY_LIST.listMember();
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

    static List<ImportSummary> deserializeImportSummaryList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ImportSummary> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ImportSummaryList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ImportSummaryList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ImportSummary>> {
        static final ImportSummaryList$MemberDeserializer INSTANCE = new ImportSummaryList$MemberDeserializer();

        @Override
        public void accept(List<ImportSummary> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ImportSummary.builder().deserializeMember(deserializer, Schemas.IMPORT_SUMMARY_LIST.listMember()).build());
        }
    }

    static final class GlobalTableListSerializer implements BiConsumer<List<GlobalTable>, ShapeSerializer> {
        static final GlobalTableListSerializer INSTANCE = new GlobalTableListSerializer();

        @Override
        public void accept(List<GlobalTable> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_TABLE_LIST.listMember();
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

    static List<GlobalTable> deserializeGlobalTableList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalTable> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalTableList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalTableList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalTable>> {
        static final GlobalTableList$MemberDeserializer INSTANCE = new GlobalTableList$MemberDeserializer();

        @Override
        public void accept(List<GlobalTable> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalTable.builder().deserializeMember(deserializer, Schemas.GLOBAL_TABLE_LIST.listMember()).build());
        }
    }

    static final class ExportSummariesSerializer implements BiConsumer<List<ExportSummary>, ShapeSerializer> {
        static final ExportSummariesSerializer INSTANCE = new ExportSummariesSerializer();

        @Override
        public void accept(List<ExportSummary> values, ShapeSerializer serializer) {
            var $m = Schemas.EXPORT_SUMMARIES.listMember();
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

    static List<ExportSummary> deserializeExportSummaries(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ExportSummary> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ExportSummaries$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ExportSummaries$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ExportSummary>> {
        static final ExportSummaries$MemberDeserializer INSTANCE = new ExportSummaries$MemberDeserializer();

        @Override
        public void accept(List<ExportSummary> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ExportSummary.builder().deserializeMember(deserializer, Schemas.EXPORT_SUMMARIES.listMember()).build());
        }
    }

    static final class ReplicaAutoScalingDescriptionListSerializer implements BiConsumer<List<ReplicaAutoScalingDescription>, ShapeSerializer> {
        static final ReplicaAutoScalingDescriptionListSerializer INSTANCE = new ReplicaAutoScalingDescriptionListSerializer();

        @Override
        public void accept(List<ReplicaAutoScalingDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_AUTO_SCALING_DESCRIPTION_LIST.listMember();
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

    static List<ReplicaAutoScalingDescription> deserializeReplicaAutoScalingDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaAutoScalingDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaAutoScalingDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaAutoScalingDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaAutoScalingDescription>> {
        static final ReplicaAutoScalingDescriptionList$MemberDeserializer INSTANCE = new ReplicaAutoScalingDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaAutoScalingDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaAutoScalingDescription.builder().deserializeMember(deserializer, Schemas.REPLICA_AUTO_SCALING_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class ReplicaGlobalSecondaryIndexAutoScalingDescriptionListSerializer implements BiConsumer<List<ReplicaGlobalSecondaryIndexAutoScalingDescription>, ShapeSerializer> {
        static final ReplicaGlobalSecondaryIndexAutoScalingDescriptionListSerializer INSTANCE = new ReplicaGlobalSecondaryIndexAutoScalingDescriptionListSerializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexAutoScalingDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_DESCRIPTION_LIST.listMember();
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

    static List<ReplicaGlobalSecondaryIndexAutoScalingDescription> deserializeReplicaGlobalSecondaryIndexAutoScalingDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaGlobalSecondaryIndexAutoScalingDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaGlobalSecondaryIndexAutoScalingDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaGlobalSecondaryIndexAutoScalingDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaGlobalSecondaryIndexAutoScalingDescription>> {
        static final ReplicaGlobalSecondaryIndexAutoScalingDescriptionList$MemberDeserializer INSTANCE = new ReplicaGlobalSecondaryIndexAutoScalingDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexAutoScalingDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaGlobalSecondaryIndexAutoScalingDescription.builder().deserializeMember(deserializer, Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class KinesisDataStreamDestinationsSerializer implements BiConsumer<List<KinesisDataStreamDestination>, ShapeSerializer> {
        static final KinesisDataStreamDestinationsSerializer INSTANCE = new KinesisDataStreamDestinationsSerializer();

        @Override
        public void accept(List<KinesisDataStreamDestination> values, ShapeSerializer serializer) {
            var $m = Schemas.KINESIS_DATA_STREAM_DESTINATIONS.listMember();
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

    static List<KinesisDataStreamDestination> deserializeKinesisDataStreamDestinations(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<KinesisDataStreamDestination> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, KinesisDataStreamDestinations$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class KinesisDataStreamDestinations$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<KinesisDataStreamDestination>> {
        static final KinesisDataStreamDestinations$MemberDeserializer INSTANCE = new KinesisDataStreamDestinations$MemberDeserializer();

        @Override
        public void accept(List<KinesisDataStreamDestination> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(KinesisDataStreamDestination.builder().deserializeMember(deserializer, Schemas.KINESIS_DATA_STREAM_DESTINATIONS.listMember()).build());
        }
    }

    static final class ReplicaSettingsDescriptionListSerializer implements BiConsumer<List<ReplicaSettingsDescription>, ShapeSerializer> {
        static final ReplicaSettingsDescriptionListSerializer INSTANCE = new ReplicaSettingsDescriptionListSerializer();

        @Override
        public void accept(List<ReplicaSettingsDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_SETTINGS_DESCRIPTION_LIST.listMember();
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

    static List<ReplicaSettingsDescription> deserializeReplicaSettingsDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaSettingsDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaSettingsDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaSettingsDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaSettingsDescription>> {
        static final ReplicaSettingsDescriptionList$MemberDeserializer INSTANCE = new ReplicaSettingsDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaSettingsDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaSettingsDescription.builder().deserializeMember(deserializer, Schemas.REPLICA_SETTINGS_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class ReplicaGlobalSecondaryIndexSettingsDescriptionListSerializer implements BiConsumer<List<ReplicaGlobalSecondaryIndexSettingsDescription>, ShapeSerializer> {
        static final ReplicaGlobalSecondaryIndexSettingsDescriptionListSerializer INSTANCE = new ReplicaGlobalSecondaryIndexSettingsDescriptionListSerializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexSettingsDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_DESCRIPTION_LIST.listMember();
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

    static List<ReplicaGlobalSecondaryIndexSettingsDescription> deserializeReplicaGlobalSecondaryIndexSettingsDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaGlobalSecondaryIndexSettingsDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaGlobalSecondaryIndexSettingsDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaGlobalSecondaryIndexSettingsDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaGlobalSecondaryIndexSettingsDescription>> {
        static final ReplicaGlobalSecondaryIndexSettingsDescriptionList$MemberDeserializer INSTANCE = new ReplicaGlobalSecondaryIndexSettingsDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexSettingsDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaGlobalSecondaryIndexSettingsDescription.builder().deserializeMember(deserializer, Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class EndpointsSerializer implements BiConsumer<List<Endpoint>, ShapeSerializer> {
        static final EndpointsSerializer INSTANCE = new EndpointsSerializer();

        @Override
        public void accept(List<Endpoint> values, ShapeSerializer serializer) {
            var $m = Schemas.ENDPOINTS.listMember();
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

    static List<Endpoint> deserializeEndpoints(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Endpoint> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, Endpoints$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class Endpoints$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Endpoint>> {
        static final Endpoints$MemberDeserializer INSTANCE = new Endpoints$MemberDeserializer();

        @Override
        public void accept(List<Endpoint> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Endpoint.builder().deserializeMember(deserializer, Schemas.ENDPOINTS.listMember()).build());
        }
    }

    static final class CsvHeaderListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final CsvHeaderListSerializer INSTANCE = new CsvHeaderListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.CSV_HEADER_LIST.listMember();
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

    static List<String> deserializeCsvHeaderList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, CsvHeaderList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class CsvHeaderList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final CsvHeaderList$MemberDeserializer INSTANCE = new CsvHeaderList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.CSV_HEADER_LIST.listMember()));
        }
    }

    static final class VectorIndexDescriptionListSerializer implements BiConsumer<List<VectorIndexDescription>, ShapeSerializer> {
        static final VectorIndexDescriptionListSerializer INSTANCE = new VectorIndexDescriptionListSerializer();

        @Override
        public void accept(List<VectorIndexDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.VECTOR_INDEX_DESCRIPTION_LIST.listMember();
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

    static List<VectorIndexDescription> deserializeVectorIndexDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<VectorIndexDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, VectorIndexDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class VectorIndexDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<VectorIndexDescription>> {
        static final VectorIndexDescriptionList$MemberDeserializer INSTANCE = new VectorIndexDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<VectorIndexDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(VectorIndexDescription.builder().deserializeMember(deserializer, Schemas.VECTOR_INDEX_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class LocalSecondaryIndexDescriptionListSerializer implements BiConsumer<List<LocalSecondaryIndexDescription>, ShapeSerializer> {
        static final LocalSecondaryIndexDescriptionListSerializer INSTANCE = new LocalSecondaryIndexDescriptionListSerializer();

        @Override
        public void accept(List<LocalSecondaryIndexDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.LOCAL_SECONDARY_INDEX_DESCRIPTION_LIST.listMember();
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

    static List<LocalSecondaryIndexDescription> deserializeLocalSecondaryIndexDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<LocalSecondaryIndexDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, LocalSecondaryIndexDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class LocalSecondaryIndexDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<LocalSecondaryIndexDescription>> {
        static final LocalSecondaryIndexDescriptionList$MemberDeserializer INSTANCE = new LocalSecondaryIndexDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<LocalSecondaryIndexDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(LocalSecondaryIndexDescription.builder().deserializeMember(deserializer, Schemas.LOCAL_SECONDARY_INDEX_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class GlobalTableWitnessDescriptionListSerializer implements BiConsumer<List<GlobalTableWitnessDescription>, ShapeSerializer> {
        static final GlobalTableWitnessDescriptionListSerializer INSTANCE = new GlobalTableWitnessDescriptionListSerializer();

        @Override
        public void accept(List<GlobalTableWitnessDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_TABLE_WITNESS_DESCRIPTION_LIST.listMember();
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

    static List<GlobalTableWitnessDescription> deserializeGlobalTableWitnessDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalTableWitnessDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalTableWitnessDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalTableWitnessDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalTableWitnessDescription>> {
        static final GlobalTableWitnessDescriptionList$MemberDeserializer INSTANCE = new GlobalTableWitnessDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<GlobalTableWitnessDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalTableWitnessDescription.builder().deserializeMember(deserializer, Schemas.GLOBAL_TABLE_WITNESS_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class GlobalSecondaryIndexDescriptionListSerializer implements BiConsumer<List<GlobalSecondaryIndexDescription>, ShapeSerializer> {
        static final GlobalSecondaryIndexDescriptionListSerializer INSTANCE = new GlobalSecondaryIndexDescriptionListSerializer();

        @Override
        public void accept(List<GlobalSecondaryIndexDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_SECONDARY_INDEX_DESCRIPTION_LIST.listMember();
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

    static List<GlobalSecondaryIndexDescription> deserializeGlobalSecondaryIndexDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalSecondaryIndexDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalSecondaryIndexDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalSecondaryIndexDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalSecondaryIndexDescription>> {
        static final GlobalSecondaryIndexDescriptionList$MemberDeserializer INSTANCE = new GlobalSecondaryIndexDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<GlobalSecondaryIndexDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalSecondaryIndexDescription.builder().deserializeMember(deserializer, Schemas.GLOBAL_SECONDARY_INDEX_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class VectorIndexListSerializer implements BiConsumer<List<VectorIndex>, ShapeSerializer> {
        static final VectorIndexListSerializer INSTANCE = new VectorIndexListSerializer();

        @Override
        public void accept(List<VectorIndex> values, ShapeSerializer serializer) {
            var $m = Schemas.VECTOR_INDEX_LIST.listMember();
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

    static List<VectorIndex> deserializeVectorIndexList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<VectorIndex> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, VectorIndexList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class VectorIndexList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<VectorIndex>> {
        static final VectorIndexList$MemberDeserializer INSTANCE = new VectorIndexList$MemberDeserializer();

        @Override
        public void accept(List<VectorIndex> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(VectorIndex.builder().deserializeMember(deserializer, Schemas.VECTOR_INDEX_LIST.listMember()).build());
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

    static final class LocalSecondaryIndexListSerializer implements BiConsumer<List<LocalSecondaryIndex>, ShapeSerializer> {
        static final LocalSecondaryIndexListSerializer INSTANCE = new LocalSecondaryIndexListSerializer();

        @Override
        public void accept(List<LocalSecondaryIndex> values, ShapeSerializer serializer) {
            var $m = Schemas.LOCAL_SECONDARY_INDEX_LIST.listMember();
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

    static List<LocalSecondaryIndex> deserializeLocalSecondaryIndexList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<LocalSecondaryIndex> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, LocalSecondaryIndexList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class LocalSecondaryIndexList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<LocalSecondaryIndex>> {
        static final LocalSecondaryIndexList$MemberDeserializer INSTANCE = new LocalSecondaryIndexList$MemberDeserializer();

        @Override
        public void accept(List<LocalSecondaryIndex> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(LocalSecondaryIndex.builder().deserializeMember(deserializer, Schemas.LOCAL_SECONDARY_INDEX_LIST.listMember()).build());
        }
    }

    static final class GlobalSecondaryIndexListSerializer implements BiConsumer<List<GlobalSecondaryIndex>, ShapeSerializer> {
        static final GlobalSecondaryIndexListSerializer INSTANCE = new GlobalSecondaryIndexListSerializer();

        @Override
        public void accept(List<GlobalSecondaryIndex> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_SECONDARY_INDEX_LIST.listMember();
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

    static List<GlobalSecondaryIndex> deserializeGlobalSecondaryIndexList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalSecondaryIndex> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalSecondaryIndexList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalSecondaryIndexList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalSecondaryIndex>> {
        static final GlobalSecondaryIndexList$MemberDeserializer INSTANCE = new GlobalSecondaryIndexList$MemberDeserializer();

        @Override
        public void accept(List<GlobalSecondaryIndex> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalSecondaryIndex.builder().deserializeMember(deserializer, Schemas.GLOBAL_SECONDARY_INDEX_LIST.listMember()).build());
        }
    }

    static final class ReplicaGlobalSecondaryIndexListSerializer implements BiConsumer<List<ReplicaGlobalSecondaryIndex>, ShapeSerializer> {
        static final ReplicaGlobalSecondaryIndexListSerializer INSTANCE = new ReplicaGlobalSecondaryIndexListSerializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndex> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_LIST.listMember();
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

    static List<ReplicaGlobalSecondaryIndex> deserializeReplicaGlobalSecondaryIndexList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaGlobalSecondaryIndex> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaGlobalSecondaryIndexList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaGlobalSecondaryIndexList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaGlobalSecondaryIndex>> {
        static final ReplicaGlobalSecondaryIndexList$MemberDeserializer INSTANCE = new ReplicaGlobalSecondaryIndexList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndex> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaGlobalSecondaryIndex.builder().deserializeMember(deserializer, Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_LIST.listMember()).build());
        }
    }

    static final class ReplicaDescriptionListSerializer implements BiConsumer<List<ReplicaDescription>, ShapeSerializer> {
        static final ReplicaDescriptionListSerializer INSTANCE = new ReplicaDescriptionListSerializer();

        @Override
        public void accept(List<ReplicaDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_DESCRIPTION_LIST.listMember();
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

    static List<ReplicaDescription> deserializeReplicaDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaDescription>> {
        static final ReplicaDescriptionList$MemberDeserializer INSTANCE = new ReplicaDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaDescription.builder().deserializeMember(deserializer, Schemas.REPLICA_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class ReplicaGlobalSecondaryIndexDescriptionListSerializer implements BiConsumer<List<ReplicaGlobalSecondaryIndexDescription>, ShapeSerializer> {
        static final ReplicaGlobalSecondaryIndexDescriptionListSerializer INSTANCE = new ReplicaGlobalSecondaryIndexDescriptionListSerializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_DESCRIPTION_LIST.listMember();
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

    static List<ReplicaGlobalSecondaryIndexDescription> deserializeReplicaGlobalSecondaryIndexDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ReplicaGlobalSecondaryIndexDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaGlobalSecondaryIndexDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaGlobalSecondaryIndexDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ReplicaGlobalSecondaryIndexDescription>> {
        static final ReplicaGlobalSecondaryIndexDescriptionList$MemberDeserializer INSTANCE = new ReplicaGlobalSecondaryIndexDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<ReplicaGlobalSecondaryIndexDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ReplicaGlobalSecondaryIndexDescription.builder().deserializeMember(deserializer, Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class ReplicaListSerializer implements BiConsumer<List<Replica>, ShapeSerializer> {
        static final ReplicaListSerializer INSTANCE = new ReplicaListSerializer();

        @Override
        public void accept(List<Replica> values, ShapeSerializer serializer) {
            var $m = Schemas.REPLICA_LIST.listMember();
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

    static List<Replica> deserializeReplicaList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Replica> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ReplicaList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ReplicaList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Replica>> {
        static final ReplicaList$MemberDeserializer INSTANCE = new ReplicaList$MemberDeserializer();

        @Override
        public void accept(List<Replica> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(Replica.builder().deserializeMember(deserializer, Schemas.REPLICA_LIST.listMember()).build());
        }
    }

    static final class ContributorInsightsSummariesSerializer implements BiConsumer<List<ContributorInsightsSummary>, ShapeSerializer> {
        static final ContributorInsightsSummariesSerializer INSTANCE = new ContributorInsightsSummariesSerializer();

        @Override
        public void accept(List<ContributorInsightsSummary> values, ShapeSerializer serializer) {
            var $m = Schemas.CONTRIBUTOR_INSIGHTS_SUMMARIES.listMember();
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

    static List<ContributorInsightsSummary> deserializeContributorInsightsSummaries(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ContributorInsightsSummary> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ContributorInsightsSummaries$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ContributorInsightsSummaries$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ContributorInsightsSummary>> {
        static final ContributorInsightsSummaries$MemberDeserializer INSTANCE = new ContributorInsightsSummaries$MemberDeserializer();

        @Override
        public void accept(List<ContributorInsightsSummary> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ContributorInsightsSummary.builder().deserializeMember(deserializer, Schemas.CONTRIBUTOR_INSIGHTS_SUMMARIES.listMember()).build());
        }
    }

    static final class ContributorInsightsRuleListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final ContributorInsightsRuleListSerializer INSTANCE = new ContributorInsightsRuleListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.CONTRIBUTOR_INSIGHTS_RULE_LIST.listMember();
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

    static List<String> deserializeContributorInsightsRuleList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ContributorInsightsRuleList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ContributorInsightsRuleList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final ContributorInsightsRuleList$MemberDeserializer INSTANCE = new ContributorInsightsRuleList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.CONTRIBUTOR_INSIGHTS_RULE_LIST.listMember()));
        }
    }

    static final class ItemCollectionSizeEstimateRangeSerializer implements BiConsumer<List<Double>, ShapeSerializer> {
        static final ItemCollectionSizeEstimateRangeSerializer INSTANCE = new ItemCollectionSizeEstimateRangeSerializer();

        @Override
        public void accept(List<Double> values, ShapeSerializer serializer) {
            var $m = Schemas.ITEM_COLLECTION_SIZE_ESTIMATE_RANGE.listMember();
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

    static List<Double> deserializeItemCollectionSizeEstimateRange(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<Double> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ItemCollectionSizeEstimateRange$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ItemCollectionSizeEstimateRange$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<Double>> {
        static final ItemCollectionSizeEstimateRange$MemberDeserializer INSTANCE = new ItemCollectionSizeEstimateRange$MemberDeserializer();

        @Override
        public void accept(List<Double> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readDouble(Schemas.ITEM_COLLECTION_SIZE_ESTIMATE_RANGE.listMember()));
        }
    }

    static final class ExpressionAttributeNameMapSerializer implements BiConsumer<Map<String, String>, MapSerializer> {
        static final ExpressionAttributeNameMapSerializer INSTANCE = new ExpressionAttributeNameMapSerializer();

        @Override
        public void accept(Map<String, String> values, MapSerializer serializer) {
            var $k = Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    ExpressionAttributeNameMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class ExpressionAttributeNameMap$ValueSerializer implements BiConsumer<String, ShapeSerializer> {
        private static final ExpressionAttributeNameMap$ValueSerializer INSTANCE = new ExpressionAttributeNameMap$ValueSerializer();

        @Override
        public void accept(String values, ShapeSerializer serializer) {
            serializer.writeString(Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP.mapValueMember(), values);
        }
    }

    static Map<String, String> deserializeExpressionAttributeNameMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, String> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, ExpressionAttributeNameMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class ExpressionAttributeNameMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, String>> {
        static final ExpressionAttributeNameMap$ValueDeserializer INSTANCE = new ExpressionAttributeNameMap$ValueDeserializer();

        @Override
        public void accept(Map<String, String> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, deserializer.readString(Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP.mapValueMember()));
        }
    }

    static final class ThrottlingReasonListSerializer implements BiConsumer<List<ThrottlingReason>, ShapeSerializer> {
        static final ThrottlingReasonListSerializer INSTANCE = new ThrottlingReasonListSerializer();

        @Override
        public void accept(List<ThrottlingReason> values, ShapeSerializer serializer) {
            var $m = Schemas.THROTTLING_REASON_LIST.listMember();
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

    static List<ThrottlingReason> deserializeThrottlingReasonList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ThrottlingReason> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ThrottlingReasonList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ThrottlingReasonList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ThrottlingReason>> {
        static final ThrottlingReasonList$MemberDeserializer INSTANCE = new ThrottlingReasonList$MemberDeserializer();

        @Override
        public void accept(List<ThrottlingReason> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ThrottlingReason.builder().deserializeMember(deserializer, Schemas.THROTTLING_REASON_LIST.listMember()).build());
        }
    }

    static final class ConsumedCapacityMultipleSerializer implements BiConsumer<List<ConsumedCapacity>, ShapeSerializer> {
        static final ConsumedCapacityMultipleSerializer INSTANCE = new ConsumedCapacityMultipleSerializer();

        @Override
        public void accept(List<ConsumedCapacity> values, ShapeSerializer serializer) {
            var $m = Schemas.CONSUMED_CAPACITY_MULTIPLE.listMember();
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

    static List<ConsumedCapacity> deserializeConsumedCapacityMultiple(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ConsumedCapacity> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, ConsumedCapacityMultiple$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class ConsumedCapacityMultiple$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ConsumedCapacity>> {
        static final ConsumedCapacityMultiple$MemberDeserializer INSTANCE = new ConsumedCapacityMultiple$MemberDeserializer();

        @Override
        public void accept(List<ConsumedCapacity> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(ConsumedCapacity.builder().deserializeMember(deserializer, Schemas.CONSUMED_CAPACITY_MULTIPLE.listMember()).build());
        }
    }

    static final class VectorIndexesCapacityMapSerializer implements BiConsumer<Map<String, VectorCapacity>, MapSerializer> {
        static final VectorIndexesCapacityMapSerializer INSTANCE = new VectorIndexesCapacityMapSerializer();

        @Override
        public void accept(Map<String, VectorCapacity> values, MapSerializer serializer) {
            var $k = Schemas.VECTOR_INDEXES_CAPACITY_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    VectorIndexesCapacityMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class VectorIndexesCapacityMap$ValueSerializer implements BiConsumer<VectorCapacity, ShapeSerializer> {
        private static final VectorIndexesCapacityMap$ValueSerializer INSTANCE = new VectorIndexesCapacityMap$ValueSerializer();

        @Override
        public void accept(VectorCapacity values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.VECTOR_INDEXES_CAPACITY_MAP.mapValueMember(), values);
        }
    }

    static Map<String, VectorCapacity> deserializeVectorIndexesCapacityMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, VectorCapacity> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, VectorIndexesCapacityMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class VectorIndexesCapacityMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, VectorCapacity>> {
        static final VectorIndexesCapacityMap$ValueDeserializer INSTANCE = new VectorIndexesCapacityMap$ValueDeserializer();

        @Override
        public void accept(Map<String, VectorCapacity> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, VectorCapacity.builder().deserializeMember(deserializer, Schemas.VECTOR_INDEXES_CAPACITY_MAP.mapValueMember()).build());
        }
    }

    static final class SecondaryIndexesCapacityMapSerializer implements BiConsumer<Map<String, Capacity>, MapSerializer> {
        static final SecondaryIndexesCapacityMapSerializer INSTANCE = new SecondaryIndexesCapacityMapSerializer();

        @Override
        public void accept(Map<String, Capacity> values, MapSerializer serializer) {
            var $k = Schemas.SECONDARY_INDEXES_CAPACITY_MAP.mapKeyMember();
            for (var valueEntry : values.entrySet()) {
                serializer.writeEntry(
                    $k,
                    valueEntry.getKey(),
                    valueEntry.getValue(),
                    SecondaryIndexesCapacityMap$ValueSerializer.INSTANCE
                );
            }
        }
    }

    private static final class SecondaryIndexesCapacityMap$ValueSerializer implements BiConsumer<Capacity, ShapeSerializer> {
        private static final SecondaryIndexesCapacityMap$ValueSerializer INSTANCE = new SecondaryIndexesCapacityMap$ValueSerializer();

        @Override
        public void accept(Capacity values, ShapeSerializer serializer) {
            serializer.writeStruct(Schemas.SECONDARY_INDEXES_CAPACITY_MAP.mapValueMember(), values);
        }
    }

    static Map<String, Capacity> deserializeSecondaryIndexesCapacityMap(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        Map<String, Capacity> result = size == -1 ? new LinkedHashMap<>() : LinkedHashMap.newLinkedHashMap(size);
        deserializer.readStringMap(schema, result, SecondaryIndexesCapacityMap$ValueDeserializer.INSTANCE);
        return result;
    }

    private static final class SecondaryIndexesCapacityMap$ValueDeserializer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, Capacity>> {
        static final SecondaryIndexesCapacityMap$ValueDeserializer INSTANCE = new SecondaryIndexesCapacityMap$ValueDeserializer();

        @Override
        public void accept(Map<String, Capacity> state, String key, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense map");
            }
            state.put(key, Capacity.builder().deserializeMember(deserializer, Schemas.SECONDARY_INDEXES_CAPACITY_MAP.mapValueMember()).build());
        }
    }

    static final class BackupSummariesSerializer implements BiConsumer<List<BackupSummary>, ShapeSerializer> {
        static final BackupSummariesSerializer INSTANCE = new BackupSummariesSerializer();

        @Override
        public void accept(List<BackupSummary> values, ShapeSerializer serializer) {
            var $m = Schemas.BACKUP_SUMMARIES.listMember();
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

    static List<BackupSummary> deserializeBackupSummaries(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<BackupSummary> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, BackupSummaries$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class BackupSummaries$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<BackupSummary>> {
        static final BackupSummaries$MemberDeserializer INSTANCE = new BackupSummaries$MemberDeserializer();

        @Override
        public void accept(List<BackupSummary> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(BackupSummary.builder().deserializeMember(deserializer, Schemas.BACKUP_SUMMARIES.listMember()).build());
        }
    }

    static final class VectorIndexesSerializer implements BiConsumer<List<VectorIndexInfo>, ShapeSerializer> {
        static final VectorIndexesSerializer INSTANCE = new VectorIndexesSerializer();

        @Override
        public void accept(List<VectorIndexInfo> values, ShapeSerializer serializer) {
            var $m = Schemas.VECTOR_INDEXES.listMember();
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

    static List<VectorIndexInfo> deserializeVectorIndexes(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<VectorIndexInfo> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, VectorIndexes$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class VectorIndexes$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<VectorIndexInfo>> {
        static final VectorIndexes$MemberDeserializer INSTANCE = new VectorIndexes$MemberDeserializer();

        @Override
        public void accept(List<VectorIndexInfo> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(VectorIndexInfo.builder().deserializeMember(deserializer, Schemas.VECTOR_INDEXES.listMember()).build());
        }
    }

    static final class SearchSchemaSerializer implements BiConsumer<List<SearchSchemaElement>, ShapeSerializer> {
        static final SearchSchemaSerializer INSTANCE = new SearchSchemaSerializer();

        @Override
        public void accept(List<SearchSchemaElement> values, ShapeSerializer serializer) {
            var $m = Schemas.SEARCH_SCHEMA.listMember();
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

    static List<SearchSchemaElement> deserializeSearchSchema(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<SearchSchemaElement> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, SearchSchema$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class SearchSchema$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<SearchSchemaElement>> {
        static final SearchSchema$MemberDeserializer INSTANCE = new SearchSchema$MemberDeserializer();

        @Override
        public void accept(List<SearchSchemaElement> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(SearchSchemaElement.builder().deserializeMember(deserializer, Schemas.SEARCH_SCHEMA.listMember()).build());
        }
    }

    static final class LocalSecondaryIndexesSerializer implements BiConsumer<List<LocalSecondaryIndexInfo>, ShapeSerializer> {
        static final LocalSecondaryIndexesSerializer INSTANCE = new LocalSecondaryIndexesSerializer();

        @Override
        public void accept(List<LocalSecondaryIndexInfo> values, ShapeSerializer serializer) {
            var $m = Schemas.LOCAL_SECONDARY_INDEXES.listMember();
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

    static List<LocalSecondaryIndexInfo> deserializeLocalSecondaryIndexes(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<LocalSecondaryIndexInfo> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, LocalSecondaryIndexes$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class LocalSecondaryIndexes$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<LocalSecondaryIndexInfo>> {
        static final LocalSecondaryIndexes$MemberDeserializer INSTANCE = new LocalSecondaryIndexes$MemberDeserializer();

        @Override
        public void accept(List<LocalSecondaryIndexInfo> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(LocalSecondaryIndexInfo.builder().deserializeMember(deserializer, Schemas.LOCAL_SECONDARY_INDEXES.listMember()).build());
        }
    }

    static final class GlobalSecondaryIndexesSerializer implements BiConsumer<List<GlobalSecondaryIndexInfo>, ShapeSerializer> {
        static final GlobalSecondaryIndexesSerializer INSTANCE = new GlobalSecondaryIndexesSerializer();

        @Override
        public void accept(List<GlobalSecondaryIndexInfo> values, ShapeSerializer serializer) {
            var $m = Schemas.GLOBAL_SECONDARY_INDEXES.listMember();
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

    static List<GlobalSecondaryIndexInfo> deserializeGlobalSecondaryIndexes(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<GlobalSecondaryIndexInfo> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, GlobalSecondaryIndexes$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class GlobalSecondaryIndexes$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<GlobalSecondaryIndexInfo>> {
        static final GlobalSecondaryIndexes$MemberDeserializer INSTANCE = new GlobalSecondaryIndexes$MemberDeserializer();

        @Override
        public void accept(List<GlobalSecondaryIndexInfo> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(GlobalSecondaryIndexInfo.builder().deserializeMember(deserializer, Schemas.GLOBAL_SECONDARY_INDEXES.listMember()).build());
        }
    }

    static final class NonKeyAttributeNameListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final NonKeyAttributeNameListSerializer INSTANCE = new NonKeyAttributeNameListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.NON_KEY_ATTRIBUTE_NAME_LIST.listMember();
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

    static List<String> deserializeNonKeyAttributeNameList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, NonKeyAttributeNameList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class NonKeyAttributeNameList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final NonKeyAttributeNameList$MemberDeserializer INSTANCE = new NonKeyAttributeNameList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.NON_KEY_ATTRIBUTE_NAME_LIST.listMember()));
        }
    }

    static final class KeySchemaSerializer implements BiConsumer<List<KeySchemaElement>, ShapeSerializer> {
        static final KeySchemaSerializer INSTANCE = new KeySchemaSerializer();

        @Override
        public void accept(List<KeySchemaElement> values, ShapeSerializer serializer) {
            var $m = Schemas.KEY_SCHEMA.listMember();
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

    static List<KeySchemaElement> deserializeKeySchema(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<KeySchemaElement> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, KeySchema$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class KeySchema$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<KeySchemaElement>> {
        static final KeySchema$MemberDeserializer INSTANCE = new KeySchema$MemberDeserializer();

        @Override
        public void accept(List<KeySchemaElement> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(KeySchemaElement.builder().deserializeMember(deserializer, Schemas.KEY_SCHEMA.listMember()).build());
        }
    }

    static final class AutoScalingPolicyDescriptionListSerializer implements BiConsumer<List<AutoScalingPolicyDescription>, ShapeSerializer> {
        static final AutoScalingPolicyDescriptionListSerializer INSTANCE = new AutoScalingPolicyDescriptionListSerializer();

        @Override
        public void accept(List<AutoScalingPolicyDescription> values, ShapeSerializer serializer) {
            var $m = Schemas.AUTO_SCALING_POLICY_DESCRIPTION_LIST.listMember();
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

    static List<AutoScalingPolicyDescription> deserializeAutoScalingPolicyDescriptionList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AutoScalingPolicyDescription> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AutoScalingPolicyDescriptionList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AutoScalingPolicyDescriptionList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AutoScalingPolicyDescription>> {
        static final AutoScalingPolicyDescriptionList$MemberDeserializer INSTANCE = new AutoScalingPolicyDescriptionList$MemberDeserializer();

        @Override
        public void accept(List<AutoScalingPolicyDescription> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AutoScalingPolicyDescription.builder().deserializeMember(deserializer, Schemas.AUTO_SCALING_POLICY_DESCRIPTION_LIST.listMember()).build());
        }
    }

    static final class AttributeNameListSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final AttributeNameListSerializer INSTANCE = new AttributeNameListSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.ATTRIBUTE_NAME_LIST.listMember();
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

    static List<String> deserializeAttributeNameList(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AttributeNameList$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AttributeNameList$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final AttributeNameList$MemberDeserializer INSTANCE = new AttributeNameList$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.ATTRIBUTE_NAME_LIST.listMember()));
        }
    }

    static final class StringSetAttributeValueSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final StringSetAttributeValueSerializer INSTANCE = new StringSetAttributeValueSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.STRING_SET_ATTRIBUTE_VALUE.listMember();
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

    static List<String> deserializeStringSetAttributeValue(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, StringSetAttributeValue$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class StringSetAttributeValue$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final StringSetAttributeValue$MemberDeserializer INSTANCE = new StringSetAttributeValue$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.STRING_SET_ATTRIBUTE_VALUE.listMember()));
        }
    }

    static final class NumberSetAttributeValueSerializer implements BiConsumer<List<String>, ShapeSerializer> {
        static final NumberSetAttributeValueSerializer INSTANCE = new NumberSetAttributeValueSerializer();

        @Override
        public void accept(List<String> values, ShapeSerializer serializer) {
            var $m = Schemas.NUMBER_SET_ATTRIBUTE_VALUE.listMember();
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

    static List<String> deserializeNumberSetAttributeValue(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<String> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, NumberSetAttributeValue$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class NumberSetAttributeValue$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<String>> {
        static final NumberSetAttributeValue$MemberDeserializer INSTANCE = new NumberSetAttributeValue$MemberDeserializer();

        @Override
        public void accept(List<String> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readString(Schemas.NUMBER_SET_ATTRIBUTE_VALUE.listMember()));
        }
    }

    static final class BinarySetAttributeValueSerializer implements BiConsumer<List<ByteBuffer>, ShapeSerializer> {
        static final BinarySetAttributeValueSerializer INSTANCE = new BinarySetAttributeValueSerializer();

        @Override
        public void accept(List<ByteBuffer> values, ShapeSerializer serializer) {
            var $m = Schemas.BINARY_SET_ATTRIBUTE_VALUE.listMember();
            if (values instanceof RandomAccess) {
                for (int i = 0, size = values.size(); i < size; i++) {
                    var value = values.get(i);
                    serializer.writeBlob($m, value);
                }
            } else {
                for (var value : values) {
                    serializer.writeBlob($m, value);
                }
            }
        }
    }

    static List<ByteBuffer> deserializeBinarySetAttributeValue(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<ByteBuffer> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, BinarySetAttributeValue$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class BinarySetAttributeValue$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<ByteBuffer>> {
        static final BinarySetAttributeValue$MemberDeserializer INSTANCE = new BinarySetAttributeValue$MemberDeserializer();

        @Override
        public void accept(List<ByteBuffer> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(deserializer.readBlob(Schemas.BINARY_SET_ATTRIBUTE_VALUE.listMember()));
        }
    }

    static final class AttributeDefinitionsSerializer implements BiConsumer<List<AttributeDefinition>, ShapeSerializer> {
        static final AttributeDefinitionsSerializer INSTANCE = new AttributeDefinitionsSerializer();

        @Override
        public void accept(List<AttributeDefinition> values, ShapeSerializer serializer) {
            var $m = Schemas.ATTRIBUTE_DEFINITIONS.listMember();
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

    static List<AttributeDefinition> deserializeAttributeDefinitions(Schema schema, ShapeDeserializer deserializer) {
        var size = Math.min(deserializer.containerSize(), deserializer.containerPreAllocationLimit());
        List<AttributeDefinition> result = size == -1 ? new ArrayList<>() : new ArrayList<>(size);
        deserializer.readList(schema, result, AttributeDefinitions$MemberDeserializer.INSTANCE);
        return result;
    }

    private static final class AttributeDefinitions$MemberDeserializer implements ShapeDeserializer.ListMemberConsumer<List<AttributeDefinition>> {
        static final AttributeDefinitions$MemberDeserializer INSTANCE = new AttributeDefinitions$MemberDeserializer();

        @Override
        public void accept(List<AttributeDefinition> state, ShapeDeserializer deserializer) {
            if (deserializer.isNull()) {
                throw new SerializationException("Null value found in dense list");
            }
            state.add(AttributeDefinition.builder().deserializeMember(deserializer, Schemas.ATTRIBUTE_DEFINITIONS.listMember()).build());
        }
    }

    private SharedSerde() {}
}
