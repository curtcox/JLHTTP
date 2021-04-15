package net.freeutils.httpserver;

import java.io.*;

import static net.freeutils.httpserver.Utils.*;

/**
 * The {@code ChunkedInputStream} decodes an InputStream whose data has the
 * "chunked" transfer encoding applied to it, providing the underlying data.
 */
final class ChunkedInputStream extends LimitedInputStream {

    protected Headers headers;
    protected boolean initialized;

    /**
     * Constructs a ChunkedInputStream with the given underlying stream, and
     * a headers container to which the stream's trailing headers will be
     * added.
     *
     * @param in the underlying "chunked"-encoded input stream
     * @param headers the headers container to which the stream's trailing
     *        headers will be added, or null if they are to be discarded
     * @throws NullPointerException if the given stream is null
     */
    public ChunkedInputStream(InputStream in, Headers headers) {
        super(in, 0, true);
        this.headers = headers;
    }

    @Override
    public int read() throws IOException {
        return limit <= 0 && initChunk() < 0 ? -1 : super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return limit <= 0 && initChunk() < 0 ? -1 : super.read(b, off, len);
    }

    /**
     * Initializes the next chunk. If the previous chunk has not yet
     * ended, or the end of stream has been reached, does nothing.
     *
     * @return the length of the chunk, or -1 if the end of stream
     *         has been reached
     * @throws IOException if an IO error occurs or the stream is corrupt
     */
    protected long initChunk() throws IOException {
        if (limit == 0) { // finished previous chunk
            // read chunk-terminating CRLF if it's not the first chunk
            if (initialized && readLine(in).length() > 0)
                throw new IOException("chunk data must end with CRLF");
            initialized = true;
            limit = parseChunkSize(readLine(in)); // read next chunk size
            if (limit == 0) { // last chunk has size 0
                limit = -1; // mark end of stream
                // read trailing headers, if any
                Headers trailingHeaders = readHeaders(in);
                if (headers != null)
                    headers.addAll(trailingHeaders);
            }
        }
        return limit;
    }

    /**
     * Parses a chunk-size line.
     *
     * @param line the chunk-size line to parse
     * @return the chunk size
     * @throws IllegalArgumentException if the chunk-size line is invalid
     */
    protected static long parseChunkSize(String line) throws IllegalArgumentException {
        int pos = line.indexOf(';');
        line = pos < 0 ? line : line.substring(0, pos); // ignore params, if any
        try {
            return parseULong(line, 16); // throws NFE
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(
                    "invalid chunk size line: \"" + line + "\"");
        }
    }
}
