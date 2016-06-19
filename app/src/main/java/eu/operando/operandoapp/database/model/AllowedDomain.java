package eu.operando.operandoapp.database.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import eu.operando.operandoapp.util.RequestFilterUtil;

/**
 * Created by periklismaravelias on 31/05/16.
 */
public class AllowedDomain {
    public String info;
    public String exfiltrated;

    public AllowedDomain(String info, String exfiltrated){
        this.info = info.replaceAll("\\(.+?\\)", "");
        this.exfiltrated = exfiltrated;
    }
}