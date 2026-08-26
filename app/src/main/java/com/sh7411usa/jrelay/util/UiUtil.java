package com.sh7411usa.jrelay.util;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

public class UiUtil {

    private UiUtil() {
    }

    /** A thin 1dp divider line for separating rows inflated into a vertical LinearLayout. */
    public static View createDivider(Context context, int colorResId) {
        View divider = new View(context);
        int heightPx = Math.round(1 * context.getResources().getDisplayMetrics().density);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightPx));
        divider.setBackgroundColor(context.getColor(colorResId));
        return divider;
    }
}
