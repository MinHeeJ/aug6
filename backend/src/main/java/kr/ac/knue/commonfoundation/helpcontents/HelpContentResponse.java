package kr.ac.knue.commonfoundation.helpcontents;

public record HelpContentResponse(
        String screenId,
        String businessDescription,
        String inputCriteria,
        String faq,
        String contact) {
}
