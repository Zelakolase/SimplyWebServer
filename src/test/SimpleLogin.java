package test;

import http.HttpBufferResponse;
import http.HttpRequest;
import http.config.HttpStatusCode;
import lib.JSON;
import server.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class SimpleLogin {
    public static String HTMLCode = "<!DOCTYPE html><html><head><title>Login Form</title></head><body><h1>Login Form (check browser log for response)</h1><form id=\"login-form\"><label for=\"username\">Username</label><input type=\"text\" id=\"username\" name=\"username\" required><br><br><label for=\"password\">Password</label><input type=\"password\" id=\"password\" name=\"password\" required><br><br><input type=\"submit\" value=\"Login\"></form><script>const form = document.getElementById('login-form');form.addEventListener('submit', async (event) => {event.preventDefault();const formData = new FormData(form);const response = await fetch('/api/login', {method: 'POST',body: JSON.stringify(Object.fromEntries(formData.entries())),headers: {'Content-Type': 'application/json'}});const data = await response.json();console.log(data);});</script></body></html>";
    public static String username = "morad";
    public static String password = "morad";
    /* The String JSON Structure for a failure response */
    public static String FailedJSON = JSON.HMQ(new HashMap<>() {{
        put("success", "false");
    }});
    /* The String JSON Structure for a successful response */
    public static String SuccessJSON = JSON.HMQ(new HashMap<>() {{
        put("success", "true");
    }});
    /* The String JSON Structure for a file-not-found reponse */
    public static String FNFJSON = JSON.HMQ(new HashMap<>() {{
        put("success", "false");
        put("error", "file not found");
    }});

    private static HttpBufferResponse handle(HttpRequest httpRequest) {
        HttpBufferResponse httpBufferResponse = new HttpBufferResponse();

        try {
            // Serve the Login Form if the user requested index.html
            if (httpRequest.getPath().equals("/index.html")) {
                httpBufferResponse.setBody(HTMLCode);
                httpBufferResponse.setHttpStatusCode(HttpStatusCode.OK);
                return httpBufferResponse;
            }

            // If the user is not posting data to /api/login, return file-not-found response
            if(! (httpRequest.getPath().equals("/api/login") && httpRequest.getHttpRequestMethod().equalsIgnoreCase("post"))) {
                httpBufferResponse.setBody(FNFJSON);
                httpBufferResponse.setHttpStatusCode(HttpStatusCode.NOT_FOUND);
                return httpBufferResponse;
            }

            // Login API Mechanism
            /* LoginInfo has the JSON Structure for the POST request */
            HashMap<String, String> LoginInfo = JSON.QHM(httpRequest.getBodyAsString());
            if (!(LoginInfo.containsKey("username") && LoginInfo.containsKey("password"))) {
                // Did not find 'username' and 'password' arguments in the POST JSON Request
                httpBufferResponse.setBody(FailedJSON);
            } else {
                if (LoginInfo.get("username").equals(username) && LoginInfo.get("password").equals(password)) {
                    // The 'username' and 'password' arguments are identical to the global variables
                    httpBufferResponse.setBody(SuccessJSON);
                } else {
                    // The 'username' and 'password' arguments are NOT identical
                    httpBufferResponse.setBody(FailedJSON);
                }
            }

            // We can add custom headers changes for each request
            httpBufferResponse.addHeader("Rand-Int", String.valueOf(new Random().nextInt(10)));
        } catch (IOException ignored) {
            httpBufferResponse.setHttpStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        return httpBufferResponse;
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server(SimpleLogin::handle);
        server.startHttp();
        /* For HTTPS
         * server.startHttps("src/etc/keystore.jks", "123456");
         */
    }

}
