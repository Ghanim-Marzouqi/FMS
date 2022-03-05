package om.omantowerco.fms.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class HttpHelper {

    // HTTP endpoints
    public static final String SEND_EMAIL_HTTP = "https://us-central1-full-maintenance-system.cloudfunctions.net/app/send-email";

    // This method for testing the availability of the network
    public static boolean isOnLine(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager != null ? manager.getActiveNetworkInfo() : null;
        return networkInfo != null && networkInfo.isConnected();
    }
}
