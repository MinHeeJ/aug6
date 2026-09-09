package kr.ac.knue.commonfoundation.signup;

public class SignupRequest {
    private String loginId;
    private String password;
    private String passwordConfirm;
    private String email;

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordConfirm() { return passwordConfirm; }
    public void setPasswordConfirm(String passwordConfirm) { this.passwordConfirm = passwordConfirm; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "SignupRequest{loginId='" + loginId + "', email='" + email + "'}";
    }
}
