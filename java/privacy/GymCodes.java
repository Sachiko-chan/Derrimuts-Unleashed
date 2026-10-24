package privacy;

import java.net.URI;
import java.util.LinkedHashMap;

/** Only public, static-format HFF gym links are eligible for saving. */
public final class GymCodes {
    public static final String WINDSOR = "https://hff.deeep.link/hff-c6bJ4";
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
