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

package software.amazon.awssdk.protocols.json;

import java.math.BigDecimal;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;

/**
 * A typed cursor over a JSON-family document (JSON text or CBOR), consumed by generated
 * {@link StructuredJsonReadable} implementations to deserialize response shapes with straight-line
 * code instead of the generic {@code SdkField} reflection loop.
 *
 * <p>Every {@code readX} method consumes exactly the tokens of one value. Structure iteration is
 * driven by the reader ({@link #readStruct}, {@link #readList}, {@link #readStringMap}) invoking a
 * caller-supplied consumer per element, so implementations can keep parse state in locals and match
 * member names without allocating.
 */
@SdkProtectedApi
public interface StructuredJsonReader {

    /**
     * Returned by {@link #nextMember} when the structure has no members left; its closing brace has been consumed.
     */
    int MEMBER_END = -1;

    /**
     * Returned by {@link #nextMember} when a member was consumed but the caller has nothing to do with it — an
     * unrecognized key, or a key whose value was JSON {@code null}. Both are skipped entirely, matching
     * {@link #readStruct}. The caller must not read a value; it should call {@link #nextMember} again.
     */
    int MEMBER_SKIPPED = -2;

    /**
     * Reads a JSON object as a structure: for each key matched by {@code table}, invokes
     * {@code consumer} with the member's ordinal, positioned to read the member's value. Members
     * with JSON {@code null} values and unknown keys are skipped entirely (the consumer is not
     * invoked), matching the generic unmarshalling loop's end state.
     */
    <T> void readStruct(T state, JsonMemberTable table, StructMemberConsumer<T> consumer);

    /**
     * Begins reading a JSON object as a structure, consuming its opening brace.
     *
     * <p>Pairs with {@link #nextMember} as a caller-driven alternative to {@link #readStruct}. {@code readStruct}
     * pushes members through a consumer, which for generated code means one consumer implementation per shape and
     * therefore a megamorphic call site inside the reader: the state object cannot be scalar-replaced and every
     * member pays a virtual dispatch. Driving the loop from the caller keeps member handling in the caller's own
     * frame, so parse state can live in locals.
     */
    void beginStruct();

    /**
     * Advances to the next member of the structure opened by {@link #beginStruct}, returning that member's ordinal
     * in {@code table} and leaving the reader positioned to read its value.
     *
     * <p>Returns {@link #MEMBER_END} when the structure is exhausted, or {@link #MEMBER_SKIPPED} when a member was
     * consumed that the caller should ignore. Callers loop until {@code MEMBER_END}.
     */
    int nextMember(JsonMemberTable table);

    /**
     * Reads a JSON array, invoking {@code consumer} once per element, positioned to read the
     * element's value. Elements may be JSON {@code null}; consumers observe that via
     * {@link #readNullIfPresent()}.
     */
    <T> void readList(T state, ListElementConsumer<T> consumer);

    /**
     * Reads a JSON object as a string-keyed map, invoking {@code consumer} once per entry with the
     * decoded key, positioned to read the entry's value. Values may be JSON {@code null}; consumers
     * observe that via {@link #readNullIfPresent()}.
     */
    <T> void readStringMap(T state, MapEntryConsumer<T> consumer);

    /**
     * If the next value is JSON {@code null}, consumes it and returns true. Otherwise leaves the
     * position unchanged and returns false.
     */
    boolean readNullIfPresent();

    String readString();

    int readInt();

    long readLong();

    short readShort();

    byte readByte();

    float readFloat();

    double readDouble();

    BigDecimal readBigDecimal();

    boolean readBoolean();

    SdkBytes readSdkBytes();

    /**
     * Reads a timestamp in the given format; {@code null} selects the protocol's wire default for
     * payload-bound timestamps.
     */
    Instant readInstant(TimestampFormatTrait.Format format);

    /**
     * Receives one structure member. {@code memberIndex} is the ordinal of the member in the
     * {@link JsonMemberTable} the caller passed to {@link #readStruct}.
     */
    @FunctionalInterface
    interface StructMemberConsumer<T> {
        void accept(T state, int memberIndex, StructuredJsonReader reader);
    }

    @FunctionalInterface
    interface ListElementConsumer<T> {
        void accept(T state, StructuredJsonReader reader);
    }

    @FunctionalInterface
    interface MapEntryConsumer<T> {
        void accept(T state, String key, StructuredJsonReader reader);
    }
}
