package org.schabi.newpipe.personal;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.App;
import org.schabi.newpipe.database.stream.model.StreamEntity;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies {@link ContentFilter} and {@link BlockedChannels} to what NewPipe shows.
 * Everything personal lives in this package, so syncing with NewPipe rarely conflicts.
 */
public final class PersonalFilter {
    private PersonalFilter() {
    }

    private static Context context() {
        return App.getInstance();
    }

    /** Returns a copy of the list without blocked items. */
    @Nullable
    public static <T extends InfoItem> List<T> filterItems(@Nullable final List<T> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }
        final List<T> result = new ArrayList<>(items.size());
        for (final T item : items) {
            if (!isBlockedItem(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /** True when an item in a list (video, channel or playlist) should be hidden. */
    public static boolean isBlockedItem(@Nullable final InfoItem item) {
        if (item == null) {
            return false;
        }

        if (item instanceof StreamInfoItem) {
            final StreamInfoItem stream = (StreamInfoItem) item;
            if (ContentFilter.BLOCK_SHORTS && stream.isShortFormContent()) {
                return true;
            }
            if (BlockedChannels.contains(context(), stream.getUploaderUrl(),
                    stream.getUploaderName())) {
                return true;
            }
            final String description = textOf(stream.getShortDescription());
            final boolean blocked = ContentFilter.isBlocked(stream.getName(), null,
                    stream.getUploaderName(), false)
                    || ContentFilter.isBlockedDescription(description);
            if (blocked && ContentFilter.mentionsPolitician(stream.getName(), description)) {
                // A channel that talks about a politician is blocked for good
                BlockedChannels.add(context(), stream.getUploaderUrl(), stream.getUploaderName());
            }
            return blocked;
        }

        if (item instanceof ChannelInfoItem) {
            final ChannelInfoItem channel = (ChannelInfoItem) item;
            return BlockedChannels.contains(context(), channel.getUrl(), channel.getName())
                    || ContentFilter.isBlockedChannel(channel.getName())
                    || ContentFilter.isBlockedText(channel.getName())
                    || ContentFilter.isBlockedDescription(channel.getDescription());
        }

        if (item instanceof PlaylistInfoItem) {
            final PlaylistInfoItem playlist = (PlaylistInfoItem) item;
            return ContentFilter.isBlockedText(playlist.getName())
                    || ContentFilter.isBlocked(playlist.getName(), null,
                    playlist.getUploaderName(), false)
                    || BlockedChannels.contains(context(), playlist.getUploaderUrl(),
                    playlist.getUploaderName());
        }

        return false;
    }

    /** True when a video saved in the subscriptions feed should be hidden. */
    public static boolean isBlockedFeedItem(@NonNull final StreamEntity stream) {
        if (BlockedChannels.contains(context(), stream.getUploaderUrl(), stream.getUploader())) {
            return true;
        }
        final boolean blocked = ContentFilter.isBlocked(stream.getTitle(), null,
                stream.getUploader(), false);
        if (blocked && ContentFilter.mentionsPolitician(stream.getTitle())) {
            BlockedChannels.add(context(), stream.getUploaderUrl(), stream.getUploader());
        }
        return blocked;
    }

    /**
     * True when an opened video should not play. Its title, channel, description,
     * tags and category are all checked. A news or politics video also blocks
     * its channel for good.
     */
    public static boolean isBlockedVideo(@NonNull final StreamInfo info) {
        if (ContentFilter.BLOCK_SHORTS && isShort(info)) {
            return true;
        }
        if (BlockedChannels.contains(context(), info.getUploaderUrl(), info.getUploaderName())) {
            return true;
        }

        final String description = textOf(info.getDescription());
        final String tags = info.getTags() == null ? "" : "#" + String.join(" #", info.getTags());

        final boolean titleBlocked = ContentFilter.isBlocked(info.getName(), null,
                info.getUploaderName(), false);
        final boolean newsChannel = ContentFilter.isBlockedCategory(info.getCategory())
                || ContentFilter.isBlockedDescription(description)
                || ContentFilter.isBlockedDescription(tags)
                || ContentFilter.mentionsPolitician(info.getName(), description, tags);

        if (newsChannel) {
            BlockedChannels.add(context(), info.getUploaderUrl(), info.getUploaderName());
        }
        return titleBlocked || newsChannel;
    }

    private static boolean isShort(@NonNull final StreamInfo info) {
        try {
            return info.isShortFormContent();
        } catch (final Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static String textOf(@Nullable final Object description) {
        if (description == null) {
            return null;
        }
        if (description instanceof Description) {
            return ((Description) description).content();
        }
        return description.toString();
    }
}
