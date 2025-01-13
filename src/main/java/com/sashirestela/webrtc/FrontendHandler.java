package com.sashirestela.webrtc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class FrontendHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendHandler.class);

    private static final String INDEX_HTML = "/index.html";
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put(".html", "text/html");
        MIME_TYPES.put(".css", "text/css");
        MIME_TYPES.put(".js", "application/javascript");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) {
            requestPath = INDEX_HTML;
        }
        String resourcePath = "/public" + requestPath;
        URL fileUrl = getClass().getResource(resourcePath);
        if (fileUrl != null) {
            String contentType = getContentType(requestPath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                byte[] fileBytes = is.readAllBytes();
                Common.responseHttp(exchange, Common.HTTP_SUCCESS, fileBytes);
                LOGGER.info("{} - {}", Common.HTTP_SUCCESS, resourcePath);
            }
        } else {
            Common.responseHttp(exchange, Common.HTTP_NOT_FOUND, "Not Found".getBytes());
            LOGGER.error("{} - {}", Common.HTTP_NOT_FOUND, resourcePath);
        }
        exchange.close();
    }

    private String getContentType(String path) {
        return MIME_TYPES.entrySet().stream()
                .filter(e -> path.endsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("application/octet-stream");
    }

}
