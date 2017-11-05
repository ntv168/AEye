package com.example.sam.aeye;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Sam on 11/5/2017.
 */

public class AppSingleton {
    private static volatile AppSingleton singleton = null;
    private static Context mContext;
    private static String content;
    private static Boolean checkToast = false;

    public static Context getmContext() {
        return mContext;
    }

    public static void setmContext(Context mContext) {
        AppSingleton.mContext = mContext;
    }

    public static AppSingleton getSingleton() {
        if (singleton == null) {
            synchronized (AppSingleton.class) {
                if (singleton == null) {
                    singleton = new AppSingleton();
                }
            }
        }
        return  singleton;
    }

    public Boolean getCheckToast() {
        return checkToast;
    }

    public void setCheckToast(Boolean checkToast) {
        this.checkToast = checkToast;
    }

    public static String getContent() {
        return content;
    }

    public static void setContent(String content) {
        AppSingleton.content = content;
    }
}
