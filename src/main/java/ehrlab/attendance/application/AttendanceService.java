package ehrlab.attendance.application;

import ehrlab.attendance.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository recordRepository;
    private final DailyAttendanceRepository dailyRepository;

    public AttendanceService(AttendanceRecordRepository recordRepository,
                            DailyAttendanceRepository dailyRepository) {
        this.recordRepository = recordRepository;
        this.dailyRepository = dailyRepository;
    }

    @Transactional
    public CheckResult check(Long tenantId, Long employeeId, RecordType type) {
        // 1. 현재 시간 (UTC)
        Instant now = Instant.now();
        
        // 2. workDate 계산 (Phase 1: UTC 기준, 추후 테넌트 타임존 적용)
        LocalDate workDate = LocalDate.ofInstant(now, ZoneOffset.UTC);
        
        // 3. DailyAttendance 조회 or 생성
        DailyAttendance daily = dailyRepository
            .findByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, workDate)
            .orElseGet(() -> DailyAttendance.createNew(tenantId, employeeId, workDate));
        
        // 4. 출근/퇴근 처리
        if (type == RecordType.CHECK_IN) {
            daily.checkIn(now);
        } else {
            daily.checkOut(now);
        }
        
        // 5. 원장 기록 생성
        AttendanceRecord record = new AttendanceRecord(
            tenantId, employeeId, type, now, workDate
        );
        
        // 6. 저장
        AttendanceRecord savedRecord = recordRepository.save(record);
        dailyRepository.save(daily);
        
        // 7. 결과 반환
        return new CheckResult(
            savedRecord.getId(),
            now,
            daily.getStatus()
        );
    }
}
