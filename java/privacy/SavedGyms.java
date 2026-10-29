package privacy;

import org.json.JSONObject;
import java.util.LinkedHashMap;

public final class SavedGyms {
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
