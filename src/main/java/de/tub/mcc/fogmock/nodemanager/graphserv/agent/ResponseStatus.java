package de.tub.mcc.fogmock.nodemanager.graphserv.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseStatus {

    @JsonProperty("status")
    private String status;

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }
}
