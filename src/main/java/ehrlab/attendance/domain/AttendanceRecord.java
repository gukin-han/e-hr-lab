package ehrlab.attendance.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_record", indexes = {
    @Index(name = "idx_attendance_record_lookup", 
           columnList = "tenantId, employeeId, workDate")
})
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType recordType;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false)
    private LocalDate workDate;

    protected AttendanceRecord() {
        // JPA용 기본 생성자
    }

    public AttendanceRecord(Long tenantId, Long employeeId, RecordType recordType, 
                           Instant recordedAt, LocalDate workDate) {
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.recordType = recordType;
        this.recordedAt = recordedAt;
        this.workDate = workDate;
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

    public RecordType getRecordType() {
        return recordType;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }
}
