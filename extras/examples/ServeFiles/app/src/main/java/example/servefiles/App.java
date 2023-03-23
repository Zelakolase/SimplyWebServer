package example.servefiles;

import sws.Server;
import sws.http.HttpBufferResponse;
import sws.http.HttpFileResponse;
import sws.http.HttpRequest;
import sws.http.HttpResponse;
import sws.http.config.HttpStatusCode;
import sws.io.Log;

import java.util.Random;

public class App {
    private static HttpResponse handle(HttpRequest httpRequest) {
        HttpResponse httpResponse;
        try {
            httpResponse = new HttpFileResponse(httpRequest.getPath());
        } catch (Exception e) {
            Log.e(Server.getStackTrace(e));
            httpResponse = new HttpBufferResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }

        // We can add custom headers changes for each request
        httpResponse.addHeader("Rand-Int", String.valueOf(new Random().nextInt(10)));

        return httpResponse;
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(App::handle);
        server.startHttp();
        /* For HTTPS
         * server.port = 443;
         * server.startHttps("./etc/keystore.jks", "123456");
         */
    }}
