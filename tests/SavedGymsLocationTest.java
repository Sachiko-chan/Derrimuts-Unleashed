import org.json.JSONObject;
import privacy.SavedGyms;

/** Checks the existing metadata API, not geographic accuracy or server eligibility. */
public class SavedGymsLocationTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        JSONObject seed = SavedGyms.resolveSeed("Albion");
        check(seed != null && seed.getString("name").equals("Albion"));
        check(seed.has("lat") && seed.has("lon"));
        JSONObject selected = SavedGyms.resolveLocation(new JSONObject().put("name", "Albion"));
        check(selected != null && selected.getString("name").equals("Albion"));
        check(SavedGyms.resolveLocation(null) == null);
        check(SavedGyms.resolveLocation(new JSONObject().put("name", "Unknown gym")) == null);
        check(SavedGyms.resolveSeed("https://hff.deeep.link/hff-Test1") == null);
        seed.put("name", "Changed copy");
        check(SavedGyms.resolveSeed("Albion").getString("name").equals("Albion"));
        SavedGyms.Selection flow = new SavedGyms.Selection(privacy.GymCodes.urlForName("Albion"), selected, 100);
        check(flow.take(100) == null);
        check(flow.route(privacy.GymCodes.urlForName("Albion"), 101));
        JSONObject result = flow.take(102);
        check(result.getDouble("lat") == selected.getDouble("lat"));
        check(result.getDouble("lon") == selected.getDouble("lon"));
        check(flow.take(103) == null);
        SavedGyms.Selection mismatch = new SavedGyms.Selection(privacy.GymCodes.urlForName("Albion"), selected, 100);
        check(!mismatch.route("https://hff.deeep.link/other", 101));
        check(mismatch.take(102) == null);
        SavedGyms.Selection expired = new SavedGyms.Selection(privacy.GymCodes.urlForName("Albion"), selected, 100);
        check(expired.route(privacy.GymCodes.urlForName("Albion"), 101));
        check(expired.take(120101) == null);
        check(!expired.route(privacy.GymCodes.urlForName("Albion"), 102));
        SavedGyms.Selection absent = new SavedGyms.Selection(privacy.GymCodes.urlForName("Albion"), null, 100);
        check(absent.route(privacy.GymCodes.urlForName("Albion"), 101));
        check(absent.take(102) == null);
        System.out.println("Catalogue lookup and saved-location selection passed.");
    }
}
