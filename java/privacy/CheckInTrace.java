package privacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** Local, bounded diagnostic events. Never receives URLs, headers, bodies or tokens. */
public final class CheckInTrace {
    private static WeakReference<Activity> owner = new WeakReference<Activity>(null);
    private static final ArrayList<String> events = new ArrayList<String>();
    private static long started;
    private static int generation;
    private static boolean running;
    private static final String PREFS = "unleashed_checkin_diagnostic";
    private static boolean supported(Activity a) {
        String n = a.getClass().getName();
        return n.equals("com.innovatise.home.NFCReader") || n.equals("com.innovatise.qrcodescanner.QRCodeScannActivity");
    }
    public static synchronized void begin(final Activity a) {
        try {
            if (!supported(a)) return;
            owner = new WeakReference<Activity>(a); events.clear(); started = SystemClock.elapsedRealtime(); running = true;
            final int attempt = ++generation;
            record("Check-in route entered");
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                public void run() {
                    synchronized (CheckInTrace.class) {
                        if (attempt != generation || !running || a.isFinishing()) return;
                        record("Still pending after 45 seconds; no result inferred");
                    }
                    show(a, true);
                }
            }, 45000L);
        } catch (Exception ignored) { }
    }
    private static synchronized void record(String event) {
        if (!running) return;
        if (events.size() == 32) events.remove(0);
        events.add(((SystemClock.elapsedRealtime() - started) / 1000L) + "s: " + event);
        Activity a = owner.get();
        if (a != null) a.getSharedPreferences(PREFS, 0).edit().putString("last", report()).apply();
    }
    private static synchronized String report() {
        StringBuilder b = new StringBuilder("Derriumuts Unleashed r8 diagnostics\n");
        for (String e : events) b.append(e).append('\n');
        return b.toString();
    }
    private static String requestName(Object request) {
        if (request == null) return null;
        String n = request.getClass().getName();
        if (n.equals("com.innovatise.utils.b")) return "Proxy authentication";
        if (n.equals("cg.h")) return "Proxy metadata request";
        if (n.equals("li.q")) return "Module metadata request";
        return null;
    }
    public static void request(Object request) {
        try { String n = requestName(request); if (n != null) record(n + " dispatched"); } catch (Exception ignored) { }
    }
    public static void refresh(Object request) {
        try { String n = requestName(request); if (n != null) record(n + " needs authentication"); } catch (Exception ignored) { }
    }
    public static void response(Object request, int status) {
        try { String n = requestName(request); if (n != null) record(n + " HTTP " + status); } catch (Exception ignored) { }
    }
    public static void error(Object request, Throwable error, Object response) {
        try {
            String n = requestName(request); if (n == null) return;
            String kind = error == null ? "HTTP failure" : error instanceof java.net.SocketTimeoutException ? "network timeout" : error instanceof javax.net.ssl.SSLException ? "TLS failure" : error instanceof java.net.UnknownHostException ? "DNS failure" : "network failure";
            record(n + ": " + kind);
        } catch (Exception ignored) { }
    }
    public static void eligibility(Context a) { try { if (a == owner.get()) record("Membership eligibility flow entered"); } catch (Exception ignored) { } }
    public static void eligibilityResponse() { try { record("Membership eligibility response received"); } catch (Exception ignored) { } }
    public static void eligibilityError() { try { record("Membership eligibility error callback received"); } catch (Exception ignored) { } }
    public static void resolved(Activity a) { try { if (a == owner.get()) record("Deep-link response decoded"); } catch (Exception ignored) { } }
    public static void routed(Activity a) { try { if (a == owner.get()) record("Routing to destination module"); } catch (Exception ignored) { } }
    public static synchronized void hidden(Activity a) {
        try { if (a == owner.get()) { record("Original flow dismissed its loading indicator"); running = false; } } catch (Exception ignored) { }
    }
    public static void show(final Activity a, boolean timedOut) {
        try {
            if (a.isFinishing()) return;
            final String report = a.getSharedPreferences(PREFS, 0).getString("last", "No check-in attempt recorded in r8 yet.");
            new AlertDialog.Builder(a).setTitle(timedOut ? "Check-in is still pending" : "Check-in diagnostics")
                .setMessage((timedOut ? "The app has not finished this attempt. This does not confirm entry or cancel the request.\n\n" : "") + report)
                .setPositiveButton("Copy details", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        ClipboardManager clipboard = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("Check-in diagnostics", report));
                    }
                }).setNegativeButton(timedOut ? "Keep waiting" : "Close", null).show();
        } catch (Exception ignored) { }
    }
}
