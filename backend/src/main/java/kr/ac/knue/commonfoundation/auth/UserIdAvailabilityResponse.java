package kr.ac.knue.commonfoundation.auth;

/**
 * Indicates whether a normalized public login identifier is available for registration.
 */
public record UserIdAvailabilityResponse(boolean available) {
}
