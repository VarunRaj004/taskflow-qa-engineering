package com.taskflow.demo.controller;

import com.taskflow.demo.dto.DashboardResponse;
import com.taskflow.demo.service.TaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TaskService taskService;

    public DashboardController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public DashboardResponse dashboard(@AuthenticationPrincipal Long userId) {
        long[] counts = taskService.getDashboardCounts(userId);
        return new DashboardResponse(counts[0], counts[1], counts[2], counts[3]);
    }
}
