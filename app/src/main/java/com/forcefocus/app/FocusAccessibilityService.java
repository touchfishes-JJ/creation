package com.forcefocus.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import java.util.Set;

public class FocusAccessibilityService extends AccessibilityService {
    private long lastBounce = 0L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.getEventType() != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;
        if (!FocusState.isActive(this)) return;
        CharSequence cs = event.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
        if (pkg.equals(getPackageName())) return;
        if (AppResolver.isSystemPackage(this, pkg)) return;
        Set<String> allowed = AppResolver.allowedPackagesForTask(this, FocusState.task(this));
        if (allowed.contains(pkg)) return;
        long now = System.currentTimeMillis();
        if (now - lastBounce < 450) return;
        lastBounce = now;
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    @Override public void onInterrupt() {}
}
