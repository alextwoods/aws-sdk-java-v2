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

package software.amazon.awssdk.dynamic.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The modeled pagination of one operation of a runtime service (Requirement 14.15): its {@code smithy.api#paginated}
 * trait merged with the service-level defaults, exactly as {@code aws-codegen} reads it through Smithy's
 * {@code PaginatedIndex} to generate the operation's paginators. Only operations with an input and an output token
 * are paginated, as for generated paginators.
 *
 * @param operationName the exact modeled operation name
 * @param inputToken the input member that carries the continuation token
 * @param outputToken the member names from the output to its continuation token, in order (at least one)
 * @param items the member names from the output to its list of items, in order; empty when no items are modeled
 * @param pageSize the input member that bounds the page size, or {@code null} when none is modeled
 */
public record DynamicPagination(String operationName, String inputToken, List<String> outputToken,
                                List<String> items, String pageSize) {

    public DynamicPagination {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(inputToken, "inputToken");
        outputToken = List.copyOf(outputToken);
        items = List.copyOf(items);
        if (outputToken.isEmpty()) {
            throw new IllegalArgumentException("A paginated operation has an output token path");
        }
    }

    /** The modeled page-size input member, if any. */
    public Optional<String> pageSizeMember() {
        return Optional.ofNullable(pageSize);
    }
}
