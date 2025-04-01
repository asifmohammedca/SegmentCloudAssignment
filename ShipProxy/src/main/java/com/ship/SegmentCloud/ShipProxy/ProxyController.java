package com.ship.SegmentCloud.ShipProxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.Socket;

@RestController
public class ProxyController {

    @Value("${offshore.host}")
    private String offshoreHost;

    @Value("${offshore.port}")
    private int offshorePort;

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> handleRequest(@RequestHeader HttpHeaders headers, @RequestParam(required = false) String url) {
        try {
            String targetUrl = headers.getHost().getHostName();
            if (targetUrl == null || targetUrl.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST).body("No target URL provided");
            }

            String response = forwardToOffshore(targetUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private String forwardToOffshore(String targetUrl) {
        try (Socket socket = new Socket(offshoreHost, offshorePort);
             BufferedWriter writer =
                     new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader =
                     new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.write(targetUrl);
            writer.newLine();
            writer.flush();

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        } catch (IOException e) {
            return "Error connecting to offshore proxy";
        }
    }
}

