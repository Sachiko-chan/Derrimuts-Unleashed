package privacy;

import java.net.URI;
import java.util.LinkedHashMap;

/** Only public, static-format HFF gym links are eligible for saving. */
public final class GymCodes {
    public static final String WINDSOR = "https://hff.deeep.link/hff-c6bJ4";

    /** Derrimut 24:7 gyms: {display name, latitude, longitude, placeholder HFF url}. */
    public static final String[][] ALL = {
        // Victoria
        {"Windsor Gardens",            "-34.88000", "138.53500", "https://hff.deeep.link/hff-c6bJ4"},
        {"Albion",                     "-37.79025", "144.81795", "https://hff.deeep.link/hff-ALBION"},
        {"Ballarat",                   "-37.56216", "143.85033", "https://hff.deeep.link/hff-BALLRT"},
        {"Bendigo",                    "-36.75848", "144.27977", "https://hff.deeep.link/hff-BENDGO"},
        {"Berwick",                    "-38.03336", "145.34806", "https://hff.deeep.link/hff-BERWCK"},
        {"Bundoora",                   "-37.69855", "145.06110", "https://hff.deeep.link/hff-BUNDRA"},
        {"Caroline Springs",           "-37.73918", "144.73627", "https://hff.deeep.link/hff-CAROLN"},
        {"Cranbourne",                 "-38.09993", "145.28338", "https://hff.deeep.link/hff-CRANBN"},
        {"Dandenong",                  "-37.98743", "145.21529", "https://hff.deeep.link/hff-DANDNG"},
        {"Epping",                     "-37.65308", "145.01369", "https://hff.deeep.link/hff-EPPING"},
        {"Frankston",                  "-38.14551", "145.12327", "https://hff.deeep.link/hff-FRNKST"},
        {"Geelong",                    "-38.14990", "144.36161", "https://hff.deeep.link/hff-GEELNG"},
        {"Hoppers Crossing",           "-37.88249", "144.69941", "https://hff.deeep.link/hff-HOPPRS"},
        {"Melton",                     "-37.68355", "144.58344", "https://hff.deeep.link/hff-MELTON"},
        {"Narre Warren",               "-38.03333", "145.30254", "https://hff.deeep.link/hff-NARREW"},
        {"Pakenham",                   "-38.08008", "145.48700", "https://hff.deeep.link/hff-PAKNHM"},
        {"Preston",                    "-37.74173", "144.99985", "https://hff.deeep.link/hff-PRESTN"},
        {"Sunshine",                   "-37.78623", "144.83264", "https://hff.deeep.link/hff-SUNSHN"},
        {"Tarneit",                    "-37.83655", "144.68030", "https://hff.deeep.link/hff-TARNET"},
        {"Werribee",                   "-37.89990", "144.66026", "https://hff.deeep.link/hff-WERRBE"},
        // South Australia
        {"Elizabeth",                  "-34.71750", "138.66700", "https://hff.deeep.link/hff-ELIZAB"},
        {"Gepps Cross",                "-34.84600", "138.60800", "https://hff.deeep.link/hff-GEPPSC"},
        {"Norwood",                    "-34.91900", "138.63000", "https://hff.deeep.link/hff-NORWOD"},
        {"Port Adelaide",              "-34.84700", "138.50300", "https://hff.deeep.link/hff-PTADLD"},
        {"Rundle Mall",                "-34.92270", "138.60100", "https://hff.deeep.link/hff-RUNDLE"},
        {"Smithfield",                 "-34.68100", "138.68800", "https://hff.deeep.link/hff-SMTHFD"},
        {"Welland",                    "-34.90800", "138.55600", "https://hff.deeep.link/hff-WELLND"},
        // Queensland
        {"Brisbane",                   "-27.47000", "153.02600", "https://hff.deeep.link/hff-BRISBN"},
        {"Gold Coast",                 "-28.01670", "153.40000", "https://hff.deeep.link/hff-GLDCTS"},
        {"Logan",                      "-27.64000", "153.11000", "https://hff.deeep.link/hff-LOGANQ"}
    };

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
        } catch (Exception ignored) { return null; }
    }
    public static String name(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{Cntrl}]", " ").trim();
    }
    /** Correlate a successful request with a recent physical scan, once only. */
    public static final class Pending {
        private final LinkedHashMap<String, Long> scans = new LinkedHashMap<String, Long>();
        public synchronized void scanned(String raw, long now) {
            String key = canonical(raw);
            scans.clear(); // Only the latest submitted scan may produce a save prompt.
            if (key != null) scans.put(key, now);
        }
        public synchronized boolean consume(String raw, long now) {
            String key = canonical(raw);
            Long start = scans.remove(key);
            return start != null && now >= start && now - start <= 300000;
        }
    }
}
