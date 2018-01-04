package com.sivaraam.apkbackup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.widget.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sivaraam on 22/6/17.
 */

public class SplashScreenActivity extends AppCompatActivity
{

    //ArrayLists to store PackageInfo about available DownloadedApps and SystemApps
    //purposefully made as STATIC and PUBLIIC to share the lists with MainActivity
    //------------------------------------------------------------------------------
    //   could NOT pass them with the intent bundle as mApplication is
    //   NOT SERIALIZABLE and I DO NOT INTEND TO MAKE IT 'PARCELABLE'
    //   so the future me listen:- "NO fancy things to do with bundling the lists"
    //-----------------------------------------------------------------------------

    public static ArrayList<mApplication> downloadedAppsList, systemAppsList;
    public static int noOfAllApps;
    static String sortType = "name_ascending";
    //Comparator to compare Apps by their name (label) in ascending order ( A -> Z )
    static final Comparator<mApplication> BY_TITLE_ASCENDING = new Comparator<mApplication>()
    {
        @Override
        public int compare(mApplication t2, mApplication t1)
        {
            //iGNorE cAsE sO tHAT sUCH aWKwaRD APP nAMES LIKE "uGET" AND "uTORRENT"
            // DO NOT GET PLACED AT THE BOTTOM OF THE LIST
            return t2.label.compareToIgnoreCase(t1.label);
        }
    };
    //Comparator to compare Apps by their name (label) in descending order ( Z -> A )
    static final Comparator<mApplication> BY_TITLE_DESCENDING = new Comparator<mApplication>()
    {
        @Override
        public int compare(mApplication t2, mApplication t1)
        {
            //iGNorE cAsE sO tHAT sUCH aWKwaRD APP nAMES LIKE "uGET" AND "uTORRENT"
            // DO NOT GET PLACED AT THE BOTTOM OF THE LIST
            return -(t2.label.compareToIgnoreCase(t1.label));
        }
    };
    //Comparator to compare Apps by their SIZE in ascending order
    static final Comparator<mApplication> BY_SIZE_ASCENDING = new Comparator<mApplication>()
    {
        @Override
        public int compare(mApplication t2, mApplication t1)
        {
            //Long.compare() is not used as it is only available from Java 7 onwards
            // To target API 19 we should not use Long.compare()
            // Hence this workaround using Strings. This work like a charm, don't mend it.
            String s1 = Long.toString(t1.fileSizeBytes);
            String s2 = Long.toString(t2.fileSizeBytes);
            if (s1.length() == s2.length())
            {
                return s2.compareTo(s1);
            } else
            {
                return s2.length() - s1.length();
            }
        }
    };
    //Comparator to compare Apps by their SIZE in descending order
    static final Comparator<mApplication> BY_SIZE_DESCENDING = new Comparator<mApplication>()
    {
        @Override
        public int compare(mApplication t1, mApplication t2)
        {
            //Long.compare() is not used as it is only available from Java 7 onwards
            // To target API 19 we should not use Long.compare()
            // Hence this workaround using Strings
            String s1 = Long.toString(t1.fileSizeBytes);
            String s2 = Long.toString(t2.fileSizeBytes);
            if (s1.length() == s2.length())
            {
                return s2.compareTo(s1);
            } else
            {
                return s2.length() - s1.length();
            }
        }
    };
    //progressBar updates the UI in Splash Screen based on the AsyncTask's work
    private ProgressBar progressBar;
    private SharedPreferences preferences;
    private boolean alreadyrunning = false;
    private boolean isrunning = false;
    //AsyncTask LoadApps is used to load the apps in the background
    private LoadApps loadApps;


    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        downloadedAppsList = new ArrayList<>();
        systemAppsList = new ArrayList<>();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(103);

        //get shared preferences to determine the preferred sortType for the user
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //
        //          Check if 'sort_type' preference is available
        //----------------------------------------------------------------------------------
        // It may be unavailable for one of the following reasons.
        //          1. This is the first run for the App.
        //          2. The User has used 'Clear Data' feature from Settings App
        //----------------------------------------------------------------------------------
        if (preferences.contains("sort_type"))
        {
            sortType = preferences.getString("sort_type", sortType);
        } else
        {
            preferences.edit().putString("sort_type", "name_ascending").apply();
        }

        if (preferences.contains("already_running"))
        {
            this.alreadyrunning = preferences.getBoolean("already_running", false);
        } else
        {
            preferences.edit().putBoolean("already_running", false).apply();
        }
        if (!this.alreadyrunning)
        {
            progressBar.setProgress(0);
            //Run the AsyncTask to load the apps
            loadApps = new LoadApps();
            loadApps.execute();
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus)
        {
            if (isrunning)
            {
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("already_running", true).apply();
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        //---------------------------------------
        //            do nothing
        //---------------------------------------
        //   problems with closing the Activity
        //   when AsyncTask is running
        //   So don't close the Activity,
        //---------------------------------------
    }

    private class LoadApps extends AsyncTask<Void, Integer, Integer>
    {
        @Override
        protected void onPreExecute()
        {
            //clear the lists before inserting into them
            downloadedAppsList.clear();
            systemAppsList.clear();
            //set progress to 0
            progressBar.setProgress(0);

            isrunning = true;

        }

        @Override
        protected Integer doInBackground(Void... voids)
        {
            //get a list of all available packages in the System
            List<PackageInfo> packList = getPackageManager()
                    .getInstalledPackages(0);
            int n = packList.size();
            noOfAllApps = n;
            //iterate through the list and determine their category
            for (int i = 0; i < n; i++)
            {

                PackageInfo packInfo = packList.get(i);
                String label, version, size;
                Drawable icon;
                long fileSizeInBytes;
                //if NOT-A-SYSTEM-APP OR is a UPDATED-SYSTEM-APP then add them to DOWNLOADED-APPS-LIST
                if (((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                        || ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0))
                {

                    label = packInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                    icon = (packInfo.applicationInfo.loadIcon(getPackageManager()));
                    version = (packInfo.versionName);
                    fileSizeInBytes = (new File(packInfo.applicationInfo.publicSourceDir)).length();
                    size = Formatter.formatFileSize(getApplicationContext(), (fileSizeInBytes));
                    downloadedAppsList.add(new mApplication(label, version, icon, size, fileSizeInBytes, i));
                }
                // else if a SYSTEM-APP then add to SYSTEM-APPS-LIST
                else if ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                {

                    label = packInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                    icon = (packInfo.applicationInfo.loadIcon(getPackageManager()));
                    version = (packInfo.versionName);
                    fileSizeInBytes = (new File(packInfo.applicationInfo.publicSourceDir)).length();
                    size = Formatter.formatFileSize(getApplicationContext(), (fileSizeInBytes));
                    systemAppsList.add(new mApplication(label, version, icon, size, fileSizeInBytes, i));
                }
                //publish the progress to the UI thread
                publishProgress((i * 100) / n);
            }
            //
            //
            //  sort both the lists by the preferred sorting criteria
            //  Arraylist.sort() is not available prior to Java 8
            //  Hence API < 23 should NOT use it and we are
            //  targeting API 19.
            //    SO STAY AWAY FROM IT.
            //
            //-------------------------------------------------
            //
            // "PREMATURE OPTIMIZATION IS THE ROOT OF ALL EVIL"
            //                                  -Donald Knuth
            //
            //------------------------------------------------
            //
            //
            switch (sortType)
            {
                case "name_ascending":
                    Collections.sort(systemAppsList, BY_TITLE_ASCENDING);
                    publishProgress(101);
                    Collections.sort(downloadedAppsList, BY_TITLE_ASCENDING);
                    publishProgress(102);
                    break;
                case "size_ascending":
                    Collections.sort(systemAppsList, BY_SIZE_ASCENDING);
                    publishProgress(101);
                    Collections.sort(downloadedAppsList, BY_SIZE_ASCENDING);
                    publishProgress(102);
                    break;
                case "name_descending":
                    Collections.sort(systemAppsList, BY_TITLE_DESCENDING);
                    publishProgress(101);
                    Collections.sort(downloadedAppsList, BY_TITLE_DESCENDING);
                    publishProgress(102);
                    break;
                case "size_descending":
                    Collections.sort(systemAppsList, BY_SIZE_DESCENDING);
                    publishProgress(101);
                    Collections.sort(downloadedAppsList, BY_SIZE_DESCENDING);
                    publishProgress(102);
                    break;
            }
            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            //update progressbar value
            progressBar.setProgress(values[0]);

        }

        @Override
        protected void onPostExecute(Integer aVoid)
        {

            isrunning = false;
            preferences.edit().putBoolean("already_running", false).apply();

            //create MainActivity intent and start it
            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
            //  intent.putParcelableArrayListExtra("sys_apps",SplashScreenActivity.systemAppsList);
            startActivity(intent);

            // when it returns from MainActivity finish() this Activity
            // to avoid showing splash screen while closing the app
            //------------------------------------------------------
            //this is  done to act as a fail-safe
            // IF
            //    android:noHistory="true" in AndroidManifest.xml
            //    is not recognised due to some odd reason
            //ELSE
            //    app will gracefully exit from MainActivity itself
            //    and no need to worry about this.
            //-------------------------------------------------------
            SplashScreenActivity.this.finish();
        }
    }
}
