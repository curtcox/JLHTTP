package net.freeutils.httpserver;

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class Utils {

    /**
     * The SimpleDateFormat-compatible formats of dates which must be supported.
     * Note that all generated date fields must be in the RFC 1123 format only,
     * while the others are supported by recipients for backwards-compatibility.
     */
    public static final String[] DATE_PATTERNS = {
            "EEE, dd MMM yyyy HH:mm:ss z", // RFC 822, updated by RFC 1123
            "EEEE, dd-MMM-yy HH:mm:ss z",  // RFC 850, obsoleted by RFC 1036
            "EEE MMM d HH:mm:ss yyyy"      // ANSI C's asctime() format
    };

    /** A GMT (UTC) timezone instance. */
    protected static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    /** Date format strings. */
    protected static final char[]
            DAYS = "Sun Mon Tue Wed Thu Fri Sat".toCharArray(),
            MONTHS = "Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".toCharArray();

    /** A convenience array containing the carriage-return and line feed chars. */
    public static final byte[] CRLF = { 0x0d, 0x0a };

    /** The HTTP status description strings. */
    protected static final String[] statuses = new String[600];

    static {
        // initialize status descriptions lookup table
        Arrays.fill(statuses, "Unknown Status");
        statuses[100] = "Continue";
        statuses[200] = "OK";
        statuses[204] = "No Content";
        statuses[206] = "Partial Content";
        statuses[301] = "Moved Permanently";
        statuses[302] = "Found";
        statuses[304] = "Not Modified";
        statuses[307] = "Temporary Redirect";
        statuses[400] = "Bad Request";
        statuses[401] = "Unauthorized";
        statuses[403] = "Forbidden";
        statuses[404] = "Not Found";
        statuses[405] = "Method Not Allowed";
        statuses[408] = "Request Timeout";
        statuses[412] = "Precondition Failed";
        statuses[413] = "Request Entity Too Large";
        statuses[414] = "Request-URI Too Large";
        statuses[416] = "Requested Range Not Satisfiable";
        statuses[417] = "Expectation Failed";
        statuses[500] = "Internal Server Error";
        statuses[501] = "Not Implemented";
        statuses[502] = "Bad Gateway";
        statuses[503] = "Service Unavailable";
        statuses[504] = "Gateway Time-out";
    }

    /**
     * A mapping of path suffixes (e.g. file extensions) to their
     * corresponding MIME types.
     */
    protected static final Map<String, String> contentTypes =
            new ConcurrentHashMap<String, String>();

    /** The MIME types that can be compressed (prefix/suffix wildcards allowed). */
    protected static String[] compressibleContentTypes =
            { "text/*", "*/javascript", "*icon", "*+xml", "*/json" };

    static {
        // add some default common content types
        // see http://www.iana.org/assignments/media-types/ for full list
        addContentType("application/font-woff", "woff");
        addContentType("application/font-woff2", "woff2");
        addContentType("application/java-archive", "jar");
        addContentType("application/javascript", "js");
        addContentType("application/json", "json");
        addContentType("application/octet-stream", "exe");
        addContentType("application/pdf", "pdf");
        addContentType("application/x-7z-compressed", "7z");
        addContentType("application/x-compressed", "tgz");
        addContentType("application/x-gzip", "gz");
        addContentType("application/x-tar", "tar");
        addContentType("application/xhtml+xml", "xhtml");
        addContentType("application/zip", "zip");
        addContentType("audio/mpeg", "mp3");
        addContentType("image/gif", "gif");
        addContentType("image/jpeg", "jpg", "jpeg");
        addContentType("image/png", "png");
        addContentType("image/svg+xml", "svg");
        addContentType("image/x-icon", "ico");
        addContentType("text/css", "css");
        addContentType("text/csv", "csv");
        addContentType("text/html; charset=utf-8", "htm", "html");
        addContentType("text/plain", "txt", "text", "log");
        addContentType("text/xml", "xml");
    }

    /**
     * Adds a Content-Type mapping for the given path suffixes.
     * If any of the path suffixes had a previous Content-Type associated
     * with it, it is replaced with the given one. Path suffixes are
     * considered case-insensitive, and contentType is converted to lowercase.
     *
     * @param contentType the content type (MIME type) to be associated with
     *        the given path suffixes
     * @param suffixes the path suffixes which will be associated with
     *        the contentType, e.g. the file extensions of served files
     *        (excluding the '.' character)
     */
    public static void addContentType(String contentType, String... suffixes) {
        for (String suffix : suffixes)
            contentTypes.put(suffix.toLowerCase(Locale.US), contentType.toLowerCase(Locale.US));
    }

    /**
     * Returns the content type for the given path, according to its suffix,
     * or the given default content type if none can be determined.
     *
     * @param path the path whose content type is requested
     * @param def a default content type which is returned if none can be
     *        determined
     * @return the content type for the given path, or the given default
     */
    public static String getContentType(String path, String def) {
        int dot = path.lastIndexOf('.');
        String type = dot < 0 ? def : contentTypes.get(path.substring(dot + 1).toLowerCase(Locale.US));
        return type != null ? type : def;
    }

    /**
     * Checks whether data of the given content type (MIME type) is compressible.
     *
     * @param contentType the content type
     * @return true if the data is compressible, false if not
     */
    public static boolean isCompressible(String contentType) {
        int pos = contentType.indexOf(';'); // exclude params
        String ct = pos < 0 ? contentType : contentType.substring(0, pos);
        for (String s : compressibleContentTypes)
            if (s.equals(ct) || s.charAt(0) == '*' && ct.endsWith(s.substring(1))
                    || s.charAt(s.length() - 1) == '*' && ct.startsWith(s.substring(0, s.length() - 1)))
                return true;
        return false;
    }

    /**
     * Returns the local host's auto-detected name.
     *
     * @return the local host name
     */
    public static String detectLocalHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException uhe) {
            return "localhost";
        }
    }

    /**
     * Parses name-value pair parameters from the given "x-www-form-urlencoded"
     * MIME-type string. This is the encoding used both for parameters passed
     * as the query of an HTTP GET method, and as the content of HTML forms
     * submitted using the HTTP POST method (as long as they use the default
     * "application/x-www-form-urlencoded" encoding in their ENCTYPE attribute).
     * UTF-8 encoding is assumed.
     * <p>
     * The parameters are returned as a list of string arrays, each containing
     * the parameter name as the first element and its corresponding value
     * as the second element (or an empty string if there is no value).
     * <p>
     * The list retains the original order of the parameters.
     *
     * @param s an "application/x-www-form-urlencoded" string
     * @return the parameter name-value pairs parsed from the given string,
     *         or an empty list if there are none
     */
    public static List<String[]> parseParamsList(String s) {
        if (s == null || s.length() == 0)
            return Collections.emptyList();
        List<String[]> params = new ArrayList<String[]>(8);
        for (String pair : split(s, "&", -1)) {
            int pos = pair.indexOf('=');
            String name = pos < 0 ? pair : pair.substring(0, pos);
            String val = pos < 0 ? "" : pair.substring(pos + 1);
            try {
                name = URLDecoder.decode(name.trim(), "UTF-8");
                val = URLDecoder.decode(val.trim(), "UTF-8");
                if (name.length() > 0)
                    params.add(new String[] { name, val });
            } catch (UnsupportedEncodingException ignore) {} // never thrown
        }
        return params;
    }

    /**
     * Converts a collection of pairs of objects (arrays of size two,
     * each representing a key and corresponding value) into a Map.
     * Duplicate keys are ignored (only the first occurrence of each key is considered).
     * The map retains the original collection's iteration order.
     *
     * @param pairs a collection of arrays, each containing a key and corresponding value
     * @param <K> the key type
     * @param <V> the value type
     * @return a map containing the paired keys and values, or an empty map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(Collection<? extends Object[]> pairs) {
        if (pairs == null || pairs.isEmpty())
            return Collections.emptyMap();
        Map<K, V> map = new LinkedHashMap<K, V>(pairs.size());
        for (Object[] pair : pairs)
            if (!map.containsKey(pair[0]))
                map.put((K)pair[0], (V)pair[1]);
        return map;
    }

    /**
     * Returns the absolute (zero-based) content range value specified
     * by the given range string. If multiple ranges are requested, a single
     * range containing all of them is returned.
     *
     * @param range the string containing the range description
     * @param length the full length of the requested resource
     * @return the requested range, or null if the range value is invalid
     */
    public static long[] parseRange(String range, long length) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        try {
            for (String token : splitElements(range, false)) {
                long start, end;
                int dash = token.indexOf('-');
                if (dash == 0) { // suffix range
                    start = length - parseULong(token.substring(1), 10);
                    end = length - 1;
                } else if (dash == token.length() - 1) { // open range
                    start = parseULong(token.substring(0, dash), 10);
                    end = length - 1;
                } else { // explicit range
                    start = parseULong(token.substring(0, dash), 10);
                    end = parseULong(token.substring(dash + 1), 10);
                }
                if (end < start)
                    throw new RuntimeException();
                if (start < min)
                    min = start;
                if (end > max)
                    max = end;
            }
            if (max < 0) // no tokens
                throw new RuntimeException();
            if (max >= length && min < length)
                max = length - 1;
            return new long[] { min, max }; // start might be >= length!
        } catch (RuntimeException re) { // NFE, IOOBE or explicit RE
            return null; // RFC2616#14.35.1 - ignore header if invalid
        }
    }

    /**
     * Parses an unsigned long value. This method behaves the same as calling
     * {@link Long#parseLong(String, int)}, but considers the string invalid
     * if it starts with an ASCII minus sign ('-') or plus sign ('+').
     *
     * @param s the String containing the long representation to be parsed
     * @param radix the radix to be used while parsing s
     * @return the long represented by s in the specified radix
     * @throws NumberFormatException if the string does not contain a parsable
     *         long, or if it starts with an ASCII minus sign or plus sign
     */
    public static long parseULong(String s, int radix) throws NumberFormatException {
        long val = Long.parseLong(s, radix); // throws NumberFormatException
        if (s.charAt(0) == '-' || s.charAt(0) == '+')
            throw new NumberFormatException("invalid digit: " + s.charAt(0));
        return val;
    }

    /**
     * Reads the token starting at the current stream position and ending at
     * the first occurrence of the given delimiter byte, in the given encoding.
     * If LF is specified as the delimiter, a CRLF pair is also treated as one.
     *
     * @param in the stream from which the token is read
     * @param delim the byte value which marks the end of the token,
     *        or -1 if the token ends at the end of the stream
     * @param enc a character-encoding name
     * @param maxLength the maximum length (in bytes) to read
     * @return the read token, excluding the delimiter
     * @throws UnsupportedEncodingException if the encoding is not supported
     * @throws EOFException if the stream end is reached before a delimiter is found
     * @throws IOException if an IO error occurs, or the maximum length
     *         is reached before the token end is reached
     */
    public static String readToken(InputStream in, int delim,
                                   String enc, int maxLength) throws IOException {
        // note: we avoid using a ByteArrayOutputStream here because it
        // suffers the overhead of synchronization for each byte written
        int b;
        int len = 0; // buffer length
        int count = 0; // number of read bytes
        byte[] buf = null; // optimization - lazy allocation only if necessary
        while ((b = in.read()) != -1 && b != delim) {
            if (count == len) { // expand buffer
                if (count == maxLength)
                    throw new IOException("token too large (" + count + ")");
                len = len > 0 ? 2 * len : 256; // start small, double each expansion
                len = maxLength < len ? maxLength : len;
                byte[] expanded = new byte[len];
                if (buf != null)
                    System.arraycopy(buf, 0, expanded, 0, count);
                buf = expanded;
            }
            buf[count++] = (byte)b;
        }
        if (b < 0 && delim != -1)
            throw new EOFException("unexpected end of stream");
        if (delim == '\n' && count > 0 && buf[count - 1] == '\r')
            count--;
        return count > 0 ? new String(buf, 0, count, enc) : "";
    }

    /**
     * Reads the ISO-8859-1 encoded string starting at the current stream
     * position and ending at the first occurrence of the LF character.
     *
     * @param in the stream from which the line is read
     * @return the read string, excluding the terminating LF character
     *         and (if exists) the CR character immediately preceding it
     * @throws EOFException if the stream end is reached before an LF character is found
     * @throws IOException if an IO error occurs, or the line is longer than 8192 bytes
     * @see #readToken(InputStream, int, String, int)
     */
    public static String readLine(InputStream in) throws IOException {
        return readToken(in, '\n', "ISO8859_1", 8192);
    }

    /**
     * Reads headers from the given stream. Headers are read according to the
     * RFC, including folded headers, element lists, and multiple headers
     * (which are concatenated into a single element list header).
     * Leading and trailing whitespace is removed.
     *
     * @param in the stream from which the headers are read
     * @return the read headers (possibly empty, if none exist)
     * @throws IOException if an IO error occurs or the headers are malformed
     *         or there are more than 100 header lines
     */
    public static Headers readHeaders(InputStream in) throws IOException {
        Headers headers = new Headers();
        String line;
        String prevLine = "";
        int count = 0;
        while ((line = readLine(in)).length() > 0) {
            int start; // start of line data (after whitespace)
            for (start = 0; start < line.length() &&
                    Character.isWhitespace(line.charAt(start)); start++);
            if (start > 0) // unfold header continuation line
                line = prevLine + ' ' + line.substring(start);
            int separator = line.indexOf(':');
            if (separator < 0)
                throw new IOException("invalid header: \"" + line + "\"");
            String name = line.substring(0, separator);
            String value = line.substring(separator + 1).trim(); // ignore LWS
            Header replaced = headers.replace(name, value);
            // concatenate repeated headers (distinguishing repeated from folded)
            if (replaced != null && start == 0) {
                value = replaced.getValue() + ", " + value;
                line = name + ": " + value;
                headers.replace(name, value);
            }
            prevLine = line;
            if (++count > 100)
                throw new IOException("too many header lines");
        }
        return headers;
    }

    /**
     * Matches the given ETag value against the given ETags. A match is found
     * if the given ETag is not null, and either the ETags contain a "*" value,
     * or one of them is identical to the given ETag. If strong comparison is
     * used, tags beginning with the weak ETag prefix "W/" never match.
     * See RFC2616#3.11, RFC2616#13.3.3.
     *
     * @param strong if true, strong comparison is used, otherwise weak
     *        comparison is used
     * @param etags the ETags to match against
     * @param etag the ETag to match
     * @return true if the ETag is matched, false otherwise
     */
    public static boolean match(boolean strong, String[] etags, String etag) {
        if (etag == null || strong && etag.startsWith("W/"))
            return false;
        for (String e : etags)
            if (e.equals("*") || (e.equals(etag) && !(strong && (e.startsWith("W/")))))
                return true;
        return false;
    }

    /**
     * Calculates the appropriate response status for the given request and
     * its resource's last-modified time and ETag, based on the conditional
     * headers present in the request.
     *
     * @param req the request
     * @param lastModified the resource's last modified time
     * @param etag the resource's ETag
     * @return the appropriate response status for the request
     */
    public static int getConditionalStatus(Request req, long lastModified, String etag) {
        Headers headers = req.getHeaders();
        // If-Match
        String header = headers.get("If-Match");
        if (header != null && !match(true, splitElements(header, false), etag))
            return 412;
        // If-Unmodified-Since
        Date date = headers.getDate("If-Unmodified-Since");
        if (date != null && lastModified > date.getTime())
            return 412;
        // If-Modified-Since
        int status = 200;
        boolean force = false;
        date = headers.getDate("If-Modified-Since");
        if (date != null && date.getTime() <= System.currentTimeMillis()) {
            if (lastModified > date.getTime())
                force = true;
            else
                status = 304;
        }
        // If-None-Match
        header = headers.get("If-None-Match");
        if (header != null) {
            if (match(false, splitElements(header, false), etag)) // RFC7232#3.2: use weak matching
                status = req.getMethod().equals("GET")
                        || req.getMethod().equals("HEAD") ? 304 : 412;
            else
                force = true;
        }
        return force ? 200 : status;
    }

    /**
     * Serves a context's contents from a file based resource.
     * <p>
     * The file is located by stripping the given context prefix from
     * the request's path, and appending the result to the given base directory.
     * <p>
     * Missing, forbidden and otherwise invalid files return the appropriate
     * error response. Directories are served as an HTML index page if the
     * virtual host allows one, or a forbidden error otherwise. Files are
     * sent with their corresponding content types, and handle conditional
     * and partial retrievals according to the RFC.
     *
     * @param base the base directory to which the context is mapped
     * @param context the context which is mapped to the base directory
     * @param req the request
     * @param resp the response into which the content is written
     * @return the HTTP status code to return, or 0 if a response was sent
     * @throws IOException if an error occurs
     */
    public static int serveFile(File base, String context,
                                Request req, Response resp) throws IOException {
        String relativePath = req.getPath().substring(context.length());
        File file = new File(base, relativePath).getCanonicalFile();
        if (!file.exists() || file.isHidden() || file.getName().startsWith(".")) {
            return 404;
        } else if (!file.canRead() || !file.getPath().startsWith(base.getPath())) { // validate
            return 403;
        } else if (file.isDirectory()) {
            if (relativePath.endsWith("/")) {
                if (!req.getVirtualHost().isAllowGeneratedIndex())
                    return 403;
                resp.send(200, createIndex(file, req.getPath()));
            } else { // redirect to the normalized directory URL ending with '/'
                resp.redirect(req.getBaseURL() + req.getPath() + "/", true);
            }
        } else if (relativePath.endsWith("/")) {
            return 404; // non-directory ending with slash (File constructor removed it)
        } else {
            serveFileContent(file, req, resp);
        }
        return 0;
    }

    /**
     * Serves the contents of a file, with its corresponding content type,
     * last modification time, etc. conditional and partial retrievals are
     * handled according to the RFC.
     *
     * @param file the existing and readable file whose contents are served
     * @param req the request
     * @param resp the response into which the content is written
     * @throws IOException if an error occurs
     */
    public static void serveFileContent(File file, Request req, Response resp) throws IOException {
        long len = file.length();
        long lastModified = file.lastModified();
        String etag = "W/\"" + lastModified + "\""; // a weak tag based on date
        int status = 200;
        // handle range or conditional request
        long[] range = req.getRange(len);
        if (range == null || len == 0) {
            status = getConditionalStatus(req, lastModified, etag);
        } else {
            String ifRange = req.getHeaders().get("If-Range");
            if (ifRange == null) {
                if (range[0] >= len)
                    status = 416; // unsatisfiable range
                else
                    status = getConditionalStatus(req, lastModified, etag);
            } else if (range[0] >= len) {
                // RFC2616#14.16, 10.4.17: invalid If-Range gets everything
                range = null;
            } else { // send either range or everything
                if (!ifRange.startsWith("\"") && !ifRange.startsWith("W/")) {
                    Date date = req.getHeaders().getDate("If-Range");
                    if (date != null && lastModified > date.getTime())
                        range = null; // modified - send everything
                } else if (!ifRange.equals(etag)) {
                    range = null; // modified - send everything
                }
            }
        }
        // send the response
        Headers respHeaders = resp.getHeaders();
        switch (status) {
            case 304: // no other headers or body allowed
                respHeaders.add("ETag", etag);
                respHeaders.add("Vary", "Accept-Encoding");
                respHeaders.add("Last-Modified", formatDate(lastModified));
                resp.sendHeaders(304);
                break;
            case 412:
                resp.sendHeaders(412);
                break;
            case 416:
                respHeaders.add("Content-Range", "bytes */" + len);
                resp.sendHeaders(416);
                break;
            case 200:
                // send OK response
                resp.sendHeaders(200, len, lastModified, etag,
                        getContentType(file.getName(), "application/octet-stream"), range);
                // send body
                InputStream in = new FileInputStream(file);
                try {
                    resp.sendBody(in, len, range);
                } finally {
                    in.close();
                }
                break;
            default:
                resp.sendHeaders(500); // should never happen
                break;
        }
    }

    /**
     * Serves the contents of a directory as an HTML file index.
     *
     * @param dir the existing and readable directory whose contents are served
     * @param path the displayed base path corresponding to dir
     * @return an HTML string containing the file index for the directory
     */
    public static String createIndex(File dir, String path) {
        if (!path.endsWith("/"))
            path += "/";
        // calculate name column width
        int w = 21; // minimum width
        for (String name : dir.list())
            if (name.length() > w)
                w = name.length();
        w += 2; // with room for added slash and space
        // note: we use apache's format, for consistent user experience
        Formatter f = new Formatter(Locale.US);
        f.format("<!DOCTYPE html>%n" +
                        "<html><head><title>Index of %s</title></head>%n" +
                        "<body><h1>Index of %s</h1>%n" +
                        "<pre> Name%" + (w - 5) + "s Last modified      Size<hr>",
                path, path, "");
        if (path.length() > 1) // add parent link if not root path
            f.format(" <a href=\"%s/\">Parent Directory</a>%"
                    + (w + 5) + "s-%n", getParentPath(path), "");
        for (File file : dir.listFiles()) {
            try {
                String name = file.getName() + (file.isDirectory() ? "/" : "");
                String size = file.isDirectory() ? "- " : toSizeApproxString(file.length());
                // properly url-encode the link
                String link = new URI(null, path + name, null).toASCIIString();
                if (!file.isHidden() && !name.startsWith("."))
                    f.format(" <a href=\"%s\">%s</a>%-" + (w - name.length()) +
                                    "s&#8206;%td-%<tb-%<tY %<tR%6s%n",
                            link, name, "", file.lastModified(), size);
            } catch (URISyntaxException ignore) {}
        }
        f.format("</pre></body></html>");
        return f.toString();
    }

    /**
     * Transfers data from an input stream to an output stream.
     *
     * @param in the input stream to transfer from
     * @param out the output stream to transfer to (or null to discard output)
     * @param len the number of bytes to transfer. If negative, the entire
     *        contents of the input stream are transferred.
     * @throws IOException if an IO error occurs or the input stream ends
     *         before the requested number of bytes have been read
     */
    public static void transfer(InputStream in, OutputStream out, long len) throws IOException {
        if (len == 0 || out == null && len < 0 && in.read() < 0)
            return; // small optimization - avoid buffer creation
        byte[] buf = new byte[4096];
        while (len != 0) {
            int count = len < 0 || buf.length < len ? buf.length : (int)len;
            count = in.read(buf, 0, count);
            if (count < 0) {
                if (len > 0)
                    throw new IOException("unexpected end of stream");
                break;
            }
            if (out != null)
                out.write(buf, 0, count);
            len -= len > 0 ? count : 0;
        }
    }

    /**
     * Adds Content-Type mappings from a standard mime.types file.
     *
     * @param in a stream containing a mime.types file
     * @throws IOException if an error occurs
     * @throws FileNotFoundException if the file is not found or cannot be read
     */
    public static void addContentTypes(InputStream in) throws IOException {
        try {
            while (true) {
                String line = readLine(in).trim(); // throws EOFException when done
                if (line.length() > 0 && line.charAt(0) != '#') {
                    String[] tokens = split(line, " \t", -1);
                    for (int i = 1; i < tokens.length; i++)
                        addContentType(tokens[0], tokens[i]);
                }
            }
        } catch (EOFException ignore) { // the end of file was reached - it's ok
        } finally {
            in.close();
        }
    }


    /**
     * Parses a date string in one of the supported {@link #DATE_PATTERNS}.
     * <p>
     * Received date header values must be in one of the following formats:
     * Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123
     * Sunday, 06-Nov-94 08:49:37 GMT ; RFC 850, obsoleted by RFC 1036
     * Sun Nov  6 08:49:37 1994       ; ANSI C's asctime() format
     *
     * @param time a string representation of a time value
     * @return the parsed date value
     * @throws IllegalArgumentException if the given string does not contain
     *         a valid date format in any of the supported formats
     */
    public static Date parseDate(String time) {
        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.US);
                df.setLenient(false);
                df.setTimeZone(GMT);
                return df.parse(time);
            } catch (ParseException ignore) {}
        }
        throw new IllegalArgumentException("invalid date format: " + time);
    }

    /**
     * Formats the given time value as a string in RFC 1123 format.
     *
     * @param time the time in milliseconds since January 1, 1970, 00:00:00 GMT
     * @return the given time value as a string in RFC 1123 format
     */
    public static String formatDate(long time) {
        // this implementation performs far better than SimpleDateFormat instances, and even
        // quite better than ThreadLocal SDFs - the server's CPU-bound benchmark gains over 20%!
        if (time < -62167392000000L || time > 253402300799999L)
            throw new IllegalArgumentException("year out of range (0001-9999): " + time);
        char[] s = "DAY, 00 MON 0000 00:00:00 GMT".toCharArray(); // copy the format template
        Calendar cal = new GregorianCalendar(GMT, Locale.US);
        cal.setTimeInMillis(time);
        System.arraycopy(DAYS, 4 * (cal.get(Calendar.DAY_OF_WEEK) - 1), s, 0, 3);
        System.arraycopy(MONTHS, 4 * cal.get(Calendar.MONTH), s, 8, 3);
        int n = cal.get(Calendar.DATE);    s[5]  += n / 10;      s[6]  += n % 10;
        n = cal.get(Calendar.YEAR);        s[12] += n / 1000;    s[13] += n / 100 % 10;
        s[14] += n / 10 % 10; s[15] += n % 10;
        n = cal.get(Calendar.HOUR_OF_DAY); s[17] += n / 10;      s[18] += n % 10;
        n = cal.get(Calendar.MINUTE);      s[20] += n / 10;      s[21] += n % 10;
        n = cal.get(Calendar.SECOND);      s[23] += n / 10;      s[24] += n % 10;
        return new String(s);
    }

    /**
     * Splits the given element list string (comma-separated header value)
     * into its constituent non-empty trimmed elements.
     * (RFC2616#2.1: element lists are delimited by a comma and optional LWS,
     * and empty elements are ignored).
     *
     * @param list the element list string
     * @param lower specifies whether the list elements should be lower-cased
     * @return the non-empty elements in the list, or an empty array
     */
    public static String[] splitElements(String list, boolean lower) {
        return split(lower && list != null ? list.toLowerCase(Locale.US) : list, ",", -1);
    }

    /**
     * Splits the given string into its constituent non-empty trimmed elements,
     * which are delimited by any of the given delimiter characters.
     * This is a more direct and efficient implementation than using a regex
     * (e.g. String.split()), trimming the elements and removing empty ones.
     *
     * @param str the string to split
     * @param delimiters the characters used as the delimiters between elements
     * @param limit if positive, limits the returned array size (remaining of str in last element)
     * @return the non-empty elements in the string, or an empty array
     */
    public static String[] split(String str, String delimiters, int limit) {
        if (str == null)
            return new String[0];
        Collection<String> elements = new ArrayList<String>();
        int len = str.length();
        int start = 0;
        int end;
        while (start < len) {
            for (end = --limit == 0 ? len : start;
                 end < len && delimiters.indexOf(str.charAt(end)) < 0; end++);
            String element = str.substring(start, end).trim();
            if (element.length() > 0)
                elements.add(element);
            start = end + 1;
        }
        return elements.toArray(new String[elements.size()]);
    }

    /**
     * Returns a string constructed by joining the string representations of the
     * iterated objects (in order), with the delimiter inserted between them.
     *
     * @param delim the delimiter that is inserted between the joined strings
     * @param items the items whose string representations are joined
     * @param <T> the item type
     * @return the joined string
     */
    public static <T> String join(String delim, Iterable<T> items) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<T> it = items.iterator(); it.hasNext(); )
            sb.append(it.next()).append(it.hasNext() ? delim : "");
        return sb.toString();
    }

    /**
     * Returns the parent of the given path.
     *
     * @param path the path whose parent is returned (must start with '/')
     * @return the parent of the given path (excluding trailing slash),
     *         or null if given path is the root path
     */
    public static String getParentPath(String path) {
        path = trimRight(path, '/'); // remove trailing slash
        int slash = path.lastIndexOf('/');
        return slash < 0 ? null : path.substring(0, slash);
    }

    /**
     * Returns the given string with all occurrences of the given character
     * removed from its right side.
     *
     * @param s the string to trim
     * @param c the character to remove
     * @return the trimmed string
     */
    public static String trimRight(String s, char c) {
        int len = s.length() - 1;
        int end;
        for (end = len; end >= 0 && s.charAt(end) == c; end--);
        return end == len ? s : s.substring(0, end + 1);
    }

    /**
     * Returns the given string with all occurrences of the given character
     * removed from its left side.
     *
     * @param s the string to trim
     * @param c the character to remove
     * @return the trimmed string
     */
    public static String trimLeft(String s, char c) {
        int len = s.length();
        int start;
        for (start = 0; start < len && s.charAt(start) == c; start++);
        return start == 0 ? s : s.substring(start);
    }

    /**
     * Trims duplicate consecutive occurrences of the given character within the
     * given string, replacing them with a single instance of the character.
     *
     * @param s the string to trim
     * @param c the character to trim
     * @return the given string with duplicate consecutive occurrences of c
     *         replaced by a single instance of c
     */
    public static String trimDuplicates(String s, char c) {
        int start = 0;
        while ((start = s.indexOf(c, start) + 1) > 0) {
            int end;
            for (end = start; end < s.length() && s.charAt(end) == c; end++);
            if (end > start)
                s = s.substring(0, start) + s.substring(end);
        }
        return s;
    }

    /**
     * Returns a human-friendly string approximating the given data size,
     * e.g. "316", "1.8K", "324M", etc.
     *
     * @param size the size to display
     * @return a human-friendly string approximating the given data size
     */
    public static String toSizeApproxString(long size) {
        final char[] units = { ' ', 'K', 'M', 'G', 'T', 'P', 'E' };
        int u;
        double s;
        for (u = 0, s = size; s >= 1000; u++, s /= 1024);
        return String.format(s < 10 ? "%.1f%c" : "%.0f%c", s, units[u]);
    }

    /**
     * Returns an HTML-escaped version of the given string for safe display
     * within a web page. The characters '&amp;', '&gt;' and '&lt;' must always
     * be escaped, and single and double quotes must be escaped within
     * attribute values; this method escapes them always. This method can
     * be used for generating both HTML and XHTML valid content.
     *
     * @param s the string to escape
     * @return the escaped string
     * @see <a href="http://www.w3.org/International/questions/qa-escapes">The W3C FAQ</a>
     */
    public static String escapeHTML(String s) {
        int len = s.length();
        StringBuilder sb = new StringBuilder(len + 30);
        int start = 0;
        for (int i = 0; i < len; i++) {
            String ref = null;
            switch (s.charAt(i)) {
                case '&': ref = "&amp;"; break;
                case '>': ref = "&gt;"; break;
                case '<': ref = "&lt;"; break;
                case '"': ref = "&quot;"; break;
                case '\'': ref = "&#39;"; break;
            }
            if (ref != null) {
                sb.append(s.substring(start, i)).append(ref);
                start = i + 1;
            }
        }
        return start == 0 ? s : sb.append(s.substring(start)).toString();
    }

    /**
     * Converts strings to bytes by casting the chars to bytes.
     * This is a fast way to encode a string as ISO-8859-1/US-ASCII bytes.
     * If multiple strings are provided, their bytes are concatenated.
     *
     * @param strings the strings to convert (containing only ISO-8859-1 chars)
     * @return the byte array
     */
    public static byte[] getBytes(String... strings) {
        int n = 0;
        for (String s : strings)
            n += s.length();
        byte[] b = new byte[n];
        n = 0;
        for (String s : strings)
            for (int i = 0, len = s.length(); i < len; i++)
                b[n++] = (byte)s.charAt(i);
        return b;
    }

}
