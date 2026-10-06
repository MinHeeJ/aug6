package kr.ac.knue.commonfoundation.signupimplementation;

/** Availability concerns login identifiers in every account state, not just active accounts. */
public record UserIdAvailabilityResponse(boolean available) {
}
