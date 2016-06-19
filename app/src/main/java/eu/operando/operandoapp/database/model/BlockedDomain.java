package eu.operando.operandoapp.database.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by periklismaravelias on 31/05/16.
 */
public class BlockedDomain {
    public String info;
    public String exfiltrated;

    public BlockedDomain(String info, String exfiltrated) {
        this.info = info.replaceAll("\\(.+?\\)", "");
        this.exfiltrated = exfiltrated;
    }
}