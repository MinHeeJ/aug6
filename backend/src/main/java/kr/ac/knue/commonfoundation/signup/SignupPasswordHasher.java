package kr.ac.knue.commonfoundation.signup;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates salted Argon2id hashes without modifying the existing authentication adapter. */
@Component
public class SignupPasswordHasher {
    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);

    public String hash(String password) {
        return encoder.encode(password);
    }
}
