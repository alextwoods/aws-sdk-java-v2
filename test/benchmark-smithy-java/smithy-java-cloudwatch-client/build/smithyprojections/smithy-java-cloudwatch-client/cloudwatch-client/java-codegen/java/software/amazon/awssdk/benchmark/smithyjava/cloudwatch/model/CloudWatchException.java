package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import software.amazon.smithy.java.core.error.ErrorFault;
import software.amazon.smithy.java.core.error.ModeledException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Base-level exception for the service.
 *
 * <p>Some exceptions do not extend from this class, including synthetic, implicit, and shared exception types.
 */
@SmithyGenerated
public abstract class CloudWatchException extends ModeledException {
    protected CloudWatchException(
            Schema schema,
            String message,
            Throwable cause,
            ErrorFault errorType,
            Boolean captureStackTrace,
            boolean deserialized
    ) {
        super(schema, message, cause, errorType, captureStackTrace, deserialized);
    }
}
