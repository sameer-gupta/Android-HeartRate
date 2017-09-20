package com.example.sameerg2.le_gap;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String TAG = "LEGAP";
    HashSet<BluetoothDevice> LeDevices;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner mBluetoothLeScanner;
    BluetoothLeAdvertiser LeAdvertiser;
    SensorManager mSensorManager;
    Sensor mSensor;
    private boolean started = false;
    private Handler mHandler = new Handler();
    public ParcelUuid ServiceUUID;
    TextView mText;
    String temp = "";
    String temp1 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // VVIP to provide runtime permission via user , 1 is just any number to be catched in onRequestPermission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},2);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS},5);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if( !BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported() ) {
            Toast.makeText( this, "Multiple advertisement not supported", Toast.LENGTH_SHORT ).show();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
        }

        mText = (TextView) findViewById( R.id.textView );
        LeDevices = new HashSet<BluetoothDevice>();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        LeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();


        ServiceUUID = new ParcelUuid(UUID.fromString( getString(R.string.ble_uuid )));
        mText.setText("");
        temp = "";

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mSensorManager!=null){
            mSensorManager.unregisterListener(SeventListner);
        }
    }

    public void advertise(View v) {
        mText.setText("");
        temp = "";

        LeAdvertiser.stopAdvertising(advertisingCallback);

        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(SeventListner, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.i(TAG, "LISTENER REGISTERED.");
    }

    public SensorEventListener SeventListner = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            String Rate="";

            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                Rate = "" + (int)event.values[0];
                //mTextViewHeart.setText(msg);
                Log.d(TAG, Rate);
            }
            else
                Log.d(TAG, "Unknown sensor type");

            LeAdvertiser.stopAdvertising(advertisingCallback);

            AdvertiseData data = new AdvertiseData.Builder()
                    //.setIncludeDeviceName( true )
                    .addServiceUuid( ServiceUUID )
                    .addServiceData( ServiceUUID, Rate.getBytes(Charset.forName("UTF-8") ) )
                    .build();


            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                    .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                    .setConnectable(false) // cos GAP in not connectable service
                    .setTimeout(900) // subjectable to change
                    .build();

            LeAdvertiser.startAdvertising( settings, data, advertisingCallback );

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
        }
    };

    public AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d( TAG, "Advertising onStartSuccess: ");
            started = true;
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e( TAG, "Advertising onStartFailure: " + errorCode );
            super.onStartFailure(errorCode);
        }
    };

    public void discover(View v) {
        mText.setText("");
        temp = "";

        mBluetoothLeScanner.stopScan(mScanCallback);

        List<ScanFilter> filters = new ArrayList<ScanFilter>();

        LeDevices.clear();
        mText.setText(" ");
        temp ="";

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid( ServiceUUID )
                .build();

        filters.add( filter );

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if( result == null || result.getDevice() == null)
                return;

            try{

                Map<ParcelUuid, byte[]> x =result.getScanRecord().getServiceData();
                if(x!=null)
                    for(ParcelUuid y:x.keySet()) {
                        Log.i(TAG,y.toString() + " %% ");
                        temp1 = new String(x.get(y));
                        if(!temp1.equals("0")){
                            mText.setText(new String(x.get(y)));
                        }
                        Log.i(TAG, new String(x.get(y)) + " && ");
                    }
                else
                    Log.i(TAG,"Returning NULL");
            }
            catch (Exception e){
                Log.d(TAG,"WAS NULL");
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.e( TAG, "Batch result");
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }
    };


/*
    // for heart rate implementation

    private SensorManager mSensorManager;
    private Sensor mSensor;

    public void Disconn_btn(View v){
        //start_adv();
        TextView mTextViewHeart = (TextView) findViewById(R.id.heart);
    }

    public SensorEventListener SeventListner = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            TextView mTextViewHeart = (TextView) findViewById(R.id.heart);
            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                String msg = "" + (int)event.values[0];
                mTextViewHeart.setText(msg);
                Log.d(TAG, msg);
            }
            else
                Log.d(TAG, "Unknown sensor type");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
        }
    };
*/



}
