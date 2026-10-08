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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * A signer that runs one {@link SignerStep} per signing: it stamps {@link #SIGNATURE_HEADER} with the signing's
 * ordinal (so a test can tell which attempt's signature a transmission carries) or throws. It never reads the
 * payload. Thread-safe.
 */
public final class ScriptedSigner implements HttpSigner<AwsCredentialsIdentity> {
    /** The header a successful signing sets, to {@code sig-<n>} for the n-th signing (from 1). */
    public static final String SIGNATURE_HEADER = "X-Fault-Signature";

    private final Deque<SignerStep> script;
    private final Violations violations;
    private final AtomicInteger signings = new AtomicInteger();

    public ScriptedSigner(List<SignerStep> script, Violations violations) {
        this.script = new ArrayDeque<>(script);
        this.violations = Objects.requireNonNull(violations, "violations");
    }

    @Override
    public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
        int n = signings.incrementAndGet();
        SignerStep step;
        synchronized (this) {
            step = script.poll();
        }
        if (step == null) {
            violations.record("signer: unscripted signing " + n);
            throw new IllegalStateException("The trace scripts no further signing");
        }
        if (step == SignerStep.FAIL) {
            throw new FaultInjectedException("Injected signer failure (signing " + n + ")");
        }
        String signature = "sig-" + n;
        return new SignedRequest(request.request().toBuilder().putHeader(SIGNATURE_HEADER, signature).build(),
                                 signature);
    }

    /** How many signings the pipeline started. */
    public int signings() {
        return signings.get();
    }

    /** The scripted steps no signing has used yet. */
    public synchronized List<SignerStep> unused() {
        return List.copyOf(script);
    }
}
