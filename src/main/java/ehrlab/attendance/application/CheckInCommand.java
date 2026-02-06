package ehrlab.attendance.application;

public record CheckInCommand(
    Long tenantId,
    Long employeeId
) {}
