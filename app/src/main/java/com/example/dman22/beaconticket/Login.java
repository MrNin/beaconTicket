package com.example.dman22.beaconticket;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Path;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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


public class Login extends ActionBarActivity {

    //global variables
    public static final String EXTRA_MESSAGE = "com.example.dman22.ticketmaster.beacon.USER";

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

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getConnection() {
        return connection;
    }

    public void setConnection(int connection) {
        this.connection = connection;
    }


    String userName;
    String password;
    int statusCode;
    boolean passed;
    int connection;
    private SQLiteDatabase main;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        reset();

        //*** submit button ***//
        Button btnSubmit = (Button) findViewById(R.id.submitButton);
        btnSubmit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //accessing UI elements
                EditText UserName = (EditText) findViewById(R.id.userName);
                EditText Password = (EditText) findViewById(R.id.password);

                setUserName(UserName.getText().toString());
                setPassword(Password.getText().toString());


                if(getPassword().equals("1234") && !getUserName().equals("")) {
                    setPassed(false);
                    executeLogin();

                }else{
                    //Toast.makeText(beacon.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    noPass();
                }
            }
        });
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        reset();
    }

    //reseting login page
    public void reset(){
        //accessing UI elements
        EditText UserName = (EditText) findViewById(R.id.userName);
        EditText Password = (EditText) findViewById(R.id.password);
        TextView result = (TextView) findViewById(R.id.result);
        result.setText("");
        UserName.setText("");
        Password.setText("");

        setConnection(0);
        setPassed(false);
        setStatusCode(0);
        runInitiate();
    }

    //execute confirmation
    public void executeLogin(){
        //confirming user/password
        new  checkLoginAsyncTask().execute();
    }
    //move on to the main menu
    public void SubmitResponse(String user) {
        Intent intent = new Intent(this, MainMenu.class);
        Bundle b = new Bundle();
        b.putString("user", getUserName());
        b.putString("password", getPassword());
        b.putInt("connection", getConnection());

        //intent.putExtra(EXTRA_MESSAGE, user);
        intent.putExtras(b);
        startActivity(intent);
    }

    /***  Verifies if a Username and password are correct. ***/
    private class checkLoginAsyncTask extends AsyncTask<URL, Integer, String> {
        @Override
        protected String doInBackground(URL... url) {
            HttpResponse response = null;
            String body = "";

            // create HttpClient
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://54.200.138.139:8080/BeaconServlet/api/rest/Login");

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("username", userName));
            pairs.add(new BasicNameValuePair("password", password));
            try {
                post.setEntity(new UrlEncodedFormEntity(pairs));
                response = client.execute(post);
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                body = reader.readLine();
            } catch(Exception e) {
                return "Error: " + e.getMessage();
            }

            setStatusCode(response.getStatusLine().getStatusCode());
            if(!body.equals("Invalid Login") && !body.equals("")){
                setPassed(true);
            }

            return response.getStatusLine().toString() + " - " + body ;
        }

        protected void onPostExecute(String result) {
            Toast.makeText(Login.this, result, Toast.LENGTH_SHORT).show();
            if(getStatusCode() == 200 && isPassed()) {
                Toast.makeText(Login.this, "Access Granted", Toast.LENGTH_SHORT).show();
                runUpdate();
                SubmitResponse(userName);
            }else if(getStatusCode() == 404 || getStatusCode() == 400){
                setConnection(2);
                if(runCheck()){
                    runUpdate();
                    SubmitResponse(userName);
                }else {
                    noPass();
                }
            }else{
                setConnection(1);
                if(runCheck()){
                    runUpdate();
                    SubmitResponse(userName);
                }else {
                    noPass();
                }
            }
        }
    }

    /****       Local Database functions    ****/
    public void runInitiate(){
        //access database
        main = null;
        main = this.openOrCreateDatabase("TicketMaster", MODE_PRIVATE, null);
        main.execSQL("CREATE TABLE IF NOT EXISTS login (username TEXT, created TEXT, last_login TEXT);");
    }

    public boolean runCheck(){
        Cursor c = main.rawQuery("select * from login",null);
        int numRows = (int)(DatabaseUtils.queryNumEntries(main, "login"));
        int Column = c.getColumnIndex("username");

        c.moveToFirst();

        int current = 0;
        while(c != null && current != numRows){
            String temp = c.getString(Column);
            if(temp.equals(getUserName())){
                return true;
            }

            c.moveToNext();
            current++;
        }
        return false;
    }

    public void runUpdate() {
        if(runCheck())
            main.execSQL("update login set last_login = '" + getNow() + "' where username = '" + getUserName() + "'");
        else
            main.execSQL("insert into login (username, created, last_login) values ('" + getUserName() + "', '" + getNow() + "', '" + getNow() + "'); ");
        //main.execSQL("insert into login (username, created, last_login) values ( 'hello', 'hello', 'hello'); ");
    }

    private String getNow(){
        // set the format to sql date time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void noPass(){
        TextView result = (TextView) findViewById(R.id.result);
        result.setText("Incorrect\nCredentials");
    }




    @Override
    public void onDestroy(){
        super.onDestroy();
        if (main != null)
            main.close();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
