package kr.ac.knue.commonfoundation.emailverification;

import java.time.Duration;
import java.util.Properties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public class GmailSmtpMailProperties {
    private String host = "smtp.gmail.com";
    private int port = 587;
    private String username = "";
    private String password = "";
    private boolean smtpAuth = true;
    private boolean starttlsEnable = true;
    private String from = "";
    private boolean verifiedFromAlias = false;
    private String verificationBaseUrl = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(5);
    private Duration writeTimeout = Duration.ofSeconds(5);

    public Properties toJavaMailProperties() {
        validate();
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", Boolean.toString(smtpAuth));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(starttlsEnable));
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.connectiontimeout", Long.toString(connectTimeout.toMillis()));
        properties.put("mail.smtp.timeout", Long.toString(readTimeout.toMillis()));
        properties.put("mail.smtp.writetimeout", Long.toString(writeTimeout.toMillis()));
        return properties;
    }

    public void validate() {
        if (from == null || from.isBlank()) {
            from = username;
        }
        if (username != null && !username.isBlank() && !from.equalsIgnoreCase(username) && !verifiedFromAlias) {
            throw new IllegalStateException("발신 주소가 Gmail 사용자와 다르면 검증된 Gmail 별칭임을 명시해야 합니다.");
        }
        if (host == null || host.isBlank() || username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Gmail SMTP 발송 설정이 완료되지 않았습니다.");
        }
        if (!starttlsEnable) {
            throw new IllegalStateException("Gmail SMTP STARTTLS 설정은 비활성화할 수 없습니다.");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public boolean isStarttlsEnable() {
        return starttlsEnable;
    }

    public void setStarttlsEnable(boolean starttlsEnable) {
        this.starttlsEnable = starttlsEnable;
    }

    public String getFrom() {
        return from == null || from.isBlank() ? username : from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public boolean isVerifiedFromAlias() {
        return verifiedFromAlias;
    }

    public void setVerifiedFromAlias(boolean verifiedFromAlias) {
        this.verifiedFromAlias = verifiedFromAlias;
    }

    public String getVerificationBaseUrl() {
        return verificationBaseUrl;
    }

    public void setVerificationBaseUrl(String verificationBaseUrl) {
        this.verificationBaseUrl = verificationBaseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(Duration writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
}
