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

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Comparator;

public enum SortBy {
    STRENGTH(new StrengthComparator()),
    SSID(new SSIDComparator()),
    CHANNEL(new ChannelComparator());

    private final Comparator<WiFiDetail> comparator;

    SortBy(@NonNull Comparator<WiFiDetail> comparator) {
        this.comparator = comparator;
    }

    public static SortBy find(int index) {
        try {
            return values()[index];
        } catch (Exception e) {
            return SortBy.STRENGTH;
        }
    }

    Comparator<WiFiDetail> comparator() {
        return comparator;
    }


    static class StrengthComparator implements Comparator<WiFiDetail> {
        @Override
        public int compare(WiFiDetail lhs, WiFiDetail rhs) {
            return new CompareToBuilder()
                    .append(rhs.getWiFiSignal().getLevel(), lhs.getWiFiSignal().getLevel())
                    .append(lhs.getSSID().toUpperCase(), rhs.getSSID().toUpperCase())
                    .append(lhs.getBSSID().toUpperCase(), rhs.getBSSID().toUpperCase())
                    .toComparison();
        }
    }


    static class SSIDComparator implements Comparator<WiFiDetail> {
        @Override
        public int compare(WiFiDetail lhs, WiFiDetail rhs) {
            return new CompareToBuilder()
                    .append(lhs.getSSID().toUpperCase(), rhs.getSSID().toUpperCase())
                    .append(rhs.getWiFiSignal().getLevel(), lhs.getWiFiSignal().getLevel())
                    .append(lhs.getBSSID().toUpperCase(), rhs.getBSSID().toUpperCase())
                    .toComparison();
        }
    }

    static class ChannelComparator implements Comparator<WiFiDetail> {
        @Override
        public int compare(WiFiDetail lhs, WiFiDetail rhs) {
            return new CompareToBuilder()
                    .append(lhs.getWiFiSignal().getWiFiChannel().getChannel(), rhs.getWiFiSignal().getWiFiChannel().getChannel())
                    .append(rhs.getWiFiSignal().getLevel(), lhs.getWiFiSignal().getLevel())
                    .append(lhs.getSSID().toUpperCase(), rhs.getSSID().toUpperCase())
                    .append(lhs.getBSSID().toUpperCase(), rhs.getBSSID().toUpperCase())
                    .toComparison();
        }
    }


}
