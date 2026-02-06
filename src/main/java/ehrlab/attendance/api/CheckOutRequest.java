package ehrlab.attendance.api;

import jakarta.validation.constraints.NotNull;

public record CheckOutRequest(
    @NotNull Long tenantId,
    @NotNull Long employeeId
) {}
