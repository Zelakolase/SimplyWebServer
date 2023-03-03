package test;

import java.util.HashMap;

import lib.HTTPCode;
import lib.JSON;
import lib.log;

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
    public HashMap<String, byte[]> main(HashMap<String, String> headers, byte[] body) {
        HashMap<String, byte[]> response = new HashMap<>();
        response.put("mime", "text/html".getBytes()); // By default, the Content-Type is text/html

        if(headers.get("path").equals("/index.html")) {
            response.put("body", HTMLCode.getBytes());
            response.put("code", HTTPCode.OK.getBytes());
        }else if(headers.get("path").equals("/api/login")) {
            // Does not matter if the request is POST or GET
            HashMap<String, String> LoginInfo = JSON.QHM(new String(body));
            if(! (LoginInfo.containsKey("username") && LoginInfo.containsKey("password"))) {
                response.put("body", "Invalid Request.".getBytes());
                response.put("code", HTTPCode.BAD_REQUEST.getBytes());
            }else {
                if(LoginInfo.get("username").equals(username) && LoginInfo.get("password").equals(password)) {
                    response.put("body", JSON.HMQ(new HashMap<>() {{
                        put("status", "success");
                    }}).getBytes());
                }else {
                    response.put("body", JSON.HMQ(new HashMap<>() {{
                        put("status", "failed");
                    }}).getBytes());
                }
                response.put("mime", "application/json".getBytes());
                response.put("code", HTTPCode.OK.getBytes());
            }
        }else {
            response.put("body", "File not found".getBytes());
            response.put("code", HTTPCode.NOT_FOUND.getBytes());
        }
        response.put("isFile", "0".getBytes());
        return response;
    }
    
}
