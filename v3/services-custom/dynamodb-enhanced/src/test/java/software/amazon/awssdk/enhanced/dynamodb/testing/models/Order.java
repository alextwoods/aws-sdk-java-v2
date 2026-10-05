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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * An annotated bean covering the mapper surface the functional suites exercise: composite primary key, a global
 * secondary index ({@code byStatus}), optimistic locking, a renamed attribute, a nested bean, a list, a map, a string
 * set, an enum and an {@link Instant}.
 */
@DynamoDbBean
public class Order {
    public enum Priority { LOW, HIGH }

    private String customerId;
    private Integer orderNumber;
    private String status;
    private Integer amount;
    private Long version;
    private String note;
    private Address shipTo;
    private List<String> lines;
    private Map<String, Integer> quantities;
    private Set<String> tags;
    private Priority priority;
    private Instant placedAt;

    public Order() {
    }

    public static Order of(String customerId, int orderNumber, String status, int amount) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setOrderNumber(orderNumber);
        order.setStatus(status);
        order.setAmount(amount);
        return order;
    }

    @DynamoDbPartitionKey
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @DynamoDbSortKey
    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "byStatus")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDbSecondarySortKey(indexNames = "byStatus")
    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    @DynamoDbVersionAttribute
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @DynamoDbAttribute("order_note")
    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Address getShipTo() {
        return shipTo;
    }

    public void setShipTo(Address shipTo) {
        this.shipTo = shipTo;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public Map<String, Integer> getQuantities() {
        return quantities;
    }

    public void setQuantities(Map<String, Integer> quantities) {
        this.quantities = quantities;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order other)) {
            return false;
        }
        return Objects.equals(customerId, other.customerId) && Objects.equals(orderNumber, other.orderNumber)
               && Objects.equals(status, other.status) && Objects.equals(amount, other.amount)
               && Objects.equals(version, other.version) && Objects.equals(note, other.note)
               && Objects.equals(shipTo, other.shipTo) && Objects.equals(lines, other.lines)
               && Objects.equals(quantities, other.quantities) && Objects.equals(tags, other.tags)
               && priority == other.priority && Objects.equals(placedAt, other.placedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, orderNumber);
    }

    @Override
    public String toString() {
        return "Order(" + customerId + "/" + orderNumber + ", status=" + status + ", amount=" + amount + ", version="
               + version + ", note=" + note + ", shipTo=" + shipTo + ", lines=" + lines + ", quantities=" + quantities
               + ", tags=" + tags + ", priority=" + priority + ", placedAt=" + placedAt + ")";
    }
}
