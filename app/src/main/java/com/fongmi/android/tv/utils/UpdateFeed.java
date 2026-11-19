package com.fongmi.android.tv.utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reads this app's own update feed: a small JSON file hosted on our server
 * (see BuildConfig.UPDATE_URL and other/server-samples/README.md for the format).
 */
public class UpdateFeed {

    public record Release(UpdatePackage update, String notes) {
    }

    /**
     * @param json           feed content
     * @param currentVersion installed versionName, e.g. "5.8.8"
     * @param flavor         "leanback" or "mobile"
     * @param filename       key to look up in "files", e.g. "leanback-arm64_v8a.apk"
     * @return the release to offer, or null when there is no newer, usable build for this device
     */
    public static Release parse(String json, String currentVersion, String flavor, String filename) throws JSONException {
        int[] current = version(currentVersion);
        if (current == null) throw new IllegalArgumentException("Invalid app version");
        JSONObject root = new JSONObject(json);
        String tag = root.optString("version");
        int[] latest = version(tag);
        if (latest == null || !isNewer(latest, current)) return null;
        JSONObject files = root.optJSONObject("files");
        JSONObject file = files == null ? null : files.optJSONObject(filename);
        if (file == null) return null;
        String url = file.optString("url");
        String sha256 = file.optString("sha256");
        long size = file.optLong("size");
        if (!url.startsWith("https://") || size <= 0 || !sha256.matches("[0-9a-fA-F]{64}")) return null;
        String name = tag.startsWith("v") ? tag.substring(1) : tag;
        return new Release(new UpdatePackage(name, url, size, "sha256:" + sha256), notes(root.opt("notes"), flavor));
    }

    private static boolean isNewer(int[] candidate, int[] current) {
        for (int i = 0; i < current.length; i++) if (candidate[i] != current[i]) return candidate[i] > current[i];
        return false;
    }

    private static int[] version(String tag) {
        if (tag == null) return null;
        String[] parts = (tag.startsWith("v") ? tag.substring(1) : tag).split("\\.", -1);
        if (parts.length != 3) return null;
        int[] version = new int[3];
        try {
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].matches("[0-9]+")) return null;
                version[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return version;
    }

    /**
     * "notes" is either one string for every device or an object keyed by flavor.
     */
    private static String notes(Object value, String flavor) {
        String text = value instanceof JSONObject object ? object.optString(flavor) : value instanceof String string ? string : "";
        StringBuilder notes = new StringBuilder();
        for (String line : text.split("\\r?\\n")) {
            String item = line.trim();
            if (item.isEmpty()) continue;
            if (notes.length() > 0) notes.append('\n');
            notes.append(item.startsWith("* ") || item.startsWith("- ") ? "• " + item.substring(2) : item);
        }
        return notes.toString();
    }
}
