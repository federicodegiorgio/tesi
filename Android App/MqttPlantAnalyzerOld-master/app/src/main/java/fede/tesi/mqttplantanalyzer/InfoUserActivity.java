package fede.tesi.mqttplantanalyzer;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InfoUserActivity extends AppCompatActivity {

    private LayoutInflater inflater;
    private DatabaseReference mDatabase;
    MqttValue val;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef;
    ImageView image;
    byte[] imgbytes;
    Context context;
    final long ONE_MEGABYTE = 1024 * 1024;
    List<Entry> lumiList = new ArrayList<>();
    List<Entry> humiList = new ArrayList<>();
    List<Entry> moisList = new ArrayList<>();
    List<Entry> tempList = new ArrayList<>();
    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    LineChart lineChart;
    LineData lineData;
    LineDataSet  luxDataSet;
    LineDataSet  humDataSet;
    LineDataSet  temDataSet;
    LineDataSet  moiDataSet;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle=getIntent().getExtras();
        String user=bundle.getString("user");
        String board=bundle.getString("board");
        Log.e("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee","eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
        context=this;
        Log.e(user,board);
        storageRef = storage.getReference();
        setContentView(R.layout.info_window_layout);
        image= (ImageView) this.findViewById(R.id.markerImage);
        lineChart = this.findViewById(R.id.mapChart);
        Log.e("eee",image.toString());
        StorageReference pathReference = storageRef.child(user+"/"+board);
        pathReference.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        Log.e("Lista",listResult.toString());
                        ArrayList<StorageReference> l=(ArrayList<StorageReference>) listResult.getItems();
                        int last=l.toArray().length-1;
                        StorageReference item=  l.get(last);

                        //for (StorageReference item : listResult.getItems()) {
                            StorageReference pathReference=storageRef.child(user+"/"+board+"/"+item.getName());
                            Log.e("PATH",pathReference.toString());
                            pathReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    // Data for "images/island.jpg" is returns, use this as needed
                                    imgbytes=bytes;
                                    Log.e("Succes immage",imgbytes.toString());
                                    Log.e("Successsss immage",image.toString());
                                    image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                                    Log.e("Succes immage","");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                    Log.e("ERR",exception.toString());
                                }
                            });
                        //}
                    }
                });

        mDatabase = FirebaseDatabase.getInstance("https://mqttplantanalyzer-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference markerRef=mDatabase.child(user).child(board);
        Query lastchield =
                markerRef.orderByKey().limitToLast(10);
        lastchield.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot ds) {
                lumiList.clear();
                humiList.clear();
                moisList.clear();
                tempList.clear();
                for (DataSnapshot snapshot : ds.getChildren()) {
                    val = snapshot.getValue(MqttValue.class);
                    long time= TimeUnit.MILLISECONDS.toMinutes(val.getTimestamp());
                    lumiList.add(new Entry(time-time*2,val.getLux()));
                    humiList.add(new Entry(time-time*2,val.getHumidity()));
                    moisList.add(new Entry(time-time*2,val.getMoisture()));
                    tempList.add(new Entry(time-time*2,val.getTemperature()));
                }
                //Collections.reverse(entryList);
                EntryXComparator comp=new EntryXComparator();


                Collections.sort(lumiList, comp);
                Collections.sort(humiList, comp);
                Collections.sort(tempList, comp);
                Collections.sort(moisList, comp);
                luxDataSet = new LineDataSet(lumiList,"Luminosity");
                luxDataSet.setColors(Color.YELLOW);
                luxDataSet.setFillAlpha(95);
                luxDataSet.setDrawFilled(false);
                //lineDataSet.setFillColor(Color.WHITE);
                luxDataSet.setCircleRadius(5f);
                luxDataSet.setCircleHoleRadius(4f);
                luxDataSet.setCircleHoleColor(Color.WHITE);
                luxDataSet.setCircleColors(Color.BLACK);
                luxDataSet.setDrawCircleHole(true);
                luxDataSet.setValueTextSize(10f);
                dataSets.add(luxDataSet);

                humDataSet = new LineDataSet(humiList,"Humidity");
                humDataSet.setColors(Color.BLUE);
                humDataSet.setFillAlpha(95);
                humDataSet.setDrawFilled(false);
                //lineDataSet.setFillColor(Color.WHITE);
                humDataSet.setCircleRadius(5f);
                humDataSet.setCircleHoleRadius(4f);
                humDataSet.setCircleHoleColor(Color.WHITE);
                humDataSet.setCircleColors(Color.BLACK);
                humDataSet.setDrawCircleHole(true);
                humDataSet.setValueTextSize(10f);
                dataSets.add(humDataSet);

                temDataSet = new LineDataSet(tempList,"Temperature");
                temDataSet.setColors(Color.RED);
                temDataSet.setFillAlpha(95);
                temDataSet.setDrawFilled(false);
                //lineDataSet.setFillColor(Color.WHITE);
                temDataSet.setCircleRadius(5f);
                temDataSet.setCircleHoleRadius(4f);
                temDataSet.setCircleHoleColor(Color.WHITE);
                temDataSet.setCircleColors(Color.BLACK);
                temDataSet.setDrawCircleHole(true);
                temDataSet.setValueTextSize(10f);
                dataSets.add(temDataSet);

                moiDataSet = new LineDataSet(moisList,"Moisture");
                moiDataSet.setColors(Color.GREEN);
                moiDataSet.setFillAlpha(95);
                moiDataSet.setDrawFilled(false);
                //lineDataSet.setFillColor(Color.WHITE);
                moiDataSet.setCircleRadius(5f);
                moiDataSet.setCircleHoleRadius(4f);
                moiDataSet.setCircleHoleColor(Color.WHITE);
                moiDataSet.setCircleColors(Color.BLACK);
                moiDataSet.setDrawCircleHole(true);
                moiDataSet.setValueTextSize(10f);
                dataSets.add(moiDataSet);



                lineData = new LineData(dataSets);
                lineChart.setBackgroundColor(Color.WHITE);
                lineChart.getDescription().setEnabled(false);
                lineChart.setDrawGridBackground(false);
                lineChart.setDragEnabled(true);
                lineChart.setScaleEnabled(true);
                lineChart.getAxisRight().setAxisMinimum(0f);
                lineChart.getAxisLeft().setAxisMinimum(0f);
                lineChart.getLegend().setEnabled(true);
                lineChart.animateY(2000);
                lineChart.setData(lineData);
                lineChart.setExtraTopOffset(10f);
                lineChart.setExtraLeftOffset(14f);
                lineChart.setExtraBottomOffset(10f);
                lineChart.setExtraRightOffset(14f);
                XAxis xAxis = lineChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.TOP);
                xAxis.setTextSize(12f);
                xAxis.setDrawAxisLine(false);
                //xAxis.setDrawGridLines(true);
                xAxis.setTextColor(Color.BLACK);
                xAxis.setCenterAxisLabels(true);
                xAxis.setGranularity(30f); // half hour
                xAxis.setValueFormatter(new MyAxisFormatter());


                lineChart.setVisibleXRangeMaximum(60f);
                lineChart.setVisibleXRangeMinimum(30f);
                lineChart.setVisibleYRangeMaximum(3600f,lineChart.getAxisLeft().getAxisDependency());
                lineChart.setVisibleYRangeMinimum(60f,lineChart.getAxisLeft().getAxisDependency());
                lineChart.setDrawGridBackground(false);
                lineChart.getAxisLeft().setDrawGridLines(true);
                lineChart.getAxisLeft().setDrawAxisLine(false);
                lineChart.getAxisRight().setEnabled(false);
                lineChart.getXAxis().setDrawGridLines(false);
                lineChart.getXAxis().setAxisMinimum(lineChart.getXAxis().getAxisMinimum()-4);
                //lineChart.fitScreen();
                lineChart.invalidate();

                /*TextView title = (TextView) findViewById(R.id.title);
                title.setText("Last detection");

                TextView address = (TextView) findViewById(R.id.distance);
                address.setText("Luminosity is : "+val.getLux()+ " lux\n"+
                        "Moisture is : " + val.getMoisture()+" %\n"+
                        "Temperature is : "+val.getTemperature()+" C\n"+
                        "Humidity is : "+val.getHumidity()+" %\n");*/
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        Log.e("eeeeeee","rrefrfs");
    }


    public void getInfoContents(@NonNull Marker marker) {
        image= (ImageView) findViewById(R.id.markerImage);
        image.setImageBitmap(BitmapFactory.decodeByteArray(imgbytes, 0, imgbytes.length));
    }
}
