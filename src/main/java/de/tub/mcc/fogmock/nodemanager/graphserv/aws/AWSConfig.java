package de.tub.mcc.fogmock.nodemanager.graphserv.aws;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "ec2_access_key",
        "ec2_secret_access_key",
        "ec2_region",
        "mgmt_network_name",
        "ssh_key_name",
        "ssh_user"
})
public class AWSConfig {

    @JsonProperty("ec2_access_key")
    private String ec2AccessKey;
    @JsonProperty("ec2_secret_access_key")
    private String ec2SecretAccessKey;
    @JsonProperty("ec2_region")
    private String ec2Region;
    @JsonProperty("mgmt_network_name")
    private String mgmtNetworkName;
    @JsonProperty("ssh_key_name")
    private String sshKeyName;
    @JsonProperty("ssh_user")
    private String sshUser;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("ec2_access_key")
    public String getEc2AccessKey() {
        return ec2AccessKey;
    }

    @JsonProperty("ec2_access_key")
    public void setEc2AccessKey(String ec2AccessKey) {
        this.ec2AccessKey = ec2AccessKey;
    }

    @JsonProperty("ec2_secret_access_key")
    public String getEc2SecretAccessKey() {
        return ec2SecretAccessKey;
    }

    @JsonProperty("ec2_secret_access_key")
    public void setEc2SecretAccessKey(String ec2SecretAccessKey) {
        this.ec2SecretAccessKey = ec2SecretAccessKey;
    }

    @JsonProperty("ec2_region")
    public String getEc2Region() {
        return ec2Region;
    }

    @JsonProperty("ec2_region")
    public void setEc2Region(String ec2Region) {
        this.ec2Region = ec2Region;
    }

    @JsonProperty("mgmt_network_name")
    public String getMgmtNetworkName() {
        return mgmtNetworkName;
    }

    @JsonProperty("mgmt_network_name")
    public void setMgmtNetworkName(String mgmtNetworkName) {
        this.mgmtNetworkName = mgmtNetworkName;
    }

    @JsonProperty("ssh_key_name")
    public String getSshKeyName() {
        return sshKeyName;
    }

    @JsonProperty("ssh_key_name")
    public void setSshKeyName(String sshKeyName) {
        this.sshKeyName = sshKeyName;
    }

    @JsonProperty("ssh_user")
    public String getSshUser() {
        if (sshUser == null) sshUser = "ec2-user";
        return sshUser;
    }

    @JsonProperty("ssh_user")
    public void setSshUser(String sshUser) { this.sshUser = sshUser; }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
