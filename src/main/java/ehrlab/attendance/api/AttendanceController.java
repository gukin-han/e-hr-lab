package ehrlab.attendance.api;

import ehrlab.attendance.application.AttendanceService;
import ehrlab.attendance.application.CheckInCommand;
import ehrlab.attendance.application.CheckOutCommand;
import ehrlab.attendance.application.CheckResult;
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

    @PostMapping("/check-in")
    public ResponseEntity<CheckResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        CheckInCommand command = new CheckInCommand(
            request.tenantId(),
            request.employeeId()
        );

        CheckResult result = attendanceService.checkIn(command);

        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/check-out")
    public ResponseEntity<CheckResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        CheckOutCommand command = new CheckOutCommand(
            request.tenantId(),
            request.employeeId()
        );

        CheckResult result = attendanceService.checkOut(command);

        return ResponseEntity.ok(toResponse(result));
    }

    private CheckResponse toResponse(CheckResult result) {
        return new CheckResponse(
            result.recordId(),
            result.recordedAt(),
            result.dailyStatus().name()
        );
    }
}
