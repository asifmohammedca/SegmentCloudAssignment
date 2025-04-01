package com.ship.SegmentCloud.OffshoreProxy;
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

    @Value("${proxy.tcpPort}")
    private int proxyPort;

    @PostConstruct
    public void startServer() {
        new Thread(this::runServer).start();
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(proxyPort)) {
            System.out.println("Offshore Proxy TCP Server is running on port " + proxyPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleRequest(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String targetUrl = reader.readLine();
            if (targetUrl == null) return;

            String response = fetchFromInternet(targetUrl);

            writer.write(response);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
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

            return response.toString();
        } catch (Exception e) {
            return "Error fetching data from " + targetUrl;
        }
    }
}



