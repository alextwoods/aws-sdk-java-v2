/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package software.amazon.awssdk.core.internal.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BufferedResponseInputStreamTest {
    @Test
    void range_exposesOnlySelectedBytesAndOriginalArray() {
        byte[] bytes = {9, 1, 2, 3, 8};
        BufferedResponseInputStream stream = new BufferedResponseInputStream(bytes, 1, 3);

        assertThat(stream.bufferUnsafe()).isSameAs(bytes);
        assertThat(stream.offset()).isOne();
        assertThat(stream.length()).isEqualTo(3);
        assertThat(stream.read()).isEqualTo(1);
        assertThat(stream.read()).isEqualTo(2);
        assertThat(stream.read()).isEqualTo(3);
        assertThat(stream.read()).isEqualTo(-1);
    }

    @Test
    void consumeToEnd_makesStreamEmptyWithoutChangingArray() {
        byte[] bytes = {1, 2, 3};
        BufferedResponseInputStream stream = new BufferedResponseInputStream(bytes, 0, bytes.length);

        stream.consumeToEnd();

        assertThat(stream.available()).isZero();
        assertThat(stream.read()).isEqualTo(-1);
        assertThat(bytes).containsExactly(1, 2, 3);
    }

    @Test
    void constructor_rejectsInvalidRanges() {
        assertThatThrownBy(() -> new BufferedResponseInputStream(new byte[3], -1, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> new BufferedResponseInputStream(new byte[3], 2, 2))
            .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> new BufferedResponseInputStream(new byte[3], 0, -1))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
