/**
 * Created by gokul on 10/24/15.
 */
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AeSimpleSHA1 {

    private static long convertToInt(byte[] data) {
        long value = 0;
        for (int i = 0; i < 4 && i < data.length; i++) {
            value += ((long) data[i] & 0xffL) << (8 * i);
        }
        return value;
    }

    public static long SHA1(String text)
            throws NoSuchAlgorithmException, UnsupportedEncodingException  {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return convertToInt(sha1hash);
    }
}