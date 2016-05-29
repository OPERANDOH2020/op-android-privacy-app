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

package eu.operando.operandoapp.wifi.band;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class WiFiChannelsGHZ5 extends WiFiChannels {
    private static final Pair<Integer, Integer> RANGE = new Pair<>(4900, 5899);
    private static final List<Pair<WiFiChannel, WiFiChannel>> SETS = Arrays.asList(
            new Pair<>(new WiFiChannel(8, 5040), new WiFiChannel(16, 5080)),
            new Pair<>(new WiFiChannel(36, 5180), new WiFiChannel(64, 5320)),
            new Pair<>(new WiFiChannel(100, 5500), new WiFiChannel(140, 5700)),
            new Pair<>(new WiFiChannel(149, 5745), new WiFiChannel(165, 5825)),
            new Pair<>(new WiFiChannel(184, 4910), new WiFiChannel(196, 4980)));

    private static final int FREQUENCY_OFFSET = WiFiChannel.FREQUENCY_SPREAD * 4;
    private static final int FREQUENCY_SPREAD = WiFiChannel.FREQUENCY_SPREAD;
    private static final int DEFAULT_PAIR = 1;

    WiFiChannelsGHZ5() {
        super(RANGE, SETS, FREQUENCY_OFFSET, FREQUENCY_SPREAD);
    }

    @Override
    public List<Pair<WiFiChannel, WiFiChannel>> getWiFiChannelPairs() {
        return Collections.unmodifiableList(SETS);
    }

    @Override
    public Pair<WiFiChannel, WiFiChannel> getWiFiChannelPairFirst(String countryCode) {
        List<Pair<WiFiChannel, WiFiChannel>> wiFiChannelPairs = getWiFiChannelPairs();
        if (!StringUtils.isBlank(countryCode)) {
            for (Pair<WiFiChannel, WiFiChannel> wiFiChannelPair : wiFiChannelPairs) {
                if (isChannelAvailable(countryCode, wiFiChannelPair.first.getChannel())) {
                    return wiFiChannelPair;
                }
            }
        }
        return wiFiChannelPairs.get(DEFAULT_PAIR);
    }

    @Override
    public List<WiFiChannel> getAvailableChannels(String countryCode) {
        List<WiFiChannel> wiFiChannels = new ArrayList<>();
        for (int channel : WiFiChannelCountry.find(countryCode).getChannelsGHZ5()) {
            wiFiChannels.add(getWiFiChannelByChannel(channel));
        }
        return wiFiChannels;
    }

    @Override
    public boolean isChannelAvailable(String countryCode, int channel) {
        return WiFiChannelCountry.find(countryCode).isChannelAvailableGHZ5(channel);
    }

    @Override
    public WiFiChannel getWiFiChannelByFrequency(int frequency, @NonNull Pair<WiFiChannel, WiFiChannel> wiFiChannelPair) {
        return isInRange(frequency) ? getWiFiChannel(frequency, wiFiChannelPair) : WiFiChannel.UNKNOWN;
    }
}
