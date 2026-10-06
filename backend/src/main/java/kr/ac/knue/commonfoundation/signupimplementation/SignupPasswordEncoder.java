package kr.ac.knue.commonfoundation.signupimplementation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Component;

/** Argon2id PHC encoding with independent random salts; legacy SHA-256 verification stays in auth. */
@Component
public class SignupPasswordEncoder {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64 = Base64.getEncoder().withoutPadding();
    private static final String PREFIX = "$argon2id$v=19$m=19456,t=2,p=1$";

    public String encode(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt);
        try {
            return PREFIX + BASE64.encodeToString(salt) + "$" + BASE64.encodeToString(hash);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    /** Only this module's bounded cost profile is accepted, preventing cost injection from stored values. */
    public boolean matches(String password, String encoded) {
        if (password == null || encoded == null || !encoded.startsWith(PREFIX)) {
            return false;
        }
        byte[] actual = null;
        byte[] expected = null;
        try {
            String[] parts = encoded.substring(PREFIX.length()).split("[$]", -1);
            if (parts.length != 2) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            expected = Base64.getDecoder().decode(parts[1]);
            if (salt.length != 16 || expected.length != 32) {
                return false;
            }
            actual = derive(password, salt);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        } finally {
            if (actual != null) {
                Arrays.fill(actual, (byte) 0);
            }
            if (expected != null) {
                Arrays.fill(expected, (byte) 0);
            }
        }
    }

    private byte[] derive(String password, byte[] salt) {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(19456)
                .withIterations(2)
                .withParallelism(1)
                .withSalt(salt)
                .build();
        byte[] input = password.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[32];
        try {
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);
            generator.generateBytes(input, output);
            return output;
        } finally {
            Arrays.fill(input, (byte) 0);
            parameters.clear();
        }
    }
}
