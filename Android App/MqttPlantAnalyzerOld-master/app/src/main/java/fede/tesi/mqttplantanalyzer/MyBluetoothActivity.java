package fede.tesi.mqttplantanalyzer;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static com.welie.blessed.BluetoothBytesParser.bytes2String;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ConnectionPriority;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.ScanFailure;
import com.welie.blessed.WriteType;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.UUID;

import timber.log.Timber;


public class MyBluetoothActivity extends AppCompatActivity {

    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    private static final int WIFI_ACTIVITY_REQUEST_CODE = 0;

    public String ssid_Text = "";
    public String pwd_Text = "";
    public LinkedList<BluetoothPeripheral> btList = new LinkedList<>();
    public BtRecyclerViewAdapter adapterr;
    public BluetoothPeripheral seldev;
    public BluetoothCentralManager central;
    public RecyclerView recyclerView;
    public View view;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;
    private Activity activity = this;
    public boolean connectedEsp=false;
    public String idEsp;
    final LoadingDialog loadingdialog = new LoadingDialog(MyBluetoothActivity.this);
    BluetoothPeripheral peripheralEsp;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_first);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        central = new BluetoothCentralManager(this, this.bluetoothCentralManagerCallback, new Handler(Looper.getMainLooper()));
        recyclerView = findViewById(R.id.my_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapterr = new BtRecyclerViewAdapter(btList);

        Button scan=(Button) findViewById(R.id.button_first);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btList.clear();
                central.scanForPeripherals();

                adapterr = new BtRecyclerViewAdapter(btList);
                recyclerView.setAdapter(adapterr);
                Handler handler = new Handler();

                final LoadingDialog loadingdialog = new LoadingDialog(MyBluetoothActivity.this);
                // invoking startLoadingDialog method
                loadingdialog.startLoadingdialog();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("Test Stop", String.valueOf(connectedEsp));

                        // after 4 seconds
                        loadingdialog.dismissdialog();
                        // starting finished activity
                        central.stopScan();
                    }
                }, 1500); // 1,5 seconds
            }
        });
        recyclerView.setAdapter(adapterr);
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener (this, recyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        seldev=btList.get(position);
                        peripheralEsp=seldev;
                        central.stopScan();
                        central.connectPeripheral(seldev, peripheralCallback);
                        Intent i = new Intent(activity, WifiConnectionActivity.class);
                        startActivityForResult(i,WIFI_ACTIVITY_REQUEST_CODE);

                    }

                    @Override public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );







        BluetoothConfiguration config = new BluetoothConfiguration();
        config.context = this.getApplicationContext();
        config.bluetoothServiceClass = BluetoothClassicService.class;
        config.bufferSize = 1024;
        config.characterDelimiter = '\n';
        config.deviceName = "MqttPlantAnalyzer";
        config.callListenersInMainThread = true;
        // Required
        config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        BluetoothService.init(config);

    }


    // Callback for peripherals
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {
            // Request a higher MTU, iOS always asks for 185
            peripheral.requestMtu(185);


            // Request a new connection priority
            peripheral.requestConnectionPriority(ConnectionPriority.HIGH);

            Log.i("Post time to" , peripheral.getName());
            // Read manufacturer and model number from the Device Information Service
            peripheral.readCharacteristic(SERVICE_UUID, CHARACTERISTIC_UUID);

            peripheral.setNotify(SERVICE_UUID, CHARACTERISTIC_UUID, true);
            // Turn on notifications for Current Time Service and write it if possible
            BluetoothGattCharacteristic usernameCharacteristic = peripheral.getCharacteristic(SERVICE_UUID, CHARACTERISTIC_UUID);
            if (usernameCharacteristic != null) {
                peripheral.setNotify(usernameCharacteristic, true);

                // If it has the write property we write the current time
                if ((usernameCharacteristic.getProperties() & PROPERTY_WRITE) > 0) {
                    BluetoothBytesParser parser = new BluetoothBytesParser();
                    String p = "$" + user.getUid();
                    parser.setString(p);
                    peripheral.writeCharacteristic(usernameCharacteristic, parser.getValue(), WriteType.WITH_RESPONSE);
                }
            }
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(activity, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            Location location = task.getResult();
                            if(location!=null)
                                Log.e("Location",location.toString());
                            else
                                Log.e("Location","Location not found");
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
                                //mDatabase.child("Locations").child("Latitude").child(String.valueOf(location.getLatitude()))
                                //        .child("Longitude").child(String.valueOf(location.getLongitude())).setValue(user.getUid());
                                mDatabase.child("users").child(user.getUid()).child("schede").child("val").child("Longitude").setValue(location.getLongitude());
                                mDatabase.child("users").child(user.getUid()).child("schede").child("val").child("Latitude").setValue(location.getLatitude());

                            }
                        }
                    });

        }

        @Override
        public void onNotificationStateUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                final boolean isNotifying = peripheral.isNotifying(characteristic);
                Timber.i("SUCCESS: Notify set to '%s' for %s", isNotifying, characteristic.getUuid());
            }
                else {
                    Timber.e("ERROR: Changing notification state failed for %s (%s)", characteristic.getUuid(), status);
                }
        }

        @Override
        public void onCharacteristicWrite(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                Timber.i("SUCCESS: Writing <%s> to <%s>", bytes2String(value), characteristic.getUuid());
            } else {
                Timber.i("ERROR: Failed writing <%s> to <%s> (%s)", bytes2String(value), characteristic.getUuid(), status);
            }
        }

        @Override
        public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            String str = new String(value, StandardCharsets.UTF_8); // for UTF-8 encodingbytes2
            Log.e("CHAR UPDATE=",str);
            if(str.equals("Connected")) {
                connectedEsp = true;
            }
            if(str.startsWith("$")) {
                idEsp = str.substring(1);
            }

            Log.e("CHAR UPDATE=", String.valueOf(connectedEsp));



            /*if (status != GattStatus.SUCCESS) return;

            UUID characteristicUUID = characteristic.getUuid();
            BluetoothBytesParser parser = new BluetoothBytesParser(value);

            if (characteristicUUID.equals(BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                BloodPressureMeasurement measurement = new BloodPressureMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_BLOODPRESSURE);
                intent.putExtra(MEASUREMENT_BLOODPRESSURE_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
                Timber.d("%s", measurement);
            }*/
        }

        @Override
        public void onMtuChanged(@NotNull BluetoothPeripheral peripheral, int mtu, @NotNull GattStatus status) {
            Timber.i("new MTU set: %d", mtu);
        }
    };





    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            Timber.i("connected to '%s'", peripheral.getName());
            central.stopScan();
        }

        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            Timber.e("connection '%s' failed with status %s", peripheral.getName(), status);
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull final BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            Timber.i("disconnected '%s' with status %s", peripheral.getName(), status);

        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            Timber.i("Found peripheral '%s'", peripheral.getName());
            //btList.add(peripheral);
            boolean ad=false;
            Log.e("Confronto",peripheral.toString());

            if(peripheral!=null&&peripheral.getName()!=null&&!peripheral.getName().equals("")) {
                ad=true;
                int i=0;
                for (BluetoothPeripheral b : btList) {
                    if (b.getName().equals(peripheral.getName())) ad = false;
                    Log.e("Confronto",b.getName()+"    skip"+i+"    "+peripheral.getName());
                    i++;
                }
                if(ad)
                    btList.add(peripheral);
            }
            for (BluetoothPeripheral b : btList)
            Log.e("LIST",b.getName());
            adapterr = new BtRecyclerViewAdapter(btList);
            recyclerView.setAdapter(adapterr);
        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            Timber.i("bluetooth adapter changed state to %d", state);
            if (state == BluetoothAdapter.STATE_ON) {
                // Bluetooth is on now, start scanning again
                // Scan for peripherals with a certain service UUIDs
                //central.startPairingPopupHack();
                central.scanForPeripherals();
            }

        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {
            Timber.i("scanning failed with error %s", scanFailure);

            BluetoothAdapter.getDefaultAdapter().disable();


            BluetoothAdapter.getDefaultAdapter().enable();
        }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check that it is the SecondActivity with an OK result
        if (requestCode == WIFI_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                // get String data from Intent
                ssid_Text = data.getStringExtra("ssid");
                pwd_Text = data.getStringExtra("pwd");
                Log.i("INTERNET CREDENTIAL=", ssid_Text+ "  "+pwd_Text);
                // send ssid e pwd
                // Turn on notifications for Current Time Service and write it if possible
                BluetoothGattCharacteristic ssidCharacteristic = seldev.getCharacteristic(SERVICE_UUID, CHARACTERISTIC_UUID);
                if (ssidCharacteristic != null) {
                    seldev.setNotify(ssidCharacteristic, true);

                    // If it has the write property we write the current time
                    if ((ssidCharacteristic.getProperties() & PROPERTY_WRITE) > 0) {
                        BluetoothBytesParser parser = new BluetoothBytesParser();
                        String p="!"+ssid_Text;
                        parser.setString(p);
                        seldev.writeCharacteristic(ssidCharacteristic, parser.getValue(), WriteType.WITH_RESPONSE);
                    }
                }

                BluetoothGattCharacteristic pwdCharacteristic = seldev.getCharacteristic(SERVICE_UUID, CHARACTERISTIC_UUID);
                if (pwdCharacteristic != null) {
                    seldev.setNotify(pwdCharacteristic, true);

                    // If it has the write property we write the current time
                    if ((pwdCharacteristic.getProperties() & PROPERTY_WRITE) > 0) {
                        BluetoothBytesParser parser = new BluetoothBytesParser();
                        String p="?"+pwd_Text;
                        parser.setString(p);
                        seldev.writeCharacteristic(pwdCharacteristic, parser.getValue(), WriteType.WITH_RESPONSE);
                    }
                }

            }
        }
        // invoking startLoadingDialog method
        loadingdialog.startLoadingdialog();

        Intent i = new Intent(activity, Checked.class);
        // using handler class to set time delay methods
        Handler handler = new Handler();

        Log.e("PROVA", String.valueOf(connectedEsp));
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // after 4 seconds
                    Log.e("what", String.valueOf(connectedEsp));
                    i.putExtra("Result", connectedEsp);
                    if (idEsp != null)
                        i.putExtra("idEsp", idEsp);
                    loadingdialog.dismissdialog();
                    // starting finished activity
                    startActivity(i);
                    if(connectedEsp&&idEsp!=null)
                        central.cancelConnection(peripheralEsp);
            }

        }, 12000); // 10 seconds
    }


}