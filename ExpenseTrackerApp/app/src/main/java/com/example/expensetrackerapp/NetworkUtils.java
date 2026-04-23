package com.example.expensetrackerapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public final class NetworkUtils {

    private NetworkUtils() {}

    /**
     * Returns true if the device has an active network connection with
     * internet capability (Wi-Fi, cellular, or Ethernet).
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) return false;

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * Returns a human-readable description of the current connection type,
     * or "No connection" if offline.
     */
    public static String getConnectionType(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "No connection";

        Network network = cm.getActiveNetwork();
        if (network == null) return "No connection";

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) return "No connection";

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))     return "Wi-Fi";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobile data";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
        return "Connected";
    }
}
