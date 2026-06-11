package com.kamran;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.util.Scanner;

public class App {

    public static void main(String[] args) throws Exception {

        // 1) Using System.out instead of logger (Maintainability)
        System.out.println("Starting Kamran DevSecOps Project...");

        // 2) Scanner resource leak + wrong string comparison (two findings)
        String username = "guest"; // safe fallback for Docker

        try {
            Scanner sc = new Scanner(System.in); // resource leak (intentional)
            System.out.print("Enter username: ");
            if (sc.hasNextLine()) {
                username = sc.nextLine();
            }
        } catch (Exception e) {
            // ignore for non-interactive mode
        }

        // 3) Wrong comparison (Bug)
        if (username == "admin") { // Sonar will flag this
            System.out.println("Welcome, admin!");
        } else {
            System.out.println("Hello, " + username);
        }

        // Simple homepage
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            String body = brandHtml();
            byte[] out = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, out.length);
            OutputStream os = exchange.getResponseBody();
            os.write(out);
            os.close();
        });

        System.out.println("CVWJ app running on port " + port);
        server.start();
    }

    // simple helper method for tests
    public static String brandHtml() {
        return "<h1>Kamran</h1><p>DevSecOps Project</p>";
    }
}