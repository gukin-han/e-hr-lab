package ehrlab.thread;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/thread")
public class ThreadTestController {

  @GetMapping("/io")
  public String ioBound() throws InterruptedException {
    // Simulate IO-bound operation
    Thread.sleep(300);
    return "IO Done";
  }

  @GetMapping("/cpu")
  public String cpuBound() {
    long result = 0;

    // Simulate CPU-bound operation
    for (long i = 0; i < 2_000_000_000L; i++) {
      result += i;
    }

    return "CPU Done: " + result;
  }
}
