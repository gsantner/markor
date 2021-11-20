package other.de.stanetz.jpencconverter.cryption;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;


public class JavaPasswordbasedCryptionTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    private JavaPasswordbasedCryption testee;

    @Test
    public void createRoundtrip() {
        for (JavaPasswordbasedCryption.Version version : JavaPasswordbasedCryption.Version.values()) {
            testee = new JavaPasswordbasedCryption(version, RANDOM);
            final String password = "Test";
            final String text = "\u00e4\u00f6\u00fc\u00dfqwe\u20acdahfla fa lfha fh ajdfh ajhf ahf ajhf lhdslahfsajlhfalh adjhf ahf lahlfhasdl\u05D0\ua707\u4e16\u754c\u60a8\u597d";
            System.out.println(text);
            directTest(password, text);
            convenientTest(password, text);
        }
    }

    @Test
    public void createRoundtripStrongPW() {
        for (JavaPasswordbasedCryption.Version version : JavaPasswordbasedCryption.Version.values()) {
            testee = new JavaPasswordbasedCryption(version, RANDOM);
            final String password = "l\u05d0\ua707\u4e16\u754c\u60a8\u597dl\u05d0\ua707\u4e16\u754c\u60a8\u597dl\u05d0\ua707\u4e16\u754c\u60a8\u597dl";
            final String text = "\u00e4\u00f6\u00fc\u00dfqwe\u20acdahfla fa lfha fh ajdfh ajhf ahf ajhf lhdslahfsajlhfalh adjhf ahf lahlfhasdl\u05D0\ua707\u4e16\u754c\u60a8\u597d";
            System.out.println(text);
            directTest(password, text);
            convenientTest(password, text);
        }
    }

    private void convenientTest(String password, String text) {
        final byte[] encrypt = testee.encrypt(text, password.toCharArray());
        final String decrypt = JavaPasswordbasedCryption.getDecryptedText(encrypt, password.toCharArray());
        assertEquals(text, decrypt);
    }

    private void directTest(String password, String text) {
        final byte[] encrypt = testee.encrypt(text, password.toCharArray());
        final String decrypt = testee.decrypt(encrypt, password.toCharArray());
        assertEquals(text, decrypt);
    }

    @Test
    public void getVersion() {
        final byte[] bytes = "V001".getBytes(StandardCharsets.US_ASCII);
        final int length = bytes.length;
        assertEquals(JavaPasswordbasedCryption.Version.NAME_LENGTH, length);
        assertEquals(JavaPasswordbasedCryption.Version.V001, JavaPasswordbasedCryption.getVersion(bytes));
    }
}