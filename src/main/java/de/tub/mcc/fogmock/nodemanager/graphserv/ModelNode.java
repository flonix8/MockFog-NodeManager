package de.tub.mcc.fogmock.nodemanager.graphserv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelNode extends ModelVertex {

    @JsonCreator
    public ModelNode(@JsonProperty("name") String name, @JsonProperty("image") String image, @JsonProperty("flavor") String flavor, @JsonProperty("cancelled") Boolean cancelled) throws ExceptionInvalidData {
        super(name, cancelled);
        setProps("flavor", flavor, "", flavorPattern);
        setProps("image", image, "", namePattern);
    }
}
