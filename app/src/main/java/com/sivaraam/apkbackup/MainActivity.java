package com.sivaraam.apkbackup;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity
{

    //Android 6.0+ Permissions Management related variables
    private static final int EXTERNAL_STORAGE_PERMISSION_CONSTANT = 100;
    private static final int REQUEST_PERMISSION_SETTING = 101;
    private static int currentFragment = 0;
    public static int noOfApps;
    public static final ArrayList<Boolean> selectedlist = new ArrayList<>();
    public static ArrayList<Integer> selectedAppList = new ArrayList<>();
    public static long sizeOfSelected = 0;
    private static ArrayList<mApplication> DwnlAppList = new ArrayList<>();
    private static ArrayList<mApplication> SysAppList = new ArrayList<>();
    //to differentiate the threads so that they have their own unique notifications
    private static int threadid = 0;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private Toast toast;
    private android.support.v4.app.NotificationCompat.Builder mBuilder;
    private NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
    private long lastBackPressTime;
    private boolean sentToSettings = false;
    private SharedPreferences permissionStatus;
    static private boolean hasStoragePermissions =false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // WHEN  the application is resuming from DEAD state i.e.,
        //       after Android has detached its VM due to lack of resources ,
        // DO
        //     goto SplashScreenActivity and load the info again.
        // -------------------------------------------------------------------------------
        // WHY
        //     Since the VM is detached the variables and their values will be lost
        //     to prevent this, Android stores the current Activity's state in a bundle
        //     and stores it,but SplashScreen is not the current Activity and so its
        //     values will be lost,thus to prevent NULL POINTER EXCEPTION , if
        //     SplashScreen's values are NULL, start the SplashScreen Activity.
        // ALTERNATIVES
        //     - Make mApplication class , PARCELABLE and bundle it
        //           * Drawables (icons) aren't natively parcelable but I'm trying.
        //     - Store info in SQLite DB and retrieve it
        //           * SQLite DB makes thing slow.
        //           * It takes up more space.
        //           * Must be updated every time App is loaded,hence not persistent
        //             and there is no point in storing non-persistent data in a DB.
        // -----------------------------------------------------------------------------
        if (SplashScreenActivity.systemAppsList == null)
        {
            //DO
            //    not call  ' super.onCreate(savedInstanceState); '
            //WHY
            //    parcelable values stored in the bundle like int,String,boolean etc will
            //    get retained and contaminate the state of the Activity.
            // -------------------------------------------------------------------------------------
            //SOLUTION
            //    call ' super.onCreate(new Bundle()); '
            // WHY
            //    to prevent SuperNotCalled Exception and also to not retain the indeterminate state
            // -------------------------------------------------------------------------------------
            // CONs
            //    the current state is not saved and is not the recommended way to do things.
            super.onCreate(new Bundle());
            SplashScreenActivity.systemAppsList = new ArrayList<>();
            SplashScreenActivity.downloadedAppsList = new ArrayList<>();
            SplashScreenActivity.noOfAllApps = 0;
            startActivity(new Intent(MainActivity.this, SplashScreenActivity.class));
        } else
        {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            //  SysAppList = getIntent().getParcelableArrayListExtra("sys_apps");
            SysAppList = SplashScreenActivity.systemAppsList;
            DwnlAppList = SplashScreenActivity.downloadedAppsList;
            noOfApps = SplashScreenActivity.noOfAllApps;
            selectedAppList = new ArrayList<>(DwnlAppList.size() + SysAppList.size() - 1);

            viewPager = (ViewPager) findViewById(R.id.viewpager);
            tabLayout = (TabLayout) findViewById(R.id.tabs);
            inboxStyle.addLine("");
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            setupViewPager(viewPager);
            tabLayout.setupWithViewPager(viewPager);
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
            fab.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    backupApp();

                }
            });

            permissionStatus = getSharedPreferences("permissionStatus", MODE_PRIVATE);
            hasStoragePermissions = this.checkForStoragePermissions();
        }
    }


    @Override
    public void onBackPressed()
    {
        //check if user really wants to exit or has accidentally pressed the back button
        if (this.lastBackPressTime < System.currentTimeMillis() - 3000)
        {
            toast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            toast.show();
            this.lastBackPressTime = System.currentTimeMillis();
        } else
        {
            if (null != toast)
            {
                toast.cancel();
            }
            super.onBackPressed();

        }
    }

    private boolean checkForStoragePermissions()
    {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Need Storage Permission");
                builder.setMessage("ApkBackup needs your permission to store the APKs");
                builder.setPositiveButton("Grant", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.cancel();
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {

                        dialogInterface.cancel();
                    }
                });
                builder.show();
            } else if (permissionStatus.getBoolean(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, false))
            {
                //Previously Permission Request was cancelled with 'Dont Ask Again',
                // Redirect to Settings after showing Information about why you need the permission
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Need Storage Permission");
                builder.setMessage("ApkBackup needs storage permission your permission to store the APKs.");
                builder.setPositiveButton("Grant", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                        sentToSettings = true;
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                        Toast.makeText(getBaseContext(), "Go to Permissions to Grant Storage", Toast.LENGTH_LONG).show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                    }
                });
                builder.show();
            } else
            {
                //just request the permission
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_CONSTANT);
            }

            SharedPreferences.Editor editor = permissionStatus.edit();
            editor.putBoolean(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, true);
            editor.commit();


        } else
        {
            //We already have the permission, just go ahead.
            return true;
        }
         return ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //  this menu is not in CustomFragment so that both fragments can be sorted using this menu
        //  SearchView has dependencies in the Fragment and is hence implemented there.
        getMenuInflater().inflate(R.menu.menu_sort, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void sortByType(String typeOfSort, Comparator<mApplication> mApplicationComparator)
    {
        if (!SplashScreenActivity.sortType.equals(typeOfSort))
        {
            SplashScreenActivity.sortType = typeOfSort;
            Collections.sort(MainActivity.DwnlAppList, mApplicationComparator);
            Collections.sort(MainActivity.SysAppList, mApplicationComparator);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("sort_type", typeOfSort).apply();
            setupViewPager(viewPager);
            tabLayout.setupWithViewPager(viewPager);
            viewPager.setCurrentItem(currentFragment);
            Toast.makeText(getApplicationContext(), "Sorted by " + typeOfSort.replace('_', ' ').toUpperCase(), Toast.LENGTH_SHORT).show();
        } else
        {
            Toast.makeText(getApplicationContext(), "Already Sorted by " + typeOfSort.replace('_', ' ').toUpperCase(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.sort_by_name_ascending)
        {
            sortByType("name_ascending", SplashScreenActivity.BY_TITLE_ASCENDING);
        } else if (item.getItemId() == R.id.sort_by_size_ascending)
        {
            sortByType("size_ascending", SplashScreenActivity.BY_SIZE_ASCENDING);
        } else if (item.getItemId() == R.id.sort_by_name_descending)
        {
            sortByType("name_descending", SplashScreenActivity.BY_TITLE_DESCENDING);
        } else if (item.getItemId() == R.id.sort_by_size_descending)
        {
            sortByType("size_descending", SplashScreenActivity.BY_SIZE_DESCENDING);
        } else if (item.getItemId() == R.id.about)
        {
            Intent i = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(i);
        } else if (item.getItemId() == R.id.help)
        {
            Intent i = new Intent(MainActivity.this, HelpActivity.class);
            startActivity(i);
        }
        return true;
    }

    /*
    * @param fragmentId the current fragment's id
    * */
    public ArrayList<mApplication> getPackInfo(int fragmentId)
    {
        if (fragmentId == 0)
        {
            return DwnlAppList;
        } else
        {
            return SysAppList;
        }
    }

    private void setupViewPager(ViewPager viewPager)
    {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                currentFragment = position;
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
        Fragment downloadedFragment = new CustomFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("FragmentID", 0);
        downloadedFragment.setArguments(bundle);
        adapter.addFragment(downloadedFragment, "DOWNLOADED");
        Fragment systemFragment = new CustomFragment();
        bundle = new Bundle();
        bundle.putInt("FragmentID", 1);
        systemFragment.setArguments(bundle);
        adapter.addFragment(systemFragment, "SYSTEM");
        viewPager.setAdapter(adapter);
    }

    private void backupApp()
    {

        List<PackageInfo> packList = getPackageManager()
                .getInstalledPackages(0);
        String input[] = new String[selectedAppList.size() * 4];
        int i = 0;
        for (int id : selectedAppList)
        {

            PackageInfo packInfo = packList.get(id);
            final String appNameout = packInfo.applicationInfo.loadLabel(getPackageManager()).toString() + "_"
                    + packInfo.versionName;
            final String src = packInfo.applicationInfo.publicSourceDir
                    .substring(0, 10);
            String dst;
            if (((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                    || ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0))
            {
                dst = Environment.getExternalStorageDirectory().getPath()
                        + "/ApkBackup/Downloaded/";
            } else
            {
                dst = Environment.getExternalStorageDirectory().getPath()
                        + "/ApkBackup/System/";
            }
            final String appNameIn = packInfo.applicationInfo.publicSourceDir
                    .substring(10);
            input[i++] = src;
            input[i++] = appNameIn;
            input[i++] = dst;
            input[i++] = appNameout;


        }
        if (input.length > 0)
        {
            int n = selectedAppList.size();
            for (int id = 0; id < selectedlist.size(); id++)
            {
                if (selectedlist.get(id))
                {
                    selectedAppList.remove((Integer) id);
                }
                selectedlist.set(id, false);
            }
            setupViewPager(viewPager);
            tabLayout.setupWithViewPager(viewPager);
            viewPager.setCurrentItem(currentFragment);
            mBuilder = new android.support.v4.app.NotificationCompat.Builder(MainActivity.this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("APK BACKUP").setAutoCancel(true);

            hasStoragePermissions = checkForStoragePermissions();
            if(hasStoragePermissions)
            {
                AsyncTask<String[], Long, String> ast = new CopyFilesTask(threadid++);
                ast.execute(input);

                Snackbar.make(tabLayout, n + " apps selected \n will be backed up shortly", Snackbar.LENGTH_SHORT).show();
            }
            else
            {
                Snackbar.make(tabLayout, "Permission Denied: Can't Store the Apps without Permission", Snackbar.LENGTH_LONG).show();
            }
        } else
        {
            Snackbar.make(tabLayout, "Please select the apps you want to backup and then click this button", Snackbar.LENGTH_SHORT).show();
        }
    }
    void openExplorer()
    {

        Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/ApkBackup/");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "resource/folder");

        if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
        {
            startActivity(intent);
        }
        else
        {
            //User has not installed any File Explorer Apps that we can detect . So do nothing
        }
    }


    class ViewPagerAdapter extends FragmentPagerAdapter
    {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager)
        {
            super(manager);
        }

        @Override
        public Fragment getItem(int position)
        {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount()
        {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title)
        {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return mFragmentTitleList.get(position);
        }
    }


    public class CopyFilesTask extends AsyncTask<String[], Long, String>
    {
        final int threadno;
        long totalRead;
        int n, c;

        CopyFilesTask(int threadno)
        {
            this.threadno = threadno;
        }

        @Override
        protected void onPreExecute()
        {
            inboxStyle = new android.support.v4.app.NotificationCompat.InboxStyle();

        }

        @Override
        protected String doInBackground(String[]... strings)
        {

            String[] fileInfo = strings[0];
            n = fileInfo.length;
            c = 0;
            String inputPath, inputFile, outputPath, outputFile;

            InputStream in = null;
            OutputStream out = null;
            String ret = "success";
            totalRead = 0;
            for (int i = 0; i < n; )
            {

                c++;
                //copyFile(fileInfo[i], fileInfo[i + 1], fileInfo[i + 2], fileInfo[i + 3]);
                inputPath = fileInfo[i];
                inputFile = fileInfo[i + 1];
                outputPath = fileInfo[i + 2];
                outputFile = fileInfo[i + 3];


                try
                {

                    File dir = new File(outputPath);
                    if (!dir.exists())
                    {
                        dir.mkdirs();
                    }


                    File outputApkFile = new File(dir + "/" + outputFile + ".apk");
                    outputApkFile.createNewFile();

                    in = new FileInputStream(inputPath + inputFile);
                    out = new FileOutputStream(outputApkFile);

                    byte[] buffer = new byte[4 * 1024];
                    int read;
                    int noc = 0;
                    while ((read = in.read(buffer)) != -1)
                    {
                        out.write(buffer, 0, read);
                        totalRead += read;
                        noc++;
                        if (noc == 25)
                        {
                            noc = 0;
                            publishProgress((totalRead * 100) / sizeOfSelected);
                        }
                    }

                    in.close();
                    in = null;
                    out.flush();
                    inboxStyle
                            .addLine(outputFile.substring(0, outputFile.indexOf("_")));

                    out.close();
                    out = null;


                } catch (FileNotFoundException fnfe1)
                {

                    ret = "File Not Found Exception:" + fnfe1.getMessage();
                    return ret;

                } catch (NullPointerException npe)
                {
                    ret = npe.getMessage();
                    return ret;
                } catch (Exception e)
                {
                    ret = "Exception:" + e.getMessage();
                    return ret;
                }

                i += 4;


            }
            return ret;
        }

        @Override
        protected void onProgressUpdate(Long... values)
        {
            mBuilder.setProgress(100, Integer.parseInt(values[0].toString()), false);
            mBuilder.setContentInfo(c + "/" + (n / 4)).setSmallIcon(R.mipmap.ic_launcher_round);
            mBuilder.setOngoing(true);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(threadno, mBuilder.build());


        }

        @Override
        protected void onPostExecute(String s)
        {
            sizeOfSelected -= this.totalRead;
            if (s.equals("success"))
            {


                Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/ApkBackup/");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(selectedUri, "resource/folder");


                mBuilder = new android.support.v4.app.NotificationCompat.Builder(MainActivity.this)
                        .setStyle(inboxStyle)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setAutoCancel(true)
                        .setSubText(Environment.getExternalStorageDirectory().getPath() + "/ApkBackup/")
                        .setContentTitle("Apps Saved Successfully!!")
                        .setContentText((n / 4) + " apps saved");

                if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
                {
                    PendingIntent explorerIntent =PendingIntent.getActivity(MainActivity.this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(explorerIntent);
                }
                else
                {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);

                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setDataAndType(selectedUri, "*/*");
                    if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
                    {
                        PendingIntent explorerIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(explorerIntent);
                    }
                    else
                    {
                        //User has not installed any File Explorer Apps that we can detect . So do nothing
                    }
                }
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(threadno);
                mNotificationManager.notify(threadno, mBuilder.build());
                Toast.makeText(getApplicationContext(), (n / 4) + " apps saved", Toast.LENGTH_LONG).show();
            } else
            {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(threadno);
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        }
    }
}
