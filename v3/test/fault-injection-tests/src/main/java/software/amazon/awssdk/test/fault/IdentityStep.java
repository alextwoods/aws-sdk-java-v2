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

/**
 * What {@link ScriptedIdentityProvider} does with one identity resolution. The pipeline resolves the identity once
 * per attempt of a signed call, so a trace lists one step per expected resolution.
 */
public enum IdentityStep {
    /** Completes with a fixed credentials identity. */
    RESOLVE,
    /** Fails with a {@link FaultInjectedException}. */
    FAIL,
    /** Never completes; the call must stop waiting for it without cancelling the provider's future. */
    HANG
}
