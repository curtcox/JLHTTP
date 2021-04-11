

JLHTTP - Java Lightweight HTTP Server 2.5
=========================================

Copyright © 2005-2019 Amichai Rothman



1. What is the Java Lightweight HTTP Server?

    The Java Lightweight HTTP Server is an open-source implementation of an
    HTTP Server (a.k.a. web server). It is lightweight, i.e. small and
    efficient, yet provides various useful features commonly found in
    heavier HTTP servers. It can be used both as a standalone web server,
    and as an easily embedded server for integration into existing
    applications. This is commonly used to provide a convenient GUI
    (Graphical User Interface) which can be viewed across the network in
    a cross-platform manner from any computer with a web browser (just
    about all of them).

    This server implements all functionality required by RFC 2616 ("Hypertext
    Transfer Protocol -- HTTP/1.1"), as well as some of the optional
    functionality (this is termed "conditionally compliant" in the RFC).

    Feature highlights are:

    - RFC compliant - correctness is not sacrificed for the sake of size
    - Virtual hosts - multiple domains and subdomains per server
    - File serving - built-in handler to serve files and folders from disk
    - Mime type mappings - configurable via API or a standard mime.types file
    - Directory index generation - enables browsing folder contents
    - Welcome files - configurable default filename (e.g. index.html)
    - All HTTP methods supported - GET/HEAD/OPTIONS/TRACE/POST/PUT/DELETE/custom
    - Conditional statuses - ETags and If-* header support
    - Chunked transfer encoding - for serving dynamically-generated data streams
    - Gzip/deflate compression - reduces bandwidth and download time
    - HTTPS - secures all server communications
    - Partial content - download continuation (a.k.a. byte range serving)
    - File upload - multipart/form-data handling as stream or iterator
    - Multiple context handlers - a different handler method per URL path
    - @Context annotations - auto-detection of context handler methods
    - Parameter parsing - from query string or x-www-form-urlencoded body
    - A single source file - super-easy to integrate into any application
    - Standalone - no dependencies other than the Java runtime
    - Small footprint - standard jar is ~50K, stripped jar is ~35K
    - Extensible design - easy to override, add or remove functionality
    - Reusable utility methods to simplify your custom code
    - Extensive documentation of API and implementation (>40% of source lines)

2. What can the Java Lightweight HTTP Server be used for?

    Being a lightweight, standalone, easily embeddable and tiny-footprint
    server, it is well-suited for:

    - Resource-constrained environments such as embedded devices.
      For really extreme constraints, you can easily remove unneeded
      functionality to make it even smaller (also make sure to compile
      without debug info and strip the jar of unnecessary files)
    - Unit and integration tests - fast setup/teardown times, small overhead
      and simple context handler setup make it a great web server for testing
      client components under various server response conditions.
    - Embedding a web console into any headless application for
      administration, monitoring, or a full portable GUI.
    - A full-fledged standalone web server serving static files,
      dynamically-generated content, REST APIs, pseudo-streaming, etc.
    - A good reference for learning how HTTP works under the hood.


3. How do I use the Java Lightweight HTTP Server?

    This server is intentionally written as a single source file, in order
    to make it as easy as possible to integrate into any existing project -
    by simply adding this single file to the project sources. It can also
    be used like any other library, by including the jar file in the
    application classpath.

    It is also available on Maven Central at the artifact coordinates
    net.freeutils:jlhttp:2.5.

    See the javadocs in the source file for the full API details. Examining
    the source code of the main method can be a good starting point for
    understanding how to embed the server in your application, as well.

    The server can also be run as a standalone application from the command
    line using the command 'java -jar jlhttp-2.5.jar' to serve files under
    a specified directory with no need for additional configuration.


4. License

    The Java Lightweight HTTP Server is provided under the GNU General
    Public License agreement. Please read the full license agreement
    in the included LICENSE.gpl.txt file.

    For non-GPL commercial licensing please contact the address below.


5. Contact

    Please write to support@freeutils.net with any bugs, suggestions, fixes,
    contributions, or just to drop a good word and let me know you've found
    this server useful and you'd like it to keep being maintained.

    Updates and additional info can be found at
    http://www.freeutils.net/source/jlhttp/
