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
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Shared implementation of {@link AwsProtocolTrait} and {@link Rpcv2ProtocolTrait}: protocol definition traits with
 * the {@code http} and {@code eventStreamHttp} properties.
 */
@SdkInternalApi
abstract class HttpProtocolTrait implements Trait {
    static final String HTTP = "http";
    static final String EVENT_STREAM_HTTP = "eventStreamHttp";

    private final ShapeId traitId;
    private final List<String> http;
    private final List<String> eventStreamHttp;

    HttpProtocolTrait(ShapeId traitId, List<String> http, List<String> eventStreamHttp) {
        this.traitId = Validate.paramNotNull(traitId, "traitId");
        this.http = TraitValues.copyOf(http, "http");
        this.eventStreamHttp = TraitValues.copyOf(eventStreamHttp, "eventStreamHttp");
    }

    @Override
    public final ShapeId traitId() {
        return traitId;
    }

    /**
     * Returns the supported HTTP protocol versions in priority order, for example {@code ["h2", "http/1.1"]}.
     *
     * @return an unmodifiable list; empty when the model does not set the property
     */
    public final List<String> getHttp() {
        return http;
    }

    /**
     * Returns the HTTP protocol versions, in priority order, that are required when using event streams.
     *
     * @return an unmodifiable list; empty when the model does not set the property
     */
    public final List<String> getEventStreamHttp() {
        return eventStreamHttp;
    }

    /**
     * Returns the trait value. Empty lists are left out, as in smithy-model.
     *
     * @return the trait value
     */
    @Override
    public final Document toDocument() {
        Map<String, Document> members = new LinkedHashMap<>();
        if (!http.isEmpty()) {
            members.put(HTTP, TraitValues.stringList(http));
        }
        if (!eventStreamHttp.isEmpty()) {
            members.put(EVENT_STREAM_HTTP, TraitValues.stringList(eventStreamHttp));
        }
        addMembers(members);
        return Document.of(members);
    }

    /**
     * Adds subclass-specific members to the trait value.
     */
    void addMembers(Map<String, Document> members) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        HttpProtocolTrait other = (HttpProtocolTrait) o;
        return traitId.equals(other.traitId) && http.equals(other.http) && eventStreamHttp.equals(other.eventStreamHttp);
    }

    @Override
    public int hashCode() {
        int result = traitId.hashCode();
        result = 31 * result + http.hashCode();
        return 31 * result + eventStreamHttp.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder(getClass().getSimpleName())
                       .add("http", http)
                       .add("eventStreamHttp", eventStreamHttp)
                       .build();
    }

    static List<String> http(ShapeId traitId, Map<String, Document> object) {
        return TraitValues.optionalStringList(traitId, object, HTTP);
    }

    static List<String> eventStreamHttp(ShapeId traitId, Map<String, Document> object) {
        return TraitValues.optionalStringList(traitId, object, EVENT_STREAM_HTTP);
    }
}
