package de.tub.mcc.fogmock.nodemanager.graphserv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelNet extends ModelVertex {

    @JsonIgnore
    public ModelIp addr;

    @JsonCreator
    public ModelNet(@JsonProperty("name") String name, @JsonProperty("addr") String addr, @JsonProperty("cancelled") Boolean cancelled) throws ExceptionInvalidData {
        super(name, cancelled);
        this.addr = new ModelIp(addr);
        if ( this.addr.intMask > 30 ) throw new ExceptionIpFormat("illegal network ip range (mask not within range 0...30)");
        setPropsIp("", this.addr);

    }
}
