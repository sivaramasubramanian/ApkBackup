package com.sivaraam.apkbackup;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_about);
        setSupportActionBar(toolbar);
        try
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e)
        {

        }
        TextView tv = (TextView) findViewById(R.id.App_version);
        try
        {
            tv.setText(this.getApplicationInfo().loadLabel(getPackageManager()).toString() + "  v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e)
        {

        }

    }
}
