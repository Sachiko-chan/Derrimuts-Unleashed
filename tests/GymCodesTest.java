import privacy.GymCodes;
public class GymCodesTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        String u="https://hff.deeep.link/hff-Test1";
        check(u.equals(GymCodes.canonical(" HTTPS://HFF.DEEEP.LINK/hff-Test1/ ")));
        check(!u.equals(GymCodes.canonical("https://hff.deeep.link/hff-test1")));
        for(String bad:new String[]{null,"", "http://hff.deeep.link/hff-a", "https://hff.deeep.link.evil/hff-a", "https://x@hff.deeep.link/hff-a", u+"?token=x",u+"#x","https://hff.deeep.link:443/hff-a","https://hff.deeep.link/hff-%61","https://hff.deeep.link/../hff-a"}) check(GymCodes.canonical(bad)==null);
        check(GymCodes.name("  Example\nGym  ").equals("Example Gym"));
        GymCodes.Pending p=new GymCodes.Pending();
        check(!p.consume(u,0));
        p.scanned(u,100); check(!p.consume("https://hff.deeep.link/hff-other",101)); check(p.consume(u,102)); check(!p.consume(u,103));
        p.scanned(u,100); check(!p.consume(u,300101));
        p.scanned(u,100); check(!p.consume(u,99));
        p.scanned(u,100); p.scanned(null,101); check(!p.consume(u,102));
        p.scanned(u,100); p.scanned("https://hff.deeep.link/hff-other",101); check(!p.consume(u,102)); check(p.consume("https://hff.deeep.link/hff-other",102));
        System.out.println("Gym URL validation, normalization, scan expiry, replacement and one-shot matching passed.");
    }
}
