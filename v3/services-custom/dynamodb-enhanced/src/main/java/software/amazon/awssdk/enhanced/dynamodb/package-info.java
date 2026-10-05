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

/**
 * Root package of the {@code software.amazon.awssdk:dynamodb-enhanced} V3 artifact: the DynamoDB Enhanced Client.
 *
 * <p>{@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient} and
 * {@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient} keep the V2 package, type and member
 * names. They share one table, index, expression, schema and extension model and map items to the native V3
 * DynamoDB model types, calling only the public API of the generated
 * {@code software.amazon.awssdk.services.dynamodb.DynamoDbClient} and {@code DynamoDbAsyncClient}.
 *
 * <p>Allowed V3 module dependencies: {@code dynamodb}, {@code aws-core}, {@code sdk-core}. Forbidden dependencies: V2
 * and bridge internals, and internal packages of other V3 modules.
 */
package software.amazon.awssdk.enhanced.dynamodb;
