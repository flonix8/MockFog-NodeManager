package de.tub.mcc.fogmock.nodemanager.graphserv;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Model {
    protected static final Pattern namePattern = Pattern.compile("^([A-Za-z0-9][-.\\w]*[A-Za-z0-9)]|[A-Za-z])$");
    protected static final Pattern flavorPattern = Pattern.compile("^([+-.\\/)(\\s\\w]*)$");
    public Map<String, Object> props = new HashMap<String, Object>(); //this properties will appear in the N4J edges
    public Map<String, Object> propsAll = new HashMap<String, Object>(); //this properties will appear in the N4J edges

    protected void setProps(String key, Object value, Object nullValue, Pattern pattern) throws ExceptionInvalidData {
        if (value != null) {
            if ( pattern != null ) checkPattern(key, (String)value, pattern);
            this.props.put(key, value);
            this.propsAll.put(key, value);
        } else {
            if (nullValue == null) throw new ExceptionInvalidData("missing value " + key);
            this.propsAll.put(key, nullValue);
        }
    }

    protected void setPropsParseLongFromString(String key, String value, Long nullValue) throws ExceptionInvalidData {
        if (value != null) {
            this.props.put(key, Long.parseLong(value));
            this.propsAll.put(key, Long.parseLong(value));
        } else {
            if (nullValue == null) throw new ExceptionInvalidData("missing value " + key);
            this.propsAll.put(key, nullValue);
        }
    }

    protected void setPropsIp(String keySuffix, ModelIp mip) {
        mip.propsAll.entrySet().forEach(e->this.propsAll.put(e.getKey()+keySuffix, e.getValue()));
        mip.props.entrySet().forEach(e->this.props.put(e.getKey()+keySuffix, e.getValue()));
    }

    protected void setPropsParseDoubleFromString (String key, String value, Double nullValue) throws ExceptionInvalidData {
        if (value != null) {
            this.props.put(key, Double.parseDouble(value));
            this.propsAll.put(key, Double.parseDouble(value));
        } else {
            if (nullValue == null) throw new ExceptionInvalidData("missing value " + key);
            this.propsAll.put(key, nullValue);
        }
    }

    protected void checkPattern(String key, String value, Pattern pattern) throws ExceptionInvalidData {
        if ( !pattern.matcher(value).find() ) {
            throw new ExceptionInvalidData("invalid value at field '"+key+"': "+value);
        }
    }


}
