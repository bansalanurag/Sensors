package com.iiit.a3_mt17005;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "MainActivity";
    private SensorManager sensorManager;


    Database database;
    String acc_db, gyro_db, timestamp, location_db, wifi_db, cell_db, micro_db;

    TextView xValue, xValueG, xValueGps, mStatusView, wifi_textview, cell_textview;
    Switch aSwitch;

    //Location Variables
    private LocationManager locationManager;
    private LocationListener locationListener;

    //Mic Variables
    MediaRecorder mediaRecorder;
    Thread runner;
    Thread t;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    final Runnable updater = new Runnable() {

        public void run() {
            updateTv();
        }

        ;
    };
    final Handler mHandler = new Handler();
    final Handler mH1 = new Handler();


    //Wifi Variables
    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;
    private List<ScanResult> wifiList;

    //Cell Tower
    private TelephonyManager telecomManager;

    private Button export;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xValue = (TextView) findViewById(R.id.x_value);
        xValueG = (TextView) findViewById(R.id.x_value_g);
        xValueGps = (TextView) findViewById(R.id.x_value_gps);
        mStatusView = (TextView) findViewById(R.id.micro);
        cell_textview = (TextView) findViewById(R.id.tower);
        wifi_textview = (TextView) findViewById(R.id.wifi);
        export = (Button) findViewById(R.id.exportBtn);

        database = new Database(this);

        aSwitch = (Switch) findViewById(R.id.switch0);
        aSwitch.setOnCheckedChangeListener(MainActivity.this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            xValue.setText(String.format("%s\n%s\n%s", sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
            //acc_db = "X:" + sensorEvent.values[0] + " Y:" + sensorEvent.values[1] + " Z:" + sensorEvent.values[2];
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            xValueG.setText(String.format("%s\n%s\n%s", sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
            //gyro_db = "X:" + sensorEvent.values[0] + " Y:" + sensorEvent.values[1] + " Z:" + sensorEvent.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (aSwitch.isChecked()) {

            //Accelerometer
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }

            // Gyroscope
            Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroscope != null) {
                sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            }

            //GPS
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    xValueGps.setText(String.valueOf(location.getLatitude() + "\n" + String.valueOf(location.getLongitude())));
                    //location_db = "Longitude:" + location.getLongitude() + " Latitude:" + location.getLatitude();
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET
                    }, 10);
                    return;
                }
            }
            locationManager.requestLocationUpdates("gps", 1000, 0, locationListener);

            // MIC
            ActivityCompat.requestPermissions(this, permissions, 30);

            if (runner == null) {
                runner = new Thread() {
                    public void run() {
                        while (runner != null) {
                            try {
                                Thread.sleep(1000);
                                Log.i("Noise", "Tock");
                            } catch (InterruptedException e) {
                            }
                            ;
                            mHandler.post(updater);
                        }
                    }
                };
                runner.start();
                startRecorder();
                Log.d("Noise", "start runner()");
            }


            //WIFI
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!(wifiManager.isWifiEnabled())) {
                wifiManager.setWifiEnabled(true);
            }
            wifiScanReceiver = new WifiScanReceiver();
            wifiManager.startScan();
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            //CELL Tower
            telecomManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            telecomManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_CELL_INFO);

            if (t == null) {
                t = new Thread() {
                    @Override
                    public void run() {
                        while (t != null) {
                            try {
                                Thread.sleep(4000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        acc_db = xValue.getText().toString();
                                        gyro_db = xValueG.getText().toString();
                                        location_db = xValueGps.getText().toString();
                                        cell_db = cell_textview.getText().toString();
                                        wifi_db = wifi_textview.getText().toString();
                                        micro_db = mStatusView.getText().toString();

                                        Long tsLong = System.currentTimeMillis() / 1000;
                                        String ts = tsLong.toString();

                                        boolean inserted = database.addData(acc_db, gyro_db, location_db, cell_db, wifi_db, micro_db, ts);
                                        if (inserted)
                                            Toast.makeText(MainActivity.this, "Data Inserted", Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(MainActivity.this, "Data NOT Inserted", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };

                t.start();
            }


        } else {
            sensorManager.unregisterListener(this);
            locationManager.removeUpdates(locationListener);
            telecomManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            unregisterReceiver(wifiScanReceiver);
            if (runner != null) {
                stopRecorder();
                runner = null;
            }
            if (t != null) {
                t = null;
            }
            database.close();
        }
    }

    public void exportOnClickListener(View view) {
        Toast.makeText(MainActivity.this, "Exporting CSV, Please wait.", Toast.LENGTH_SHORT).show();
        exportDB();
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = wifiManager.getScanResults();
            wifi_textview.setText("");
            for (int i = 0; i < wifiScanList.size(); i++) {
                String info = ((wifiScanList.get(i)).toString());
                String t1[] = info.split(",");
                String t2 = t1[0];
                int sidx = t2.indexOf(":") + 1;
                String t3 = t2.substring(sidx);
                wifi_textview.append(t3 + "\n");
            }
            //wifi_db = wifi_textview.getText().toString();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            case 20:
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, 20);
                }
        }
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET
                }, 20);
                return;
            }
            List<CellInfo> cellinfo = telecomManager.getAllCellInfo();
            String strength = "", cellid = "";
            for (int i = 0; i < cellinfo.size(); i++) {
                String temp[] = cellinfo.get(i).toString().split(":");
                int cellidstart = temp[2].indexOf("{");
                int cellidend = temp[2].indexOf("{", cellidstart);
                cellid = temp[2].substring(cellidstart, cellidend);
                String cellidinfo[] = cellid.split(" ");
                for (String aCellidinfo : cellidinfo) {
                    cellid += aCellidinfo + "\n";
                }
                int endidx = temp[3].indexOf("}");
                strength = temp[3].substring(0, endidx);
            }

            if (telecomManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                final GsmCellLocation location = (GsmCellLocation) telecomManager.getCellLocation();
                if (location != null) {
                    cellid = String.valueOf(location.getCid());
                }
            }

            if (telecomManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                final CdmaCellLocation location = (CdmaCellLocation) telecomManager.getCellLocation();
                if (location != null) {
                    cellid = String.valueOf(location.getBaseStationId());
                }
            }

            Log.e("Cell ID:", cellid);
            cell_textview.setText(String.format("CellID:%s" + "\n" + "Signal:%s", cellid, strength));
            //cell_db = String.format("CellID:%s Signal:%s", cellid, strength);
        }
    };

    public void startRecorder() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile("/dev/null");
            try {
                mediaRecorder.prepare();
            } catch (java.io.IOException ioe) {
                android.util.Log.e("[Monkey]", "IOException: " +
                        android.util.Log.getStackTraceString(ioe));

            } catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }
            try {
                mediaRecorder.start();
            } catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }
        }
    }

    public void stopRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    public void updateTv() {
        mStatusView.setText(String.format("%s dB", Double.toString((getAmplitudeEMA()))));
        //micro_db = Double.toString((getAmplitudeEMA()));
    }

    public double getAmplitude() {
        if (mediaRecorder != null)
            return (mediaRecorder.getMaxAmplitude());
        else
            return 0;
    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }


    private void exportDB() {

        Database db_helper = new Database(this);
        File exportDir = new File(Environment.getExternalStorageDirectory(), "");


        if (!exportDir.exists()) {
            Toast.makeText(MainActivity.this, "Dir not present, Making New Dir", Toast.LENGTH_SHORT).show();
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "SensorReadings.csv");
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                            , 10);
                }
                return;
            }

            boolean y = file.createNewFile();
            if (y)
                Toast.makeText(MainActivity.this, "New File Created", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(MainActivity.this, "Overwriting Already Existing File", Toast.LENGTH_SHORT).show();

            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase DB = db_helper.getReadableDatabase();

            Cursor csv_cursor = DB.rawQuery("SELECT * FROM Sensors", null);
            csvWrite.writeNext(csv_cursor.getColumnNames());
            while (csv_cursor.moveToNext()) {
                String arrStr[] = {csv_cursor.getString(0), csv_cursor.getString(1), csv_cursor.getString(2), csv_cursor.getString(3), csv_cursor.getString(4), csv_cursor.getString(5), csv_cursor.getString(6), csv_cursor.getString(7)};
                csvWrite.writeNext(arrStr);
            }
            Toast.makeText(MainActivity.this, "Data exported", Toast.LENGTH_SHORT).show();
            csvWrite.close();
            csv_cursor.close();
        } catch (Exception sqlEx) {
            Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
        }
    }
}
