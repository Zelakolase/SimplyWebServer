package test;

import http.*;
import lib.JSON;
import server.Server;

import java.util.HashMap;
import java.util.Random;

public class SimpleLogin {
    public static String HTMLCode = "<!DOCTYPE html><html><head><title>Login Form</title></head><body><h1>Login Form (check browser log for response)</h1><form id=\"login-form\"><label for=\"username\">Username</label><input type=\"text\" id=\"username\" name=\"username\" required><br><br><label for=\"password\">Password</label><input type=\"password\" id=\"password\" name=\"password\" required><br><br><input type=\"submit\" value=\"Login\"></form><script>const form = document.getElementById('login-form');form.addEventListener('submit', async (event) => {event.preventDefault();const formData = new FormData(form);const response = await fetch('/api/login', {method: 'POST',body: JSON.stringify(Object.fromEntries(formData.entries())),headers: {'Content-Type': 'application/json'}});const data = await response.json();console.log(data);});</script></body></html>";
    public static String username = "morad";
    public static String password = "morad";

    private static HttpResponse handle(HttpRequest httpRequest) {
        HttpResponse httpResponse = new HttpResponse();

        if (httpRequest.getPath().equals("/index.html")) {
            httpResponse.setHttpContentType(HttpContentType.TEXT_HTML);
            httpResponse.getBuffer().put(HTMLCode.getBytes());
            httpResponse.setHttpStatusCode(HttpStatusCode.OK);
        } else if (httpRequest.getPath().equals("/api/login") && httpRequest.getHttpRequestMethod() == HttpRequestMethod.POST) {
            httpResponse.setHttpContentType(HttpContentType.APPLICATION_JSON);
            HashMap<String, String> LoginInfo = JSON.QHM(httpRequest.getBody());
            if (!(LoginInfo.containsKey("username") && LoginInfo.containsKey("password"))) {
                httpResponse.setBuffer("{\"success\": \"false\"}");
            } else {
                if (LoginInfo.get("username").equals(username) && LoginInfo.get("password").equals(password)) {
                    httpResponse.setBuffer("{\"success\": \"true\"}");
                } else {
                    httpResponse.setBuffer("{\"success\": \"false\"}");
                }
            }
        } else {
            httpResponse.setBuffer("{\"success\": \"false\", \"error\": \"file not found\"}");
            httpResponse.setHttpStatusCode(HttpStatusCode.NOT_FOUND);
        }
        // We can add custom headers changes for each request
        httpResponse.addHeader("Rand-Int", new Random().nextInt(10));
        
        return httpResponse;
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server(SimpleLogin::handle);
        server.startHttp();
    }

}
