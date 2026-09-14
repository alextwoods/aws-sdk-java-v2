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

package software.amazon.awssdk.core.interceptor;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A fixed set of execution attribute values, captured once and written onto an {@link ExecutionAttributes} in a single
 * pass per call.
 *
 * <p>Most of the attributes the SDK seeds at the start of every call are functions of the client configuration, not of
 * the call: region, service name, endpoint and auth-scheme settings, checksum preferences, and so on. Reading each one
 * from the configuration (a hash lookup plus a type conversion) and writing it through the attribute's storage on
 * every call added up to a measurable share of a small request. A template captures the result of those writes once
 * — the client builder holds one per configuration — and {@link #applyTo} replays it as a tight loop over the ids that
 * were set.
 *
 * <p>Applying a template <em>overwrites</em> the target's values for the attributes it holds, which is the same
 * precedence the per-call {@code putAttribute}s had: client constants win over client- and request-level execution
 * attribute overrides. The template stores raw slots, so attributes whose storage is derived from another attribute's
 * slot (the pre-SRA signer attributes, for example) must not be put into a template meant for a target that may already
 * hold the underlying attribute; the attributes the SDK seeds this way all have plain storage.
 *
 * <p>Immutable once created, so safe to share across calls and threads.
 */
@SdkInternalApi
public final class ExecutionAttributesTemplate {
    private final int[] ids;
    private final Object[] values;

    private ExecutionAttributesTemplate(int[] ids, Object[] values) {
        this.ids = ids;
        this.values = values;
    }

    /**
     * Capture the attributes currently set on {@code source}. Later changes to {@code source} do not affect the template.
     */
    public static ExecutionAttributesTemplate of(ExecutionAttributes source) {
        Object[] raw = source.rawValues();
        int count = 0;
        for (Object value : raw) {
            if (value != null) {
                count++;
            }
        }
        int[] ids = new int[count];
        Object[] values = new Object[count];
        int n = 0;
        for (int id = 0; id < raw.length; id++) {
            if (raw[id] != null) {
                ids[n] = id;
                values[n] = raw[id];
                n++;
            }
        }
        return new ExecutionAttributesTemplate(ids, values);
    }

    /**
     * Write every attribute in this template onto {@code target}, replacing any value it already holds for them.
     */
    public void applyTo(ExecutionAttributes target) {
        int[] currentIds = ids;
        Object[] currentValues = values;
        for (int i = 0; i < currentIds.length; i++) {
            target.rawSet(currentIds[i], currentValues[i]);
        }
    }

    /**
     * The number of attributes in this template.
     */
    public int size() {
        return ids.length;
    }
}
