package lib;

import java.util.HashMap;
import java.util.Map;

public class JSON {
    /*
     * Query String to Hashmap
     */
    public static HashMap<String, String> QHM(String Q) {
        HashMap<String, String> HM = new HashMap<>();
        Q = Q.replaceFirst("\\{", "").substring(0, Q.length() - 2); // {“user”:”x”,”pass”:”y”} -> “user”:”x”,”pass”:”y”
        String[] pairs = Q.split(","); // ["user":"x" , "pass":"y"]
        for (String pair2 : pairs) {
            String[] pair = pair2.split(":"); // "user" , "x"
            HM.put(pair[0].replaceFirst("\"", "").substring(0, pair[0].length() - 2), pair[1].replaceFirst("\"", "").substring(0, pair[1].length() - 2));
        }
        return HM;
    }

    /*
     * Hashmap to Query string
     */
    public static String HMQ(HashMap<String, String> HM) {
        StringBuilder Q = new StringBuilder("{");

        int iter = 0;
        for (Map.Entry<String, String> entry : HM.entrySet()) {
            String E = "\"" + entry.getKey() + "\":" + "\"" + entry.getValue() + "\"";
            iter++;
            if (!(iter + 1 > HM.size())) E += ",";
            Q.append(E);
        }
        Q.append("}");
        return Q.toString();
    }

}
