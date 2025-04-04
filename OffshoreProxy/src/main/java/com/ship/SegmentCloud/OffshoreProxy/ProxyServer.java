package com.ship.SegmentCloud.OffshoreProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.HttpURLConnection;

@Component
public class ProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    @Value("${proxy.tcpPort}")
    private int proxyPort;

    @PostConstruct
    public void startServer() {
        new Thread(this::runServer).start();
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(proxyPort)) {
            logger.info("Offshore Proxy TCP Server started on port {}", proxyPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Accepted connection from {}", clientSocket.getInetAddress());
                new Thread(() -> handleClientConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            logger.error("Error running server: {}", e.getMessage(), e);
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String targetUrl;
            while ((targetUrl = reader.readLine()) != null) {
                logger.debug("Received request for URL: {}", targetUrl);
                String response = fetchFromInternet(targetUrl);
                writer.write(response);
                writer.newLine();
                writer.flush();
                logger.debug("Sent response for URL: {}", targetUrl);
            }
            logger.info("Client {} disconnected.", clientSocket.getInetAddress());
        } catch (IOException e) {
            logger.error("Error handling client connection: {}", e.getMessage(), e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Error closing client socket: {}", e.getMessage(), e);
            }
        }
    }

    private String fetchFromInternet(String targetUrl) {
        try {
            URL url = new URL("http://" + targetUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            logger.debug("Fetched data from URL: {}", targetUrl);
            return response.toString();
        } catch (Exception e) {
            logger.error("Error fetching data from URL {}: {}", targetUrl, e.getMessage(), e);
            return "Error fetching data from " + targetUrl;
        }
    }
}