package ehrlab.attendance.api;

import jakarta.validation.constraints.NotNull;

public record CheckRequest(
    @NotNull Long tenantId,
    @NotNull Long employeeId,
    @NotNull String type
) {}
