package fede.tesi.mqttplantanalyzer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;


import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.annotations.NotNull;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import fede.tesi.mqttplantanalyzer.databinding.ActivityMainBinding;
import timber.log.Timber;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int ACCESS_LOCATION_REQUEST = 2;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static final String TAG = "MyActivity";
    private FirebaseAuth auth;
    //LineChart chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set up mqtt connection

        registerReceiver(locationServiceStateReceiver, new IntentFilter((LocationManager.MODE_CHANGED_ACTION)));
        Intent intent = new Intent(this, MyMqttService.class);
        startService(intent);
        checkPermissions();
        checkLocationServices();
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());

        // Create and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            signInLauncher.launch(signInIntent);
        }
        else
            Log.e("FirebaseUser =", auth.getUid());
        SharedPreferences sharedPref = this.getSharedPreferences(auth.getUid(),Context.MODE_PRIVATE);
        if(sharedPref.getStringSet(auth.getUid(), null)!=null)
            Log.e("PRESOOOOOO",sharedPref.getStringSet(auth.getUid(), null).toString());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            FirebaseAuth.getInstance().signOut();
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build());
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build();
            auth = FirebaseAuth.getInstance();
            signInLauncher.launch(signInIntent);
            return true;
        }
        if (id == R.id.action_bt) {
            Intent i =new Intent(this , MyBluetoothActivity.class);
            this.startActivity(i);
            return true;
        }
        if (id == R.id.action_map) {
            Intent i =new Intent(this , MyMapFragment.class);
            this.startActivity(i);
            return true;
        }
        Log.e("MENU TOUCHED", String.valueOf(id));

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = getBluetoothManager().getAdapter();
        if(bluetoothAdapter == null) return false;

        return bluetoothAdapter.isEnabled();
    }


    @NotNull
    private BluetoothManager getBluetoothManager() {
        return Objects.requireNonNull((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE),"cannot get BluetoothManager");
    }


    private final BroadcastReceiver locationServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(LocationManager.MODE_CHANGED_ACTION)) {
                boolean isEnabled = areLocationServicesEnabled();
                Timber.i("Location service state changed to: %s", isEnabled ? "on" : "off");
                checkPermissions();
            }
        }
    };

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] missingPermissions = getMissingPermissions(getRequiredPermissions());
            if (missingPermissions.length > 0) {
                requestPermissions(missingPermissions, ACCESS_LOCATION_REQUEST);
            } else {
                permissionsGranted();
            }
        }
    }

    private String[] getMissingPermissions(String[] requiredPermissions) {
        List<String> missingPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String requiredPermission : requiredPermissions) {
                if (getApplicationContext().checkSelfPermission(requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(requiredPermission);
                }
            }
        }
        return missingPermissions.toArray(new String[0]);
    }

    private String[] getRequiredPermissions() {
        int targetSdkVersion = getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        } else return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    private void permissionsGranted() {
        // Check if Location services are on because they are required to make scanning work for SDK < 31
        int targetSdkVersion = getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && targetSdkVersion < Build.VERSION_CODES.S)
            return;
    }

    private boolean areLocationServicesEnabled() {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            //Timber.e("could not get location manager");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            return isGpsEnabled || isNetworkEnabled;
        }
    }

    private boolean checkLocationServices() {
        if (!areLocationServicesEnabled()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Location services are not enabled")
                    .setMessage("Scanning for Bluetooth peripherals requires locations services to be enabled.") // Want to enable?
                    .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // if this button is clicked, just close
                            // the dialog box and do nothing
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Check if all permission were granted
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            permissionsGranted();
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Permission is required for scanning Bluetooth peripherals")
                    .setMessage("Please grant permissions")
                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            checkPermissions();
                        }
                    })
                    .create()
                    .show();
        }
    }

    // See: https://developer.android.com/training/basics/intents/result
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Log.i("AUTH", user.getUid());
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }

}