package test;

import http.HttpBufferResponse;
import http.HttpRequest;
import http.config.HttpStatusCode;
import lib.JSON;
import lib.log;
import server.Server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class SimpleLogin {
    public static String HTMLCode = "<!DOCTYPE html><html><head><title>Login Form</title></head><body><h1>Login Form (check browser log for response)</h1><form id=\"login-form\"><label for=\"username\">Username</label><input type=\"text\" id=\"username\" name=\"username\" required><br><br><label for=\"password\">Password</label><input type=\"password\" id=\"password\" name=\"password\" required><br><br><input type=\"submit\" value=\"Login\"></form><script>const form = document.getElementById('login-form');form.addEventListener('submit', async (event) => {event.preventDefault();const formData = new FormData(form);const response = await fetch('/api/login', {method: 'POST',body: JSON.stringify(Object.fromEntries(formData.entries())),headers: {'Content-Type': 'application/json'}});const data = await response.json();console.log(data);});</script></body></html>";
    public static String username = "morad";
    public static String password = "morad";

    private static HttpBufferResponse handle(HttpRequest httpRequest) {
        HttpBufferResponse httpBufferResponse = new HttpBufferResponse();

        if (httpRequest.getPath().equals("/index.html")) {
            httpBufferResponse.setBuffer(HTMLCode);
            httpBufferResponse.setHttpStatusCode(HttpStatusCode.OK);
        } else if (httpRequest.getPath().equals("/api/login") &&
                httpRequest.getHttpRequestMethod().equalsIgnoreCase("post")) {
            HashMap<String, String> LoginInfo = JSON.QHM(new String(httpRequest.getBody()));
            log.i(LoginInfo+"");
            log.i(Arrays.toString(new String(httpRequest.getBody()).getBytes()));
            if (!(LoginInfo.containsKey("username") && LoginInfo.containsKey("password"))) {
                httpBufferResponse.setBuffer(JSON.HMQ(new HashMap<>() {{
                    put("success", "false");
                }}));
            } else {
                if (LoginInfo.get("username").equals(username) && LoginInfo.get("password").equals(password)) {
                    httpBufferResponse.setBuffer(JSON.HMQ(new HashMap<>() {{
                        put("success", "true");
                    }}));
                } else {
                    httpBufferResponse.setBuffer(JSON.HMQ(new HashMap<>() {{
                        put("success", "false");
                    }}));
                }
            }
        } else {
            httpBufferResponse.setBuffer(JSON.HMQ(new HashMap<>() {{
                put("success", "false");
                put("error", "File Not Found");
            }}));
            httpBufferResponse.setHttpStatusCode(HttpStatusCode.NOT_FOUND);
        }
        // We can add custom headers changes for each request
        httpBufferResponse.headers.put("Rand-Int", String.valueOf(new Random().nextInt(10)));

        return httpBufferResponse;
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server(SimpleLogin::handle);
        server.startHttp();
        /* For HTTPS
         * server.port = 443;
         * server.startHttps("./etc/keystore.jks", "123456");
         */
    }

}
