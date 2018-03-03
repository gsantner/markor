/*
 * ------------------------------------------------------------------------------
 * Lonami Exo <lonamiwebs.github.io> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me
 * a coke in return. Provided as is without any kind of warranty. Do not blame
 * or sue me if something goes wrong. No attribution required.
 *                                                             - Lonami Exo
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
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
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            input = connection.getInputStream();

            if (!outFile.getParentFile().isDirectory())
                if (!outFile.getParentFile().mkdirs())
                    return false;
            output = new FileOutputStream(outFile);

            int count;
            int written = 0;
            final float invLength = 1f / connection.getContentLength();

            byte data[] = new byte[BUFFER_SIZE];
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
        try {
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setDoInput(true);

            if (data != null && !data.isEmpty()) {
                conn.setDoOutput(true);
                final OutputStream output = conn.getOutputStream();
                output.write(data.getBytes(Charset.forName(UTF8)));
                output.flush();
                output.close();
            }

            return FileUtils.readCloseTextStream(conn.getInputStream());
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
}
