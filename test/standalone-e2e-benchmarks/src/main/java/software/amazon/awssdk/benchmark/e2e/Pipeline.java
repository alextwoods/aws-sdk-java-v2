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

package software.amazon.awssdk.benchmark.e2e;

/**
 * Which request pipeline a V2 client arm runs. Declared by the arm and verified against the SDK actually on
 * the classpath by {@link PipelineCheck}, because the two builds are indistinguishable by class name.
 */
enum Pipeline {
    /** V2's own request pipeline. */
    STOCK,

    /** The smithy-java bridge: the generated client delegates the whole call to a smithy-java client. */
    BRIDGED
}
