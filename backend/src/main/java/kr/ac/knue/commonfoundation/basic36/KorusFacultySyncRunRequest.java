package kr.ac.knue.commonfoundation.basic36;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record KorusFacultySyncRunRequest(@NotNull LocalDate targetStartDate, @NotNull LocalDate targetEndDate) {
}
