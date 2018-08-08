package mogic.toad.proxyhandler;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

import mogic.toad.ToadHosts;

public class ConnectionHandler {
    private static final String TAG = "ProxyServer";

    private static boolean mForceHttps = false;

    public static boolean getForceHttps() {
        return mForceHttps;
    }

    public static void setForceHttps(boolean value) {
        mForceHttps = value;
    }

    public static void handleConnection(Socket connection) {
        try {
            String requestLine = getLine(connection.getInputStream());
            String[] splitLine = requestLine.split(" ");
            if (splitLine.length < 3) {
                connection.close();
                return;
            }
            String requestType = splitLine[0];
            String urlString = splitLine[1];
            String httpVersion = splitLine[2];

            URI url = null;
            String host;
            int port;
            boolean isHttpConnectMethod = requestType.equals("CONNECT");

            if (isHttpConnectMethod) {
                String[] hostPortSplit = urlString.split(":");
                host = hostPortSplit[0];
                // Use default SSL port if not specified. Parse it otherwise
                if (hostPortSplit.length < 2) {
                    port = 443;
                } else {
                    try {
                        port = Integer.parseInt(hostPortSplit[1]);
                    } catch (NumberFormatException nfe) {
                        connection.close();
                        return;
                    }
                }
            } else {
                try {
                    url = new URI(urlString);
                    host = url.getHost();
                    port = url.getPort();
                    if (port < 0) {
                        port = 80;
                    }
                } catch (URISyntaxException e) {
                    connection.close();
                    return;
                }
            }

            String newHost = ToadHosts.lookup(host);
            if (newHost != null) {
                if (newHost == "0.0.0.0") {
                    connection.close();
                    return;
                }
                if (!isHttpConnectMethod && port == 80 && mForceHttps) {
                    sendLine(connection, "HTTP/1.1 301 Moved Permanently");
                    sendLine(connection, "Location: https://" + host + getAbsolutePathFromAbsoluteURI(url));
                    sendLine(connection, "Content-Type: text/html");
                    sendLine(connection, "Content-Length: 0");
                    sendLine(connection, "");
                    connection.close();
                    return;
                }
                host = newHost;
            }

            Socket server = new Socket(host, port);
            if (isHttpConnectMethod) {
                skipToRequestBody(connection);
                sendLine(connection, "HTTP/1.1 200 Connection Established");
                sendLine(connection, "");
            } else {
                // Proxying the request directly to the origin server.
                sendAugmentedRequestToHost(connection, server, requestType, url, httpVersion);
            }

            // Pass data back and forth until complete.
            SocketConnect.connect(connection, server);

        } catch (Exception e) {
            Log.d(TAG, "Problem Proxying", e);
        }

        try {
            connection.close();
        } catch (IOException ioe) {
            // Do nothing
        }
    }

    /**
     * Sends HTTP request-line (i.e. the first line in the request)
     * that contains absolute path of a given absolute URI.
     *
     * @param server server to send the request to.
     * @param requestType type of the request, a.k.a. HTTP method.
     * @param absoluteUri absolute URI which absolute path should be extracted.
     * @param httpVersion version of HTTP, e.g. HTTP/1.1.
     * @throws IOException if the request-line cannot be sent.
     */
    private static void sendRequestLineWithPath(Socket server, String requestType,
                                         URI absoluteUri, String httpVersion) throws IOException {

        String absolutePath = getAbsolutePathFromAbsoluteURI(absoluteUri);
        String outgoingRequestLine = String.format("%s %s %s",
                requestType, absolutePath, httpVersion);
        sendLine(server, outgoingRequestLine);
    }

    /**
     * Extracts absolute path form a given URI. E.g., passing
     * <code>http://google.com:80/execute?query=cat#top</code>
     * will result in <code>/execute?query=cat#top</code>.
     *
     * @param uri URI which absolute path has to be extracted,
     * @return the absolute path of the URI,
     */
    private static String getAbsolutePathFromAbsoluteURI(URI uri) {
        String rawPath = uri.getRawPath();
        String rawQuery = uri.getRawQuery();
        String rawFragment = uri.getRawFragment();
        StringBuilder absolutePath = new StringBuilder();

        if (rawPath != null) {
            absolutePath.append(rawPath);
        } else {
            absolutePath.append("/");
        }
        if (rawQuery != null) {
            absolutePath.append("?").append(rawQuery);
        }
        if (rawFragment != null) {
            absolutePath.append("#").append(rawFragment);
        }
        return absolutePath.toString();
    }

    private static String getLine(InputStream inputStream) throws IOException {
        StringBuilder buffer = new StringBuilder();
        int byteBuffer = inputStream.read();
        if (byteBuffer < 0) return "";
        do {
            if (byteBuffer != '\r') {
                buffer.append((char)byteBuffer);
            }
            byteBuffer = inputStream.read();
        } while ((byteBuffer != '\n') && (byteBuffer >= 0));

        return buffer.toString();
    }

    private static void sendLine(Socket socket, String line) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(line.getBytes());
        os.write('\r');
        os.write('\n');
        os.flush();
    }

    /**
     * Reads from socket until an empty line is read which indicates the end of HTTP headers.
     *
     * @param socket socket to read from.
     * @throws IOException if an exception took place during the socket read.
     */
    private static void skipToRequestBody(Socket socket) throws IOException {
        while (getLine(socket.getInputStream()).length() != 0);
    }

    /**
     * Sends an augmented request to the final host (DIRECT connection).
     *
     * @param src socket to read HTTP headers from.The socket current position should point
     *            to the beginning of the HTTP header section.
     * @param dst socket to write the augmented request to.
     * @param httpMethod original request http method.
     * @param uri original request absolute URI.
     * @param httpVersion original request http version.
     * @throws IOException if an exception took place during socket reads or writes.
     */
    private static void sendAugmentedRequestToHost(Socket src, Socket dst,
                                            String httpMethod, URI uri, String httpVersion) throws IOException {

        sendRequestLineWithPath(dst, httpMethod, uri, httpVersion);
        filterAndForwardRequestHeaders(src, dst);

        // Currently the proxy does not support keep-alive connections; therefore,
        // the proxy has to request the destination server to close the connection
        // after the destination server sent the response.
        sendLine(dst, "Connection: close");

        // Sends and empty line that indicates termination of the header section.
        sendLine(dst, "");
    }

    /**
     * Forwards original request headers filtering out the ones that have to be removed.
     *
     * @param src source socket that contains original request headers.
     * @param dst destination socket to send the filtered headers to.
     * @throws IOException if the data cannot be read from or written to the sockets.
     */
    private static void filterAndForwardRequestHeaders(Socket src, Socket dst) throws IOException {
        String line;
        do {
            line = getLine(src.getInputStream());
            if (line.length() > 0 && !shouldRemoveHeaderLine(line)) {
                sendLine(dst, line);
            }
        } while (line.length() > 0);
    }

    /**
     * Returns true if a given header line has to be removed from the original request.
     *
     * @param line header line that should be analysed.
     * @return true if the header line should be removed and not forwarded to the destination.
     */
    private static boolean shouldRemoveHeaderLine(String line) {
        int colIndex = line.indexOf(":");
        if (colIndex != -1) {
            String headerName = line.substring(0, colIndex).trim();
            if (headerNameMatches(headerName, "connection") ||
                    headerNameMatches(headerName, "proxy-connection")) {
                return true;
            }
        }
        return false;
    }

    private static boolean headerNameMatches(String headerName, String pattern) {
        return headerName.regionMatches(true, 0, pattern, 0, pattern.length());
    }
}
