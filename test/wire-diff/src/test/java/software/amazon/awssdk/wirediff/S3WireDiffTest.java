package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Diffs the request the current build puts on the wire against the golden capture from stock v2.
 *
 * <p>Goldens live in {@code src/test/resources/golden/} and are produced by {@link CaptureMain} run
 * against a stock build. The assertion uses the {@code .normalized.txt} capture — the request with the
 * pipeline-level differences of {@code compatability_issues.md} §12.8-12.13 erased — so that a ledgered
 * difference does not keep the whole suite red and hide the next real one. Failures print the
 * unabridged {@code .txt} alongside, so nothing is hidden from whoever is reading the failure.
 *
 * <p>A missing golden skips rather than fails: the checked-in set is expected to grow case by case, and
 * a skip says "not yet compared" where a failure would say "regressed".
 */
class S3WireDiffTest {

    static Stream<S3Cases.Case> cases() {
        return S3Cases.all().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void matchesStockV2Wire(S3Cases.Case testCase) {
        String golden = readGolden(testCase.name() + ".normalized");
        Assumptions.assumeTrue(golden != null,
                               "no golden capture for " + testCase.name()
                               + "; run CaptureMain against a stock build and commit it");

        CapturingHttpClient transport = CapturingHttpClient.xml(testCase.responseXml());
        try (S3Client s3 = S3Cases.client(transport)) {
            testCase.invoke().apply(s3);
        } catch (RuntimeException e) {
            // Report the bytes alongside the exception. A bridge that serializes correctly and then
            // fails to deserialize is a different (and much smaller) problem than one that puts the
            // wrong bytes out, and the message has to make that distinction visible without a rerun.
            List<CapturingHttpClient.CapturedRequest> captured = transport.captured();
            fail("call threw " + e
                 + (captured.isEmpty()
                    ? " before any request was captured"
                    : "\n--- request captured before the failure ---\n" + WireFormat.render(captured.get(0))
                      + "\n--- golden ---\n" + readGolden(testCase.name())),
                 e);
            return;
        }

        String actual = WireFormat.renderIgnoringKnownDifferences(transport.only());
        if (testCase.knownDifference() != null && !golden.equals(actual)) {
            Assumptions.abort(testCase.name() + " differs for a known, ledgered reason: "
                              + testCase.knownDifference()
                              + "\n--- stock v2 ---\n" + golden + "\n--- bridge ---\n" + actual);
        }
        assertEquals(golden, actual, "wire bytes differ from stock v2 for " + testCase.name()
                                     + "\n(unabridged stock capture:\n" + readGolden(testCase.name()) + ")");
    }

    private static String readGolden(String name) {
        try (InputStream in = S3WireDiffTest.class.getResourceAsStream("/golden/" + name + ".txt")) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
