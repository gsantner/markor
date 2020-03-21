package net.gsantner.markor.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CryptWithPasswordTest {

    private final CryptWithPassword testee = new CryptWithPassword();


    @Test
    public void createRoundtrip() throws Exception {
        final String password = "Test";
        final String text = "äöüßqwe€dahfla fa lfha fh ajdfh ajhf ahf ajhf lhdslahfsajlhfalh adjhf ahf lahlfhasdl";
        final byte[] encrypt = testee.encrypt(text, password.toCharArray());
        final String decrypt = testee.decrypt(encrypt, password.toCharArray());
        assertEquals(text, decrypt);
    }

}