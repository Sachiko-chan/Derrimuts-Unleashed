package privacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** UI-only extension. Check-in still calls the existing scanner's T(String). */
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
    public static void scanned(String url) { pending.scanned(url, SystemClock.elapsedRealtime()); }
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
            TextView note = new TextView(a); note.setText("Choose your gym, then check in. Your membership is checked as usual.");
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
                            // Mirror the existing scanner's duplicate guard, then use its original route.
                            if (isScanner(a)) a.getClass().getField("Z").setBoolean(a, true);
                            pause(a);
                            pending.scanned(null, SystemClock.elapsedRealtime());
                            // A saved QR code must carry the same scanType as a physical QR scan.
                            a.getSharedPreferences("com.myfitapp.config", 0).edit().putString("SHARED_PREFERENCE_SCAN_TYPE", "QR").apply();
                            // NFCReader already routes tag URLs through the same J(String) resolver.
                            a.getClass().getMethod(isScanner(a) ? "T" : "J", String.class).invoke(a, urls.get(spinner.getSelectedItemPosition()));
                            submitted[0] = true;
                        } catch (Exception ex) { toast(a, "Could not start check-in. Try scanning the gym code."); }
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
}
