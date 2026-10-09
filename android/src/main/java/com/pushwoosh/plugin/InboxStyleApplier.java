package com.pushwoosh.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.pushwoosh.inbox.ui.PushwooshInboxStyle;
import com.pushwoosh.inbox.ui.model.customizing.formatter.InboxDateFormatter;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Writes one call's style onto the SDK's inbox style.
 *
 * The SDK style is a process-wide singleton that the SDK never resets, so the defaults are
 * captured the first time this class runs and put back before every apply: without that, a key
 * set by one presentInboxUI call would still be in effect for the next one, which is not what
 * the published contract promises.
 *
 * Keys that belong to the other platform are skipped silently; a value this platform cannot parse
 * is skipped with a warning and the remaining keys still apply.
 */
final class InboxStyleApplier {

    private static final Pattern COLOR =
            Pattern.compile("^(#[0-9A-Fa-f]{3}|(0x|#)([0-9A-Fa-f]{2})?[0-9A-Fa-f]{6})$");

    private static Defaults defaults;

    private InboxStyleApplier() {
    }

    static synchronized void apply(Context context, JSONObject style) {
        if (defaults == null) {
            defaults = new Defaults();
        }
        defaults.restore();
        if (style == null) {
            return;
        }

        PushwooshInboxStyle target = PushwooshInboxStyle.INSTANCE;

        String barTitle = string(style, "barTitle");
        if (barTitle != null) {
            target.setBarTitle(barTitle);
        }
        String listEmptyMessage = string(style, "listEmptyMessage");
        if (listEmptyMessage != null) {
            target.setListEmptyText(listEmptyMessage);
        }
        String listErrorMessage = string(style, "listErrorMessage");
        if (listErrorMessage != null) {
            target.setListErrorMessage(listErrorMessage);
        }

        InboxDateFormatter formatter = dateFormatter(style);
        if (formatter != null) {
            target.setDateFormatter(formatter);
        }

        Drawable defaultImageIcon = image(context, style, "defaultImageIcon");
        if (defaultImageIcon != null) {
            target.setDefaultImageIconDrawable(defaultImageIcon);
        }
        Drawable listEmptyImage = image(context, style, "listEmptyImage");
        if (listEmptyImage != null) {
            target.setListEmptyImageDrawable(listEmptyImage);
        }
        Drawable listErrorImage = image(context, style, "listErrorImage");
        if (listErrorImage != null) {
            target.setListErrorImageDrawable(listErrorImage);
        }

        Integer value;
        if ((value = color(style, "accentColor")) != null) {
            target.setAccentColor(value);
        }
        if ((value = color(style, "backgroundColor")) != null) {
            target.setBackgroundColor(value);
        }
        if ((value = color(style, "highlightColor")) != null) {
            target.setHighlightColor(value);
        }
        if ((value = color(style, "titleColor")) != null) {
            target.setTitleColor(value);
        }
        if ((value = color(style, "descriptionColor")) != null) {
            target.setDescriptionColor(value);
        }
        if ((value = color(style, "dateColor")) != null) {
            target.setDateColor(value);
        }
        if ((value = color(style, "dividerColor")) != null) {
            target.setDividerColor(value);
        }
        if ((value = color(style, "barBackgroundColor")) != null) {
            target.setBarBackgroundColor(value);
        }
        if ((value = color(style, "barAccentColor")) != null) {
            target.setBarAccentColor(value);
        }
        if ((value = color(style, "barTextColor")) != null) {
            target.setBarTextColor(value);
        }
        if ((value = color(style, "readTitleColor")) != null) {
            target.setReadTitleColor(value);
        }
        if ((value = color(style, "readDescriptionColor")) != null) {
            target.setReadDescriptionColor(value);
        }
        if ((value = color(style, "readDateColor")) != null) {
            target.setReadDateColor(value);
        }
    }

    private static String string(JSONObject style, String key) {
        Object raw = style.opt(key);
        if (raw instanceof String && !((String) raw).isEmpty()) {
            return (String) raw;
        }
        warnIfPresent(key, raw);
        return null;
    }

    private static Integer color(JSONObject style, String key) {
        String raw = string(style, key);
        if (raw == null) {
            return null;
        }
        if (!COLOR.matcher(raw).matches()) {
            warn(key, raw);
            return null;
        }
        String normalized = raw.startsWith("0x") ? "#" + raw.substring(2) : raw;
        if (normalized.length() == 4) {
            normalized = "#"
                    + normalized.charAt(1) + normalized.charAt(1)
                    + normalized.charAt(2) + normalized.charAt(2)
                    + normalized.charAt(3) + normalized.charAt(3);
        }
        try {
            return Color.parseColor(normalized);
        } catch (IllegalArgumentException e) {
            warn(key, raw);
            return null;
        }
    }

    private static InboxDateFormatter dateFormatter(JSONObject style) {
        String pattern = string(style, "dateFormat");
        if (pattern == null) {
            return null;
        }
        final SimpleDateFormat format;
        try {
            format = new SimpleDateFormat(pattern, Locale.getDefault());
        } catch (IllegalArgumentException e) {
            warn("dateFormat", pattern);
            return null;
        }
        return new InboxDateFormatter() {
            @Override
            public String transform(Date input) {
                return format.format(input);
            }
        };
    }

    private static Drawable image(Context context, JSONObject style, String key) {
        String raw = string(style, key);
        if (raw == null || context == null) {
            return null;
        }
        // AssetManager rejects the doubled separator a leading slash leaves behind.
        String path = raw.replaceFirst("^/+", "");
        if (path.isEmpty()) {
            warn(key, raw);
            return null;
        }
        try (InputStream stream = context.getAssets().open("public/" + path)) {
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            if (bitmap == null) {
                warn(key, raw);
                return null;
            }
            return new BitmapDrawable(context.getResources(), bitmap);
        } catch (IOException e) {
            warn(key, raw);
            return null;
        }
    }

    private static void warnIfPresent(String key, Object raw) {
        if (raw != null) {
            warn(key, raw);
        }
    }

    private static void warn(String key, Object raw) {
        PWLog.warn(Inbox.TAG, "Inbox style: ignoring invalid value for '" + key + "': " + raw);
    }

    /** The style the SDK had before the plugin first touched it. */
    private static final class Defaults {

        private final PushwooshInboxStyle style = PushwooshInboxStyle.INSTANCE;

        private final InboxDateFormatter dateFormatter = style.getDateFormatter();
        private final CharSequence listEmptyText = style.getListEmptyText();
        private final CharSequence listErrorMessage = style.getListErrorMessage();
        private final String barTitle = style.getBarTitle();
        private final Drawable defaultImageIconDrawable = style.getDefaultImageIconDrawable();
        private final Drawable listEmptyImageDrawable = style.getListEmptyImageDrawable();
        private final Drawable listErrorImageDrawable = style.getListErrorImageDrawable();
        private final Integer accentColor = style.getAccentColor();
        private final Integer backgroundColor = style.getBackgroundColor();
        private final Integer highlightColor = style.getHighlightColor();
        private final Integer titleColor = style.getTitleColor();
        private final Integer readTitleColor = style.getReadTitleColor();
        private final Integer descriptionColor = style.getDescriptionColor();
        private final Integer readDescriptionColor = style.getReadDescriptionColor();
        private final Integer dateColor = style.getDateColor();
        private final Integer readDateColor = style.getReadDateColor();
        private final Integer dividerColor = style.getDividerColor();
        private final Integer barBackgroundColor = style.getBarBackgroundColor();
        private final Integer barAccentColor = style.getBarAccentColor();
        private final Integer barTextColor = style.getBarTextColor();

        void restore() {
            style.setDateFormatter(dateFormatter);
            style.setListEmptyText(listEmptyText);
            style.setListErrorMessage(listErrorMessage);
            style.setBarTitle(barTitle);
            style.setDefaultImageIconDrawable(defaultImageIconDrawable);
            style.setListEmptyImageDrawable(listEmptyImageDrawable);
            style.setListErrorImageDrawable(listErrorImageDrawable);
            style.setAccentColor(accentColor);
            style.setBackgroundColor(backgroundColor);
            style.setHighlightColor(highlightColor);
            style.setTitleColor(titleColor);
            style.setReadTitleColor(readTitleColor);
            style.setDescriptionColor(descriptionColor);
            style.setReadDescriptionColor(readDescriptionColor);
            style.setDateColor(dateColor);
            style.setReadDateColor(readDateColor);
            style.setDividerColor(dividerColor);
            style.setBarBackgroundColor(barBackgroundColor);
            style.setBarAccentColor(barAccentColor);
            style.setBarTextColor(barTextColor);
        }
    }
}
