package com.ship.SegmentCloud.ShipProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.*;
import java.net.Socket;
import java.io.IOException;

@RestController
public class ProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);

    @Value("${offshore.host}")
    private String offshoreHost;

    @Value("${offshore.port}")
    private int offshorePort;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    @PostConstruct
    public void initializeConnection() throws IOException, InterruptedException {
        int retryDelay = 2000;
        while(true) {
            try {
                logger.info("Attempting to establish connection to offshore proxy: {}:{}", offshoreHost, offshorePort);
                socket = new Socket(offshoreHost, offshorePort);
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                logger.info("Established connection to offshore proxy: {}:{}", offshoreHost, offshorePort);
                break;
            } catch (IOException e) {
                logger.error("Failed to establish connection to offshore proxy: {}:{}", offshoreHost, offshorePort, e);
                Thread.sleep(retryDelay);
            }
        }
    }

    @PreDestroy
    public void closeConnection() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            logger.info("Closed connection to offshore proxy.");
        }
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> handleRequest(@RequestHeader HttpHeaders headers, @RequestParam(required = false) String url) {
        try {
            String targetUrl = headers.getHost().getHostName();
            logger.info("Received request for URL: {}", targetUrl);
            if (targetUrl == null || targetUrl.isEmpty()) {
                logger.warn("No target URL provided.");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST).body("No target URL provided");
            }

            String response = forwardToOffshore(targetUrl);
            logger.info("Response from offshore: {}", response);
            return ResponseEntity.ok(response);
        } catch (IOException | InterruptedException e) {
            logger.error("Error handling request: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private synchronized String forwardToOffshore(String targetUrl) throws IOException, InterruptedException {
        try {
            writer.write(targetUrl);
            writer.newLine();
            writer.flush();
            logger.debug("Sent URL to offshore: {}", targetUrl);

            String line = reader.readLine();

            if (line != null) {
                logger.debug("Received response line: {}", line);
                return line;
            } else {
                logger.warn("No response from offshore proxy.");
                return "No response from offshore proxy.";
            }

        } catch (IOException e) {
            logger.error("Error forwarding to offshore proxy, attempting to reconnect: {}", e.getMessage(), e);
            initializeConnection();
            return "Error connecting to offshore proxy. Connection re-established try again.";
        }
    }
}