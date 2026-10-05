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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/** A nested bean, stored as a DynamoDB map. */
@DynamoDbBean
public class Address {
    private String city;
    private Integer zip;

    public Address() {
    }

    public Address(String city, Integer zip) {
        this.city = city;
        this.zip = zip;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getZip() {
        return zip;
    }

    public void setZip(Integer zip) {
        this.zip = zip;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Address a && Objects.equals(city, a.city) && Objects.equals(zip, a.zip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, zip);
    }

    @Override
    public String toString() {
        return "Address(" + city + ", " + zip + ")";
    }
}
