package com.taskflow.demo.controller;

import com.taskflow.demo.dto.TaskCreateRequest;
import com.taskflow.demo.dto.TaskListResponse;
import com.taskflow.demo.dto.TaskResponse;
import com.taskflow.demo.dto.TaskUpdateRequest;
import com.taskflow.demo.entity.Task;
import com.taskflow.demo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public TaskListResponse list(@AuthenticationPrincipal Long userId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String priority,
                                  @RequestParam(required = false) String search,
                                  @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
                                  @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        List<Task> tasks = taskService.listTasks(userId, status, priority, search, sortBy, sortDir);
        List<TaskResponse> responses = tasks.stream().map(TaskResponse::from).toList();
        return new TaskListResponse(responses, responses.size());
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Long userId,
                                                @Valid @RequestBody TaskCreateRequest request) {
        Task task = taskService.createTask(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }

    @GetMapping("/{id}")
    public TaskResponse get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return TaskResponse.from(taskService.getOwnedTask(userId, id));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                @Valid @RequestBody TaskUpdateRequest request) {
        return TaskResponse.from(taskService.updateTask(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        taskService.deleteTask(userId, id);
        return ResponseEntity.noContent().build();
    }
}
