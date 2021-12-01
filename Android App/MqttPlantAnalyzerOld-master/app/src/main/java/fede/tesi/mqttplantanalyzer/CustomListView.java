package fede.tesi.mqttplantanalyzer;

import java.util.ArrayList;

import android.os.Bundle;
import android.widget.ListView;
import android.app.Activity;

public class CustomListView extends Activity {
    ArrayList<Chart_Type> imageArry = new ArrayList<Chart_Type>();
    ChartTypeAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_chart_list);
        // add image and text in arraylist
        imageArry.add(new Chart_Type(R.drawable.ic_baseline_cloud_24, "Luminosity"));
        imageArry.add(new Chart_Type(R.drawable.ic_baseline_cloud_24, "Temperature"));
        imageArry.add(new Chart_Type(R.drawable.ic_baseline_cloud_24, "Moisture"));
        imageArry.add(new Chart_Type(R.drawable.ic_baseline_cloud_24, "Image"));
        // add data in contact image adapter
        adapter = new ChartTypeAdapter(this, R.layout.sensor_row, imageArry);
        ListView dataList = (ListView) findViewById(R.id.chartList);
        dataList.setAdapter(adapter);
    }
}