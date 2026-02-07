package ehrlab.attendance.application;

import ehrlab.attendance.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository recordRepository;
    private final DailyAttendanceRepository dailyRepository;

    @Transactional
    public CheckResult checkIn(CheckInCommand command) {
        Instant now = Instant.now();
        LocalDate workDate = LocalDate.ofInstant(now, ZoneOffset.UTC);

        DailyAttendance daily = findOrCreateDaily(command.tenantId(), command.employeeId(), workDate);
        daily.checkIn(now);

        AttendanceRecord record = new AttendanceRecord(
            command.tenantId(), command.employeeId(), RecordType.CHECK_IN, now, workDate
        );

        AttendanceRecord savedRecord = recordRepository.save(record);
        dailyRepository.save(daily);

        return new CheckResult(savedRecord.getId(), now, daily.getStatus());
    }

    @Transactional
    public CheckResult checkOut(CheckOutCommand command) {
        Instant now = Instant.now();
        LocalDate workDate = LocalDate.ofInstant(now, ZoneOffset.UTC);

        DailyAttendance daily = findOrCreateDaily(command.tenantId(), command.employeeId(), workDate);
        daily.checkOut(now);

        AttendanceRecord record = new AttendanceRecord(
            command.tenantId(), command.employeeId(), RecordType.CHECK_OUT, now, workDate
        );

        AttendanceRecord savedRecord = recordRepository.save(record);
        dailyRepository.save(daily);

        return new CheckResult(savedRecord.getId(), now, daily.getStatus());
    }

    private DailyAttendance findOrCreateDaily(Long tenantId, Long employeeId, LocalDate workDate) {
        return dailyRepository
            .findByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, workDate)
            .orElseGet(() -> DailyAttendance.createNew(tenantId, employeeId, workDate));
    }
}
