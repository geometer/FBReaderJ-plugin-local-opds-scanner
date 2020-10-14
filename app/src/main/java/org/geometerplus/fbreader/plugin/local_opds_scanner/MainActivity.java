package org.geometerplus.fbreader.plugin.local_opds_scanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openScanLocalNetworkActivity(View view) {
        startActivity(new Intent(this, ScanLocalNetworkActivity.class));
    }

    public void openLocanIpActivity(View view) {
        startActivity(new Intent(this, LocalIPActivity.class));
    }
}