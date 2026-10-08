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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Contract violations the fakes observe while a call runs: a use beyond the end of a script, a second terminal signal
 * of one exchange, a second {@code afterExecution}, a second telemetry record, and so on. Thread-safe; shared by every
 * fake of one {@link FaultHarness} (or created by a test that wires the fakes itself). A clean call records none.
 */
public final class Violations {
    private final List<String> recorded = new CopyOnWriteArrayList<>();

    /** Records {@code violation}. Never throws: a fake reports what it saw and keeps the pipeline's contract. */
    public void record(String violation) {
        recorded.add(violation);
    }

    /** Every violation so far, in order. */
    public List<String> all() {
        return List.copyOf(recorded);
    }

    public boolean isEmpty() {
        return recorded.isEmpty();
    }

    @Override
    public String toString() {
        return recorded.toString();
    }
}
