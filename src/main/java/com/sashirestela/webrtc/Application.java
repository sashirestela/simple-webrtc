package com.sashirestela.webrtc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static final int HTTP_PORT = 8080;
    private static final int THREAD_POOL_SIZE = 3;

    private HttpServer httpServer;

    public Application() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        httpServer.createContext("/", new FrontendHandler());
        httpServer.createContext("/sessions", new SessionsHandler());
    }

    public void start() {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors
                .newFixedThreadPool(THREAD_POOL_SIZE);
        httpServer.setExecutor(threadPoolExecutor);
        httpServer.start();
        LOGGER.info("Server was started on port {}.", HTTP_PORT);
    }

    public void stop() {
        httpServer.stop(0);
        LOGGER.info("Server was stopped.");
    }

    public static void main(String[] args) throws IOException {
        Application app = new Application();

        app.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                app.stop();
            }
        });
    }
}
