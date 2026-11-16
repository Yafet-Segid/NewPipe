package org.schabi.newpipe.personal;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Channels blocked with "Block the channel" or blocked automatically
 * (e.g. a channel that talks about a politician). Blocks are permanent:
 * there is no way to unblock a channel from inside the app.
 */
public final class BlockedChannels {
    private static final String PREFS = "personal_blocked_channels";
    private static final String KEY_URLS = "urls";
    private static final String KEY_NAMES = "names";

    private static Set<String> urls;
    private static Set<String> names;

    private BlockedChannels() {
    }

    private static synchronized void load(final Context context) {
        if (urls != null) {
            return;
        }
        final SharedPreferences prefs = prefs(context);
        urls = new HashSet<>(prefs.getStringSet(KEY_URLS, new HashSet<>()));
        names = new HashSet<>(prefs.getStringSet(KEY_NAMES, new HashSet<>()));
    }

    private static SharedPreferences prefs(final Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Blocks a channel for good. */
    public static synchronized void add(final Context context,
                                        @Nullable final String url,
                                        @Nullable final String name) {
        load(context);
        boolean changed = false;
        final String u = normalizeUrl(url);
        final String n = normalizeName(name);
        if (u != null) {
            changed |= urls.add(u);
        }
        if (n != null) {
            changed |= names.add(n);
        }
        if (changed) {
            prefs(context).edit()
                    .putStringSet(KEY_URLS, new HashSet<>(urls))
                    .putStringSet(KEY_NAMES, new HashSet<>(names))
                    .apply();
        }
    }

    /** True when the channel was blocked before. */
    public static synchronized boolean contains(final Context context,
                                                @Nullable final String url,
                                                @Nullable final String name) {
        load(context);
        final String u = normalizeUrl(url);
        final String n = normalizeName(name);
        return (u != null && urls.contains(u)) || (n != null && names.contains(n));
    }

    @Nullable
    private static String normalizeUrl(@Nullable final String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        u = u.replaceFirst("^https?://", "").replaceFirst("^(www\\.|m\\.)", "");
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    @Nullable
    private static String normalizeName(@Nullable final String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
