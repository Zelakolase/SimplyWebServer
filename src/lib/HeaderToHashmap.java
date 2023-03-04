package lib;

import java.util.HashMap;

public class HeaderToHashmap {
    /*
     * Reads the HTTP headers, then makes a hashmap to store variable names and values.
     */
    public static HashMap<String, String> convert(String headers) {
        HashMap<String, String> data = new HashMap<>();
        String[] lines = headers.split("\r\n");
        String[] fir_data = lines[0].split("\\s+");
        data.put("method", fir_data[0]);
        data.put("path", PathFilter.filter(fir_data[1]));
        for (int i = 1; i < lines.length; i++) {
            String[] temp = lines[i].split(":");
            data.put(temp[0], temp[1].stripLeading());
        }
        return data;
    }
}
