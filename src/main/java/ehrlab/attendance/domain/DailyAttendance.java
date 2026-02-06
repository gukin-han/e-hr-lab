package ehrlab.attendance.domain;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_attendance", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_attendance", 
                         columnNames = {"tenantId", "employeeId", "workDate"})
    },
    indexes = {
        @Index(name = "idx_daily_attendance_lookup", 
               columnList = "tenantId, employeeId, workDate")
    })
public class DailyAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    private Instant firstCheckIn;

    private Instant lastCheckOut;

    @Column(nullable = false)
    private Integer totalWorkMinutes;

    protected DailyAttendance() {
        // JPA용 기본 생성자
    }

    private DailyAttendance(Long tenantId, Long employeeId, LocalDate workDate) {
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.status = AttendanceStatus.NOT_STARTED;
        this.totalWorkMinutes = 0;
    }

    public static DailyAttendance createNew(Long tenantId, Long employeeId, LocalDate workDate) {
        return new DailyAttendance(tenantId, employeeId, workDate);
    }

    public void checkIn(Instant time) {
        if (this.status == AttendanceStatus.WORKING) {
            throw new IllegalStateException("이미 출근 상태입니다.");
        }
        
        if (this.firstCheckIn == null) {
            this.firstCheckIn = time;
        }
        this.status = AttendanceStatus.WORKING;
    }

    public void checkOut(Instant time) {
        if (this.status != AttendanceStatus.WORKING) {
            throw new IllegalStateException("출근 상태가 아닙니다.");
        }
        
        this.lastCheckOut = time;
        this.status = AttendanceStatus.LEFT;
        
        // 근무시간 계산 (이번 세션)
        Instant sessionStart = (this.firstCheckIn != null) ? this.firstCheckIn : time;
        if (this.lastCheckOut != null && sessionStart != null) {
            long minutes = Duration.between(sessionStart, this.lastCheckOut).toMinutes();
            this.totalWorkMinutes += (int) minutes;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public Instant getFirstCheckIn() {
        return firstCheckIn;
    }

    public Instant getLastCheckOut() {
        return lastCheckOut;
    }

    public Integer getTotalWorkMinutes() {
        return totalWorkMinutes;
    }
}
