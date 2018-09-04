package de.tub.mcc.fogmock.nodemanager.graphserv.openstack;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.tub.mcc.fogmock.nodemanager.graphserv.Model;


@JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "ssh_key_name",
            "ssh_user",
            "external_network",
//            "mgmt_network_name",
            "auth_url",
            "username",
            "password",
            "project_name"
    })
    public class ResponseOpenstackConfig extends Model {

        @JsonProperty("ssh_key_name")
        private String osSshKeyName;
        @JsonProperty("ssh_user")
        private String sshUser;
        @JsonProperty("external_network")
        private String externalNetwork;
//        @JsonProperty("mgmt_network_name")
//        private String mgmtNetworkName;
        @JsonProperty("auth_url")
        private String authUrl;
        @JsonProperty("username")
        private String username;
        @JsonProperty("password")
        private String password;
        @JsonProperty("project_name")
        private String projectName;

        @JsonProperty("ssh_key_name")
        public String getOsSshKeyName() {
            if (osSshKeyName==null) osSshKeyName ="";
            return osSshKeyName;
        }

        @JsonProperty("ssh_key_name")
        public void setOsSshKeyName(String osSshKeyName) {
            this.osSshKeyName = osSshKeyName;
        }

        @JsonProperty("ssh_user")
        public String getSshUser() {
            if (sshUser==null) sshUser ="ubuntu";
            return sshUser;
        }

        @JsonProperty("ssh_user")
        public void setSshUser(String sshUser) {
            this.sshUser = sshUser;
        }

        @JsonProperty("external_network")
        public String getExternalNetwork() {
            if (externalNetwork==null) externalNetwork="";
            return externalNetwork;
        }

        @JsonProperty("external_network")
        public void setExternalNetwork(String externalNetwork) {
            this.externalNetwork = externalNetwork;
        }

//        @JsonProperty("mgmt_network_name")
//        public String getMgmtNetworkName() {
//            return mgmtNetworkName;
//        }
//
//        @JsonProperty("mgmt_network_name")
//        public void setMgmtNetworkName(String mgmtNetworkName) {
//            this.mgmtNetworkName = mgmtNetworkName;
//        }

        @JsonProperty("auth_url")
        public String getAuthUrl() {
            if (authUrl==null) authUrl="";
            return authUrl;
        }

        @JsonProperty("auth_url")
        public void setAuthUrl(String authUrl) {
            this.authUrl = authUrl;
        }

        @JsonProperty("username")
        public String getUsername() {
            if (username==null) username="";
            return username;
        }

        @JsonProperty("username")
        public void setUsername(String username) {
            this.username = username;
        }

        @JsonProperty("password")
        public String getPassword() {
            if(password==null) password="";
            return password;
        }

        @JsonProperty("password")
        public void setPassword(String password) {
            this.password = password;
        }

        @JsonProperty("project_name")
        public String getProjectName() {
            if(projectName==null) projectName="";
            return projectName;
        }

        @JsonProperty("project_name")
        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

    }