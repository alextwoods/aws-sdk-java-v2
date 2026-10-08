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

package software.amazon.awssdk.test.fault;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The complete, deterministic description of one faulted call: pure immutable data, so a property test can generate,
 * shrink and print it, and an independent oracle can predict its outcome from it alone (Property 63).
 *
 * <p>Each script is consumed in order by the component it drives, one element per use; a use beyond the end of a
 * script is a harness violation ({@link FaultOutcome#violations()}), never a silent default, so a trace states every
 * interaction the call is expected to have.
 *
 * @param transmissions what the transport does with each successive transmission ({@link FaultyHttpClient})
 * @param identities what the identity provider does with each successive resolution ({@link ScriptedIdentityProvider})
 * @param signings what the signer does with each successive signing ({@link ScriptedSigner})
 * @param endpoint how every attempt's endpoint resolves ({@link FaultEndpoints})
 * @param body the call's streaming request body, if the operation has one ({@link FaultBodies})
 * @param cancel where the caller cancels the call, if at all
 * @param retry the deterministic retry strategy
 */
public record FaultTrace(List<TransportFault> transmissions, List<IdentityStep> identities,
                         List<SignerStep> signings, EndpointMode endpoint, BodyKind body, CancelPoint cancel,
                         RetrySettings retry) {
    public FaultTrace {
        transmissions = List.copyOf(transmissions);
        identities = List.copyOf(identities);
        signings = List.copyOf(signings);
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(cancel, "cancel");
        Objects.requireNonNull(retry, "retry");
    }

    /**
     * A trace of {@code transmissions} in which every identity resolution and signing succeeds, the endpoint resolves,
     * there is no streaming body and no cancellation, under {@link RetrySettings#DEFAULT}: one identity resolution and
     * one signing per transmission.
     */
    public static FaultTrace of(TransportFault... transmissions) {
        int n = transmissions.length;
        return new FaultTrace(Arrays.asList(transmissions), repeat(IdentityStep.RESOLVE, n),
                              repeat(SignerStep.SIGN, n), EndpointMode.RESOLVE, BodyKind.NONE, CancelPoint.NONE,
                              RetrySettings.DEFAULT);
    }

    public FaultTrace withTransmissions(List<TransportFault> value) {
        return new FaultTrace(value, identities, signings, endpoint, body, cancel, retry);
    }

    public FaultTrace withIdentities(IdentityStep... value) {
        return new FaultTrace(transmissions, Arrays.asList(value), signings, endpoint, body, cancel, retry);
    }

    public FaultTrace withSignings(SignerStep... value) {
        return new FaultTrace(transmissions, identities, Arrays.asList(value), endpoint, body, cancel, retry);
    }

    public FaultTrace withEndpoint(EndpointMode value) {
        return new FaultTrace(transmissions, identities, signings, value, body, cancel, retry);
    }

    public FaultTrace withBody(BodyKind value) {
        return new FaultTrace(transmissions, identities, signings, endpoint, value, cancel, retry);
    }

    public FaultTrace withCancel(CancelPoint value) {
        return new FaultTrace(transmissions, identities, signings, endpoint, body, value, retry);
    }

    public FaultTrace withRetry(RetrySettings value) {
        return new FaultTrace(transmissions, identities, signings, endpoint, body, cancel, value);
    }

    /** A compact one-line rendering for test names and failure messages. */
    public String describe() {
        List<String> parts = new ArrayList<>();
        parts.add("tx=" + transmissions.stream().map(TransportFault::label).toList());
        if (!identities.stream().allMatch(IdentityStep.RESOLVE::equals)) {
            parts.add("identity=" + identities);
        }
        if (!signings.stream().allMatch(SignerStep.SIGN::equals)) {
            parts.add("signer=" + signings);
        }
        if (endpoint != EndpointMode.RESOLVE) {
            parts.add("endpoint=" + endpoint);
        }
        if (body != BodyKind.NONE) {
            parts.add("body=" + body);
        }
        if (cancel != CancelPoint.NONE) {
            parts.add("cancel=" + cancel);
        }
        if (!retry.equals(RetrySettings.DEFAULT)) {
            parts.add("retry=" + retry);
        }
        return String.join(" ", parts);
    }

    private static <T> List<T> repeat(T value, int n) {
        List<T> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(value);
        }
        return list;
    }
}
