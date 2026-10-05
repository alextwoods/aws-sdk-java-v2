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

package software.amazon.awssdk.protocols.core.xml;

import java.util.Optional;

/**
 * The AWS XML error envelope of an error response, in any of its three forms:
 * <ul>
 *   <li>{@code <ErrorResponse><Error>...</Error><RequestId/></ErrorResponse>} ({@code awsQuery}, wrapped
 *       {@code restXml});</li>
 *   <li>{@code <Response><Errors><Error>...</Error></Errors><RequestID/></Response>} ({@code ec2Query});</li>
 *   <li>{@code <Error>...<RequestId/></Error>} ({@code restXml} with {@code noErrorWrapping}).</li>
 * </ul>
 * The {@code <Error>} element holds {@code <Code>}, optional {@code <Type>} ({@code Sender}/{@code Receiver}) and
 * {@code <Message>} (or {@code <message>}), and the modeled error's members.
 *
 * @param error the {@code <Error>} element, whose children are the modeled error's members
 * @param code the trimmed {@code <Code>}, or {@code null}
 * @param type the trimmed {@code <Type>}, or {@code null}
 * @param message the trimmed {@code <Message>}, or {@code null}
 * @param requestId the trimmed request ID of the envelope, or {@code null}
 */
public record XmlErrorEnvelope(XmlElement error, String code, String type, String message, String requestId) {

    /** The envelope rooted at {@code root}, if {@code root} is one of the three forms. */
    public static Optional<XmlErrorEnvelope> read(XmlElement root) {
        if (root == null) {
            return Optional.empty();
        }
        XmlElement error;
        String requestId;
        if (root.matches("ErrorResponse")) {
            error = root.child("Error");
            requestId = firstText(root, "RequestId", "RequestID");
        } else if (root.matches("Response")) {
            XmlElement errors = root.child("Errors");
            error = errors == null ? null : errors.child("Error");
            requestId = firstText(root, "RequestID", "RequestId");
        } else if (root.matches("Error")) {
            error = root;
            requestId = firstText(root, "RequestId", "RequestID");
        } else {
            return Optional.empty();
        }
        if (error == null) {
            return Optional.empty();
        }
        return Optional.of(new XmlErrorEnvelope(error, error.childText("Code"), error.childText("Type"),
                                                firstText(error, "Message", "message"), requestId));
    }

    private static String firstText(XmlElement element, String name, String alternative) {
        String text = element.childText(name);
        return text != null ? text : element.childText(alternative);
    }
}
