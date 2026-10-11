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

package software.amazon.awssdk.schema.traits;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The {@code aws.protocols#awsQueryError} trait: the error code and HTTP status code that the {@code awsQuery}
 * protocol uses for an error shape.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait}.
 */
@SdkPublicApi
public final class AwsQueryErrorTrait implements Trait {

    /**
     * The trait's shape ID, {@code aws.protocols#awsQueryError}.
     */
    public static final ShapeId ID = ShapeId.fromParts("aws.protocols", "awsQueryError");

    private static final String CODE = "code";
    private static final String HTTP_RESPONSE_CODE = "httpResponseCode";

    private final String code;
    private final int httpResponseCode;

    /**
     * Creates the trait.
     *
     * @param code the error code, for example {@code AWS.SimpleQueueService.NonExistentQueue}
     * @param httpResponseCode the HTTP status code
     * @throws NullPointerException if {@code code} is null
     */
    public AwsQueryErrorTrait(String code, int httpResponseCode) {
        this.code = Validate.paramNotNull(code, "code");
        this.httpResponseCode = httpResponseCode;
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    @Override
    public Document toDocument() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put(CODE, Document.of(code));
        members.put(HTTP_RESPONSE_CODE, Document.of(httpResponseCode));
        return Document.of(members);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AwsQueryErrorTrait)) {
            return false;
        }
        AwsQueryErrorTrait other = (AwsQueryErrorTrait) o;
        return httpResponseCode == other.httpResponseCode && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return 31 * code.hashCode() + httpResponseCode;
    }

    @Override
    public String toString() {
        return ToString.builder("AwsQueryErrorTrait").add("code", code).add("httpResponseCode", httpResponseCode).build();
    }

    static AwsQueryErrorTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new AwsQueryErrorTrait(TraitValues.requiredString(ID, object, CODE),
                                      TraitValues.requiredInteger(ID, object, HTTP_RESPONSE_CODE));
    }
}
