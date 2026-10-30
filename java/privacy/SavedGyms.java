package privacy;

import android.app.Activity;
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Saved-gym UI and explicitly selected venue coordinates. Server checks remain upstream. */
public final class SavedGyms {
    private static final GymCodes.Pending pending = new GymCodes.Pending();
    private static final Map<Object, String> requests = new WeakHashMap<Object, String>();
    private static final String PREFS = "unleashed_saved_gyms_v1";
    private static final String TAG = "unleashed.saved.gyms";
    private static SharedPreferences prefs(Activity a) { return a.getSharedPreferences(PREFS, 0); }
    private static void toast(Activity a, String text) { Toast.makeText(a, text, Toast.LENGTH_LONG).show(); }
    private static boolean alive(Activity a) { return !a.isFinishing(); }
    private static LinkedHashMap<String, String> load(Activity a) {
        LinkedHashMap<String, String> entries = new LinkedHashMap<String, String>();
        String saved = prefs(a).getString("entries", null);
        if (saved == null) { entries.put(GymCodes.WINDSOR, "Windsor Gardens"); return entries; }
        try {
            JSONArray array = new JSONArray(saved);
            for (int i = 0; i < array.length() && i < 500; i++) {
                JSONObject row = array.getJSONObject(i);
                String url = GymCodes.canonical(row.optString("url"));
                String name = GymCodes.name(row.optString("name"));
                if (url != null && !name.isEmpty() && name.length() <= 80) entries.put(url, name);
            }
        } catch (Exception ex) { throw new IllegalStateException("Saved gym list could not be read", ex); }
        return entries;
    }
    private static void save(Activity a, LinkedHashMap<String, String> entries) {
        try {
            JSONArray array = new JSONArray();
            for (Map.Entry<String, String> e : entries.entrySet()) {
                JSONObject row = new JSONObject(); row.put("url", e.getKey()); row.put("name", e.getValue()); array.put(row);
            }
            prefs(a).edit().putString("entries", array.toString()).apply();
        } catch (Exception ex) { throw new IllegalStateException("Could not save gym list", ex); }
    }
    public static void scanned(String url) {
        synchronized (SavedGyms.class) { locations.clear(); }
        pending.scanned(url, SystemClock.elapsedRealtime());
    }
    // Bound to the originating activity, route URL, and one location challenge.
    public static final class Selection {
        private final String url;
        private final JSONObject gym;
        private final long started;
        private boolean routed;
        private boolean consumed;
        public Selection(String url, JSONObject gym, long now) {
            this.url = GymCodes.canonical(url); this.gym = resolveLocation(gym); this.started = now;
        }
        public boolean route(String next, long now) {
            if (routed || consumed || !fresh(now) || url == null || !url.equals(GymCodes.canonical(next))) return false;
            routed = true; return true;
        }
        private boolean fresh(long now) { return now >= started && now - started <= 120000L; }
        public JSONObject take(long now) {
            if (!routed || consumed || !fresh(now)) return null;
            consumed = true; return resolveLocation(gym);
        }
    }
    private static final Map<Object, Selection> locations = new WeakHashMap<Object, Selection>();
    public static synchronized void route(Activity a, String url) {
        Selection selection = locations.get(a);
        if (selection != null && !selection.route(url, SystemClock.elapsedRealtime())) locations.remove(a);
    }
    public static synchronized JSONObject takeLocation(Context a) {
        Selection selection = locations.remove(a);
        return selection == null ? null : selection.take(SystemClock.elapsedRealtime());
    }
    private static synchronized void select(Activity a, String url, String name) {
        // Resolve seeded entries by code so display-name edits preserve their identity.
        JSONObject gym = resolveSeed(GymCodes.WINDSOR.equals(url) ? "Windsor Gardens" : name);
        locations.put(a, new Selection(url, gym, SystemClock.elapsedRealtime()));
    }
    public static synchronized void bind(Object request, JSONObject body) {
        try {
            String url = GymCodes.canonical(body.optString("url"));
            if (url != null && pending.consume(url, SystemClock.elapsedRealtime())) requests.put(request, url);
        } catch (Exception ignored) { }
    }
    public static void success(final Activity a, Object request) {
        // Called only by the upstream Success=true UI callback; never by HTTP completion alone.
        try {
            final String url;
            synchronized (SavedGyms.class) { url = requests.remove(request); }
            if (url == null || load(a).containsKey(url) || !alive(a)) return;
            new AlertDialog.Builder(a).setTitle("Save this gym?")
                .setMessage("Check-in succeeded. Save this code so you can select the gym next time?")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Save gym", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { edit(a, url, "", false); }
                }).show();
        } catch (Exception ignored) { /* Saving must never interrupt the successful check-in. */ }
    }
    public static void install(final Activity a) {
        try {
            // Both native check-in layouts share this vertical container and toolbar.
            LinearLayout content = (LinearLayout) a.findViewById(0x7f0a05f1);
            if (content == null || content.findViewWithTag(TAG) != null) return;
            Button saved = new Button(a); saved.setText("Saved gyms"); saved.setTag(TAG);
            saved.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { show(a); } });
            // Place directly below the toolbar; do not reparent the scanner or append off-screen.
            Button diagnostics = new Button(a); diagnostics.setText("Check-in diagnostics");
            diagnostics.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { CheckInTrace.show(a, false); } });
            content.addView(diagnostics, Math.min(1, content.getChildCount()), new LinearLayout.LayoutParams(-1, -2));
            content.addView(saved, Math.min(1, content.getChildCount()), new LinearLayout.LayoutParams(-1, -2));
        } catch (Exception ignored) { /* Original scanner remains usable if this optional UI fails. */ }
    }
    private static void pause(Activity a) throws Exception {
        if (!isScanner(a)) return;
        Object view = a.getClass().getField("V").get(a);
        if (view != null) view.getClass().getMethod("a").invoke(view);
    }
    private static void resume(Activity a) {
        try { if (alive(a) && isScanner(a)) a.getClass().getMethod("X").invoke(a); } catch (Exception ignored) { }
    }
    private static boolean isScanner(Activity a) { return a.getClass().getName().equals("com.innovatise.qrcodescanner.QRCodeScannActivity"); }
    private static void show(final Activity a) {
        try {
            if (isScanner(a) && a.getClass().getField("Z").getBoolean(a)) { toast(a, "A check-in is already in progress."); return; }
            final LinkedHashMap<String, String> entries = load(a);
            if (entries.isEmpty()) { toast(a, "Scan a gym code and check in successfully to add it here."); return; }
            final ArrayList<String> urls = new ArrayList<String>(entries.keySet());
            final Spinner spinner = new Spinner(a);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(a, android.R.layout.simple_spinner_item, new ArrayList<String>(entries.values()));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
            LinearLayout panel = new LinearLayout(a); panel.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (16 * a.getResources().getDisplayMetrics().density); panel.setPadding(pad, pad, pad, pad);
            TextView note = new TextView(a); note.setText("Choose a gym, then tap Check in.");
            panel.addView(note); panel.addView(spinner, new LinearLayout.LayoutParams(-1, -2));
            final boolean[] submitted = {false};
            pause(a);
            AlertDialog dialog = new AlertDialog.Builder(a).setTitle("Saved gyms").setView(panel)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Manage", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) { submitted[0] = true; manage(a, urls.get(spinner.getSelectedItemPosition())); }
                })
                .setPositiveButton("Check in", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        try {
                            if (isScanner(a) && a.getClass().getField("a0").getBoolean(a)) { toast(a, "Check-in is still loading. Please try again shortly."); return; }
                            String selectedUrl = urls.get(spinner.getSelectedItemPosition());
                            select(a, selectedUrl, entries.get(selectedUrl));
                            // Mirror the existing scanner's duplicate guard, then use its original route.
                            if (isScanner(a)) a.getClass().getField("Z").setBoolean(a, true);
                            pause(a);
                            pending.scanned(null, SystemClock.elapsedRealtime());
                            // A saved QR code must carry the same scanType as a physical QR scan.
                            a.getSharedPreferences("com.myfitapp.config", 0).edit().putString("SHARED_PREFERENCE_SCAN_TYPE", "QR").apply();
                            // NFCReader already routes tag URLs through the same J(String) resolver.
                            a.getClass().getMethod(isScanner(a) ? "T" : "J", String.class).invoke(a, urls.get(spinner.getSelectedItemPosition()));
                            submitted[0] = true;
                        } catch (Exception ex) {
                            synchronized (SavedGyms.class) { locations.remove(a); }
                            try { if (isScanner(a)) a.getClass().getField("Z").setBoolean(a, false); } catch (Exception ignored) { }
                            toast(a, "Could not start check-in. Try scanning the gym code.");
                        }
                    }
                }).create();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface d) { if (!submitted[0]) resume(a); }
            });
            dialog.show();
        } catch (Exception ex) { toast(a, "Could not open saved gyms. You can still scan a code."); resume(a); }
    }
    private static void manage(final Activity a, final String url) {
        try {
            final String name = load(a).get(url);
            final boolean[] nested = {false};
            AlertDialog options = new AlertDialog.Builder(a).setTitle(name).setItems(new String[]{"Rename", "Delete"}, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    nested[0] = true;
                    if (which == 0) edit(a, url, name, true);
                    else {
                        AlertDialog confirm = new AlertDialog.Builder(a).setTitle("Delete saved gym?").setMessage(name)
                        .setNegativeButton("Cancel", null).setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                try { LinkedHashMap<String,String> rows=load(a); rows.remove(url); save(a,rows); }
                                catch(Exception ex) { toast(a,"Could not update the gym list."); }
                            }
                        }).create();
                        confirm.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            public void onDismiss(DialogInterface d) { resume(a); }
                        });
                        confirm.show();
                    }
                }
            }).create();
            options.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface d) { if (!nested[0]) resume(a); }
            });
            options.show();
        } catch (Exception ex) { toast(a, "Could not open saved gyms."); resume(a); }
    }
    private static void edit(final Activity a, final String url, String current, final boolean rename) {
        final EditText name = new EditText(a); name.setSingleLine(true); name.setHint("Gym name"); name.setText(current);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        final AlertDialog dialog = new AlertDialog.Builder(a).setTitle(rename ? "Rename gym" : "Save gym")
            .setView(name).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) { resume(a); }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            public void onShow(DialogInterface unused) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String label = GymCodes.name(name.getText().toString());
                        if (label.isEmpty()) { name.setError("Enter a gym name"); return; }
                        try {
                            LinkedHashMap<String,String> rows = load(a);
                            if (!rename && rows.containsKey(url)) { dialog.dismiss(); return; }
                            if (!rows.containsKey(url) && rows.size() >= 500) { name.setError("The gym list is full"); return; }
                            rows.put(url,label); save(a,rows); dialog.dismiss(); toast(a,"Gym saved");
                        } catch(Exception ex) { toast(a,"Could not update the gym list."); }
                    }
                });
            }
        });
        dialog.show();
    }

    // Venue catalogue used by Saved Gyms check-in.
    public static final class GymLocation {
        public final String name;
        public final double latitude;
        public final double longitude;

        public GymLocation(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public JSONObject asJson() {
            JSONObject row = new JSONObject();
            row.put("name", name);
            row.put("lat", latitude);
            row.put("lon", longitude);
            return row;
        }
    }

    private static final LinkedHashMap<String, GymLocation> BY_NAME = new LinkedHashMap<String, GymLocation>();

    static {
        registerSeed("Albion",              -37.79025, 144.81795);
        registerSeed("Ballarat",            -37.56216, 143.85033);
        registerSeed("Bendigo",             -36.75848, 144.27977);
        registerSeed("Berwick",             -38.03336, 145.34806);
        registerSeed("Bundoora",            -37.69855, 145.06110);
        registerSeed("Caroline Springs",    -37.73918, 144.73627);
        registerSeed("Cranbourne",          -38.09993, 145.28338);
        registerSeed("Dandenong",           -37.98743, 145.21529);
        registerSeed("Epping",              -37.65308, 145.01369);
        registerSeed("Frankston",           -38.14551, 145.12327);
        registerSeed("Geelong",             -38.14990, 144.36161);
        registerSeed("Hoppers Crossing",    -37.88249, 144.69941);
        registerSeed("Melton",              -37.68355, 144.58344);
        registerSeed("Narre Warren",        -38.03333, 145.30254);
        registerSeed("Pakenham",            -38.08008, 145.48700);
        registerSeed("Preston",             -37.74173, 144.99985);
        registerSeed("Sunshine",            -37.78623, 144.83264);
        registerSeed("Tarneit",             -37.83655, 144.68030);
        registerSeed("Werribee",            -37.89990, 144.66026);
        registerSeed("Elizabeth",           -34.71750, 138.66700);
        registerSeed("Gepps Cross",         -34.84600, 138.60800);
        registerSeed("Norwood",             -34.91900, 138.63000);
        registerSeed("Port Adelaide",       -34.84700, 138.50300);
        registerSeed("Rundle Mall",         -34.92270, 138.60100);
        registerSeed("Smithfield",          -34.68100, 138.68800);
        registerSeed("Windsor Gardens",     -34.88000, 138.53500);
        registerSeed("Welland",             -34.90800, 138.55600);
        registerSeed("Brisbane",            -27.47000, 153.02600);
        registerSeed("Gold Coast",          -28.01670, 153.40000);
        registerSeed("Logan",               -27.64000, 153.11000);
    }

    private static void registerSeed(String name, double latitude, double longitude) {
        String key = GymCodes.name(name);
        if (key == null || key.isEmpty()) return;
        BY_NAME.put(key, new GymLocation(key, latitude, longitude));
    }

    public static JSONObject resolveSeed(String name) {
        String key = GymCodes.name(name);
        if (key == null || key.isEmpty() || !BY_NAME.containsKey(key)) return null;
        return BY_NAME.get(key).asJson();
    }

    public static JSONObject resolveLocation(JSONObject gym) {
        if (gym == null) return null;
        String name = GymCodes.name(gym.optString("name"));
        if (name != null && !name.isEmpty() && BY_NAME.containsKey(name)) {
            return BY_NAME.get(name).asJson();
        }
        return null;
    }
}
