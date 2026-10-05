/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.enhanced.dynamodb.testing;

import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.Document;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

/**
 * One blocking view of the sync ({@link DynamoDbEnhancedClient}) and async ({@link DynamoDbEnhancedAsyncClient})
 * enhanced APIs, so the parity suites run the same scenario through both. The async view joins each future (bounded
 * by {@link Collect#TIMEOUT}) and drains each publisher one element at a time.
 */
public interface EnhancedApi {
    <T> TableApi<T> table(String name, TableSchema<T> schema);

    List<BatchGetResultPage> batchGetItem(BatchGetItemEnhancedRequest request);

    BatchWriteResult batchWriteItem(BatchWriteItemEnhancedRequest request);

    List<Document> transactGetItems(TransactGetItemsEnhancedRequest request);

    void transactWriteItems(TransactWriteItemsEnhancedRequest request);

    static EnhancedApi sync(DynamoDbEnhancedClient client) {
        return new EnhancedApi() {
            @Override
            public <T> TableApi<T> table(String name, TableSchema<T> schema) {
                DynamoDbTable<T> table = client.table(name, schema);
                return new TableApi<>() {
                    @Override
                    public MappedTableResource<T> resource() {
                        return table;
                    }

                    @Override
                    public void createTable(CreateTableEnhancedRequest request) {
                        table.createTable(request);
                    }

                    @Override
                    public PutItemEnhancedResponse<T> putItem(PutItemEnhancedRequest<T> request) {
                        return table.putItemWithResponse(request);
                    }

                    @Override
                    public T getItem(Key key) {
                        return table.getItem(key);
                    }

                    @Override
                    public T updateItem(UpdateItemEnhancedRequest<T> request) {
                        return table.updateItem(request);
                    }

                    @Override
                    public DeleteItemEnhancedResponse<T> deleteItem(Key key) {
                        return table.deleteItemWithResponse(r -> r.key(key));
                    }

                    @Override
                    public List<Page<T>> query(QueryEnhancedRequest request) {
                        return table.query(request).stream().toList();
                    }

                    @Override
                    public List<T> queryItems(QueryEnhancedRequest request) {
                        return table.query(request).items().stream().toList();
                    }

                    @Override
                    public List<Page<T>> scan(ScanEnhancedRequest request) {
                        return table.scan(request).stream().toList();
                    }

                    @Override
                    public List<Page<T>> queryIndex(String index, QueryEnhancedRequest request) {
                        return table.index(index).query(request).stream().toList();
                    }

                    @Override
                    public DescribeTableEnhancedResponse describeTable() {
                        return table.describeTable();
                    }

                    @Override
                    public void deleteTable() {
                        table.deleteTable();
                    }
                };
            }

            @Override
            public List<BatchGetResultPage> batchGetItem(BatchGetItemEnhancedRequest request) {
                return client.batchGetItem(request).stream().toList();
            }

            @Override
            public BatchWriteResult batchWriteItem(BatchWriteItemEnhancedRequest request) {
                return client.batchWriteItem(request);
            }

            @Override
            public List<Document> transactGetItems(TransactGetItemsEnhancedRequest request) {
                return client.transactGetItems(request);
            }

            @Override
            public void transactWriteItems(TransactWriteItemsEnhancedRequest request) {
                client.transactWriteItems(request);
            }
        };
    }

    static EnhancedApi async(DynamoDbEnhancedAsyncClient client) {
        return new EnhancedApi() {
            @Override
            public <T> TableApi<T> table(String name, TableSchema<T> schema) {
                DynamoDbAsyncTable<T> table = client.table(name, schema);
                return new TableApi<>() {
                    @Override
                    public MappedTableResource<T> resource() {
                        return table;
                    }

                    @Override
                    public void createTable(CreateTableEnhancedRequest request) {
                        Collect.join(table.createTable(request));
                    }

                    @Override
                    public PutItemEnhancedResponse<T> putItem(PutItemEnhancedRequest<T> request) {
                        return Collect.join(table.putItemWithResponse(request));
                    }

                    @Override
                    public T getItem(Key key) {
                        return Collect.join(table.getItem(key));
                    }

                    @Override
                    public T updateItem(UpdateItemEnhancedRequest<T> request) {
                        return Collect.join(table.updateItem(request));
                    }

                    @Override
                    public DeleteItemEnhancedResponse<T> deleteItem(Key key) {
                        return Collect.join(table.deleteItemWithResponse(r -> r.key(key)));
                    }

                    @Override
                    public List<Page<T>> query(QueryEnhancedRequest request) {
                        return Collect.join(Collect.all(table.query(request)));
                    }

                    @Override
                    public List<T> queryItems(QueryEnhancedRequest request) {
                        return Collect.join(Collect.all(table.query(request).items()));
                    }

                    @Override
                    public List<Page<T>> scan(ScanEnhancedRequest request) {
                        return Collect.join(Collect.all(table.scan(request)));
                    }

                    @Override
                    public List<Page<T>> queryIndex(String index, QueryEnhancedRequest request) {
                        return Collect.join(Collect.all(table.index(index).query(request)));
                    }

                    @Override
                    public DescribeTableEnhancedResponse describeTable() {
                        return Collect.join(table.describeTable());
                    }

                    @Override
                    public void deleteTable() {
                        Collect.join(table.deleteTable());
                    }
                };
            }

            @Override
            public List<BatchGetResultPage> batchGetItem(BatchGetItemEnhancedRequest request) {
                return Collect.join(Collect.all(client.batchGetItem(request)));
            }

            @Override
            public BatchWriteResult batchWriteItem(BatchWriteItemEnhancedRequest request) {
                return Collect.join(client.batchWriteItem(request));
            }

            @Override
            public List<Document> transactGetItems(TransactGetItemsEnhancedRequest request) {
                return Collect.join(client.transactGetItems(request));
            }

            @Override
            public void transactWriteItems(TransactWriteItemsEnhancedRequest request) {
                Collect.join(client.transactWriteItems(request));
            }
        };
    }

    /** One table of an {@link EnhancedApi}. */
    interface TableApi<T> {
        MappedTableResource<T> resource();

        void createTable(CreateTableEnhancedRequest request);

        PutItemEnhancedResponse<T> putItem(PutItemEnhancedRequest<T> request);

        default PutItemEnhancedResponse<T> putItem(T item) {
            return putItem(PutItemEnhancedRequest.builder(resource().tableSchema().itemType().rawClass())
                                                 .item(item).build());
        }

        T getItem(Key key);

        T updateItem(UpdateItemEnhancedRequest<T> request);

        DeleteItemEnhancedResponse<T> deleteItem(Key key);

        List<Page<T>> query(QueryEnhancedRequest request);

        List<T> queryItems(QueryEnhancedRequest request);

        List<Page<T>> scan(ScanEnhancedRequest request);

        List<Page<T>> queryIndex(String index, QueryEnhancedRequest request);

        DescribeTableEnhancedResponse describeTable();

        void deleteTable();
    }
}
