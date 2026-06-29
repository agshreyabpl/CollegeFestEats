package com.collegefest;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.collegefest.db.Database;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MainServer {
    public static void main(String[] args) throws Exception {
        // Connect to MongoDB
        Database.connect();

        // Create HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Test route
        server.createContext("/ping", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                String response = "{\"status\":\"CollegeFest server is running!\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }
        });

        // Thread pool — this is your multithreading
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("🚀 CollegeFest server running on http://localhost:8080");
    }
}