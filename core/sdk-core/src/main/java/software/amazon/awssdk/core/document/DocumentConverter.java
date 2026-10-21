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

package software.amazon.awssdk.core.document;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.schema.internal.LegacyDocumentAdapter;
import software.amazon.awssdk.utils.Validate;

/**
 * Converts between this package's {@link Document} and the schema serde
 * {@link software.amazon.awssdk.schema.serde.document.Document}, which also holds blobs, timestamps, structures and
 * unions. The conversion does not depend on a protocol.
 *
 * <p>Neither direction copies the document. Both return views that convert a list element or map value when it is
 * read, so later changes to the source's lists and maps are visible through the result. Converting the result of
 * {@link #toSchemaDocument(Document)} back with {@link #toLegacy} returns the original document itself.
 *
 * <p>{@link #toSchemaDocument(Document)} maps each value as follows:
 * <ul>
 *     <li>A string or boolean becomes a string or boolean document.</li>
 *     <li>A number becomes a numeric document that keeps the {@link software.amazon.awssdk.core.SdkNumber} and its
 *     string form. Its {@code type()} is the first of integer, long, bigInteger, double and bigDecimal that holds the
 *     value exactly; {@code NaN} and infinities are doubles. Coercions such as {@code asInteger()} use Java primitive
 *     conversions. A number whose string form is not a number fails when the view is created or read, with a
 *     {@link software.amazon.awssdk.schema.serde.SerializationException}.</li>
 *     <li>A list or map becomes a list or map document, with its elements and values converted in the same way.</li>
 *     <li>A null element or value becomes a Java {@code null}, the Smithy null of schema documents. The top-level
 *     {@link Document#fromNull()} becomes a null-valued document, whose {@code type()} is {@code DOCUMENT}, whose
 *     {@code asObject()} is null, and which serializers write as null.</li>
 * </ul>
 *
 * <p>{@link #toLegacy(software.amazon.awssdk.schema.serde.document.Document)} maps each value as follows:
 * <ul>
 *     <li>A string, enum or boolean becomes a string or boolean.</li>
 *     <li>A number becomes a number through the {@link Document#fromNumber} factory of its Java type (byte and short
 *     as int), so its {@link software.amazon.awssdk.core.SdkNumber} string is that type's: a double {@code 1.0} becomes
 *     {@code 1.0}.</li>
 *     <li>A blob becomes a string of its standard base64 encoding, with padding.</li>
 *     <li>A timestamp becomes a number of epoch seconds with at most three fraction digits, as the JSON protocols write
 *     {@code epoch-seconds} timestamps. Digits below a millisecond are dropped toward negative infinity.</li>
 *     <li>A list or map becomes a list or map, with its elements and values converted in the same way. A null element
 *     or value becomes {@link Document#fromNull()}.</li>
 *     <li>A structure or union becomes a map of the members that have a value, keyed by Smithy member name, without a
 *     discriminator entry.</li>
 * </ul>
 *
 * <p>Generated model classes keep their {@link Document} accessors and convert {@code document} members with these
 * rules.
 */
@SdkPublicApi
public final class DocumentConverter {
    private DocumentConverter() {
    }

    /**
     * Returns a schema document view of a {@link Document}.
     *
     * @param document the document to convert
     * @return the schema document
     * @throws NullPointerException if {@code document} is null
     */
    public static software.amazon.awssdk.schema.serde.document.Document toSchemaDocument(Document document) {
        Validate.notNull(document, "document");
        software.amazon.awssdk.schema.serde.document.Document result = LegacyDocumentAdapter.toSchema(document);
        return result == null ? LegacyDocumentAdapter.NULL : result;
    }

    /**
     * Returns a {@link Document} view of a schema document. A document returned by {@link #toSchemaDocument(Document)}
     * converts back to the document it was created from.
     *
     * @param document the schema document to convert
     * @return the document
     * @throws NullPointerException if {@code document} is null
     * @throws software.amazon.awssdk.schema.serde.SerializationException if the document, or a nested document when
     *     it is read, has a type with no {@link Document} form
     */
    public static Document toLegacy(software.amazon.awssdk.schema.serde.document.Document document) {
        Validate.notNull(document, "document");
        return LegacyDocumentAdapter.toLegacy(document);
    }
}
