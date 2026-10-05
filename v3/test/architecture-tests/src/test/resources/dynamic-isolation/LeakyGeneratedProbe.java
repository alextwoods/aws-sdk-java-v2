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

package probe;

import java.util.function.Function;

/**
 * Negative control: a generated call preceded by an optional, non-initializing lookup of a dynamic-model type, as a
 * runtime that probes for the dynamic layer would do. The recording loader must report the lookup whether or not the
 * dynamic layer is present.
 */
public final class LeakyGeneratedProbe implements Function<String, String> {
    @Override
    public String apply(String message) {
        try {
            Class.forName("software.amazon.awssdk.dynamic.model.DynamicModel", false,
                          LeakyGeneratedProbe.class.getClassLoader());
        } catch (ClassNotFoundException absent) {
            // The dynamic layer is not installed; carry on as a generated client would.
        }
        return new GeneratedProbe().apply(message);
    }
}
