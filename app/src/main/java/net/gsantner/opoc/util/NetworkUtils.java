/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "deprecation"})
public class NetworkUtils {
    private static final String UTF8 = "UTF-8";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PATCH = "PATCH";

    private final static int BUFFER_SIZE = 4096;

    // Downloads a file from the give url to the output file
    // Creates the file's parent directory if it doesn't exist
    public static boolean downloadFile(final String url, final File out) {
        return downloadFile(url, out, null);
    }

    public static boolean downloadFile(final String url, final File out, final Callback.a1<Float> progressCallback) {
        try {
            return downloadFile(new URL(url), out, progressCallback);
        } catch (MalformedURLException e) {
            // Won't happen
            e.printStackTrace();
            return false;
        }
    }

    public static boolean downloadFile(final URL url, final File outFile, final Callback.a1<Float> progressCallback) {
        return downloadFile(url, outFile, null, progressCallback);
    }

    public static boolean downloadFile(final URL url, final File outFile, HttpURLConnection connection, final Callback.a1<Float> progressCallback) {
        InputStream input = null;
        OutputStream output = null;
        try {
            if (connection == null) {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.connect();
            input = connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST
                    ? connection.getInputStream() : connection.getErrorStream();


            if (!outFile.getParentFile().isDirectory())
                if (!outFile.getParentFile().mkdirs())
                    return false;
            output = new FileOutputStream(outFile);

            int count;
            int written = 0;
            final float invLength = 1f / connection.getContentLength();

            byte[] data = new byte[BUFFER_SIZE];
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
                if (invLength != -1f && progressCallback != null) {
                    written += count;
                    progressCallback.callback(written * invLength);
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }
            if (connection != null)
                connection.disconnect();
        }
    }

    // No parameters, method can be GET, POST, etc.
    public static String performCall(final String url, final String method) {
        try {
            return performCall(new URL(url), method, "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String performCall(final String url, final String method, final String data) {
        try {
            return performCall(new URL(url), method, data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    // URL encoded parameters
    public static String performCall(final String url, final String method, final HashMap<String, String> params) {
        try {
            return performCall(new URL(url), method, encodeQuery(params));
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Defaults to POST
    public static String performCall(final String url, final JSONObject json) {
        return performCall(url, POST, json);
    }

    public static String performCall(final String url, final String method, final JSONObject json) {
        try {
            return performCall(new URL(url), method, json.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String performCall(final URL url, final String method, final String data) {
        return performCall(url, method, data, null);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private static String performCall(final URL url, final String method, final String data, final HttpURLConnection existingConnection) {
        try {
            final HttpURLConnection connection = existingConnection != null
                    ? existingConnection : (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setDoInput(true);

            if (data != null && !data.isEmpty()) {
                connection.setDoOutput(true);
                final OutputStream output = connection.getOutputStream();
                output.write(data.getBytes(Charset.forName("UTF-8")));
                output.flush();
                output.close();
            }

            InputStream input = connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST
                    ? connection.getInputStream() : connection.getErrorStream();

            return FileUtils.readCloseTextStream(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String encodeQuery(final HashMap<String, String> params) throws UnsupportedEncodingException {
        final StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) first = false;
            else result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), UTF8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), UTF8));
        }

        return result.toString();
    }

    public static HashMap<String, String> getDataMap(final String query) {
        final HashMap<String, String> result = new HashMap<>();
        final StringBuilder sb = new StringBuilder();
        String name = "";

        try {
            for (int i = 0; i < query.length(); i++) {
                char c = query.charAt(i);
                switch (c) {
                    case '=':
                        name = URLDecoder.decode(sb.toString(), UTF8);
                        sb.setLength(0);
                        break;
                    case '&':
                        result.put(name, URLDecoder.decode(sb.toString(), UTF8));
                        sb.setLength(0);
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            if (!name.isEmpty())
                result.put(name, URLDecoder.decode(sb.toString(), UTF8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void httpGetAsync(final String url, final Callback.a1<String> callback) {
        new Thread(() -> {
            try {
                String c = NetworkUtils.performCall(url, GET);
                callback.callback(c);
            } catch (Exception ignored) {
            }
        }).start();
    }
}
