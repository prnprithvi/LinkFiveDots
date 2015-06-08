package by.klnvch.link5dots;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import by.klnvch.link5dots.bluetooth.BluetoothService;
import by.klnvch.link5dots.bluetooth.DevicePickerActivity;
import by.klnvch.link5dots.nsd.NsdPickerActivity;
import by.klnvch.link5dots.nsd.NsdService;

public class MultiPlayerMenuActivity extends Activity implements View.OnClickListener{

    private static final String IS_BLUETOOTH_ENABLED = "IS_BLUETOOTH_ENABLED";

    private static final int REQUEST_ENABLE_BT = 3;
    private static final int CHOOSE_BT_DEVICE = 4;
    private static final int CHOOSE_NSD_SERVICE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multiplayer_menu);

        findViewById(R.id.multi_player_bluetooth).setOnClickListener(this);
        findViewById(R.id.multi_player_lan).setOnClickListener(this);

        // ads
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.multi_player_bluetooth:
                // Get local Bluetooth adapter
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                // If the adapter is null, then Bluetooth is not supported
                if (mBluetoothAdapter != null) {
                    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    if (!mBluetoothAdapter.isEnabled()) {
                        // enable bluetooth
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                        // remember
                        editor.putBoolean(IS_BLUETOOTH_ENABLED, false);
                    } else {
                        // launch bluetooth device chooser
                        Intent i2 = new Intent(this, DevicePickerActivity.class);
                        startActivityForResult(i2, CHOOSE_BT_DEVICE);
                        // remember
                        editor.putBoolean(IS_BLUETOOTH_ENABLED, true);
                        // start Bluetooth service
                        startService(new Intent(this, BluetoothService.class));
                    }
                    editor.apply();
                }
                break;
            case R.id.multi_player_lan:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    Intent intent = new Intent(this, NsdPickerActivity.class);
                    startActivityForResult(intent, CHOOSE_NSD_SERVICE);

                    startService(new Intent(this, NsdService.class));
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setCancelable(false);
                    builder.setTitle("Error");
                    builder.setMessage("Your have old device");
                    builder.setPositiveButton(android.R.string.ok, null);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Intent i2 = new Intent(this, DevicePickerActivity.class);
                    startActivityForResult(i2, CHOOSE_BT_DEVICE);
                    // start Bluetooth service
                    startService(new Intent(this, BluetoothService.class));
                }
                break;
            case CHOOSE_BT_DEVICE:
                // bluetooth game finished, make an order
                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                if(!prefs.getBoolean(IS_BLUETOOTH_ENABLED, false)){
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    mBluetoothAdapter.disable();
                }
                // stop Bluetooth service
                stopService(new Intent(this, BluetoothService.class));
                break;
            case CHOOSE_NSD_SERVICE:
                stopService(new Intent(this, NsdService.class));
                break;
        }
    }
}