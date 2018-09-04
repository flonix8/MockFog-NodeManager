package de.tub.mcc.fogmock.nodemanager.graphserv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelIp extends Model implements Comparable<ModelIp> {
    private static final Pattern pattern = Pattern.compile("^(?<o1>1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.(?<o2>1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.(?<o3>1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.(?<o4>1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\/(?<mask>[0-9]|[1-2][0-9]|3[0-2]){1}$");
    private static final int mask32 = 0xffffffff;
    private static final int mask8 = 0x000000ff;

    public ModelIp() {
    }

    @JsonCreator
    public ModelIp(@JsonProperty("addr") String addr) throws ExceptionIpFormat {
        this.setIp(addr);
        if (addr != null) {
            this.props.put("addr", this.getFullIp());
            this.props.put("_mask", this.intMask);
        }
        this.propsAll.put("addr", this.getFullIp());
        this.propsAll.put("_mask", this.intMask);
    }


    @JsonIgnore
    public int intIp;
    @JsonIgnore
    public int intMask;
    @JsonIgnore
    public Integer getIntMask() {
        return intMask;
    }

    @JsonIgnore
    protected String getFullIp() {
        return ((intIp >>> 24) & mask8) + "." + ((intIp >>> 16) & mask8) + "." + ((intIp >>> 8) & mask8) + "." + (intIp & mask8) + "/" + intMask;
    }

    @JsonIgnore
    protected String getIpWithoutMask() {
        return ((intIp >>> 24) & mask8) + "." + ((intIp >>> 16) & mask8) + "." + ((intIp >>> 8) & mask8) + "." + (intIp & mask8);
    }


    @JsonIgnore
    protected void setIp(String ip) throws ExceptionIpFormat { // example: "10.7.1.2/8" or "10.7.1.2"
        if (ip == null) {
            ip = "0.0.0.0/0";
        }
        Matcher m = pattern.matcher(ip);
        if ( !m.matches() ) {
            m = pattern.matcher(ip+"/32");
            if ( !m.matches() ) {
                throw new ExceptionIpFormat("illegal ip address format detected.");
            }
        }

        this.intIp =  (Integer.parseInt(m.group("o1")) << 24) |
                (Integer.parseInt(m.group("o2")) << 16) |
                (Integer.parseInt(m.group("o3")) << 8) |
                Integer.parseInt(m.group("o4"));

        this.intMask = Integer.parseInt(m.group("mask"));

        //check for invalid network ip
        if ( intMask < 32 && this.getHostPart() != 0 ) {
            throw new ExceptionIpFormat("illegal network ip (non-zero host part)");
        }
    }


    @JsonIgnore
    protected String getNetworkIp() {
        int nwp = getNetworkPart();
        return ((nwp >>> 24) & mask8) + "." + ((nwp >>> 16) & mask8) + "." + ((nwp >>> 8) & mask8) + "." + (nwp & mask8) + "/" + intMask;
    }
//    @JsonIgnore
//    protected String getNetworkIp(int oMask) {
//        int nwp = getNetworkPart(oMask);
//        return ((nwp >>> 24) & mask8) + "." + ((nwp >>> 16) & mask8) + "." + ((nwp >>> 8) & mask8) + "." + (nwp & mask8) + "/" + oMask;
//    }
    @JsonIgnore
    protected int getNetworkPart() {
        return getNetworkPart(intMask);
    }

    @JsonIgnore
    protected int getHostPart() {
        return intIp & (mask32 >>> (intMask));
    }

    @JsonIgnore
    protected int getNetworkPart(int otherMask) {
        if (otherMask > 0)  return intIp & (mask32 << (32-otherMask));
        return 0;
    }

    /**
     * Returns the last ip address right before the broadcast address
     */
    @JsonIgnore
    protected String getLastIp() {
        int newIp = -2;
        if (intMask > 0) newIp = getNetworkPart() + (1 << (32-intMask)) - 2;
        return ((newIp >>> 24) & mask8) + "." + ((newIp >>> 16) & mask8) + "." + ((newIp >>> 8) & mask8) + "." + (newIp & mask8);
    }



    @JsonIgnore
    @Override
    public int compareTo(ModelIp o) {
        if (this.intMask == o.intMask) {
            if (this.intIp == o.intIp) return 0;
            return ((this.intIp & 0xffffffffL) > (o.intIp & 0xffffffffL) ) ? 1 : -1;
        }
        return (this.intMask < o.intMask) ? 1 : -1;
        //example: 11.0.2.0/28 < 11.1.1.0/28 < 10.1.2.0/26 < 13.1.2.0/26 < 11.0.0.0/25
    }
}
