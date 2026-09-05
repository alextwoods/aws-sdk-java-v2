package software.amazon.awssdk.wirediff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Writes a golden capture per case to a directory.
 *
 * <p>Run this against a <em>stock</em> build of the SDK — {@code generateSmithyJavaSerde} off for S3 —
 * and commit the result. The test then runs the same cases against the bridged build and diffs. That
 * two-build shape is the only honest way to compare: both sides must go through the entire client, so
 * the diff covers marshalling, endpoint resolution, and signing rather than just serialization.
 *
 * <p>Usage: {@code CaptureMain <output-dir>}. Each case becomes two files: {@code <name>.txt}, the
 * unabridged request, and {@code <name>.normalized.txt}, the same request with the differences ledgered
 * in §12.8-12.13 erased. The test asserts on the normalized form and shows the unabridged one when it
 * fails — so the gate is not permanently red over differences that are already written down, while the
 * full bytes stay in the repo for anyone who wants to check what was normalized away.
 *
 * <p>A case that throws is written as {@code <name>.error.txt} instead of aborting the run, so one
 * broken fixture does not cost the whole capture — a stock client failing to make a call is itself
 * worth seeing in a diff.
 */
public final class CaptureMain {

    private CaptureMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("usage: CaptureMain <output-dir>");
            System.exit(2);
        }
        Path out = Paths.get(args[0]);
        Files.createDirectories(out);

        int ok = 0;
        int failed = 0;
        for (S3Cases.Case testCase : S3Cases.all()) {
            CapturingHttpClient transport = CapturingHttpClient.xml(testCase.responseXml());
            try (S3Client s3 = S3Cases.client(transport)) {
                testCase.invoke().apply(s3);
                Files.writeString(out.resolve(testCase.name() + ".txt"),
                                  WireFormat.render(transport.only()),
                                  StandardCharsets.UTF_8);
                Files.writeString(out.resolve(testCase.name() + ".normalized.txt"),
                                  WireFormat.renderIgnoringKnownDifferences(transport.only()),
                                  StandardCharsets.UTF_8);
                ok++;
                System.out.println("captured " + testCase.name());
            } catch (RuntimeException e) {
                // The request may still have been captured before the failure -- a deserialization
                // error happens after the wire bytes exist -- so write whatever was captured alongside
                // the failure. That combination is exactly what a partial-support bridge produces.
                StringBuilder sb = new StringBuilder();
                sb.append("THREW: ").append(e).append('\n');
                if (!transport.captured().isEmpty()) {
                    sb.append("--- request that was captured before the failure ---\n");
                    sb.append(WireFormat.render(transport.captured().get(0)));
                }
                Files.writeString(out.resolve(testCase.name() + ".error.txt"), sb.toString(), StandardCharsets.UTF_8);
                failed++;
                System.out.println("FAILED  " + testCase.name() + ": " + e);
            }
        }
        System.out.println("captured=" + ok + " failed=" + failed + " dir=" + out.toAbsolutePath());
    }
}
