package de.tub.mcc.fogmock.nodemanager.graphserv.agent;

import com.fasterxml.jackson.annotation.*;
import de.tub.mcc.fogmock.nodemanager.graphserv.ExceptionIpFormat;
import de.tub.mcc.fogmock.nodemanager.graphserv.Model;
import de.tub.mcc.fogmock.nodemanager.graphserv.ModelIp;

import java.util.HashMap;
import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "dst_net",
        "out_rate",
        "delay",
        "dispersion",
        "loss",
        "corrupt",
        "duplicate",
        "reorder"
})
public class Rule {

    @JsonProperty("dst_net")
    private String dstNet;

    @JsonProperty("out_rate")
    private String outRate;

    @JsonProperty("delay")
    private String delay;

    @JsonProperty("dispersion")
    private String dispersion;

    @JsonProperty("loss")
    private String loss;

    @JsonProperty("corrupt")
    private String corrupt;

    @JsonProperty("duplicate")
    private String duplicate;

    @JsonProperty("reorder")
    private String reorder;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("dst_net")
    public String getDstNet() {
        return dstNet;
    }

    @JsonProperty("dst_net")
    public void setDstNet(String dstNet) {
        this.dstNet = dstNet;
    }

    @JsonProperty("out_rate")
    public String getOutRate() { return outRate; }

    @JsonProperty("out_rate")
    public void setOutRate(String outRate) { this.outRate = outRate; }

    @JsonProperty("delay")
    public String getDelay() {
        return delay;
    }

    @JsonProperty("delay")
    public void setDelay(String delay) {
        this.delay = delay;
    }

    @JsonProperty("dispersion")
    public String getDispersion() {
        return dispersion;
    }

    @JsonProperty("dispersion")
    public void setDispersion(String dispersion) {
        this.dispersion = dispersion;
    }

    @JsonProperty("loss")
    public String getLoss() { return loss; }

    @JsonProperty("loss")
    public void setLoss(String loss) { this.loss = loss; }

    @JsonProperty("corrupt")
    public String getCorrupt() { return corrupt; }

    @JsonProperty("corrupt")
    public void setCorrupt(String corrupt) { this.corrupt = corrupt; }

    @JsonProperty("duplicate")
    public String getDuplicate() { return duplicate; }

    @JsonProperty("duplicate")
    public void setDuplicate(String duplicate) { this.duplicate = duplicate; }

    @JsonProperty("reorder")
    public String getReorder() { return reorder; }

    @JsonProperty("reorder")
    public void setReorder(String reorder) { this.reorder = reorder; }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}