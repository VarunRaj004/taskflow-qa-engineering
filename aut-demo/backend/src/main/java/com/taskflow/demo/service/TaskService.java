package com.taskflow.demo.service;

import com.taskflow.demo.dto.TaskCreateRequest;
import com.taskflow.demo.dto.TaskUpdateRequest;
import com.taskflow.demo.entity.Task;
import com.taskflow.demo.entity.TaskPriority;
import com.taskflow.demo.entity.TaskStatus;
import com.taskflow.demo.exception.BadRequestException;
import com.taskflow.demo.exception.ResourceNotFoundException;
import com.taskflow.demo.repository.TaskRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    private static final String NOT_FOUND_MESSAGE = "Task not found";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "title", "priority", "status");

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> listTasks(Long ownerId, String statusParam, String priorityParam, String search,
                                 String sortByParam, String sortDirParam) {
        TaskStatus status = parseStatus(statusParam);
        TaskPriority priority = parsePriority(priorityParam);

        String sortBy = ALLOWED_SORT_FIELDS.contains(sortByParam) ? sortByParam : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirParam) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Specification<Task> spec = (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }

        List<Task> tasks = taskRepository.findAll(spec, Sort.by(direction, sortBy));

        if (search != null && !search.isEmpty()) {
            tasks = tasks.stream()
                    .filter(task -> matches(task, search))
                    .toList();
        }

        return tasks;
    }

    private boolean matches(Task task, String search) {
        boolean titleMatches = task.getTitle() != null && task.getTitle().contains(search);
        boolean descriptionMatches = task.getDescription() != null && task.getDescription().contains(search);
        return titleMatches || descriptionMatches;
    }

    @Transactional
    public Task createTask(Long ownerId, TaskCreateRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        task.setOwnerId(ownerId);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task getOwnedTask(Long ownerId, Long taskId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MESSAGE));
    }

    @Transactional
    public Task updateTask(Long ownerId, Long taskId, TaskUpdateRequest request) {
        Task task = getOwnedTask(ownerId, taskId);

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long ownerId, Long taskId) {
        Task task = getOwnedTask(ownerId, taskId);
        taskRepository.delete(task);
    }

    private TaskStatus parseStatus(String statusParam) {
        if (statusParam == null || statusParam.isEmpty()) {
            return null;
        }
        try {
            return TaskStatus.valueOf(statusParam);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value");
        }
    }

    private TaskPriority parsePriority(String priorityParam) {
        if (priorityParam == null || priorityParam.isEmpty()) {
            return null;
        }
        try {
            return TaskPriority.valueOf(priorityParam);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid priority value");
        }
    }

    @Transactional(readOnly = true)
    public long[] getDashboardCounts(Long ownerId) {
        long total = taskRepository.countByOwnerId(ownerId);
        long todo = taskRepository.countByOwnerIdAndStatus(ownerId, TaskStatus.TODO);
        long inProgress = taskRepository.countByOwnerIdAndStatus(ownerId, TaskStatus.IN_PROGRESS);
        long done = taskRepository.countByOwnerIdAndStatus(ownerId, TaskStatus.DONE);
        return new long[] {total, todo, inProgress, done};
    }
}
