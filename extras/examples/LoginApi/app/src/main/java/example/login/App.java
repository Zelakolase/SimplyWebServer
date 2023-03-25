package example.login;

import sws.http.HttpBufferResponse;
import sws.http.HttpRequest;
import sws.http.config.HttpStatusCode;
import sws.utils.JSON;
import sws.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class App {

    private final static String HTMLCode = "<!DOCTYPE html><html><head><title>Login Form</title></head><body><h1>Login Form (check browser log for response)</h1><form id=\"login-form\"><label for=\"username\">Username</label><input type=\"text\" id=\"username\" name=\"username\" required><br><br><label for=\"password\">Password</label><input type=\"password\" id=\"password\" name=\"password\" required><br><br><input type=\"submit\" value=\"Login\"></form><script>const form = document.getElementById('login-form');form.addEventListener('submit', async (event) => {event.preventDefault();const formData = new FormData(form);const response = await fetch('/api/login', {method: 'POST',body: JSON.stringify(Object.fromEntries(formData.entries())),headers: {'Content-Type': 'application/json'}});const data = await response.json();console.log(data);});</script></body></html>";
    private final static String username = "morad";
    private final static String password = "morad";

    private static HttpBufferResponse handle(HttpRequest httpRequest) {
        HttpBufferResponse httpBufferResponse = new HttpBufferResponse();


        try {
            if (httpRequest.getPath().equals("/index.html")) {
                httpBufferResponse.setBody(HTMLCode);
                httpBufferResponse.setHttpStatusCode(HttpStatusCode.OK);
            } else if (httpRequest.getPath().equals("/api/login") && httpRequest.getHttpRequestMethod().equalsIgnoreCase("post")) {
                HashMap<String, String> LoginInfo = JSON.QHM(httpRequest.getBodyAsString());
                if (!(LoginInfo.containsKey("username") && LoginInfo.containsKey("password"))) {
                    httpBufferResponse.setBody(JSON.HMQ(new HashMap<>() {{
                        put("success", "false");
                    }}));
                } else {
                    if (LoginInfo.get("username").equals(username) && LoginInfo.get("password").equals(password)) {
                        httpBufferResponse.setBody(JSON.HMQ(new HashMap<>() {{
                            put("success", "true");
                        }}));
                    } else {
                        httpBufferResponse.setBody(JSON.HMQ(new HashMap<>() {{
                            put("success", "false");
                        }}));
                    }
                }
            } else {
                httpBufferResponse.setBody(JSON.HMQ(new HashMap<>() {{
                    put("success", "false");
                    put("error", "file not found");
                }}));

                httpBufferResponse.setHttpStatusCode(HttpStatusCode.NOT_FOUND);
            }
            // We can add custom headers changes for each request
            httpBufferResponse.addHeader("Rand-Int", String.valueOf(new Random().nextInt(10)));
        } catch (IOException ignored) {
            httpBufferResponse.setHttpStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        return httpBufferResponse;
    }


    public static void main(String[] args) throws Exception {

        Server server = new Server(App::handle);
        server.startHttp();
        // For HTTPS
        // server.startHttps("./etc/keystore.jks", "123456");
    }

}