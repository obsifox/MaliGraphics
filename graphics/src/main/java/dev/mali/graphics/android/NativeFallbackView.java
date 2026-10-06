package dev.mali.graphics.android;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Native Android UI fallback (mission §24). The launcher must NEVER become
 * unusable because the custom renderer failed. Host calls
 * {@link #installFallback(Activity, LinearLayout, String)} on onRuntimeFailed.
 */
public final class NativeFallbackView {

    private NativeFallbackView() {}

    /** Builds a simple native launcher-like screen and swaps it in. */
    public static LinearLayout installFallback(Activity activity, LinearLayout root, String reason) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.parseColor("#09090B"));
        box.setPadding(dp(activity, 24), dp(activity, 24), dp(activity, 24), dp(activity, 24));

        TextView title = new TextView(activity);
        title.setText("حالت سازگاری (Native UI)");
        title.setTextColor(Color.parseColor("#10B981"));
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView msg = new TextView(activity);
        msg.setText("رندر سفارشی در دسترس نیست:\n" + safe(reason)
                + "\n\nلانچر با رابط بومی اندروید ادامه می‌دهد.");
        msg.setTextColor(Color.parseColor("#D4D4D8"));
        msg.setTextSize(14);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, dp(activity, 12), 0, 0);

        box.addView(title);
        box.addView(msg);

        if (root != null) {
            root.removeAllViews();
            root.addView(box, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            root.setVisibility(View.VISIBLE);
        } else {
            activity.setContentView(box);
        }
        return box;
    }

    private static String safe(String s) {
        return s == null ? "unknown error" : s.length() > 400 ? s.substring(0, 400) : s;
    }

    private static int dp(Activity a, int v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }
}
