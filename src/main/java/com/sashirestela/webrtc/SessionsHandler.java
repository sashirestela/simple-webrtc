package com.sashirestela.webrtc;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.realtime.RealtimeSession;
import io.github.sashirestela.openai.domain.realtime.RealtimeSessionToken;
import io.github.sashirestela.openai.exception.OpenAIExceptionConverter;
import io.github.sashirestela.openai.exception.OpenAIException.AuthenticationException;
import io.github.sashirestela.openai.exception.OpenAIException.BadRequestException;
import io.github.sashirestela.openai.exception.OpenAIException.InternalServerException;
import io.github.sashirestela.openai.exception.OpenAIException.PermissionDeniedException;

public class SessionsHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionsHandler.class);

    private static final ObjectMapper JACKSON = new ObjectMapper();
    private static final String OPENAI_API_KEY;
    static {
        OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        InputStream inputStream = exchange.getRequestBody();
        if (method.equals("POST")) {
            try {
                SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(OPENAI_API_KEY).build();
                String body = new String(inputStream.readAllBytes());
                RealtimeSession sessionRequest = jsonToObject(body, RealtimeSession.class);
                RealtimeSessionToken sessionResponse = openAI.sessionTokens().create(sessionRequest).join();
                RealtimeSessionToken.Secret clientSecret = sessionResponse.getClientSecret();
                String jsonResponse = objectToJson(clientSecret);
                Common.responseHttp(exchange, Common.HTTP_SUCCESS, jsonResponse.getBytes());
                LOGGER.info("{} - {} /sessions", Common.HTTP_SUCCESS, method);
            } catch (Exception e) {
                handleOpenAIException(exchange, e);
                LOGGER.error("{}", e.getMessage());
            }
        } else {
            Common.responseHttp(exchange, Common.HTTP_BAD_METHOD, "Method Not Allowed".getBytes());
            LOGGER.error("{} - {} is not allowed for /sessions", Common.HTTP_BAD_METHOD, method);
        }
        exchange.close();
    }

    private void handleOpenAIException(HttpExchange exchange, Exception e) throws IOException {
        try {
            OpenAIExceptionConverter.rethrow(e);
        } catch (BadRequestException be) {
            Common.responseHttp(exchange, be.getResponseInfo().getStatus(), be.getMessage().getBytes());
        } catch (AuthenticationException ae) {
            Common.responseHttp(exchange, ae.getResponseInfo().getStatus(), ae.getMessage().getBytes());
        } catch (PermissionDeniedException pe) {
            Common.responseHttp(exchange, pe.getResponseInfo().getStatus(), pe.getMessage().getBytes());
        } catch (InternalServerException ie) {
            Common.responseHttp(exchange, ie.getResponseInfo().getStatus(), ie.getMessage().getBytes());
        } catch (RuntimeException re) {
            Common.responseHttp(exchange, Common.HTTP_SERVER_ERROR, re.getMessage().getBytes());
        }
    }

    private <T> T jsonToObject(String json, Class<T> clazz) throws IOException {
        return JACKSON.readValue(json, clazz);
    }

    private <T> String objectToJson(T object) throws IOException {
        return JACKSON.writeValueAsString(object);
    }

}
