package lib;


import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.Map.Entry;

/**
 * SparkDB Object
 */
public class SparkDB {
    /**
     * Maps column name with a list of corresponding values
     */
    public HashMap<String, HMList> Mapper = new HashMap<>();
    /**
     * column names
     */
    public ArrayList<String> Headers = new ArrayList<>();
    /**
     * Number of entries (excluding the column name row)
     */
    public int num_queries = 0;
    /**
     * Number of columns
     */
    public int num_header = 0;

    public SparkDB(String filepath) {
        try {
        this.readFromFile(filepath);
        }catch(Exception E) {
            log.e(E, "SparkDB", "Constructor");
        }
    }

    /**
     * Create a database in memory
     *
     * @param headers every element is the header name
     */
    public void create(ArrayList<String> headers) throws Exception {
        StringBuilder rawHeader = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            boolean isLast = (headers.size() - 1) == i;
            if (!isLast) rawHeader.append("\"").append(headers.get(i)).append("\",");
            else rawHeader.append("\"").append(headers.get(i)).append("\"");
        }
        readFromString(rawHeader.toString());
    }

    /**
     * Reads encrypted CSV data from disk, decrypts it, then processes it.
     *
     * @param filename  The filename of the CSV file. <i>./data.csv</i> to point at
     *                  <b>data.csv</b> in the current directory.
     * @param Crypt_Key The decryption key
     */
    public void readFromFile(String filename, String Crypt_Key) throws Exception {
        BufferedInputStream BIF = new BufferedInputStream(new FileInputStream(filename), 4096);
        readFromString(new String(AES.decrypt(BIF.readAllBytes(), Crypt_Key)));
        BIF.close();
    }

    /**
     * Reads CSV data from disk, then processes it.
     *
     * @param filename The filename of the CSV file. <i>./data.csv</i> to point at
     *                 <b>data.csv</b> in the current directory.
     * @see #readFromString(String)
     */
    public void readFromFile(String filename) throws Exception {
        readFromString(new String(Objects.requireNonNull(IO.read(filename))));
    }

    /**
     * Processes CSV content into data structure
     *
     * @param data The input lines. First line should be the header. New line
     *             delimiters are '\n' and '\r'.<br>
     *             Headers example: <i>"username","password"</i><br>
     *             Query Example: <i>"morad","123"</i><br>
     *             Input String Argument Example:
     *             <i><code>"username","password"\n"morad","123"</code></i>
     */
    public void readFromString(String data) throws Exception {
        zero();
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        boolean isHeaderProcessed = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line;
        String[] header = null;
        String temp_1 = "";
        String temp_0 = "";
        while ((line = br.readLine()) != null) {
            if (!isHeaderProcessed) {
                header = line.split("\",\""); // ","
                temp_1 = header[header.length - 1].substring(0, header[header.length - 1].length() - 1);
                temp_0 = header[0].substring(1);
                Mapper.put(temp_0, new HMList());
                Headers.add(temp_0);
                for (int i = 1; i < (header.length - 1); i++) {
                    Mapper.put(header[i], new HMList());
                    Headers.add(header[i]);
                }
                Mapper.put(temp_1, new HMList());
                Headers.add(temp_1);
                num_header = header.length;
                isHeaderProcessed = true;
            } else {
                num_queries++;
                String[] single_col = line.split("\",\""); // ","
                Mapper.get(temp_0).add(single_col[0].substring(1));
                for (int x = 1; x < (num_header - 1); x++) {
                    Mapper.get(header[x]).add(single_col[x]);
                }
                Mapper.get(temp_1).add(single_col[num_header - 1].substring(0, single_col[num_header - 1].length() - 1));
            }
        }
        br.close();
    }

    /**
     * Get indices of the rows that apply certain rules.<br>
     * HashMap argument [pass=123] will return all rows that have '123' in 'pass'
     * column
     *
     * @param in   Rules in form of Key:Column name and Value:Column Value
     * @param iter How many indices should the function return
     * @return ArrayList with all IDs applying rules passed in HashMap argument
     */
    public ArrayList<Integer> getIDs(HashMap<String, String> in, int iter) {
        if (in.size() < 1) throw new IllegalArgumentException("HashMap argument passed in getIDs(..) has no elements");
        Entry<String, String> FirstElement = in.entrySet().iterator().next();
        ArrayList<Integer> out = new ArrayList<>(Mapper.get(FirstElement.getKey()).multipleGet(FirstElement.getValue(), iter));
        for (Integer temp : out) {
            boolean match = false;
            for (Entry<String, String> entry : in.entrySet()) {
                match = Mapper.get(entry.getKey()).get(temp).equals(entry.getValue());
            }
            if (!match) out.remove(temp);
        }
        return out;
    }

    /**
     * See {@link #getIDs(HashMap, int)}. Grab maximum indices possible that apply
     * certain rules.
     *
     * @param in Rules in form of Key:Column name and Value:Column Value
     * @return ArrayList with all IDs applying rules passed in HashMap argument
     */
    public ArrayList<Integer> getIDs(HashMap<String, String> in) {
        return getIDs(in, Integer.MAX_VALUE);
    }

    /**
     * Gets column values from of a certain column name for rows that apply certain
     * rules. See {@link #getIDs(HashMap, int)}.
     *
     * @param input     Rules in form of Key:Column name and Value:Column Value
     * @param ColToFind Target column name
     * @param iter      How many indices should the function return
     * @return Column values of certain rows that apply certain rows
     */
    public ArrayList<String> get(HashMap<String, String> input, String ColToFind, int iter) {
        ArrayList<String> Query = new ArrayList<>();
        ArrayList<Integer> indices = getIDs(input, iter);
        for (int index : indices) {
            Query.add(Mapper.get(ColToFind).get(index));
        }
        return Query;
    }

    /**
     * See {@link #get(HashMap, String, int)}. Grab maximum indices possible.
     *
     * @param input     Rules in form of Key:Column name and Value:Column Value
     * @param ColToFind Target column name
     * @return Column values of certain rows that apply certain rows
     */
    public ArrayList<String> get(HashMap<String, String> input, String ColToFind) {
        return get(input, ColToFind, Integer.MAX_VALUE);
    }

    /**
     * See {@link #get(ArrayList)}
     *
     * @param index Target index
     * @return The whole row in form of Key:Column name and Value:Column value
     */
    public HashMap<String, String> get(int index) {
        return get(new ArrayList<>() {
            {
                add(index);
            }
        }).get(0);
    }

    /**
     * Gets multiple rows based on its index value
     *
     * @param indices Target Indices
     * @return The rows in form of Key:Column name and Value:Column value
     */
    public ArrayList<HashMap<String, String>> get(ArrayList<Integer> indices) {
        ArrayList<HashMap<String, String>> out = new ArrayList<>();
        for (int index : indices) {
            HashMap<String, String> row = new HashMap<>();
            for (Entry<String, HMList> column : Mapper.entrySet()) {
                row.put(column.getKey(), column.getValue().get(index));
            }
            out.add(row);
        }
        return out;
    }

    /**
     * Gets a whole column as a HMList.
     *
     * @param column Target Column name
     * @return HMList that has all the column values
     */
    public HMList getColumn(String column) {
        return Mapper.get(column);
    }

    /**
     * Delete certain rows that apply certain rules. See
     * {@link #getIDs(HashMap, int)}
     *
     * @param input Rules in form of Key:Column name and Value:Column Value
     * @param iter  How many rows that apply certain rules should be removed
     */
    public void delete(HashMap<String, String> input, int iter) {
        ArrayList<Integer> indices = getIDs(input, iter);
        for (int index : indices) {
            for (String header : Headers) {
                Mapper.get(header).delete(index);
            }
        }
        num_queries = num_queries - indices.size();
    }

    /**
     * See {@link #delete(HashMap, int)}. Delete all rows that apply certain rules
     *
     * @param input Rules in form of Key:Column name and Value:Column Value
     */
    public void delete(HashMap<String, String> input) {
        delete(input, Integer.MAX_VALUE);
    }

    /**
     * Delete a row based on its index value
     *
     * @param index Target Index Value
     */
    public void delete(int index) {
        for (Entry<String, HMList> column : Mapper.entrySet()) {
            Mapper.get(column.getKey()).delete(index);
        }
        num_queries--;
    }

    /**
     * Adds a row. See {@link #add(ArrayList)}
     *
     * @param in Row in form of Key: Column name and Value: Column value
     */
    public void add(HashMap<String, String> in) {
        add(new ArrayList<>() {
            {
                add(in);
            }
        });
    }

    /**
     * Adds multiple rows. Every element in variable 'in' is a row
     *
     * @param in List of rows to be added. See {@link #add(HashMap)} for more
     *           details on HashMap structure
     */
    public void add(ArrayList<HashMap<String, String>> in) {
        for (HashMap<String, String> cmd : in) {
            if (!cmd.keySet().containsAll(Headers)) {
                throw new IllegalArgumentException("All supposed headers are not included in add(..) argument." + "Supposed Headers: " + Headers + "." + "Received Headers: " + cmd.keySet() + ".");
            }
            for (Entry<String, String> inputaya : cmd.entrySet()) {
                String input = inputaya.getValue();
                input = (input == null) || input.isBlank() || input.isEmpty() ? "0" : input;
                Mapper.get(inputaya.getKey()).add(input);
            }
        }
        num_queries++;
    }

    /**
     * Modifies column values for a specific column name(s) for rows that apply
     * certain rules. See {@link #getIDs(HashMap, int)}
     *
     * @param in   Rules in form of Key:Column name and Value:Column Value
     * @param edit Modification(s) to apply. In form of Key: Column name and Value:
     *             Column value
     * @param iter How many indices to modify
     */
    public void modify(HashMap<String, String> in, HashMap<String, String> edit, int iter) {
        ArrayList<Integer> indices = getIDs(in, iter);
        for (int index : indices) {
            for (Entry<String, String> modification : edit.entrySet()) {
                Mapper.get(modification.getKey()).edit(index, modification.getValue());
            }
        }
    }

    /**
     * See {@link #modify(HashMap, HashMap, int)}. Modifies all rows that apply
     * certain rules
     *
     * @param in   Rules in form of Key:Column name and Value:Column Value
     * @param edit Modification(s) to apply. In form of Key: Column name and Value:
     *             Column value
     */
    public void modify(HashMap<String, String> in, HashMap<String, String> edit) {
        modify(in, edit, Integer.MAX_VALUE);
    }

    /**
     * Modifies a certain row based on its index value
     *
     * @param index Target index value
     * @param edit  Modification(s) to apply. In form of Key: Column name and Value:
     *              Column value
     */
    public void modify(int index, HashMap<String, String> edit) {
        for (Entry<String, String> modification : edit.entrySet()) {
            Mapper.get(modification.getKey()).edit(index, modification.getValue());
        }
    }

    /**
     * Get the number of queries (size) of the database
     *
     * @return The number of queries, excluding headers
     */
    public int size() {
        return num_queries;
    }

    /**
     * Override for toString(). returns the current data structure in CSV format
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        // Print Headers
        for (int i = 0; i < num_header; i++) {
            out.append("\"").append(Headers.get(i)).append("\"");
            if (!((i + 1) == num_header)) {
                out.append(",");
            }
        }
        out.append("\n");
        // Print Data
        for (int i = 0; i < num_queries; i++) {
            for (int x = 0; x < num_header; x++) {
                out.append("\"").append(Mapper.get(Headers.get(x)).get(i)).append("\"");
                if (!((x + 1) == num_header)) {
                    out.append(",");
                }
            }
            out.append("\n");
        }
        return out.toString();
    }

    private void zero() {
        num_queries = 0;
        num_header = 0;
        Mapper = new HashMap<>();
        Headers = new ArrayList<>();
    }

    /**
     * A bidirectional list between index value and String value
     *
     * @author Morad Abdelrasheed Mokhtar Ali Gill
     */
    public static class HMList extends ArrayList<String> {
        /**
         * Get multiple indices that are linked with the same String value
         *
         * @param in   The String value
         * @param iter How many indices to grab
         * @return
         */
        public ArrayList<Integer> multipleGet(String in, int iter) {
            ArrayList<Integer> out = new ArrayList<>();
            int current = 0;
            for (int i = 0; i < this.size(); i++) {
                if (in.equals(this.get(i)) && current <= iter) {
                    out.add(i);
                    current++;
                }
            }
            return out;
        }

        /**
         * Edits an entry based on its index value
         *
         * @param i  Target Index Value
         * @param ns The new String value
         */
        public void edit(int i, String ns) {
            this.set(i, ns);
        }

        /**
         * Get String value based on its index
         *
         * @param i Target Index Value
         * @return The String value that is linked to the input index value
         */
        @Override
        public String get(int i) {
            return super.get(i);
        }

        /**
         * Deletes an element
         *
         * @param i Target Index value to delete
         */
        public void delete(int i) {
            this.remove(i);
        }
    }

    /**
     * AES Library for encryption and decryption
     */
    public static class AES {
        private static SecretKeySpec secretKey;
        private static byte[] key;

        public static void setKey(String myKey) throws Exception {
            MessageDigest sha;
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 32);
            secretKey = new SecretKeySpec(key, "AES");
        }

        public static byte[] decrypt(byte[] strToDecrypt, String secret) throws Exception {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(Arrays.copyOf(key, 16)));
            return cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
        }
    }
}