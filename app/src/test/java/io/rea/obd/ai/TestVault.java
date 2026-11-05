package io.rea.obd.ai;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-GCM with an in-memory key, standing in for the Android Keystore, which JVM tests do not have. */
final class TestVault implements KeyVault.Backend {
    static final TestVault INSTANCE = new TestVault();
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    private TestVault() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        key = new SecretKeySpec(bytes, "AES");
    }

    @Override
    public String seal(String plain) throws GeneralSecurityException {
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        return encoder.encodeToString(iv) + ":" + encoder.encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String open(String sealed) throws GeneralSecurityException {
        String[] parts = sealed.split(":", -1);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, java.util.Base64.getDecoder().decode(parts[0])));
        return new String(cipher.doFinal(java.util.Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
    }
}
