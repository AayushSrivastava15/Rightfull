package com.rightful.common.response;

import java.time.Instant;
import java.util.List;

public class ErrorDetails {

    private String code;
    private String message;
    private String path;
    private List<String> details;
    private Instant timestamp;

    public ErrorDetails() {
    }

    public ErrorDetails(String code, String message, String path, Instant timestamp) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
