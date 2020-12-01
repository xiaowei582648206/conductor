/*
 * Copyright 2020 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.rest.controllers;

import static com.netflix.conductor.rest.config.RequestMappingConstants.TASKS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.netflix.conductor.common.metadata.tasks.PollData;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskExecLog;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.ExternalStorageLocation;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.TaskSummary;
import com.netflix.conductor.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(TASKS)
public class TaskResource {

    private final TaskService taskService;

    public TaskResource(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/poll/{tasktype}")
    @Operation(summary = "Poll for a task of a certain type")
    public Task poll(@PathVariable("tasktype") String taskType,
        @RequestParam(value = "workerid", required = false) String workerId,
        @RequestParam(value = "domain", required = false) String domain) {
        return taskService.poll(taskType, workerId, domain);
    }

    @GetMapping("/poll/batch/{tasktype}")
    @Operation(summary = "Batch poll for a task of a certain type")
    public List<Task> batchPoll(@PathVariable("tasktype") String taskType,
        @RequestParam(value = "workerid", required = false) String workerId,
        @RequestParam(value = "domain", required = false) String domain,
        @RequestParam(value = "count", defaultValue = "1") int count,
        @RequestParam(value = "timeout", defaultValue = "100") int timeout) {
        return taskService.batchPoll(taskType, workerId, domain, count, timeout);
    }

    @GetMapping("/in_progress/{tasktype}")
    @Operation(summary = "Get in progress tasks. The results are paginated.")
    public List<Task> getTasks(@PathVariable("tasktype") String taskType,
        @RequestParam(value = "startKey", required = false) String startKey,
        @RequestParam(value = "count", defaultValue = "100", required = false) int count) {
        return taskService.getTasks(taskType, startKey, count);
    }

    @GetMapping("/in_progress/{workflowId}/{taskRefName}")
    @Operation(summary = "Get in progress task for a given workflow id.")
    public Task getPendingTaskForWorkflow(@PathVariable("workflowId") String workflowId,
        @PathVariable("taskRefName") String taskReferenceName) {
        return taskService.getPendingTaskForWorkflow(workflowId, taskReferenceName);
    }

    @PostMapping(produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @Operation(summary = "Update a task")
    public String updateTask(@RequestBody TaskResult taskResult) {
        return taskService.updateTask(taskResult);
    }

    @PostMapping("/{taskId}/ack")
    @Operation(summary = "Ack Task is received")
    public String ack(@PathVariable("taskId") String taskId,
        @RequestParam(value = "workerid", required = false) String workerId) {
        return taskService.ackTaskReceived(taskId, workerId);
    }

    @PostMapping("/{taskId}/log")
    @Operation(summary = "Log Task Execution Details")
    public void log(@PathVariable("taskId") String taskId, String log) {
        taskService.log(taskId, log);
    }

    @GetMapping("/{taskId}/log")
    @Operation(summary = "Get Task Execution Logs")
    public List<TaskExecLog> getTaskLogs(@PathVariable("taskId") String taskId) {
        return taskService.getTaskLogs(taskId);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by Id")
    public Task getTask(@PathVariable("taskId") String taskId) {
        return taskService.getTask(taskId);
    }

    @DeleteMapping("/queue/{taskType}/{taskId}")
    @Operation(summary = "Remove Task from a Task type queue")
    public void removeTaskFromQueue(@PathVariable("taskType") String taskType,
        @PathVariable("taskId") String taskId) {
        taskService.removeTaskFromQueue(taskType, taskId);
    }

    @GetMapping("/queue/sizes")
    @Operation(summary = "Get Task type queue sizes")
    public Map<String, Integer> size(@RequestParam(value = "taskType", required = false) List<String> taskTypes) {
        return taskService.getTaskQueueSizes(taskTypes);
    }

    @GetMapping("/queue/all/verbose")
    @Operation(summary = "Get the details about each queue")
    public Map<String, Map<String, Map<String, Long>>> allVerbose() {
        return taskService.allVerbose();
    }

    @GetMapping("/queue/all")
    @Operation(summary = "Get the details about each queue")
    public Map<String, Long> all() {
        return taskService.getAllQueueDetails();
    }

    @GetMapping("/queue/polldata")
    @Operation(summary = "Get the last poll data for a given task type")
    public List<PollData> getPollData(@RequestParam("taskType") String taskType) {
        return taskService.getPollData(taskType);
    }

    @GetMapping("/queue/polldata/all")
    @Operation(summary = "Get the last poll data for all task types")
    public List<PollData> getAllPollData() {
        return taskService.getAllPollData();
    }

    @PostMapping(value = "/queue/requeue/{taskType}", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @Operation(summary = "Requeue pending tasks")
    public String requeuePendingTask(@PathVariable("taskType") String taskType) {
        return taskService.requeuePendingTask(taskType);
    }

    @Operation(summary = "Search for tasks based in payload and other parameters",
        description = "use sort options as sort=<field>:ASC|DESC e.g. sort=name&sort=workflowId:DESC." +
            " If order is not specified, defaults to ASC")
    @GetMapping(value = "/search", produces = APPLICATION_JSON_VALUE)
    public SearchResult<TaskSummary> search(
        @RequestParam(value = "start", defaultValue = "0", required = false) int start,
        @RequestParam(value = "size", defaultValue = "100", required = false) int size,
        @RequestParam(value = "sort", required = false) String sort,
        @RequestParam(value = "freeText", defaultValue = "*", required = false) String freeText,
        @RequestParam(value = "query", required = false) String query) {
        return taskService.search(start, size, sort, freeText, query);
    }

    @Operation(summary = "Get the external uri where the task payload is to be stored")
    @GetMapping("/externalstoragelocation")
    public ExternalStorageLocation getExternalStorageLocation(@RequestParam("path") String path,
        @RequestParam("operation") String operation, @RequestParam("payloadType") String payloadType) {
        return taskService.getExternalStorageLocation(path, operation, payloadType);
    }
}