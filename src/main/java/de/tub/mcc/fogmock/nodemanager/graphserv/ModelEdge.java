package de.tub.mcc.fogmock.nodemanager.graphserv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown=true)
public class ModelEdge extends ModelIp {


    @JsonCreator
    public ModelEdge(@JsonProperty("idEdge") String idEdge,
                     @JsonProperty("addr") String addr,
                     @JsonProperty("in_rate") String in_rate,
                     @JsonProperty("out_rate") String out_rate,
                     @JsonProperty("delay") String delay,
                     @JsonProperty("dispersion") String dispersion,
                     @JsonProperty("loss") String loss,
                     @JsonProperty("corrupt") String corrupt,
                     @JsonProperty("duplicate") String duplicate,
                     @JsonProperty("reorder") String reorder,
                     @JsonProperty("cancelled") Boolean cancelled)
            throws ExceptionInvalidData {

        super(addr);
        setPropsParseLongFromString("in_rate", in_rate, 1000000L);
        setPropsParseLongFromString("out_rate", out_rate, 1000000L);
        setPropsParseLongFromString("delay", delay, 0L);
        setPropsParseLongFromString("dispersion", dispersion, 0L);
        setPropsParseDoubleFromString("loss", loss,0.0);
        setPropsParseDoubleFromString("corrupt", corrupt, 0.0);
        setPropsParseDoubleFromString("duplicate", duplicate, 0.0);
        setPropsParseDoubleFromString("reorder", reorder, 0.0);
        setProps("cancelled", cancelled, false, null);
    }

    @JsonIgnore
    public static ModelEdge STANDARD_EDGE;
    @JsonIgnore
    public static ModelEdge MAX_DISTANT_EDGE;
    @JsonIgnore
    public static ModelEdge MIN_DISTANT_EDGE;

    static {
        try {
            STANDARD_EDGE = new ModelEdge( null, null, null, null, null , null, null, null, null, null, null);
            MAX_DISTANT_EDGE = new ModelEdge( null, null, "0", "0", String.valueOf(Integer.MAX_VALUE), String.valueOf(Integer.MAX_VALUE), null, null, null, null, null);
            MIN_DISTANT_EDGE = new ModelEdge( null, null,  String.valueOf(Long.MAX_VALUE), String.valueOf(Long.MAX_VALUE),  "0", "0", null, null, null, null, null);
        } catch (ExceptionInvalidData e) {}
    }


}
