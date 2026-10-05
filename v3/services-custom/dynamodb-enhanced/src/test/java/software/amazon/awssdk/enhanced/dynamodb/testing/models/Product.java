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
package software.amazon.awssdk.enhanced.dynamodb.testing.models;

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** An immutable value class mapped through its builder, with an atomic counter. */
@DynamoDbImmutable(builder = Product.Builder.class)
public final class Product {
    private final String sku;
    private final String name;
    private final Long views;

    private Product(Builder b) {
        this.sku = b.sku;
        this.name = b.name;
        this.views = b.views;
    }

    public static Builder builder() {
        return new Builder();
    }

    @DynamoDbPartitionKey
    public String sku() {
        return sku;
    }

    public String name() {
        return name;
    }

    @DynamoDbAtomicCounter(startValue = 10, delta = 5)
    public Long views() {
        return views;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Product p && Objects.equals(sku, p.sku) && Objects.equals(name, p.name)
               && Objects.equals(views, p.views);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, name, views);
    }

    @Override
    public String toString() {
        return "Product(" + sku + ", " + name + ", views=" + views + ")";
    }

    public static final class Builder {
        private String sku;
        private String name;
        private Long views;

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder views(Long views) {
            this.views = views;
            return this;
        }

        public Product build() {
            return new Product(this);
        }
    }
}
