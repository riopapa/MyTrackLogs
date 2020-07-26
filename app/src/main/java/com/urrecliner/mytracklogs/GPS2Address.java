package com.urrecliner.mytracklogs;

import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;

class GPS2Address {

    final static String noInfo = "No_Info";
    String[] getPlace(Geocoder geocoder, double latitude, double longitude) {

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                String Feature = address.getFeatureName();
                String Thorough = address.getThoroughfare();
                String Locality = address.getLocality();
                String SubLocality = address.getSubLocality();
                String Country = address.getCountryName();
                String CountryCode = address.getCountryCode();
                String SState = address.getSubAdminArea();
                String State = address.getAdminArea();
                Feature = (Feature == null) ? noInfo : Feature;
                Thorough = (Thorough == null) ? noInfo : Thorough;  // Kakakaua Avernue
                SubLocality = (SubLocality == null) ? noInfo : SubLocality; // 분당구
                Locality = (Locality == null) ? noInfo : Locality;  // Honolulu, 성남시
                SState = (SState == null) ? noInfo : SState;
                State = (State == null) ? noInfo : State;   // Hawaii, 경기도
                if (Country == null || CountryCode.equals("KR"))
                    Country = noInfo;

                return MergedAddress(Feature, Thorough, SubLocality, Locality, State, SState, Country, CountryCode);
            } else {
                return new String[]{" "," "," "," "," "};
            }
        } catch (IOException e) {
            return new String[]{"No","Address","Found","",""};
        }
    }

    static String[] MergedAddress(String Feature, String Thorough, String SubLocality, String Locality, String SState, String State, String Country, String CountryCode) {

        if (Thorough.equals(Feature)) Feature = noInfo;
        if (SubLocality.equals(Feature)) Feature = noInfo;
        if (SubLocality.equals(Thorough)) Thorough = noInfo;
        if (Locality.equals(Thorough)) Thorough = noInfo;
        if (Locality.equals(SubLocality)) SubLocality = noInfo;
        if (SState.equals(Locality)) Locality = noInfo;
        if (State.equals(SState)) SState = noInfo;

        String addressMerged = "";
        if (CountryCode.equals("KR")) {
            if (!State.equals(noInfo)) addressMerged += ";" + State;
            if (!SState.equals(noInfo)) addressMerged += ";" + SState;
            if (!Locality.equals(noInfo)) addressMerged += ";" + Locality;
            if (!SubLocality.equals(noInfo)) addressMerged += ";" + SubLocality;
            if (!Thorough.equals(noInfo)) addressMerged += ";" + Thorough;
            if (!Feature.equals(noInfo)) addressMerged += ";" + Feature;
        }
        else {
            if (!Feature.equals(noInfo)) addressMerged += ";" + Feature;
            if (!Thorough.equals(noInfo)) addressMerged += ";" + Thorough;
            if (!SubLocality.equals(noInfo)) addressMerged += ";" + SubLocality;
            if (!Locality.equals(noInfo)) addressMerged += ";" + Locality;
            if (!SState.equals(noInfo)) addressMerged += ";" + SState;
            if (!State.equals(noInfo)) addressMerged += ";" + State;
            if (!Country.equals(noInfo)) addressMerged += ";" + Country;
        }
        addressMerged += "; ; ; ";
        return addressMerged.split(";");
    }

}
