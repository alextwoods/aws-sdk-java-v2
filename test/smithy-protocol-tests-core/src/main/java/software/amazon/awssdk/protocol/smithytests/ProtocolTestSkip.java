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

package software.amazon.awssdk.protocol.smithytests;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.utils.Validate;

/**
 * One entry of {@code protocol-test-skips.json}: a protocol test case {@code id}, the non-empty reason it is skipped,
 * and optionally the protocol ShapeId (for example {@code aws.protocols#restJson1}) the skip is limited to. An entry
 * without a protocol applies to the case id under every protocol.
 */
public final class ProtocolTestSkip {
    private final String id;
    private final String reason;
    private final String protocol;

    private ProtocolTestSkip(String id, String reason, String protocol) {
        this.id = Validate.paramNotBlank(id, "id");
        this.reason = Validate.paramNotBlank(reason, "reason");
        this.protocol = protocol;
    }

    /**
     * Create an entry. {@code id} and {@code reason} must be non-blank; {@code protocol} may be null.
     */
    public static ProtocolTestSkip create(String id, String reason, String protocol) {
        if (protocol != null) {
            Validate.paramNotBlank(protocol, "protocol");
        }
        return new ProtocolTestSkip(id, reason, protocol);
    }

    public String id() {
        return id;
    }

    public String reason() {
        return reason;
    }

    /**
     * The protocol ShapeId this skip is limited to, or empty when it applies to every protocol.
     */
    public Optional<String> protocol() {
        return Optional.ofNullable(protocol);
    }

    /**
     * Whether this entry applies to the case {@code id} run under {@code protocol}.
     */
    public boolean appliesTo(String caseId, String caseProtocol) {
        return id.equals(caseId) && (protocol == null || protocol.equals(caseProtocol));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtocolTestSkip)) {
            return false;
        }
        ProtocolTestSkip that = (ProtocolTestSkip) o;
        return id.equals(that.id) && reason.equals(that.reason) && Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + reason.hashCode();
        return 31 * result + Objects.hashCode(protocol);
    }

    @Override
    public String toString() {
        return "{id=" + id + (protocol == null ? "" : ", protocol=" + protocol) + ", reason=" + reason + "}";
    }
}
