package test;

import java.util.HashMap;
import java.util.Random;

import lib.HTTPCode;

public class ServeFiles extends server.Server {
    public static void main(String[] args) throws Exception {
        ServeFiles SF = new ServeFiles();
        SF.HTTPStart(); // -> For HTTP Servers (default port: 80)
        /* For HTTPS Servers
        SF.setPort(443);
        SF.HTTPSStart("./etc/keystore.jks", "123456");
        */ 
    }

    @Override
    public HashMap<String, Object> main(HashMap<String, String> headers, byte[] body) {
        HashMap<String, Object> response = new HashMap<>();
        // if you're trying to read a file, pass the path and SWS will take care of the rest
        HashMap<String, String> FileData = this.pathFiltration(headers);
        response.put("body", FileData.get("path").getBytes());
        response.put("mime", FileData.get("mime"));
        response.put("code", HTTPCode.OK);
        response.put("isFile", "1");
        // We can add custom headers for every request, this can be useful for setting cookies
        response.put("CustomHeaders", new HashMap<String, String>() {{
            put("RandomInteger", String.valueOf(new Random().nextInt(10)));
        }});
        return response;
    }
}
