package ehrlab.attendance;

import ehrlab.attendance.application.AttendanceService;
import ehrlab.attendance.application.CheckResult;
import ehrlab.attendance.domain.AttendanceStatus;
import ehrlab.attendance.domain.DailyAttendance;
import ehrlab.attendance.domain.DailyAttendanceRepository;
import ehrlab.attendance.domain.RecordType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Transactional
class AttendanceServiceTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private DailyAttendanceRepository dailyAttendanceRepository;

    @Test
    @DisplayName("출근 기록 시 상태가 WORKING으로 변경된다")
    void checkIn_changesStatusToWorking() {
        // given
        Long tenantId = 1L;
        Long employeeId = 100L;

        // when
        CheckResult result = attendanceService.check(tenantId, employeeId, RecordType.CHECK_IN);

        // then
        assertAll(
            () -> assertThat(result.dailyStatus()).isEqualTo(AttendanceStatus.WORKING),
            () -> assertThat(result.recordId()).isNotNull(),
            () -> assertThat(result.recordedAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("퇴근 기록 시 상태가 LEFT로 변경된다")
    void checkOut_changesStatusToLeft() {
        // given
        Long tenantId = 1L;
        Long employeeId = 100L;
        attendanceService.check(tenantId, employeeId, RecordType.CHECK_IN);

        // when
        CheckResult result = attendanceService.check(tenantId, employeeId, RecordType.CHECK_OUT);

        // then
        assertThat(result.dailyStatus()).isEqualTo(AttendanceStatus.LEFT);
    }

    @Test
    @DisplayName("이미 출근 상태에서 다시 출근하면 예외가 발생한다")
    void checkIn_whenAlreadyWorking_throwsException() {
        // given
        Long tenantId = 1L;
        Long employeeId = 100L;
        attendanceService.check(tenantId, employeeId, RecordType.CHECK_IN);

        // when & then
        assertThatThrownBy(() -> 
            attendanceService.check(tenantId, employeeId, RecordType.CHECK_IN)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("이미 출근 상태");
    }

    @Test
    @DisplayName("출근하지 않은 상태에서 퇴근하면 예외가 발생한다")
    void checkOut_whenNotWorking_throwsException() {
        // given
        Long tenantId = 1L;
        Long employeeId = 100L;

        // when & then
        assertThatThrownBy(() -> 
            attendanceService.check(tenantId, employeeId, RecordType.CHECK_OUT)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("출근 상태가 아닙니다");
    }

    @Test
    @DisplayName("출퇴근 후 재출근이 가능하다")
    void canCheckInAgainAfterCheckOut() {
        // given
        Long tenantId = 1L;
        Long employeeId = 100L;
        attendanceService.check(tenantId, employeeId, RecordType.CHECK_IN);
        attendanceService.check(tenantId, employeeId, RecordType.CHECK_OUT);

        // when
        CheckResult result = attendanceService.check(tenantId, employeeId, RecordType.CHECK_IN);

        // then
        assertThat(result.dailyStatus()).isEqualTo(AttendanceStatus.WORKING);
    }
}
