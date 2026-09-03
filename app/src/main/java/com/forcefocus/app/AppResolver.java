package com.forcefocus.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Resolves the four user-facing app labels without assuming one vendor build. */
public final class AppResolver {
    private static final Map<String, List<String>> PACKAGE_CANDIDATES = new HashMap<>();
    private static final Set<String> ESSENTIAL_SYSTEM_PACKAGES = new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.securitycenter",
            "com.miui.packageinstaller",
            "com.samsung.android.permissioncontroller",
            "com.coloros.safecenter",
            "com.oplus.safecenter"
    ));

    static {
        PACKAGE_CANDIDATES.put("WPS", Arrays.asList(
                "cn.wps.moffice_eng", "cn.wps.moffice", "cn.wps.moffice_i18n"));
        PACKAGE_CANDIDATES.put("小红书", Collections.singletonList("com.xingin.xhs"));
        PACKAGE_CANDIDATES.put("粉笔", Arrays.asList(
                "com.fenbi.android.servant", "com.fenbi.android.leo"));
        PACKAGE_CANDIDATES.put("录音机", Arrays.asList(
                "com.android.soundrecorder",
                "com.google.android.apps.recorder",
                "com.miui.soundrecorder",
                "com.sec.android.app.voicenote",
                "com.huawei.soundrecorder",
                "com.coloros.soundrecorder",
                "com.oplus.soundrecorder",
                "com.vivo.soundrecorder",
                "com.android.bbksoundrecorder",
                "com.oneplus.soundrecorder",
                "com.motorola.audiorecorder"));
    }

    private AppResolver() {}

    public static List<String> labelsForTask(String task) {
        if ("简历".equals(task)) return Collections.singletonList("WPS");
        if ("岗位调研".equals(task)) return Arrays.asList("小红书", "WPS");
        if ("考公".equals(task)) return Collections.singletonList("粉笔");
        if ("磨耳朵".equals(task)) return Arrays.asList("录音机", "WPS");
        return Collections.emptyList();
    }

    public static Set<String> allowedPackagesForTask(Context context, String task) {
        Set<String> packages = new HashSet<>();
        for (String label : labelsForTask(task)) {
            packages.addAll(findInstalledPackages(context, label));
        }
        return packages;
    }

    public static Intent findLaunchIntent(Context context, String label) {
        PackageManager manager = context.getPackageManager();
        for (String packageName : findInstalledPackages(context, label)) {
            Intent launch = manager.getLaunchIntentForPackage(packageName);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                return launch;
            }
        }
        return null;
    }

    private static Set<String> findInstalledPackages(Context context, String wantedLabel) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        PackageManager manager = context.getPackageManager();

        List<String> candidates = PACKAGE_CANDIDATES.get(wantedLabel);
        if (candidates != null) {
            for (String candidate : candidates) {
                try {
                    manager.getApplicationInfo(candidate, 0);
                    result.add(candidate);
                } catch (PackageManager.NameNotFoundException ignored) {
                    // Try the other regional/vendor package names.
                }
            }
        }

        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = manager.queryIntentActivities(launcher, 0);
        String wanted = wantedLabel.toLowerCase(Locale.ROOT);
        for (ResolveInfo info : activities) {
            String label = String.valueOf(info.loadLabel(manager)).toLowerCase(Locale.ROOT);
            if (labelMatches(wantedLabel, wanted, label)) {
                result.add(info.activityInfo.packageName);
            }
        }
        return result;
    }

    private static boolean labelMatches(String original, String wanted, String installed) {
        if (installed.equals(wanted) || installed.contains(wanted) || wanted.contains(installed)) {
            return true;
        }
        if ("WPS".equals(original)) {
            return installed.contains("wps") || installed.contains("金山文档");
        }
        if ("小红书".equals(original)) {
            return installed.contains("小红书") || installed.contains("rednote");
        }
        if ("粉笔".equals(original)) return installed.contains("粉笔");
        if ("录音机".equals(original)) {
            return installed.contains("录音") || installed.contains("recorder")
                    || installed.contains("voice record");
        }
        return false;
    }

    public static boolean isEssentialSystemPackage(String packageName) {
        if (packageName == null) return false;
        if (ESSENTIAL_SYSTEM_PACKAGES.contains(packageName)) return true;
        return packageName.startsWith("com.android.intentresolver")
                || packageName.startsWith("com.android.inputmethod")
                || packageName.startsWith("com.google.android.inputmethod")
                || packageName.startsWith("com.sohu.inputmethod")
                || packageName.startsWith("com.baidu.input")
                || packageName.startsWith("com.iflytek.inputmethod");
    }
}
