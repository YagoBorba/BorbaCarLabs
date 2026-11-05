package io.rea.obd.ai;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts the user's API keys with an AES-GCM key that never leaves the Android Keystore. */
final class KeyVault {
    interface Backend {
        String seal(String plain) throws GeneralSecurityException;

        String open(String sealed) throws GeneralSecurityException;
    }

    /** Replaced in JVM tests, where the Android Keystore is not available. */
    static Backend backend = new AndroidKeystore();

    private KeyVault() {
    }

    static String seal(String plain) throws GeneralSecurityException {
        return backend.seal(plain);
    }

    static String open(String sealed) throws GeneralSecurityException {
        return backend.open(sealed);
    }

    static final class AndroidKeystore implements Backend {
        private static final String STORE = "AndroidKeyStore";
        private static final String ALIAS = "borbacar-ai-api-keys";
        private static final String TRANSFORMATION = "AES/GCM/NoPadding";

        private static SecretKey key() throws GeneralSecurityException {
            KeyStore store = KeyStore.getInstance(STORE);
            try {
                store.load(null);
            } catch (IOException e) {
                throw new GeneralSecurityException(e);
            }
            KeyStore.Entry entry = store.getEntry(ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE);
            generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            return generator.generateKey();
        }

        @Override
        public String seal(String plain) throws GeneralSecurityException {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipher.getIV()) + ":" + Base64.getEncoder().encodeToString(sealed);
        }

        @Override
        public String open(String sealed) throws GeneralSecurityException {
            String[] parts = sealed.split(":", -1);
            if (parts.length != 2) {
                throw new GeneralSecurityException("Unexpected sealed value");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
        }
    }
}
