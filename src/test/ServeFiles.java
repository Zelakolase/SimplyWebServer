package test;

import http.HttpFileResponse;
import http.HttpRequest;
import http.HttpBufferResponse;
import http.HttpResponse;
import http.config.HttpStatusCode;
import http.exceptions.HttpResponseException;
import lib.log;
import server.Server;

import java.io.IOException;
import java.util.Random;

public class ServeFiles {
    private static HttpResponse handle(HttpRequest httpRequest) {
        HttpResponse httpResponse;
        try {
            httpResponse = new HttpFileResponse(httpRequest.getPath());
        } catch (IOException | HttpResponseException e) {
            log.e(Server.getStackTrace(e));
            httpResponse = new HttpBufferResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        httpResponse.headers.put("Rand-Int", String.valueOf(new Random().nextInt(10)));
        
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
