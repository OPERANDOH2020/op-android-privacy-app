/*
 * Copyright (c) 2016 {UPRC}.
 *
 * OperandoApp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OperandoApp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OperandoApp.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *       Nikos Lykousas {UPRC}, Constantinos Patsakis {UPRC}
 * Initially developed in the context of OPERANDO EU project www.operando.eu
 */

package eu.operando.operandoapp.wifi.model;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class WiFiUtils {

    private static final double DISTANCE_MHZ_M = 27.55;
    private static final int MIN_RSSI = -100;
    private static final int MAX_RSSI = -55;
    private static final String QUOTE = "\"";

    public static double calculateDistance(int frequency, int level) {
        return Math.pow(10.0, (DISTANCE_MHZ_M - (20 * Math.log10(frequency)) + Math.abs(level)) / 20.0);
    }

    public static int calculateSignalLevel(int rssi, int numLevels) {
        if (rssi <= MIN_RSSI) {
            return 0;
        }
        if (rssi >= MAX_RSSI) {
            return numLevels - 1;
        }
        return (rssi - MIN_RSSI) * (numLevels - 1) / (MAX_RSSI - MIN_RSSI);
    }

    public static String convertSSID(@NonNull String SSID) {
        return StringUtils.removeEnd(StringUtils.removeStart(SSID, QUOTE), QUOTE);
    }

    public static String convertIpAddress(int ipAddress) {
        byte[] bytes = BigInteger.valueOf(ipAddress).toByteArray();
        ArrayUtils.reverse(bytes);
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            return StringUtils.EMPTY;
        }
    }

}
