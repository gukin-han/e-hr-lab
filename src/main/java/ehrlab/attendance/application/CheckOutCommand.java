package ehrlab.attendance.application;

public record CheckOutCommand(
    Long tenantId,
    Long employeeId
) {}
