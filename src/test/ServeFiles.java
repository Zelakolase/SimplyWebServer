package test;

import java.util.HashMap;

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
    public HashMap<String, byte[]> main(HashMap<String, String> headers, byte[] body) {
        HashMap<String, byte[]> response = new HashMap<>();
        // if you're trying to read a file, pass the path and SWS will take care of the rest
        HashMap<String, String> FileData = this.pathFilteration(headers);
        response.put("body", FileData.get("path").getBytes());
        response.put("mime", FileData.get("mime").getBytes());
        response.put("code", HTTPCode.OK.getBytes());
        response.put("isFile", "1".getBytes());
        return response;
    }
}
