<!-- Copyright &copy; 1999-2016 by Amichai Rothman. All Rights Reserved. -->
# JLHTTP FAQ

## Where do I begin?

First, go over the <a href="./">JLHTTP</a> project page to get a feel for
<a href="./#whatis">what JLHTTP is</a>, <a href="./#uses">what it can be used for</a>
and <a href="./#download">where to download it</a>.</p>

If you're a hands-on kinda person, you can start playing around with it right away,
and follow the code and/or javadocs wherever they may lead you.

If you prefer doing your research first, or want to slack off at work a bit longer,
continue reading this FAQ.

##How is JLHTTP different from other lightweight servers?

There are two main types of lightweight HTTP servers we've come across:
        
1. Servers that are not all that lightweight. Some such servers boast "lightweight"
1MB binaries - that's an entirely different scale than the one we're talking about.
We prefer putting the emphasis on 'light' rather than on 'weight'.
            
1. Servers that aren't actually HTTP servers, since they don't even try to comply
with the HTTP spec. These are basically glorified ServerSocket examples.
            
JLHTTP strives to be as lightweight as possible while still being a true HTTP server.

## How lightweight is it?
    
The standard jar is ~50K.

## 50K? That's huge! Can we do better?
    
In many use cases (integration tests, embedding in large applications etc.)
it won't matter much so you can just use the standard jar - it's the same code,
just in a slightly larger jar file. But if you want something smaller,
the stripped jar is for you. It compiles the code without any debug info,
strips the jar of non-essential content such as the license file, and uses a
stronger compression utility. You can build it using the <code>-Dstripped</code>
maven option.

The stripped jar is ~35K.

## 35K? That's huge! Can we do better?

Although size is a primary concern, we do also care about readability, design,
compliance, a usable API, and practical real-world functionality. So we made some
sacrifices. The good news is, if you're really tight for space you can tweak the trade-off
yourself and reduce the size further by ripping out the parts you don't need!

For example, if you don't need file uploads, remove the multipart stuff.
You can add contexts explicitly and delete the annotation-handling code.
For embedded-only uses, you can drop the main method.
If you're only implementing dynamic content or a REST API, perhaps you can do away
with all the file-related functionality, or at least the directory index generating
code. You can remove mapped content types you don't need, and .mime.types file support.
If you remove all of the above, you can get the jar size down to **~25K**.

You can also run an obfuscator - renaming classes and methods to one-letter names
can reduce the jar size some more without much effort.

Finally, there is picky byte-counting for the truly obsessive... you may further
reduce the compiled bytecode size by deleting exception error message strings,
in-lining accessors and other simple methods, reducing imports per class,
refactoring code to be more compact (though less readable), reusing variables and
variable names, reordering local variables to utilize the local variable slots
differently, etc. You can trim off a bit more fat with all of these, though this
involves greater effort and diminishing returns and hurts the maintainability and
elegance of the code. On the bright side, you might learn a thing or two about
bytecode optimization along the way!

Let us know if you find any other optimization tricks!

## How lightweight are the minimum system requirements?
    
We don't know what the absolute minimum is. If your system can run a JVM,
it should be able to run JLHTTP. We know it's been used on an Arduino Yún
using JamVM and GNU Classpath, for example, which has about 32M of total
userspace RAM. It can certainly run on a Raspberry Pi without even a dent
in memory usage. If you have an interesting configuration, do tell!

For what it's worth, on an Oracle 64-bit Linux Java 8 JVM it can run with
<code>-Xms1m -Xmx1m</code> (although the Xmx docs say the minimum value is 2M,
and the JVM itself still takes up many MB of non-heap memory), but a
small-footprint JVM would be more appropriate in that territory anyway.

If in doubt - just give it a try! (and let us know!)

## Is it fast?
    
Given that our primary goal is remaining lightweight, and that sometimes
means forgoing optimizations that would add complexity and size, performance
is still pretty decent.

But a better question would be whether it suits your requirements: since JLHTTP
can run on an IP camera taking still photos, a smartphone accessed by another
smartphone, a laptop serving files from a spinning disk or an enterprise-grade
server providing a complex database-backed REST API...
there's no one-size-fits-all answer.

So the best answer would be: benchmark it yourself according to your specific
environment and requirements.

## How about an actual benchmark?
    
Ok, fine. On a quad-core i7-4771 desktop running Oracle JDK 8 on
(untweaked) 64-bit Linux accessing localhost (i.e. excluding network latency)
we ran JLHTTP on 2 cores (4 hyper-threads):

```
taskset -c 0,1,2,3 java -jar jlhttp-2.2.jar /tmp/benchfiles 9000
```

and ran the load generating tool several times on the other 2 cores:

```
taskset -c 4,5,6,7 wrk -t 4 -c 16 -d 60s --latency http://localhost:9000/resource
```

Here are some results (not necessarily the best - we didn't tweak anything):
    
<table>
    <thead>
    <tr>
        <th>Resource</th>
        <th>Requests/sec (sustained)</th>
        <th>Avg. Latency</th>
        <th>99% Latency</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>Minimal "Hello, World!" in-memory response</td>
        <td>297K</td>
        <td>147&micro;s</td>
        <td>3.23ms</td>
    </tr>
    <tr>
        <td>Dynamically formatted current time</td>
        <td>218K</td>
        <td>188&micro;s</td>
        <td>4.28ms</td>
    </tr>
    <tr>
        <td>2.4K file (SSD)</td>
        <td>116K</td>
        <td>1.72ms</td>
        <td>43.76ms</td>
    </tr>
    <tr>
        <td>16K file (SSD)</td>
        <td>72K</td>
        <td>1.20ms</td>
        <td>34.15ms</td>
    </tr>
    <tr>
        <td>96K file (SSD)</td>
        <td>25K</td>
        <td>1.45ms</td>
        <td>21.56ms</td>
    </tr>
    <tr>
        <td>2.8M file (SSD)</td>
        <td>1118</td>
        <td>14.41ms</td>
        <td>34.38ms</td>
    </tr>
    <tr>
        <td>404 error page</td>
        <td>170K</td>
        <td>245&micro;s</td>
        <td>5.65ms</td>
    </tr>
    </tbody>
</table>

The usual benchmarking disclaimers apply. Our main takeaway is that the
server's performance is unlikely to be your bottleneck. YMMV.

## How fast is the server startup?

To check this, we wrote a small timing benchmark that initializes a server instance,
starts it, opens a client socket to it, requests a trivial (in-memory) resource and
reads the response, stops the server, shuts down its Executor threads and closes the
client socket. That's not only initialization, but the entire lifecycle overhead,
plus an actual access of the server.

On a quad-core i7-4771 desktop running Oracle JDK 8 on 64-bit Linux with an SSD,
this takes around 100 milliseconds, including starting and shutting down the JVM itself.
Excluding the JVM initialization time, it takes around 25-30 milliseconds on the first run,
and around 1 millisecond on subsequent benchmark cycles within the same JVM.

This means the great majority of this time goes towards initial JVM initialization
and class loading the first time the server is used, which is normally incurred by any
Java application, but the net runtime is very fast.

Importantly, for use in e.g. unit or integration tests, you could set up and
tear down server instances within the test JVM tens or hundreds of times,
once or more per test, with a negligible 1 millisecond overhead each and
only a miniscule effect on the total running time.

There are no more excuses for not testing your HTTP client code properly!

## How many threads does the server use?

One thread accepts all new client connections, and immediately passes
them on to an Executor for further handling, requiring one thread per concurrently
handled connection.

By default, a cached thread pool executor is used, which spawns new threads if none
are available, reuses existing idle threads from the pool if they are available,
and closes threads that are idle for a minute.

If you want better control over the threads, such as leaving some idle threads
lying around (lower initial latency for low throughput loads) or limiting the
maximum number of threads (for better resource management under high load),
you can create your own custom Executor (or use one from the standard Executors
factory class) for the server to use:

```
server.setExecutor(myExecutor);
```

Note that if you set your own Executor, you are also responsible for shutting
it down and disposing of its resources after you shut down the server.

We currently do not support NIO, async processing or multiple concurrent connections
per thread, in order to keep the code simple, and more importantly, small and quick.

## Is it compliant with RFC 2616? RFC 7230?
    
JLHTTP originally implemented all functionality required by RFC 2616
("Hypertext Transfer Protocol -- HTTP/1.1"), as well as some of the optional
functionality. This is termed "conditionally compliant" in the RFC.

Then RFC 2616 was rewritten and split into RFCs 7230-7235 which supersede it.
Strangely, the new RFCs define the same protocol version as the old RFC,
and although most changes are clarifications and editorial changes, there are
some places where the two RFCs do contradict each other. This means that in theory,
two implementations might both support the same protocol version, yet still be
in conflict!

At some point, JLHTTP will be officially ported to comply with the new RFCs,
at which point it will reject requests that are acceptable according to the old RFC
but invalid according to the new ones. All comments and code references to RFC
sections will be updated to the new RFCs as well.

Until that cutoff date, the implementation still sides with RFC 2616
in places of conflict, which generally makes it more lenient, but otherwise
does already reference RFC 7230 and friends in some places where the two
agree, or where the behavior is better clarified in the new RFCs.

In practice, you will likely never notice the difference.
If you find any violations of either of the RFCs in the server's implementation,
please report the issue so we may fix it.

## Which charset is used to encode/decode data?
    
Anywhere the RFC specifies which charset to use, we use it.
Anywhere the RFC allows a charset to be specified as part of the protocol metadata,
we use the specified charset (if available). Everywhere else we use UTF-8.

## Can you add &#36;{feature}?


There is a long list of features we've considered adding:
JSON serialization, basic/digest authentication, a cookie API, server sessions,
NIO async socket handling, file caching, logging, serving jar resources,
and many more. All of them will prove useful in some use cases, and are
technically not that difficult to implement.

However, they all come at the cost of size and complexity, and this project's
primary goal is to remain as lightweight as possible while still being
generally useful. Deciding when to make the trade-off is a tough call.

Please let us know which existing and new features you would find most useful.
With enough user demand, we may tip the scale in favor of adding some of
the features after all (or removing them).


## How do I run a standalone server to serve files from disk?

Once you got the jar file, run the following command in a terminal:

```
java -jar jlhttp-2.5.jar &lt;directory&gt; [port]
```

where directory is the root directory that will be served by the web server
(along with everything underneath it, recursively), and port is the port on
which the server will accept client connections.

If omitted, the default port 80 will be used. Note that ports in the range
1-1024 may require running the server with root privileges on some systems.

If both parameters are omitted, a short usage reminder will be printed out.

If the JLHTTP jar file is not in the current directory, you'll need to
specify its fully qualified path.

If you need to access the server remotely, make sure your firewall is
properly configured to allow access to the port.

So for example, your command might be

```
java -jar /opt/jlhttp/jlhttp-2.5.jar /var/www 9000
```

At this point you should be able to point your browser at
<a href="http://localhost:9000/">http://localhost:9000/</a> and see the
index of the directory /var/www, from which you can download the files.

You can stop the server by pressing Ctrl-C or its equivalent.

## How do I embed JLHTTP in my own application?

First of all, you need to add it to your project. You can either -
1. add it to your maven (or equivalent) build script to be downloaded automatically from Maven Central
1. or, you can take the pre-built standard jar from the distribution zip file

2. or, you can build the standard or stripped jar from the sources in the zip file yourself

3. or, you can just copy the single source file from the zip file alongside your own sources and add it to your project

## After I add it to my project, how do I use it?

At the very least, you need to add code to start the server:

```
int port = 9000;
HTTPServer server = new HTTPServer(port);
server.start();
```

And later stop it:

```
server.stop();
```

Where you insert the code depends on your application, of course.

Now run your application, and you should be able to point your browser at
<a href="http://localhost:9000">http://localhost:9000</a> and get a
<code>404 Not Found</code> page.


## Why 404? Where's my content?

You need to add it! In order to do that, you need to add at least one context handler
to the virtual host for your domain (or the default virtual host).

## What is a virtual host?

A single HTTP server can serve content for multiple domains. For example, you might have
example.com, www.example.com, api.example.com and www.my-pet-project.com all configured
via DNS to point to the same IP address, where one server will serve them all.

In this case, you might configure 3 different virtual hosts: one named example.com
with an alias of www.example.com to serve the html, javascript, css and images for the
website, a second virtual host named api.example.com which will provide a REST API,
and a third one named www.my-pet-project.com for your unrelated pet project website.

Every server also has a default virtual host, which is used as fallback when a request
is made for a domain name that doesn't have an explicitly configured virtual host.

## How do I add a virtual host?

The default virtual host exists by default, and suffices in most cases.
You can get it like so:

```
VirtualHost host = server.getVirtualHost(null);  // default virtual host
```

If, however, you need to add additional virtual hosts, you can do so easily:

```
VirtualHost wwwHost = new VirtualHost("example.com");
wwwHost.addAlias("www.example.com"); // if it has aliases
server.addVirtualHost(wwwHost);
```

and you can later retrieve them if necessary:

```
VirtualHost wwwHost = server.getVirtualHost("www.example.com"); // or "example.com"
VirtualHost defaultHost = server.getVirtualHost(null); // default always exists
```

Once you have a virtual host instance, you can configure it, add context handlers to it, etc.

Note that all virtual host configuration must be performed before the server is started.

## What is a context handler?

Every HTTP request specifies the <i>path</i> of the resource that is being requested.
For example, "/" is the root path, and "/images/new/background.png" is the path of some
image resource. The paths are organized hierarchically, like in a file system.

Each path has a <i>handler</i> associated with it, which is simply a method that is
invoked when that path is requested, and is responsible for processing the request
and sending the appropriate response, for example updating a database record or
sending the content of a file back to the client.

Obviously, configuring a separate handler for every single resource is not
very practical, so instead, we group them into <i>contexts</i> - a context is basically
a group of paths which share a single handler method. Importantly, we associate a
unique base path with each context, and sometimes interchangeably call the base path
itself "the context".

So how do we determine which context a given path belongs to? When given a requested path,
we check if there is a context associated with it. If not, we check if its parent path has
an associated context. If not, we check the parent's parent. And so we go up the hierarchy
of paths looking for one with an associated context, and the first one we encounter is
used to handle the request. If we get all the way up to the root path, and there is no
context handler associated with the root path either, then we give up and return a
<code>404 Not Found</code> error.

For example, when a request is made for the image path "/images/new/background.png",
we'll check if there is a context associated with each of the paths
"/images/new/background.png", "/images/new", "/images" and "/", in that order, and use
the first one we find, or give up if none of them have a context.

### How do I implement a context handler?

A context handler is just a method that receives a <code>Request</code>
and <code>Response</code> and returns an integer. The request instance provides all the
request details - HTTP method, path, headers and body, as well as some helper methods
to get request parameters etc. The handler inspects the relevant request details
and decides how to respond.

The given response instance is used to send back the response, by setting
the response headers and body (if relevant). Here too there are several convenience
methods available, such as for sending commonly used and mandatory headers,
for sending a stock error response, an error response with a custom message,
a response body from a string, a response body from a stream (including
proper range handling), a redirect response, etc.

Rather than repeating the whole request/response API here, you can just read
their javadocs for details - they are pretty straightforward.

The context handler returns an integer which makes returning stock
responses (not found, forbidden, server processing error, default success response, etc.)
as easy as possible - by simply returning the appropriate HTTP status code.
If the handler already sent (not just set) any headers, or any body content,
it should return zero to prevent further processing.

Context handlers must be thread-safe, since they may be invoked concurrently
to handle requests from multiple connections on different threads.

Don't forget to add your context handler to the virtual host.

## How do I add a context handler?

Either by defining an inline anonymous class and adding it immediately:

```
host.addContext("/hello", new ContextHandler() {
    public int serve(Request req, Response resp) throws IOException {
        resp.getHeaders().add("Content-Type", "text/plain");
        resp.send(200, "Hello, World!");
        return 0;
    }
});
```

or using a lambda function (on Java 8 or later):

```
    host.addContext("/hello", (req, resp) -> {
    resp.getHeaders().add("Content-Type", "text/plain");
    resp.send(200, "Hello, World!");
    return 0;
});
```

or by defining a named class:

```
class HelloContextHandler implements ContextHandler {
    public int serve(Request req, Response resp) throws IOException {
        resp.getHeaders().add("Content-Type", "text/plain");
        resp.send(200, "Hello, World!");
        return 0;
    }
}
```

and adding it in your initialization code:

```
host.addContext("/hello", new HelloContextHandler());
```

or by adding the <code>@Context</code> annotation to a method with the proper signature:

```
class MyHandlers {
    @Context("/hello")
    public int serveHello(Request req, Response resp) throws IOException {
        resp.getHeaders().add("Content-Type", "text/plain");
        resp.send(200, "Hello, World!");
        return 0;
    }
}
```

and adding all annotated context handlers in the class at once:

```
host.addContexts(new MyHandlers()); // adds all annotated context handlers
```

## How do I add a context handler that serves files from disk?

Use the built-in <code>FileContextHandler</code> class, which takes a base directory
and serves it at the path of your choosing. For example:

```
host.addContext("/files", new FileContextHandler("/var/www/public_files"));
```

will serve the contents of the /var/www/public_files folder (and everything under it,
recursively) when requests are made under the /files path. So a request for the
/files/docs/readme.txt path will return the contents of the
/var/www/public_files/docs/readme.txt file (if it exists).

## Why can't I see the directory index page?

For security reasons, when using a <code>FileContextHandler</code> the generated
directory index page is disabled by default, and accessing a directory path results
in a <code>403 Forbidden</code> response. You can easily enable this feature:

```
host.setAllowGeneratedIndex(true);
```

## Why does the directory index page look rather quaint?
The generated index page design is based on the Apache server non-fancy
index page, sans the icons (which are too bulky for our needs). It gets the job
done with very little code. Feel free to spice it up.

## How do I process POST requests and other HTTP methods (verbs)?

When you add a context handler, it handles only GET requests by default.
The <code>addContext</code> method accepts an optional additional parameter which
specifies one or more HTTP methods which the context handler can handle:

```
host.addContext("/readme", myGetHandler); // handles GET requests
host.addContext("/readme2", myOtherGetHandler, "GET"); // handles GET requests
host.addContext("/upload", myPostHandler, "POST"); // handles POST requests
host.addContext("/form", mySmartHandler, "GET", "POST"); // handles GET and POST requests
```

The <code>@Context</code> annotation similarly accepts an optional <code>methods</code> parameter:

```
@Context(value="/form", methods={"GET", "POST"})
```

If a context handler supports more than one method, it is responsible for checking
the method of each request it receives and processing it differently if necessary.

In addition, there is built-in support for the HEAD, OPTIONS and TRACE methods,
as required by the RFC.

## How do I handle GET query strings and POST form fields?

An HTML form can be submitted using either the GET method or the POST method,
as determined by the form's "method" attribute (GET is the default if unspecified).
Often, ordinary links are also used to send additional name-value parameters
in the target URL's query string, in the same format as a form's GET request.

POST requests submitted via forms encode the name-value pairs in the request
body using the application/x-www-form-urlencoded content type by default.

You can get both types of parameters, as a name-value map, in the same way:

```
Map&lt;String, String&gt; params = request.getParams();
```

This map combines the GET parameters and the POST parameters (if any), and
is the most convenient way to get them in most cases.

Technically, the parameter names need not be unique, so duplicate-named
parameters might get lost using this simple map. For those rare cases, you can
get the parameters as an ordered list of name-value pairs instead:

```
List&lt;String[]&gt; paramsList = request.getParamsList();
```

All strings are decoded using the UTF-8 charset.

## How do I handle uploaded files?

When an HTML form is used to upload a file or other large content, the default
application/x-www-form-urlencoded encoding is insufficient. In this case, the form
is submitted as a multipart body of a POST request with the multipart/form-data
content type instead. The parts are sent in the same order as the form fields.

You can use a <code>MultipartIterator</code> to parse the request body
and iterate over its parts. String values can be conveniently retrieved
as such, but the file can be read directly as a stream, thus preventing various
issues that would arise from holding the entire file contents in memory at once.

Here's a basic example:

```
String comment;
String filename;
File file;
Iterator&lt;Part&gt; it = new MultipartIterator(request);
while (it.hasNext()) {
    Part part = it.next();
    if ("comment".equals(part.getName())) {
        comment = part.getString()
    } else if ("file".equals(part.getName())) {
        filename = part.getFilename();
        file = File.createTempFile(filename, ".uploaded");
        FileOutputStream out = new FileOutputStream(file);
        try {
            transfer(part.getBody(), out, -1);
        } finally {
            out.close();
        }
    }
}
// now do something with the comment, filename and file
```

Alternatively, you can use the lower-level <code>MultipartInputStream</code>
directly.

## How do I use SSL/TLS/HTTPS?
    
First you need to generate a key and store it in a keystore. You can use Java's
<code>keytool</code> utility for that. There are plenty of tutorials on doing this.

Next, you need to set a <code>SSLServerSocketFactory</code>. You can obtain the default
JSSE implementation by calling <code>SSLServerSocketFactory.getDefault()</code>:

```
server.setServerSocketFactory(SSLServerSocketFactory.getDefault());
```

This default implementation is configured using system properties such as
<code>javax.net.ssl.keyStore</code> and <code>javax.net.ssl.keyStorePassword</code>.
In fact, if you run JLHTTP from the command line and specify these properties,
JLHTTP will use the default SSLServerSocketFactory automatically:

```
java -Djavax.net.ssl.keyStore=/path/to/myKeystore.jks \
-Djavax.net.ssl.keyStorePassword=mypassword \
-jar jlhttp-2.5.jar /var/www 9000
```

Note that system properties specified with <code>-D</code> must come before
the executable jar or classname on the command line. You can specify any
additional SSL system properties supported by JSSE.

Alternatively, rather than using the default, you can create and configure a custom
<code>SSLContext</code> programmatically and use it to create your own
<code>SSLServerSocketFactory</code>. For example:

```
char[] password = "mypassword".toCharArray();
KeyStore ks = KeyStore.getInstance("jks");
ks.load(new FileInputStream("/path/to/myKeystore.jks"), password);
KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
kmf.init(ks, password);
SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(kmf.getKeyManagers(), null, null);
server.setServerSocketFactory(sslContext.getServerSocketFactory());
```

For additional details, see the JSSE Reference Guide.

## Why not a catchier name?

JLHTTP is descriptive and to the point, as the server aims to be.
But once you get to know it better, you may start pronouncing it <b>Jelly</b>.

## Are these questions really asked frequently?
Not really, no. We made up most of them.

## Who is "We"?
<i>...the royal "we", you know, the editorial...</i> - J. Lebowski

## What if I still have questions?
<a href="./#contact">Contact us</a>. This FAQ will be updated with good questions
and clarifications.

<a id="footer-link-homepage" href="/">www.freeutils.net</a></div>
Copyright &copy; 1999-2016 by Amichai Rothman. All Rights Reserved.</div>
<div class="right">Contact: <a id="footer-link-contact" href="mailto:support@freeutils.net">support@freeutils.net</a></div>
    &nbsp;
