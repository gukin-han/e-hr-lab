package ehrlab.attendance.api;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
    @NotNull Long tenantId,
    @NotNull Long employeeId
) {}
