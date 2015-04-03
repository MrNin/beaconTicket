package com.example.dman22.beaconticket;


import android.bluetooth.BluetoothAdapter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.estimote.sdk.BeaconManager.MonitoringListener;
public class MainMenu extends ActionBarActivity {
    String test = "hello";
    private static final String TAG = MainMenu.class.getSimpleName();

    private BeaconManager beaconManager;
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId",ESTIMOTE_PROXIMITY_UUID,54212,511);//6245,46072);
    //test for commit
    //global variables
    public static final String URL = "http://54.200.138.139:8080/BeaconServlet/api/rest/ticket";

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String eventID;
    public String userName;
    public String password;
    public int statusCode;
    public boolean status;
    public Bundle b;
    private SQLiteDatabase main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Get the message from the intent
        Intent intent = getIntent();
        //String message = intent.getStringExtra(beacon.EXTRA_MESSAGE);
        b = intent.getExtras();
        //show message
        TextView UserName = (TextView) findViewById(R.id.UserNameLabel);

        //initializing userName, password, and eventID
        //setUserName(message);
        setUserName(b.getString("user"));
        //setPassword("1234");
        setPassword(b.getString("password"));
        setEventID("1");
        setStatus(false);
        runInitiate();

        //UserName.setText(message);
        UserName.setText(getUserName());

        //determining status of bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Button btnStatus = (Button) findViewById(R.id.status);
        Switch toggle = (Switch) findViewById(R.id.switcher);
        if(mBluetoothAdapter.isEnabled()) {
            toggle.setChecked(true);
            //btnStatus.setText("ON");
            Resources res = getResources();
            Drawable myImage = res.getDrawable(R.drawable.status_on);
            btnStatus.setBackground(myImage);
        }
        else if(!mBluetoothAdapter.isEnabled()) {
            toggle.setChecked(false);
            //btnStatus.setText("OFF");
            Resources res = getResources();
            Drawable myImage = res.getDrawable(R.drawable.status_off);
            btnStatus.setBackground(myImage);
        }

        //switch controls

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Button btnStatus = (Button) findViewById(R.id.status);
                if (isChecked) {
                    // Enable bluetooth
                    if(!status){
                        setStatus(true);
                        //btnStatus.setText("ON");

                        Resources res = getResources();
                        Drawable myImage = res.getDrawable(R.drawable.status_on);
                        btnStatus.setBackground(myImage);
                        blueTooth(true);
                    }
                } else {
                    // Disable bluetooth
                    if(status){
                        setStatus(false);
                        //btnStatus.setText("OFF");

                        Resources res = getResources();
                        Drawable myImage = res.getDrawable(R.drawable.status_off);
                        btnStatus.setBackground(myImage);
                        blueTooth(false);
                    }
                }
            }
        });


        beaconManager = new BeaconManager(this);
        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1),0);
        beaconManager.setRangingListener(new BeaconManager.RangingListener(){
            @Override public void onBeaconsDiscovered(Region region, List<Beacon> beacons){
                Log.d(TAG, "Ranged beacons:" + beacons);
            }
        });

        beaconManager.setMonitoringListener(new MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> beacons) {
                /*RelativeLayout layout = (RelativeLayout) findViewById(R.id.my_layout);
                layout.setBackgroundColor(Color.GREEN);*/
                Button btnTest = (Button) findViewById(R.id.result);
                btnTest.setBackgroundColor(Color.GREEN);
                Toast.makeText(MainMenu.this, "sending", Toast.LENGTH_SHORT).show();
                new setTicketAsyncTask().execute();
            }

            @Override
            public void onExitedRegion(Region region) {
                /*RelativeLayout layout = (RelativeLayout) findViewById(R.id.my_layout);
                layout.setBackgroundColor(Color.RED);*/
                Button btnTest = (Button) findViewById(R.id.result);
                btnTest.setBackgroundColor(Color.RED);
                Button btnStatus = (Button) findViewById(R.id.status);
                setStatus(false);
                //btnStatus.setText("OFF");
                Resources res = getResources();
                Drawable myImage = res.getDrawable(R.drawable.status_off);
                btnStatus.setBackground(myImage);
                blueTooth(false);
                Switch toggle = (Switch) findViewById(R.id.switcher);
                toggle.setChecked(false);

            }
        });
    }

    public void blueTooth(boolean status){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            // Device does support Bluetooth
            if (!mBluetoothAdapter.isEnabled() && status) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                //mBluetoothAdapter.enable();
            }else if(mBluetoothAdapter.isEnabled() && !status){
                mBluetoothAdapter.disable();
            }
        }
    }

    private class setTicketAsyncTask extends AsyncTask<java.net.URL, Integer, String> {
        @Override
        protected String doInBackground(URL... url) {
            HttpResponse response = null;
            String body ="";
            // create HttpClient
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://54.200.138.139:8080/BeaconServlet/api/rest/Roster");

            String err = getPassword();

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("username", getUserName()));
            pairs.add(new BasicNameValuePair("eventId", getEventID()));
            try {
                post.setEntity(new UrlEncodedFormEntity(pairs));
                response = client.execute(post);
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                body = reader.readLine();
            } catch(Exception e) {
                return "Error: " + e.getMessage();
            }

            setStatusCode(response.getStatusLine().getStatusCode());
            return response.getStatusLine().toString() + " - " + body ;
        }

        protected void onPostExecute(String result) {
            Toast.makeText(MainMenu.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    /****       Local Database functions    ****/
    public void runInitiate(){
        //access database
        main = null;
        main = this.openOrCreateDatabase("TicketMaster", MODE_PRIVATE, null);
        main.execSQL("CREATE TABLE IF NOT EXISTS event (username TEXT, eventID TEXT, entered TEXT);");
    }

    public boolean runCheck(){
        Cursor c = main.rawQuery("select * from event",null);
        int numRows = (int)(DatabaseUtils.queryNumEntries(main, "event"));
        int Column1 = c.getColumnIndex("username");
        int Column2 = c.getColumnIndex("EventID");

        c.moveToFirst();

        int current = 0;
        while(c != null && current != numRows){
            String temp1 = c.getString(Column1);
            String temp2 = c.getString(Column2);
            if(temp1.equals(getUserName()) && temp2.equals(getEventID())){
                return true;
            }

            c.moveToNext();
            current++;
        }
        return false;
    }

    public void runUpdate() {
        if(!runCheck())
            main.execSQL("insert into login (username, eventID, entered) values ('" + getUserName() + "', '" + getEventID() + "', '" + getNow() + "'); ");
        //main.execSQL("insert into login (username, created, last_login) values ( 'hello', 'hello', 'hello'); ");
    }

    private String getNow(){
        // set the format to sql date time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //noinspection SimplifiableIfStatement
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        beaconManager.connect(new BeaconManager.ServiceReadyCallback(){
            @Override
            public void onServiceReady(){
                try {
                    beaconManager.startMonitoring(ALL_ESTIMOTE_BEACONS);
                }catch (RemoteException e){
                    Log.d(TAG, "Error while starting monitoring");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        beaconManager.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if device supports Bluetooth Low Energy.
        if (!beaconManager.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!beaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
            /* for the GUI */
            Button btnStatus = (Button) findViewById(R.id.status);
            Switch toggle = (Switch) findViewById(R.id.switcher);
            toggle.setChecked(true);
            Resources res = getResources();
            Drawable myImage = res.getDrawable(R.drawable.status_on);
            btnStatus.setBackground(myImage);
        } else {
            connectToService();
        }
    }

    @Override
    protected void onStop() {
        try{
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
        }catch (RemoteException e){
            Log.d(TAG,"Error while stopping ranging",e);
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                connectToService();
            }else{
                Toast.makeText(this,"Bluetooth not enabled",Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService(){
        beaconManager.connect(new BeaconManager.ServiceReadyCallback(){
            @Override public void onServiceReady(){
                try{
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
                }catch (RemoteException e){
                    Toast.makeText(MainMenu.this,"Cannot start ranging",Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }


}
