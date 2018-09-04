package de.tub.mcc.fogmock.nodemanager.graphserv.ansible;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseAnsible {

    /*
    This class provides the status and message of the ansible process.
    Each time a status is set and there is an Error occuring the User should see in which process (displaying the Status) this Error occurred.
     */

    public enum Status {
        NOT_STARTED, STARTED, BOOTSTRAPPING, BOOTSTRAPPED, PARSING_DHCP, PARSED_DHCP, PROPAGATING, PROPAGATED, GENERATING_YML, GENERATED_YML, PROCESSING, DESTROYING, DESTROYED, DONE, ERROR
    }

    public enum ErrorStatus {
        NO_ERROR, NOT_BOOTSTRAPPED, NOT_PARSED_DHCP, NOT_PROPAGATED, NOT_GENERATED_YML, NOT_DESTROYED
    }

    @JsonProperty("status")
    private Status status;


    @JsonProperty("error")
    private ErrorStatus error;

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("status")
    public Status getStatus() { return status; }

    @JsonProperty("status")
    public void setStatus(Status status) { this.status = status; }

    @JsonProperty("error")
    public ErrorStatus getError() { return error; }

    @JsonProperty("error")
    public void setError(ErrorStatus error) { this.error = error; }

    @JsonProperty("msg")
    public String getMessage() {
        msg = "neither error nor status set";
        if (status != null) {
            switch (status) {
                case NOT_STARTED: msg = "Ansible not started."; break;
                case STARTED: msg = "Ansible started."; break;
                case BOOTSTRAPPING: msg = "Bootstrapping..."; break;
                case BOOTSTRAPPED: msg = "Bootstrapping done."; break;
                case PARSING_DHCP: msg = "Ansible is parsing DHCP..."; break;
                case PARSED_DHCP: msg = "Ansible DHCP parsed."; break;
                case PROPAGATING: msg = "Ansible is propagating graph..."; break;
                case PROPAGATED: msg = "Ansible propagated graph and communicated properties to Agents."; break;
                case GENERATING_YML: msg = "Generating YML..."; break;
                case GENERATED_YML: msg = "Generated YML"; break;
                case PROCESSING: msg ="Ansible is processing..."; break;
                case DESTROYING: msg ="Ansible is currently destroying the setup..."; break;
                case DESTROYED: msg="Setup was destroyed successfully."; break;
                case ERROR: msg = "Error."; break;
                case DONE: msg = "Ansible is done with the environment setup. Please call GET /doc/{docId}."; break;
                default: msg = "No Status set"; break;
            }
        }
        if (error != null) {
            switch (error) {
                case NO_ERROR: msg += "\nNo Error occurred until last status."; break;
                case NOT_BOOTSTRAPPED: msg += "\nBootstrapping failed."; break;
                case NOT_PARSED_DHCP: msg += "\nParsing Dhcp failed."; break;
                case NOT_PROPAGATED: msg += "\nBootstrapping..."; break;
                case NOT_GENERATED_YML: msg += "\nGenerating Yml failed."; break;
                case NOT_DESTROYED: msg +="\nThe setup was not (fully) destroyed."; break;
                default: msg += "No Error set."; break;
            }
        }
        return msg; }

    @JsonProperty("msg")
    public void setMessage(String message) {
        this.msg = message;
    }
}
