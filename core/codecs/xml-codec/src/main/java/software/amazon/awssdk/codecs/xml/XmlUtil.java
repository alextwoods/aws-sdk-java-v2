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

package software.amazon.awssdk.codecs.xml;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.xml.internal.XmlSerdeFactory;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;

/**
 * Utilities for the XML protocols.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.XmlUtil}.
 */
@SdkPublicApi
public final class XmlUtil {

    private XmlUtil() {
    }

    /**
     * Parses the {@code Code} element value of an error response for both the AWS restXml and AWS Query protocols:
     * {@code <ErrorResponse><Error><Code>}, {@code <Response><Errors><Error><Code>} (ec2Query) or
     * {@code <Error><Code>}. Closes the deserializer.
     *
     * @param deserializer the deserializer for the error response, created by an {@link XmlCodec}
     * @return the value of the {@code Code} element
     * @throws SerializationException if the body is not an XML error response with a {@code Code}, or the
     *     deserializer was not created by an {@link XmlCodec} over a non-empty body
     */
    public static String parseErrorCodeName(ShapeDeserializer deserializer) {
        return XmlSerdeFactory.parseErrorCodeName(deserializer);
    }
}
