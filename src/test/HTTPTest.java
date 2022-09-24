package test;

import java.util.HashMap;

import lib.HTTPCode;
import lib.HeaderToHashmap;
import lib.log;

public class HTTPTest extends server.Server {
    public static void main(String[] args) throws Exception {
        HTTPTest test = new HTTPTest();
        test.setDynamic(true);
        test.setHTTPPort(80);
        test.setGZip(false);
        test.HTTPStart();
    }

    @Override
    public HashMap<String, byte[]> main(byte[] request) {
        HashMap<String, byte[]> response = new HashMap<>();
        response.put("body", HeaderToHashmap.convert(new String(request)).get("User-Agent").getBytes());
        response.put("mime", "text/html".getBytes());
        response.put("code", HTTPCode.OK.getBytes());
        return response;
    }
}
