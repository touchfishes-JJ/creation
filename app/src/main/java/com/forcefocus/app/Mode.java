package com.forcefocus.app;

import java.util.*;

public enum Mode {
    RESUME("简历相关", "手机完全锁住，去电脑上做简历", new String[]{}),
    JOB("秋招岗位研究", "只允许小红书 + WPS", new String[]{"com.xingin.xhs","cn.wps.moffice_eng","cn.wps.moffice"}),
    EXAM("考公专业课", "只允许粉笔", new String[]{"com.fenbi.android.servant"}),
    AUDIO("专业课磨耳朵", "只允许录音机 + WPS", new String[]{
        "cn.wps.moffice_eng","cn.wps.moffice","com.android.soundrecorder","com.miui.voicerecorder",
        "com.huawei.soundrecorder","com.coloros.soundrecorder","com.oplus.soundrecorder",
        "com.vivo.soundrecorder","com.sec.android.app.voicenote"
    });

    public final String title;
    public final String desc;
    public final Set<String> allowed;
    Mode(String title, String desc, String[] pkgs){
        this.title=title; this.desc=desc; this.allowed=new HashSet<>(Arrays.asList(pkgs));
    }
}
