package ehrlab.attendance.infra;

import ehrlab.attendance.domain.AttendanceRecord;
import ehrlab.attendance.domain.AttendanceRecordRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAttendanceRecordRepository 
    extends JpaRepository<AttendanceRecord, Long>, AttendanceRecordRepository {
}
