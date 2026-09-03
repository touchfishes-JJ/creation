package com.forcefocus.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppResolver {
    private AppResolver() {}
    public static Set<String> allowedPackagesForTask(Context c,String task){Set<String>labels=new HashSet<>();if("简历".equals(task))labels.add("WPS");else if("岗位调研".equals(task)){labels.add("小红书");labels.add("WPS");}else if("考公".equals(task))labels.add("粉笔");else if("磨耳朵".equals(task)){labels.add("录音机");labels.add("WPS");}Set<String>pkgs=new HashSet<>();for(String label:labels){String p=findPackageByLabel(c,label);if(p!=null)pkgs.add(p);}return pkgs;}
    public static Intent findLaunchIntentByLabel(Context c,String label){String pkg=findPackageByLabel(c,label);return pkg==null?null:c.getPackageManager().getLaunchIntentForPackage(pkg);}
    private static String findPackageByLabel(Context c,String wanted){PackageManager pm=c.getPackageManager();Intent main=new Intent(Intent.ACTION_MAIN,null);main.addCategory(Intent.CATEGORY_LAUNCHER);List<ResolveInfo>list=pm.queryIntentActivities(main,0);String wantedNorm=wanted.toLowerCase(Locale.ROOT),best=null;for(ResolveInfo ri:list){String label=ri.loadLabel(pm).toString(),ln=label.toLowerCase(Locale.ROOT);if(ln.equals(wantedNorm)||ln.contains(wantedNorm)||wantedNorm.contains(ln)){best=ri.activityInfo.packageName;if(ln.equals(wantedNorm))break;}if("WPS".equals(wanted)&&(ln.contains("wps")||ln.contains("金山文档")))best=ri.activityInfo.packageName;if("小红书".equals(wanted)&&(ln.contains("小红书")||ln.contains("rednote")))best=ri.activityInfo.packageName;if("粉笔".equals(wanted)&&ln.contains("粉笔"))best=ri.activityInfo.packageName;if("录音机".equals(wanted)&&(ln.contains("录音")||ln.contains("recorder")))best=ri.activityInfo.packageName;}return best;}
    public static boolean isSystemPackage(Context c,String pkg){try{ApplicationInfo ai=c.getPackageManager().getApplicationInfo(pkg,0);return(ai.flags&ApplicationInfo.FLAG_SYSTEM)!=0||(ai.flags&ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0;}catch(Exception e){return false;}}
}
