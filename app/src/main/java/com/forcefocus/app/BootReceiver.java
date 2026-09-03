package com.forcefocus.app;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){ Scheduler.scheduleNext14Days(c); LockState.refresh(c); }
}
