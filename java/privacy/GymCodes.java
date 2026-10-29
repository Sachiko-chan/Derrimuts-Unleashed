package privacy;

import java.net.URI;
import java.util.LinkedHashMap;

public final class GymCodes {
    public static final String[][] ALL = {
        // Victoria
        {"Albion",                  "https://hff.deeep.link/hff-Vae5u"},
        {"Ballarat",                "https://hff.deeep.link/hff-eGha0"},
        {"Bendigo",                 "https://hff.deeep.link/hff-IH0ru"},
        {"Berwick",                 "https://hff.deeep.link/hff-ooY2U"},
        {"Bundoora",                "https://hff.deeep.link/hff-toh0A"},
        {"Caroline Springs",        "https://hff.deeep.link/hff-zoM2e"},
        {"Cranbourne",              "https://hff.deeep.link/hff-Rae2E"},
        {"Dandenong",               "https://hff.deeep.link/hff-sah0E"},
        {"Epping",                  "https://hff.deeep.link/hff-mox2E"},
        {"Frankston",               "https://hff.deeep.link/hff-uj9Lo"},
        {"Geelong",                 "https://hff.deeep.link/hff-eth2E"},
        {"Hoppers Crossing",        "https://hff.deeep.link/hff-ooB7m"},
        {"Melton",                  "https://hff.deeep.link/hff-iZ8do"},
        {"Narre Warren",            "https://hff.deeep.link/hff-Iem3x"},
        {"Pakenham",                "https://hff.deeep.link/hff-Tah4B"},
        {"Preston",                 "https://hff.deeep.link/hff-Ceew9"},
        {"Sunshine",                "https://hff.deeep.link/hff-feeP7"},
        {"Tarneit",                 "https://hff.deeep.link/hff-Phoi0"},
        {"Werribee",                "https://hff.deeep.link/hff-Lie6t"},
        // South Australia
        {"Elizabeth",               "https://hff.deeep.link/hff-Igoo2"},
        {"Gepps Cross",             "https://hff.deeep.link/hff-wohF0"},
        {"Norwood",                 "https://hff.deeep.link/hff-woVa7"},
        {"Port Adelaide",           "https://hff.deeep.link/hff-chu0U"},
        {"Rundle Mall",             "https://hff.deeep.link/hff-gei7J"},
        {"Smithfield",              "https://hff.deeep.link/hff-ahr8O"},
        {"Windsor Gardens",         "https://hff.deeep.link/hff-c6bJ4"},
        {"Welland",                 "https://hff.deeep.link/hff-Caeb7"},
        // Queensland
        {"Brisbane",                "https://hff.deeep.link/hff-Aek7h"},
        {"Gold Coast",              "https://hff.deeep.link/hff-lah7N"},
        {"Logan",                   "https://hff.deeep.link/hff-Fiiz6"}
    };

    private static final LinkedHashMap<String, String> BY_NAME = new LinkedHashMap<String, String>();

    static {
        for (String[] row : ALL) {
            String name = name(row[0]);
            String url = canonical(row[1]);
            if (name != null && !name.isEmpty() && url != null) {
                BY_NAME.put(name, url);
            }
        }
    }

    public static String urlForName(String name) {
        return BY_NAME.get(name(name));
    }

    public static String canonical(String text) {
        if (text == null || text.length() > 512) return null;
        try {
            URI uri = new URI(text.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                !"hff.deeep.link".equalsIgnoreCase(uri.getHost()) ||
                uri.getUserInfo() != null || uri.getPort() != -1 ||
                uri.getRawQuery() != null || uri.getRawFragment() != null) return null;

            String path = uri.getRawPath();
            if (!path.matches("/hff-[A-Za-z0-9]+/?")) return null;
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            return "https://hff.deeep.link" + path;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String name(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{Cntrl}]", " ").trim();
    }
}
