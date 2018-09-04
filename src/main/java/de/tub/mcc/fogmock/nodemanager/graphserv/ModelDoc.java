package de.tub.mcc.fogmock.nodemanager.graphserv;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.util.stream.Collectors.*;

/*
 * 
 */
public class ModelDoc extends Model {
    @JsonIgnore
	public Long id;

	public Map<String, ModelNet> allNets;
    public Map<String, ModelNode> allNodes;

    @JsonIgnore
    public ModelIp addr;

    @JsonCreator
    public ModelDoc(@JsonProperty("name") String name, @JsonProperty("docName") String docName, @JsonProperty("addr") String addr, @JsonProperty("allNodes") Map<String, ModelNode> allNodes, @JsonProperty("allNets") Map<String, ModelNet> allNets) throws ExceptionInvalidData {
        this.addr = new ModelIp(addr);
        setPropsIp("", this.addr);
        setProps("name", name, "mgmt", namePattern);
        setProps("docName", docName, "New Document", null);

        if (allNets == null) {
            allNets = Collections.emptyMap();
        }
        if (allNodes == null) {
            allNodes = Collections.emptyMap();
        }
        this.allNets = allNets;
        this.allNodes = allNodes;

        /*
         * Check edge references
         */
        for ( String cand : allNets.values().stream().flatMap( x->x.edgesBack.keySet().stream() ).collect(toSet()) ) {
            if ( !allNets.containsKey(cand) ) {
                throw new ExceptionInvalidData("Illegal graph: orphaned edge refers to non-existing source net: "+cand);
            }
        }


    }

}
