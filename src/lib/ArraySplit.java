package lib;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ArraySplit {
    /*
     * Splits arr according to delimiter sequence
     * For example: [3,2,5,2], if delimiter is [2], then the new byte array is [3,5]
     */
    public static List<byte[]> split(byte[] array, byte[] delimiter) {
        List<byte[]> byteArrays = new LinkedList<>();
        int begin = 0;
        outer:
        for (int i = 0; i < array.length - delimiter.length + 1; i++) {
            for (int j = 0; j < delimiter.length; j++) if (array[i + j] != delimiter[j]) continue outer;
            byteArrays.add(Arrays.copyOfRange(array, begin, i));
            begin = i + delimiter.length;
        }
        byteArrays.add(Arrays.copyOfRange(array, begin, array.length));
        return byteArrays;
    }
}
