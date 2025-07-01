package com.storywave.core.external.web.rsocket.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * RSocket 응답을 위한 공통 DTO
 */
public class StatusResponse {
    private boolean success;
    private String message;
    private Map<String, Object> data;

    public StatusResponse() {
        this.data = new HashMap<>();
    }

    public static StatusResponse success() {
        StatusResponse response = new StatusResponse();
        response.setSuccess(true);
        response.setMessage("요청이 성공적으로 처리되었습니다.");
        return response;
    }

    public static StatusResponse success(String message) {
        StatusResponse response = new StatusResponse();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    public static StatusResponse error(String message) {
        StatusResponse response = new StatusResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public StatusResponse addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
