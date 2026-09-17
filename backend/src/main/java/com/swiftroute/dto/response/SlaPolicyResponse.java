package com.swiftroute.dto.response;

import com.swiftroute.domain.enums.Priority;

public class SlaPolicyResponse {

    private Long id;
    private Priority priority;
    private Integer responseDeadlineMinutes;
    private Integer resolutionDeadlineMinutes;

    public SlaPolicyResponse() {
    }

    public SlaPolicyResponse(Long id, Priority priority, Integer responseDeadlineMinutes, Integer resolutionDeadlineMinutes) {
        this.id = id;
        this.priority = priority;
        this.responseDeadlineMinutes = responseDeadlineMinutes;
        this.resolutionDeadlineMinutes = resolutionDeadlineMinutes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Integer getResponseDeadlineMinutes() {
        return responseDeadlineMinutes;
    }

    public void setResponseDeadlineMinutes(Integer responseDeadlineMinutes) {
        this.responseDeadlineMinutes = responseDeadlineMinutes;
    }

    public Integer getResolutionDeadlineMinutes() {
        return resolutionDeadlineMinutes;
    }

    public void setResolutionDeadlineMinutes(Integer resolutionDeadlineMinutes) {
        this.resolutionDeadlineMinutes = resolutionDeadlineMinutes;
    }
}
