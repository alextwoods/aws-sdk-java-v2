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

package software.amazon.awssdk.benchmark.harness;

/** An operation ended in something other than its cell's outcome; it counts as a failed operation. */
public final class UnexpectedOutcomeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnexpectedOutcomeException(BenchmarkCell cell, Throwable actual) {
        super(cell + ": expected outcome " + cell.outcome().id() + " but got "
              + (actual == null ? "a successful response" : actual.toString()), actual, false, false);
    }
}
