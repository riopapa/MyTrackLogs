package com.urrecliner.mytracklogs;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Collator;
import java.util.Date;

import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.sdfDate;
import static com.urrecliner.mytracklogs.Vars.sdfDateDay;
import static com.urrecliner.mytracklogs.Vars.sdfDateDayTime;
import static com.urrecliner.mytracklogs.Vars.sdfDateTimeLog;
import static com.urrecliner.mytracklogs.Vars.sdfTimeOnly;

class Utils {

    final private String PREFIX = "log_";

    void log(String tag, String text) {
        StackTraceElement[] traces;
        String log;
        traces = Thread.currentThread().getStackTrace();
        log = (traces.length >6) ? traceName(traces[6].getMethodName()):"";
        log = log + traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(sdfDateTimeLog.format(System.currentTimeMillis())+" " +log);
    }

    private final String [] ignoreTraces = { "dispatchTransaction", "zza", "performResume", "performCreate", "callActivityOnResume",
            "access$1200", "access$1900", "access$000", "handleReceiver", "_handleMessage", "handleMessage", "dispatchMessage",
            "handleServiceArgs", "loop"};
    private String traceName (String s) {
        for (String i : ignoreTraces) {
            if (s.equals(i))
                return "";
        }
        return s + "> ";
    }

    private String traceClassName(String s) {
        return s.substring(s.lastIndexOf(".")+1);
    }

    void logE(String tag, String text, Exception e) {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())
                +"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text +"\n"+getStackTrace(e);
        Log.e("<" + tag + ">" , log);
        append2file(sdfDateTimeLog.format(new Date())+" : " +log);
    }

    String getStackTrace(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    private void append2file(String textLine) {

        File directory = getPackageDirectory();
        BufferedWriter bw = null;
        FileWriter fw = null;
        String fullName = directory.toString() + "/" + PREFIX + sdfDate.format(new Date())+".txt";
        try {
            File file = new File(fullName);
            if (!file.exists())
                file.createNewFile();
            String outText = textLine+"\n";
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(outText);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    File getPackageDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(), getAppLabel(mContext));
        try {
            if (!directory.exists()) {
                if(directory.mkdirs()) {
                    Log.e("mkdirs","Failed "+directory);
                }
            }
        } catch (Exception e) {
            Log.e("creating Directory error", directory.toString() + "_" + e.toString());
        }
        return directory;
    }

//    ArrayList<File> getFilteredFileList(String fullPath) {
//        File[] fullFileList = new File(fullPath).listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.endsWith("jpg");
//            }
//        });
//        ArrayList<File> sortedFileList = new ArrayList<>();
//        if (fullFileList != null)
//            sortedFileList.addAll(Arrays.asList(fullFileList));
//        return sortedFileList;
//    }
//
    private String getAppLabel(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
    }

    String long2DateDay(long l) {
        return sdfDateDay.format(l);
    }
    String long2DateDayTime(long l) { return sdfDateDayTime.format(l); }
    String long2Time (long l) {
        return sdfTimeOnly.format(l);
    }
    String minute2Text(int minute) { return (minute < 60) ? minute+"분":(minute/60)+"시간 "+(minute%60)+"분"; }

    void deleteOldLogFiles() {

        File directory = getPackageDirectory();
        File[] files = new File(directory.toString()).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(PREFIX);
            }
        });
        String oldDate = sdfDate.format(System.currentTimeMillis() - 2*24*60*60*1000L);
        if (files != null) {
            Collator myCollator = Collator.getInstance();
            for (File file : files) {
                String shortFileName = file.getName();
                if (myCollator.compare(shortFileName, PREFIX+ oldDate) < 0) {
                    if (!file.delete())
                        Log.e("file", "Delete Error " + file);
                }
            }
        }
    }
}
