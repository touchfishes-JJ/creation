package com.forcefocus.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

import java.lang.ref.WeakReference;
import java.util.Set;

/** Enforces the current task whitelist while leaving essential permission UI usable. */
public final class FocusAccessibilityService extends AccessibilityService {
    private static WeakReference<FocusAccessibilityService> currentService =
            new WeakReference<>(null);

    private long lastBounceAt;
    private String lastBlockedPackage = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        currentService = new WeakReference<>(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;
        if (!FocusState.isActive(this)) return;

        CharSequence packageChars = event.getPackageName();
        if (packageChars == null) return;
        String packageName = packageChars.toString();
        if (getPackageName().equals(packageName)) return;
        if (AppResolver.isEssentialSystemPackage(packageName)) return;

        Set<String> allowed = AppResolver.allowedPackagesForTask(this, FocusState.focusTask(this));
        if (allowed.contains(packageName)) return;

        long now = System.currentTimeMillis();
        if (packageName.equals(lastBlockedPackage) && now - lastBounceAt < 500L) return;
        lastBlockedPackage = packageName;
        lastBounceAt = now;
        MainActivity.bringToFront(this);
    }

    public static void bringForceFocusToFront() {
        FocusAccessibilityService service = currentService.get();
        if (service != null) MainActivity.bringToFront(service);
    }

    @Override
    public void onInterrupt() {
        // No continuous feedback channel to interrupt.
    }

    @Override
    public void onDestroy() {
        if (currentService.get() == this) currentService.clear();
        super.onDestroy();
    }
}
