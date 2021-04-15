package net.freeutils.httpserver;

import java.io.File;
import java.io.IOException;

import static net.freeutils.httpserver.HTTPServer.serveFile;

/**
 * The {@code FileContextHandler} services a context by mapping it
 * to a file or folder (recursively) on disk.
 */
final class FileContextHandler implements ContextHandler {

    protected final File base;

    public FileContextHandler(File dir) throws IOException {
        this.base = dir.getCanonicalFile();
    }

    public int serve(Request req, Response resp) throws IOException {
        return serveFile(base, req.getContext().getPath(), req, resp);
    }
}
