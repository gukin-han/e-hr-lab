package ehrlab.attendance.infra;

import ehrlab.attendance.domain.DailyAttendance;
import ehrlab.attendance.domain.DailyAttendanceRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface JpaDailyAttendanceRepository 
    extends JpaRepository<DailyAttendance, Long>, DailyAttendanceRepository {
    
    Optional<DailyAttendance> findByTenantIdAndEmployeeIdAndWorkDate(
        Long tenantId, Long employeeId, LocalDate workDate);
}
