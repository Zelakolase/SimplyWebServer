package test;

import lib.HTTPCode;
import lib.JSON;

import java.util.HashMap;
import java.util.Random;

public class SimpleLogin extends server.Server {
    public static String HTMLCode = "<!DOCTYPE html><html><head><title>Login Form</title></head><body><h1>Login Form (check browser log for response)</h1><form id=\"login-form\"><label for=\"username\">Username</label><input type=\"text\" id=\"username\" name=\"username\" required><br><br><label for=\"password\">Password</label><input type=\"password\" id=\"password\" name=\"password\" required><br><br><input type=\"submit\" value=\"Login\"></form><script>const form = document.getElementById('login-form');form.addEventListener('submit', async (event) => {event.preventDefault();const formData = new FormData(form);const response = await fetch('/api/login', {method: 'POST',body: JSON.stringify(Object.fromEntries(formData.entries())),headers: {'Content-Type': 'application/json'}});const data = await response.json();console.log(data);});</script></body></html>";
    public static String username = "morad";
    public static String password = "morad";

    public static void main(String[] args) throws Exception {
        SimpleLogin SL = new SimpleLogin();
        SL.HTTPStart(); // -> For HTTP Servers (default port: 80)
        /* For HTTPS Servers
        SL.setPort(443);
        SL.HTTPSStart("./etc/keystore.jks", "123456");
        */
    }

    @Override
    public HashMap<String, Object> main(HashMap<String, String> headers, byte[] body) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("mime", "text/html"); // By default, the Content-Type is text/html

        if (headers.get("path").equals("/index.html")) {
            response.put("body", HTMLCode.getBytes());
            response.put("code", HTTPCode.OK);
        } else if (headers.get("path").equals("/api/login")) {
            // Does not matter if the request is POST or GET
            HashMap<String, String> LoginInfo = JSON.QHM(new String(body));
            if (!(LoginInfo.containsKey("username") && LoginInfo.containsKey("password"))) {
                response.put("body", "Invalid Request.".getBytes());
                response.put("code", HTTPCode.BAD_REQUEST);
            } else {
                if (LoginInfo.get("username").equals(username) && LoginInfo.get("password").equals(password)) {
                    response.put("body", JSON.HMQ(new HashMap<>() {{
                        put("status", "success");
                    }}).getBytes());
                } else {
                    response.put("body", JSON.HMQ(new HashMap<>() {{
                        put("status", "failed");
                    }}).getBytes());
                }
                response.put("mime", "application/json");
                response.put("code", HTTPCode.OK);
            }
        } else {
            response.put("body", "File not found".getBytes());
            response.put("code", HTTPCode.NOT_FOUND);
        }
        response.put("isFile", "0");
        // We can add custom headers for every request, this can be useful for setting cookies
        response.put("CustomHeaders", new HashMap<String, String>() {{
            put("RandomInteger", String.valueOf(new Random().nextInt(10)));
        }});
        return response;
    }

}
