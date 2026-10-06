package dev.mali.graphics.core.compat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Machine-readable Mali compatibility database (mission §23).
 * Asset: mali/compat-db.json — grows with every real-device Lab run (§32).
 */
public final class CompatDatabase {

    public static final class Entry {
        public String gpu = "";
        public String architecture = "";
        public String driver = "";
        public String gles = "";
        public String vulkan = "";
        public String backend = "GLES";
        public List<String> limitations = new ArrayList<>();
        public List<String> knownIssues = new ArrayList<>();
        public boolean tested = false;
        public String device = "";
        public String android = "";
        public long timestamp = 0;
    }

    private final List<Entry> entries = new ArrayList<>();

    public static CompatDatabase loadFromAssets(java.io.InputStream in) throws IOException {
        CompatDatabase db = new CompatDatabase();
        try {
            String json = readAll(in);
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("devices");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    db.entries.add(fromJson(arr.getJSONObject(i)));
                }
            }
        } catch (org.json.JSONException e) {
            throw new IOException("compat-db.json parse error: " + e.getMessage(), e);
        }
        return db;
    }

    private static Entry fromJson(JSONObject o) {
        Entry e = new Entry();
        e.gpu = o.optString("gpu");
        e.architecture = o.optString("architecture");
        e.driver = o.optString("driver");
        e.gles = o.optString("gles");
        e.vulkan = o.optString("vulkan");
        e.backend = o.optString("backend", "GLES");
        e.device = o.optString("device");
        e.android = o.optString("android");
        e.tested = o.optBoolean("tested", false);
        e.timestamp = o.optLong("timestamp", 0);
        JSONArray lim = o.optJSONArray("limitations");
        if (lim != null) for (int i = 0; i < lim.length(); i++) e.limitations.add(lim.optString(i));
        JSONArray ki = o.optJSONArray("knownIssues");
        if (ki != null) for (int i = 0; i < ki.length(); i++) e.knownIssues.add(ki.optString(i));
        return e;
    }

    public void add(Entry e) { entries.add(e); }

    public List<Entry> entries() { return entries; }

    /** Finds a matching entry by gpu+driver; null when unseen. */
    public Entry find(String gpu, String driver) {
        for (Entry e : entries) {
            if (e.gpu.equals(gpu) && e.driver.equals(driver)) return e;
        }
        return null;
    }

    /** Serializes this DB (base + runtime-added results) back to JSON. */
    public String toJson() {
        try {
            JSONObject root = new JSONObject();
            JSONArray arr = new JSONArray();
            for (Entry e : entries) arr.put(toJson(e));
            root.put("devices", arr);
            return root.toString(2);
        } catch (Exception ex) {
            return "{\"devices\":[]}";
        }
    }

    private static JSONObject toJson(Entry e) {
        JSONObject o = new JSONObject();
        try {
            o.put("gpu", e.gpu);
            o.put("architecture", e.architecture);
            o.put("driver", e.driver);
            o.put("gles", e.gles);
            o.put("vulkan", e.vulkan);
            o.put("backend", e.backend);
            o.put("device", e.device);
            o.put("android", e.android);
            o.put("tested", e.tested);
            o.put("timestamp", e.timestamp);
            o.put("limitations", new JSONArray(e.limitations));
            o.put("knownIssues", new JSONArray(e.knownIssues));
        } catch (Exception ignored) {}
        return o;
    }

    public static void writeReport(OutputStream out, String json) throws IOException {
        out.write(json.getBytes("UTF-8"));
        out.flush();
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return bos.toString("UTF-8");
    }
}
