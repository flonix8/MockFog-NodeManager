package de.tub.mcc.fogmock.nodemanager.graphserv.agent;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;
import de.tub.mcc.fogmock.nodemanager.graphserv.Model;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "out_rate",
        "in_rate",
        "rules"
})
public class ResponseTcConfig {

    @JsonIgnore
    private String mgmtIp;

    @JsonProperty("out_rate")
    private String out_rate;

    @JsonProperty("in_rate")
    private String in_rate;

    @JsonProperty("rules")
    private List<Rule> rules = null;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonIgnore
    public String getMgmtIp() { return mgmtIp; }

    @JsonIgnore
    public void setMgmtIp(String mgmtIp) { this.mgmtIp = mgmtIp; }

    @JsonProperty("out_rate")
    public String getOut_rate() { return out_rate; }

    @JsonProperty("out_rate")
    public void setOut_rate(String out_rate) { this.out_rate = out_rate; }

    @JsonProperty("in_rate")
    public String getIn_rate() { return in_rate; }

    @JsonProperty("in_rate")
    public void setIn_rate(String in_rate) { this.in_rate = in_rate; }


    @JsonProperty("rules")
    public List<Rule> getRules() {
        return rules;
    }

    @JsonProperty("rules")
    public void setRules(List<Rule> rules) { this.rules = rules; }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

//    @JsonCreator
//    public ResponseTcConfig (@JsonProperty("out_rate") String out_rate, @JsonProperty("in_rate") String in_rate,
//                             @JsonProperty("rules") List<Rule> rules){
//        setProps("out_rate", out_rate, 1000000L);
//        setProps("in_rate", out_rate, 1000000L);
//        setRules(rules);
//    }

}