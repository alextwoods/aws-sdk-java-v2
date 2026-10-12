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

package software.amazon.awssdk.protocol.smithytests.tool;

import java.util.List;
import java.util.Set;

/**
 * A model-format-neutral view of a shape, so that C2J test models and Smithy test models can be fingerprinted with the
 * same {@link FeatureExtractor}.
 */
interface ShapeView {

    /** Data-model kind of a shape, coarse enough that C2J and Smithy shapes compare equal. */
    enum Kind {
        STRUCTURE, UNION, LIST, MAP, STRING, ENUM, INT_ENUM, BOOLEAN, INTEGER, FLOAT, BIG_DECIMAL, BIG_INTEGER,
        TIMESTAMP, BLOB, DOCUMENT;

        String token() {
            return name().toLowerCase(java.util.Locale.ROOT).replace("_", "");
        }
    }

    /** Where a member is bound on the wire. RPC protocols bind everything to {@link #BODY}. */
    enum Binding {
        BODY, HEADER, PREFIX_HEADERS, QUERY, QUERY_PARAMS, LABEL, GREEDY_LABEL, PAYLOAD, STATUS_CODE;

        String token() {
            return name().toLowerCase(java.util.Locale.ROOT).replace("_", "");
        }
    }

    /** A unique id for recursion detection. */
    String id();

    Kind kind();

    /** Structure and union members, in declaration order. Empty for other kinds. */
    List<MemberView> members();

    /** The member with this name, or null. */
    MemberView member(String name);

    MemberView listMember();

    MemberView mapValue();

    /** True for a streaming blob or an event stream union. */
    boolean streaming();

    /** A structure or union member, or a list or map element. */
    interface MemberView {
        String name();

        ShapeView target();

        Binding binding();

        /** The name this member uses in a serialized body (jsonName, xmlName, locationName), or {@link #name()}. */
        String serializedName();

        /** Normalized trait tokens, for example {@code xmlFlattened} or {@code timestampFormat:http-date}. */
        Set<String> traitTokens();
    }

    /** An operation of a test service. */
    interface OperationView {
        String name();

        /** Null when the operation has no input (C2J: no {@code input}; Smithy: {@code smithy.api#Unit}). */
        ShapeView input();

        /** Null when the operation has no output. */
        ShapeView output();

        /** Normalized operation trait tokens, for example {@code endpoint} or {@code requestCompression}. */
        Set<String> traitTokens();
    }
}
