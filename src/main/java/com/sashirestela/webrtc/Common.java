package com.sashirestela.webrtc;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

public class Common {

    private Common() {
    }

    public static final int HTTP_SUCCESS = 200;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_SERVER_ERROR = 500;

    public static void responseHttp(HttpExchange exchange, int code, byte[] data) throws IOException {
        exchange.sendResponseHeaders(code, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

}
