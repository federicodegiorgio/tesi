package fede.tesi.mqttplantanalyzer;

import static fede.tesi.mqttplantanalyzer.R.drawable.ic_baseline_brightness_high_24;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Checked extends AppCompatActivity {

    FusedLocationProviderClient mFusedLocationClient;
    int PERMISSION_ID = 44;
    Button button;
    Activity activity=this;
    String espId;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    double longitude;
    double latitude;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle=getIntent().getExtras();
        boolean connected=bundle.getBoolean("Result");
        mDatabase = FirebaseDatabase.getInstance("https://mqttplantanalyzer-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        setContentView(R.layout.connection_check);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        button=this.findViewById(R.id.conn_res_button);

        // method to get the location
        getLastLocation();
        Log.e("Intent result", String.valueOf(connected));
        Log.e("Intent result","EEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");

        if(!connected) {
            TextView result = this.findViewById(R.id.textView3);
            result.setText("Connection refused, retry to give Wifi credentials to Esp");
            ImageView imm=this.findViewById(R.id.imageView);
            imm.setImageResource(R.drawable.ic_baseline_close_24);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(activity, MyBluetoothActivity.class);
                    startActivity(i);
                }
            });

        }
        else if(connected) {
            TextView result = this.findViewById(R.id.textView3);
            result.setText("Esp is now connected, go to main menù to select it");
            ImageView imm=this.findViewById(R.id.imageView);
            imm.setImageResource(R.drawable.ic_baseline_wifi_24);

            if(bundle.getString("idEsp")!=null)
                espId=bundle.getString("idEsp");

            Log.e("Nel nuovo act",espId);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText("Do you want to make the position and data of this card public (check for true) ?");
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setLayoutParams( new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            EditText persEspName=new EditText(this);
            linearLayout.addView(persEspName);
            linearLayout.addView(checkBox);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setView(linearLayout);
            alertDialogBuilder.setTitle("Set the Card");
            alertDialogBuilder.setMessage("How you want to call this card ?");
            alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    //overriden below
                }
            });
            final AlertDialog dialog = alertDialogBuilder.create();

            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Boolean wantToCloseDialog = false;
                    Log.e("NUOVO NOME",persEspName.getText().toString());
                    String newName=persEspName.getText().toString();
                    Set<String> listBoard=new HashSet<>();
                    SharedPreferences sharedPref = activity.getSharedPreferences(user.getUid(),Context.MODE_PRIVATE);
                    if(sharedPref.getStringSet(user.getUid(), null)!=null) {
                        listBoard=sharedPref.getStringSet(user.getUid(), null);
                    }
                    else {
                        listBoard = new HashSet<>();
                    }
                    SharedPreferences.Editor editor = sharedPref.edit();
                    if(!newName.isEmpty()&&!newName.trim().isEmpty()){
                        wantToCloseDialog=true;
                    }
                    editor.putString(espId,newName);
                    editor.apply();
                    editor.putString(newName,espId);
                    editor.apply();

                    listBoard.add(espId);
                    editor.putStringSet(user.getUid(),listBoard);
                    editor.apply();

                    Log.e("PROVAAAA",sharedPref.getStringSet(user.getUid(),null).toString());
                    //Do stuff, possibly set wantToCloseDialog to true then...
                    if(wantToCloseDialog) {
                        if (checkBox.isChecked()) {
                            LatLangModel latlang=new LatLangModel();
                            latlang.setBoard(espId);
                            latlang.setLatitude(latitude);
                            latlang.setLongitude(longitude);
                            latlang.setName(user.getUid());
                            mDatabase.child("publicLocation").child(espId).setValue(latlang);
                        }
                        dialog.dismiss();
                    }
                    else {
                        @SuppressLint("WrongConstant") final Toast error_name_invalid = Toast.makeText(activity, "Error name invalid", 3000);
                        error_name_invalid.show();
                    }
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(activity, MainActivity.class);
                    startActivity(i);
                }
            });
        }
    }


        @SuppressLint("MissingPermission")
        private void getLastLocation() {
            // check if permissions are given
            if (checkPermissions()) {

                // check if location is enabled
                if (isLocationEnabled()) {

                    // getting last
                    // location from
                    // FusedLocationClient
                    // object
                    mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            Location location = task.getResult();
                            if (location == null) {
                                requestNewLocationData();
                            } else {
                                latitude=location.getLatitude();
                                longitude=location.getLongitude();
                                Log.e("LOC",location.getLatitude() + "");
                                //longitTextView.setText(location.getLongitude() + "");
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            } else {
                // if permissions aren't available,
                // request for permissions
                requestPermissions();
            }
        }

        @SuppressLint("MissingPermission")
        private void requestNewLocationData() {

            // Initializing LocationRequest
            // object with appropriate methods
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(5);
            mLocationRequest.setFastestInterval(0);
            mLocationRequest.setNumUpdates(1);

            // setting LocationRequest
            // on FusedLocationClient
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }

        private LocationCallback mLocationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location mLastLocation = locationResult.getLastLocation();
                Log.e("Latitude: ",mLastLocation.getLatitude() + "");
            }
        };

        // method to check for permissions
        private boolean checkPermissions() {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            // If we want background location
            // on Android 10.0 and higher,
            // use:
            // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        // method to request for permissions
        private void requestPermissions() {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
        }

        // method to check
        // if location is enabled
        private boolean isLocationEnabled() {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        // If everything is alright then
        @Override
        public void
        onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == PERMISSION_ID) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (checkPermissions()) {
                getLastLocation();
            }
        }

}