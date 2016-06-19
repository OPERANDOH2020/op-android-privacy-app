package eu.operando.operandoapp.database.model;

/**
 * Created by periklismaravelias on 09/06/16.
 */
public class TrustedAccessPoint {
    public String ssid, bssid;

    public TrustedAccessPoint(String ssid, String bssid){
        this.ssid = ssid;
        this.bssid = bssid;
    }

    public boolean isEqual(TrustedAccessPoint trustedAccessPoint){
        return ((this.ssid.equals(trustedAccessPoint.ssid)) && (this.bssid.equals(trustedAccessPoint.bssid)));
    }
}
