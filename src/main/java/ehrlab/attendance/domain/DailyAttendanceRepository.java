package ehrlab.attendance.domain;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyAttendanceRepository {
    Optional<DailyAttendance> findByTenantIdAndEmployeeIdAndWorkDate(
        Long tenantId, Long employeeId, LocalDate workDate);
    
    DailyAttendance save(DailyAttendance daily);
}
