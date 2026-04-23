package com.example.expensetrackerapp;

public final class AppConstants {

    private AppConstants() {}

    /** SharedPreferences file name */
    public static final String PREF_NAME = "ExpenseTrackerPrefs";

    /** Key for the cloud server base URL */
    public static final String PREF_SERVER_URL = "server_url";

    /**
     * Default endpoint — change this to your own REST API server.
     * The app will POST JSON to  BASE_URL + "/upload".
     */
    public static final String DEFAULT_SERVER_URL = "http://your-server.example.com/api";

    /** POST path appended to the base URL */
    public static final String UPLOAD_PATH = "/upload";

    /** Connection / read timeouts in milliseconds */
    public static final int CONNECT_TIMEOUT_MS = 10_000;
    public static final int READ_TIMEOUT_MS    = 20_000;
}
