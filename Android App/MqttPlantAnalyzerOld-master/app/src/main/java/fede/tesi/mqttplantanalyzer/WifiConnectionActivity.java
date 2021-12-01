package fede.tesi.mqttplantanalyzer;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static com.welie.blessed.BluetoothBytesParser.bytes2String;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ConnectionPriority;
import com.welie.blessed.GattStatus;
import com.welie.blessed.WriteType;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import fede.tesi.mqttplantanalyzer.databinding.ActivityMainBinding;
import timber.log.Timber;

public class WifiConnectionActivity extends AppCompatActivity {
    private static final UUID SERVICE_UUID=UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    public BluetoothPeripheral seldev;
    public BluetoothCentralManager central;

    WifiManager wifi;
    ListView lv;
    TextView textStatus;
    Button buttonScan;
    int size = 0;
    List<ScanResult> results;
    String ITEM_KEY = "key";
    ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    SimpleAdapter adapter;

    public String ssid_Text = "";
    public String pwd_Text = "";
    public View view;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_list);
        lv = (ListView)findViewById(R.id.wifilist);

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            Toast.makeText(WifiConnectionActivity.this, "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }
        Toast.makeText(WifiConnectionActivity.this, "Scanning...." , Toast.LENGTH_SHORT).show();
        wifi.startScan();
        this.adapter = new SimpleAdapter(WifiConnectionActivity.this , arraylist, R.layout.row, new String[] { ITEM_KEY }, new int[] { R.id.listwifi_value });
        lv.setAdapter(this.adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String item = (String) arraylist.get(i).get(ITEM_KEY);
                ssid_Text= item;
                AlertDialog.Builder builder = new AlertDialog.Builder(WifiConnectionActivity.this);
                builder.setTitle("Password per "+ssid_Text);

                // Set up the input
                final EditText input = new EditText(WifiConnectionActivity.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);

// Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pwd_Text = input.getText().toString();
                        Intent intent = new Intent();
                        intent.putExtra("ssid", ssid_Text);
                        intent.putExtra("pwd", pwd_Text);
                        setResult(RESULT_OK, intent);
                        finish();

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                results = wifi.getScanResults();
                size = results.size();
                arraylist.clear();
                try {
                    size = size - 1;
                    while (size >= 0) {
                        HashMap<String, String> item = new HashMap<String, String>();
                        if(results.get(size).SSID!=null && !results.get(size).SSID.equals("")) {
                            if(!item.containsValue(results.get(size).SSID)) {
                                item.put(ITEM_KEY, results.get(size).SSID);

                                arraylist.add(item);
                            }
                        }
                        size--;
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                }
                adapter.notifyDataSetChanged();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


    }


}
