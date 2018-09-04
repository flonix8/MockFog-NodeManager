package de.tub.mcc.fogmock.nodemanager.graphserv.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseMessage {

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("msg")
    public String getMessage() {
        return msg;
    }

    @JsonProperty("msg")
    public void setMessage(String message) {
        this.msg = message;
    }
}