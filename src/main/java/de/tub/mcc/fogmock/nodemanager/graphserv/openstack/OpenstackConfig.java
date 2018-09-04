package de.tub.mcc.fogmock.nodemanager.graphserv.openstack;

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
        "openstack",
        "external_network",
        "mgmt_network_name",
        "ssh_key_name",
        "ssh_user"
})
public class OpenstackConfig {

    @JsonProperty("openstack")
    private Openstack openstack;
    @JsonProperty("external_network")
    private String externalNetwork;
    @JsonProperty("mgmt_network_name")
    private String mgmtNetworkName;
    @JsonProperty("ssh_key_name")
    private String sshKeyName;
    @JsonProperty("ssh_user")
    private String sshUser;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("openstack")
    public Openstack getOpenstack() {
        return openstack;
    }

    @JsonProperty("openstack")
    public void setOpenstack(Openstack openstack) {
        this.openstack = openstack;
    }

    @JsonProperty("external_network")
    public String getExternalNetwork() {
        return externalNetwork;
    }

    @JsonProperty("external_network")
    public void setExternalNetwork(String externalNetwork) {
        this.externalNetwork = externalNetwork;
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
        if (sshUser == null) sshUser = "ubuntu";
        return sshUser;
    }

    @JsonProperty("ssh_user")
    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
