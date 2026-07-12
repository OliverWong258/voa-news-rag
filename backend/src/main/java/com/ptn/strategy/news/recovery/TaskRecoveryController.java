package com.ptn.strategy.news.recovery;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tasks")
public class TaskRecoveryController {

    private final TaskRecoveryService recoveryService;

    public TaskRecoveryController(TaskRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/{taskId}/replay")
    public ResponseEntity<Map<String, Object>> replay(@PathVariable long taskId) {
        boolean accepted = recoveryService.replayFailedTask(taskId);
        if (!accepted) {
            return ResponseEntity.badRequest().body(Map.of(
                    "taskId", taskId,
                    "accepted", false,
                    "message", "Task does not exist or is not FAILED/DEAD"));
        }
        return ResponseEntity.accepted().body(Map.of("taskId", taskId, "accepted", true));
    }

    @PostMapping("/recover-stale")
    public ResponseEntity<Void> recoverStale() {
        recoveryService.recoverStaleTasks();
        return ResponseEntity.accepted().build();
    }
}
