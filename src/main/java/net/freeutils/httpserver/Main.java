package net.freeutils.httpserver;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import static net.freeutils.httpserver.Utils.addContentTypes;
import static net.freeutils.httpserver.Utils.parseULong;

/**
* For an example and a good starting point for learning how to use the API,
* see the {@link #main main} method at the bottom of the file, and follow
* the code into the API from there. Alternatively, you can just browse through
* the classes and utility methods and read their documentation and code.
 */
final class Main {
    /**
     * Starts a stand-alone HTTP server, serving files from disk.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.err.printf("Usage: java [-options] %s <directory> [port]%n" +
                        "To enable SSL: specify options -Djavax.net.ssl.keyStore, " +
                        "-Djavax.net.ssl.keyStorePassword, etc.%n", HTTPServer.class.getName());
                return;
            }
            File dir = new File(args[0]);
            if (!dir.canRead())
                throw new FileNotFoundException(dir.getAbsolutePath());
            int port = args.length < 2 ? 80 : (int)parseULong(args[1], 10);
            // set up server
            for (File f : Arrays.asList(new File("/etc/mime.types"), new File(dir, ".mime.types")))
                if (f.exists())
                    addContentTypes(new FileInputStream(f));
            HTTPServer server = new HTTPServer(port);
            if (System.getProperty("javax.net.ssl.keyStore") != null) // enable SSL if configured
                server.setServerSocketFactory(SSLServerSocketFactory.getDefault());
            VirtualHost host = server.getVirtualHost(null); // default host
            host.setAllowGeneratedIndex(true); // with directory index pages
            host.addContext("/", new FileContextHandler(dir));
            host.addContext("/api/time", new ContextHandler() {
                public int serve(Request req, Response resp) throws IOException {
                    long now = System.currentTimeMillis();
                    resp.getHeaders().add("Content-Type", "text/plain");
                    resp.send(200, String.format("%tF %<tT", now));
                    return 0;
                }
            });
            server.start();
            System.out.println("HTTPServer is listening on port " + port);
        } catch (Exception e) {
            System.err.println("error: " + e);
        }
    }

}
