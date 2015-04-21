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
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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
import org.json.JSONException;
import org.json.JSONObject;

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

    private static final String TAG = MainMenu.class.getSimpleName();

    private BeaconManager beaconManager;
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId",ESTIMOTE_PROXIMITY_UUID,54212,511);//6245,46072);

    //global variables

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTicketResponse() {
        return ticketResponse;
    }

    public void setTicketResponse(String ticketResponse) {
        this.ticketResponse = ticketResponse;
    }

    public String eventID;
    public String userName;
    public String password;
    public int statusCode;
    public boolean status;
    public String userId;
    public String ticketResponse;

    public Bundle b;
    private SQLiteDatabase main;
    private boolean check = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Get the message from the intent
        Intent intent = getIntent();
        b = intent.getExtras();
        //run settup function
        settup();

        //switch controls
        Switch toggle = (Switch) findViewById(R.id.switcher);
        toggle.setChecked(true);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Button btnStatus = (Button) findViewById(R.id.status);
                if (isChecked) {
                    // Enable beacon connection
                    if(!status){
                        setStatus(true);

                        Resources res = getResources();
                        Drawable myImage = res.getDrawable(R.drawable.status_on);
                        btnStatus.setBackground(myImage);
                    }
                } else {
                    // Disable beacon connection
                    if(status){
                        setStatus(false);

                        Resources res = getResources();
                        Drawable myImage = res.getDrawable(R.drawable.status_off);
                        btnStatus.setBackground(myImage);
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!check && status) {
                            Button btnTest = (Button) findViewById(R.id.result);
                            btnTest.setBackgroundColor(Color.GREEN);
                            Toast.makeText(MainMenu.this, "sending", Toast.LENGTH_SHORT).show();
                            new setTicketAsyncTask().execute();
                            check = true;
                        }
                    }
                });
            }

            @Override
            public void onExitedRegion(Region region) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run(){
                        Button btnTest = (Button) findViewById(R.id.result);
                        btnTest.setBackgroundColor(Color.RED);
                        check = false;
                    }
                });
            }
        });
    }

    //run on create
    public void settup() {
        //initializing userName, password, and eventID
        setUserName(b.getString("user"));
        setPassword(b.getString("password"));
        setUserId(b.getString("userId"));
        setEventID("N/A");
        setStatus(true);
        runInitiate();

        //display username
        TextView UserName = (TextView) findViewById(R.id.UserNameLabel);
        UserName.setText(getUserName());

    }

    //***   notification handler    ***//
    public void notification(String update){
        /*Intent intent = new Intent(this, MainMenu.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, 0);*/

        Notification notifications = new Notification.Builder(this)
                .setContentTitle(update)
                .setContentText("Open BeaconTicket App for more.").setSmallIcon(R.drawable.ic_launcher)
                //.setContentIntent(pending)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifications.flags |= Notification.FLAG_AUTO_CANCEL;
        manager.notify(0, notifications);
    }

    private class setTicketAsyncTask extends AsyncTask<java.net.URL, Integer, String> {
        private boolean isOnline = true;
        private boolean passed = false;

        @Override
        protected String doInBackground(URL... url) {
            HttpResponse response = null;
            String body ="";
            // create HttpClient
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://54.200.138.139:8080/BeaconServlet/api/rest/Roster/uuid");

            //sending input pararmeters
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("uuid", ESTIMOTE_PROXIMITY_UUID));
            pairs.add(new BasicNameValuePair("userId", getUserId()));
            try {
                post.setEntity(new UrlEncodedFormEntity(pairs));
                response = client.execute(post);
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                body = reader.readLine();
            } catch(Exception e) {
                isOnline = false;
                return "Error: " + e.getMessage();
            }

            setStatusCode(response.getStatusLine().getStatusCode());
            if(body != null)
                passed = parser(body);
            return body ;
        }

        protected void onPostExecute(String result) {
            //Toast.makeText(MainMenu.this, result, Toast.LENGTH_SHORT).show();
            if(getStatusCode() == 200) {
                if(passed){
                    if(getTicketResponse().equals("Successfully Scanned Entry"))
                        runUpdate("Entered");
                    else
                        runUpdate("Left");
                    updateEventInfo();
                    notification(getTicketResponse());
                }else{
                    noPass(isOnline,true);
                }
            }else if(getStatusCode() == 404 || getStatusCode() == 400){
                noPass(isOnline,false);
                notification("Couldn't connect to server.");
            }else{
                noPass(isOnline,false);
                notification("Couldn't connect to internet.");
            }
        }
    }

    /***  Json Parser ***/
    public boolean parser(String input){
        boolean response = false;

        try {
            JSONObject results = new JSONObject(input);
            setEventID(results.getString("eventName"));
            response = results.getBoolean("didSucceedScan");
            setTicketResponse(results.getString("response"));
        }
        catch(JSONException ex) {
            ex.printStackTrace();
        }

        return response;
    }

    /****       Local Database functions    ****/
    public void runInitiate(){
        //access database
        main = null;
        main = this.openOrCreateDatabase("BeaconMaster", MODE_PRIVATE, null);
        main.execSQL("CREATE TABLE IF NOT EXISTS eventRecord (username TEXT, eventID TEXT, entered TEXT, status TEXT);");
    }

    public boolean runCheck(){
        Cursor c = main.rawQuery("select * from event",null);
        int numRows = (int)(DatabaseUtils.queryNumEntries(main, "eventRecord"));
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

    public void runUpdate(String status) {
        String insert = "insert into eventRecord (username, eventID, entered, status) values ('" + getUserName() + "', '" + getEventID() + "', '" + getNow() + "', '" + status + "'); ";
        main.execSQL(insert);
    }

    private String getNow(){
        // set the format to sql date time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    //happens upon successful scan
    public void updateEventInfo() {
        TextView eventTime = (TextView) findViewById(R.id.eventTime);
        TextView eventName = (TextView) findViewById(R.id.eventName);
        TextView eventResponse = (TextView) findViewById(R.id.eventResponse);

        eventTime.setText(getNow());
        eventName.setText(getEventID());
        eventResponse.setText(getTicketResponse());
    }

    //happens on non-successful scan
    public void noPass(boolean connected, boolean server){
        //textviews to be manipulated
        TextView eventTime = (TextView) findViewById(R.id.eventTime);
        TextView eventName = (TextView) findViewById(R.id.eventName);
        TextView eventResponse = (TextView) findViewById(R.id.eventResponse);

        //internet but no success
        if(connected){

            if(server) {
                eventTime.setText(getNow());
                eventResponse.setText(getTicketResponse());
                eventName.setText(getEventID());
            }
            else {
                eventTime.setText("N/A");
                eventName.setText("N/A");
                eventResponse.setText("Can't connect to server. (" + getStatusCode() + ")");
            }
        }//no internet
        else{
            eventTime.setText("N/A");
            eventName.setText("N/A");
            eventResponse.setText("Can't connect to internet.");
        }
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
