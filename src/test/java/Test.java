import org.apache.commons.codec.binary.Base64;

import java.security.SecureRandom;
import java.util.Random;

public class Test {
    public static void main(String[] args) {
        final Random r = new SecureRandom();
        byte[] salt = new byte[32];
        r.nextBytes(salt);
        String encodedSalt = Base64.encodeBase64String(salt);
        System.out.println(encodedSalt.length());
    }
}
