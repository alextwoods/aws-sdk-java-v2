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

package software.amazon.awssdk.awscore.eventstream;

import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * The handler of an async operation whose output is an event stream, source-preserved from V2 (design "Event Stream
 * publishers use the same demand and cancellation contract"; Requirements 5.11, 8.4, 8.5).
 *
 * <p>The SDK calls {@link #responseReceived} once with the output's non-stream members (from the response headers, or
 * the RPC {@code initial-response} event), then {@link #onEventStream} once with the publisher of the events, which
 * decodes events only under its subscriber's demand. The call then ends exactly once: {@link #complete()} after the
 * stream completed, or {@link #exceptionOccurred} with the call's failure (a failed response, a modeled exception
 * event, an {@code error} frame, a truncated stream, or cancellation). Once the event stream is handed over the call
 * is never retried. Cancelling the event subscription, or the call's future, cancels the call and its exchange.
 *
 * @param <ResponseT> the operation's output type
 * @param <EventT> the event stream's event (union) type
 */
public interface EventStreamResponseHandler<ResponseT, EventT> {
    /** The output's non-stream members; called once, before {@link #onEventStream}. */
    void responseReceived(ResponseT response);

    /** The demand-driven publisher of the events; called once. It accepts one subscriber. */
    void onEventStream(SdkPublisher<EventT> publisher);

    /** The call's one failure; called at most once, and never with {@link #complete()}. */
    void exceptionOccurred(Throwable throwable);

    /** The event stream completed; called at most once, and never with {@link #exceptionOccurred}. */
    void complete();
}
