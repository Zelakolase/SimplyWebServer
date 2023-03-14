package test;

import http.HttpContentType;
import http.HttpRequest;
import http.HttpResponse;
import http.HttpStatusCode;
import server.Server;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class ServeFiles {
    private static HttpResponse handle(HttpRequest httpRequest) {
        HttpResponse httpResponse = new HttpResponse();
        String path = httpRequest.getPath().replaceFirst("/", "www/");
        try (BufferedInputStream f = new BufferedInputStream(new FileInputStream(path))) {
            httpResponse.setHttpStatusCode(HttpStatusCode.OK);
            httpResponse.setHttpContentType(HttpContentType.TEXT_PLAIN);
            byte[] fileContents = f.readAllBytes();
            httpResponse.setBody(new String(fileContents, 0, fileContents.length, StandardCharsets.UTF_8));
        } catch (IOException e) {
            httpResponse.setHttpStatusCode(HttpStatusCode.NOT_FOUND);
            httpResponse.setHttpContentType(HttpContentType.TEXT_PLAIN);
            httpResponse.setBody("File not found");
        }
        // We can add custom headers changes for each request
        httpResponse.addHeader("Rand-Int", new Random().nextInt(10));
        
        return httpResponse;
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(ServeFiles::handle);
        server.startHttp();
        /* For HTTPS
         * server.port = 443;
         * server.startHttps("./etc/keystore.jks", "123456");
         */
    }
}
