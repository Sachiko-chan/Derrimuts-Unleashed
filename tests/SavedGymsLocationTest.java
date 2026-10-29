import android.location.Location;
import org.json.JSONObject;
import privacy.SavedGyms;

public class SavedGymsLocationTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        String url = "https://hff.deeep.link/hff-c6bJ4";

        JSONObject seed = SavedGyms.resolveSeed(url);
        check(seed != null);
        check(seed.has("url"));
        check(seed.has("name"));
        check(seed.has("lat"));
        check(seed.has("lon"));
        check(seed.getString("url").equals(url));
        check(Math.abs(seed.getDouble("lat") + 34.88000d) < 1e-6);
        check(Math.abs(seed.getDouble("lon") - 138.53500d) < 1e-6);

        JSONObject selected = new JSONObject().put("url", url);
        Location location = SavedGyms.resolveLocation(selected);
        check(location != null);
        check(Math.abs(location.getLatitude() + 34.88000d) < 1e-6);
        check(Math.abs(location.getLongitude() - 138.53500d) < 1e-6);

        JSONObject missing = new JSONObject().put("url", "https://hff.deeep.link/hff-UNKNOWN");
        check(SavedGyms.resolveLocation(missing) == null);

        JSONObject payload = new JSONObject();
        payload.put("lat", location.getLatitude());
        payload.put("lon", location.getLongitude());
        check(payload.has("lat"));
        check(payload.has("lon"));
        check(Math.abs(payload.getDouble("lat") + 34.88000d) < 1e-6);
        check(Math.abs(payload.getDouble("lon") - 138.53500d) < 1e-6);

        System.out.println("Saved gym seed coordinates match Android-style lat/lon payloads for selected gyms only.");
    }
}
