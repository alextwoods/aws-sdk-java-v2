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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.Validate;

/**
 * Helpers for reading trait values from documents and writing them back. Every read failure is a
 * {@link SerializationException} whose message names the trait ID.
 *
 * <p>Like the smithy-model trait providers, the object readers ignore members they do not know, so that models with
 * newer trait properties still load.
 */
@SdkInternalApi
final class TraitValues {
    private static final String VALUE = "value";

    private TraitValues() {
    }

    static SerializationException invalid(ShapeId traitId, String detail) {
        return new SerializationException("Invalid value for trait " + traitId + ": " + detail);
    }

    static Map<String, Document> expectObject(ShapeId traitId, Document value) {
        return checkType(traitId, VALUE, value, ShapeType.MAP, "a map").asMap();
    }

    static String expectString(ShapeId traitId, Document value) {
        return checkType(traitId, VALUE, value, ShapeType.STRING, "a string").asString();
    }

    static int expectInteger(ShapeId traitId, Document value) {
        return toInteger(traitId, VALUE, value);
    }

    static List<String> expectStringList(ShapeId traitId, Document value) {
        return toStringList(traitId, VALUE, value);
    }

    static String requiredString(ShapeId traitId, Map<String, Document> object, String member) {
        return checkType(traitId, member(member), required(traitId, object, member), ShapeType.STRING, "a string")
            .asString();
    }

    static String optionalString(ShapeId traitId, Map<String, Document> object, String member) {
        Document value = object.get(member);
        return value == null ? null : checkType(traitId, member(member), value, ShapeType.STRING, "a string").asString();
    }

    static int requiredInteger(ShapeId traitId, Map<String, Document> object, String member) {
        return toInteger(traitId, member(member), required(traitId, object, member));
    }

    static int optionalInteger(ShapeId traitId, Map<String, Document> object, String member, int defaultValue) {
        Document value = object.get(member);
        return value == null ? defaultValue : toInteger(traitId, member(member), value);
    }

    static boolean optionalBoolean(ShapeId traitId, Map<String, Document> object, String member) {
        Document value = object.get(member);
        return value != null && checkType(traitId, member(member), value, ShapeType.BOOLEAN, "a boolean").asBoolean();
    }

    static List<String> requiredStringList(ShapeId traitId, Map<String, Document> object, String member) {
        return toStringList(traitId, member(member), required(traitId, object, member));
    }

    static List<String> optionalStringList(ShapeId traitId, Map<String, Document> object, String member) {
        Document value = object.get(member);
        return value == null ? Collections.<String>emptyList() : toStringList(traitId, member(member), value);
    }

    /**
     * Returns an unmodifiable copy of a list passed to a trait constructor, rejecting null elements.
     */
    static List<String> copyOf(List<String> values, String paramName) {
        Validate.paramNotNull(values, paramName);
        List<String> copy = new ArrayList<>(values.size());
        for (String value : values) {
            copy.add(Validate.paramNotNull(value, paramName + " element"));
        }
        return Collections.unmodifiableList(copy);
    }

    static Document stringList(List<String> values) {
        List<Document> elements = new ArrayList<>(values.size());
        for (String value : values) {
            elements.add(Document.of(value));
        }
        return Document.of(elements);
    }

    private static String member(String member) {
        return "member '" + member + "'";
    }

    private static Document required(ShapeId traitId, Map<String, Document> object, String member) {
        Document value = object.get(member);
        if (value == null) {
            throw invalid(traitId, "missing required member '" + member + "'");
        }
        return value;
    }

    private static Document checkType(ShapeId traitId, String what, Document value, ShapeType type, String expected) {
        if (value == null || value.type() != type) {
            throw invalid(traitId, what + " must be " + expected + ", but found " + describe(value));
        }
        return value;
    }

    private static int toInteger(ShapeId traitId, String what, Document value) {
        if (value != null && isNumeric(value.type())) {
            try {
                return value.asBigDecimal().intValueExact();
            } catch (ArithmeticException | SerializationException e) {
                throw invalid(traitId, what + " must be an integer, but found " + value.type() + " " + value.asNumber());
            }
        }
        throw invalid(traitId, what + " must be an integer, but found " + describe(value));
    }

    private static List<String> toStringList(ShapeId traitId, String what, Document value) {
        List<Document> elements = checkType(traitId, what, value, ShapeType.LIST, "a list of strings").asList();
        List<String> result = new ArrayList<>(elements.size());
        for (Document element : elements) {
            if (element == null || element.type() != ShapeType.STRING) {
                throw invalid(traitId, what + " must be a list of strings, but found an element of type "
                                       + describe(element));
            }
            result.add(element.asString());
        }
        return Collections.unmodifiableList(result);
    }

    private static String describe(Document value) {
        return value == null ? "null" : value.type().toString();
    }

    private static boolean isNumeric(ShapeType type) {
        switch (type) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case BIG_DECIMAL:
                return true;
            default:
                return false;
        }
    }
}
