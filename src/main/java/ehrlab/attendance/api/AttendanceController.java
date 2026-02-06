package ehrlab.attendance.api;

import ehrlab.attendance.application.AttendanceService;
import ehrlab.attendance.application.CheckResult;
import ehrlab.attendance.domain.RecordType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check")
    public ResponseEntity<CheckResponse> check(@Valid @RequestBody CheckRequest request) {
        RecordType type = RecordType.valueOf(request.type().toUpperCase());
        
        CheckResult result = attendanceService.check(
            request.tenantId(),
            request.employeeId(),
            type
        );
        
        CheckResponse response = new CheckResponse(
            result.recordId(),
            result.recordedAt(),
            result.dailyStatus().name()
        );
        
        return ResponseEntity.ok(response);
    }
}
