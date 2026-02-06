package ehrlab.attendance.api;

import java.time.Instant;

public record CheckResponse(
    Long recordId,
    Instant recordedAt,
    String dailyStatus
) {}
