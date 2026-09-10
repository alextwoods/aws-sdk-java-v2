package software.amazon.awssdk.benchmark.e2e;

/**
 * The object sizes the S3 streaming scenarios move, and the paths that encode them.
 *
 * <h2>Why three sizes</h2>
 *
 * <p>A streaming operation's cost has two parts that scale differently, and one number cannot show
 * both. At 8 KiB the per-call pipeline work — serialization, signing, interceptors, endpoint
 * resolution — is most of the operation, so that scenario measures the bridge itself. At 8 MiB the
 * bytes dominate and any per-call difference should vanish into the noise; that is the assertion worth
 * making, because a pipeline that touched the body once more than it needed to (a copy, a hash, a
 * buffer) would show up there and nowhere else. 256 KiB sits between them and catches a per-chunk
 * rather than per-call cost.
 *
 * <h2>Why the size is in the key</h2>
 *
 * <p>The mock server derives the object's length from the last path segment, so one server instance
 * serves every scenario with no state and no configuration. That keeps the server out of the
 * comparison: both arms of a paired run talk to the same process started the same way, and switching
 * scenarios does not restart or reconfigure it.
 */
final class S3Objects {

    static final String BUCKET = "benchmark-bucket";
    static final int SMALL_BYTES = 8 * 1024;
    static final int MEDIUM_BYTES = 256 * 1024;
    static final int LARGE_BYTES = 8 * 1024 * 1024;

    /** Refuses a size the scenarios do not use, so a mistyped key cannot make the server allocate 4 GiB. */
    static final int MAX_BYTES = LARGE_BYTES;

    private S3Objects() {
    }

    /** {@code objects/8192} — path-style, so the whole request is {@code /benchmark-bucket/objects/8192}. */
    static String key(int bytes) {
        return "objects/" + bytes;
    }

    /**
     * The object's bytes, positionally patterned.
     *
     * <p>Not zeros: a buffer of zeros compresses and deduplicates in ways real payloads do not, and if
     * anything in either pipeline ever grows a fast path for repeated bytes this would quietly measure
     * it. The pattern is cheap enough to generate that the server can fill a response buffer at startup
     * rather than holding one per size.
     */
    static byte[] payload(int bytes) {
        byte[] out = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            out[i] = (byte) ((i * 31 + (i >> 13)) & 0xFF);
        }
        return out;
    }

    /**
     * The object length a request path asks for, or -1 if the path is not an S3 object path.
     *
     * @param path the request URI, e.g. {@code /benchmark-bucket/objects/8192}.
     */
    static int lengthFromPath(String path) {
        String prefix = "/" + BUCKET + "/";
        if (path == null || !path.startsWith(prefix)) {
            return -1;
        }
        int lastSlash = path.lastIndexOf('/');
        try {
            int bytes = Integer.parseInt(path.substring(lastSlash + 1));
            return bytes >= 0 && bytes <= MAX_BYTES ? bytes : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
