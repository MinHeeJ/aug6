package kr.ac.knue.commonfoundation.emailverification;

import org.springframework.stereotype.Repository;

@Repository
public class SignupEmailPolicy {
    private final SignupEmailPolicyMapper mapper;

    public SignupEmailPolicy(SignupEmailPolicyMapper mapper) {
        this.mapper = mapper;
    }

    public String validateNewSignupEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (EmailAddressPolicy.isSubstituteEmail(normalizedEmail)) {
            throw new IllegalArgumentException("admin@kndadmin.com is reserved for existing exempt account compatibility");
        }
        if (!EmailAddressPolicy.isValidEmailFormat(normalizedEmail)) {
            throw new IllegalArgumentException("email format is invalid");
        }
        if (mapper.countNormalUsersByEmail(normalizedEmail) > 0) {
            throw new IllegalArgumentException("duplicate email");
        }
        return normalizedEmail;
    }
}
