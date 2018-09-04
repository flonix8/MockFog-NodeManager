package de.tub.mcc.fogmock.nodemanager.graphserv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.parboiled.common.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModelVertex extends Model {

    public Map<String, ModelEdge> edgesBack = Collections.emptyMap();

    @JsonCreator
    public ModelVertex(@JsonProperty("name") String name, @JsonProperty("cancelled") Boolean cancelled) throws ExceptionInvalidData {
        setProps("name", name, "new", namePattern);
        setProps("cancelled", cancelled, false, null);
    }


}
