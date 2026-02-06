package ehrlab.attendance.application;

import ehrlab.attendance.domain.AttendanceStatus;

import java.time.Instant;

public record CheckResult(
    Long recordId,
    Instant recordedAt,
    AttendanceStatus dailyStatus
) {}
