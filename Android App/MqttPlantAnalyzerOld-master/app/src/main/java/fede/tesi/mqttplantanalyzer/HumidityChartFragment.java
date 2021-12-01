package fede.tesi.mqttplantanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.github.mikephil.charting.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fede.tesi.mqttplantanalyzer.databinding.FragmentSecondBinding;

public class HumidityChartFragment extends Fragment {
    private FragmentSecondBinding binding;
    LineChart lineChart;
    LineData lineData;
    LineDataSet  lineDataSet;
    List<Entry> entryList = new ArrayList<>();
    private FirebaseAuth auth;
    private DatabaseReference mDatabase;
    Context baseContext;
    String boardId;
    SharedPreferences sharedPref;
    boolean all=false;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);

        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        baseContext=this.getContext();
        auth = FirebaseAuth.getInstance();
        sharedPref = this.getActivity().getSharedPreferences(auth.getUid(), Context.MODE_PRIVATE);
        boardId=sharedPref.getString("CurrentBoard","");
        lineChart = view.findViewById(R.id.lineChart);
        mDatabase = FirebaseDatabase.getInstance("https://mqttplantanalyzer-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        if(boardId!=null) {
            DatabaseReference userRef = mDatabase.child(auth.getUid()).child(boardId);
            Query recentPostsQuery = userRef
                    .limitToLast(100);
            Query allPostsQuery = userRef
                    .limitToLast(10000);
            ValueEventListener valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // whenever data at this location is updated.
                    Log.e("Data CHANGE", dataSnapshot.toString());
                    entryList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        MqttValue val = snapshot.getValue(MqttValue.class);
                        Log.e("Data CHANGE", "Value is: " + val.getHumidity());
                        long time = TimeUnit.MILLISECONDS.toMinutes(val.getTimestamp());
                        entryList.add(new Entry(time - time * 2, val.getHumidity()));

                    }
                    EntryXComparator comp = new EntryXComparator();


                    Collections.sort(entryList, comp);
                    Log.i("LIST", entryList.toString());
                    //Collections.reverse(entryList);
                    lineDataSet = new LineDataSet(entryList, "Humidity");
                    lineDataSet.setColors(Color.GRAY);
                    lineDataSet.setFillAlpha(95);
                    lineDataSet.setDrawFilled(true);
                    if (Utils.getSDKInt() >= 18) {
                        // fill drawable only supported on api level 18 and above
                        Drawable drawable = ContextCompat.getDrawable(baseContext, R.drawable.blue_scaled);
                        lineDataSet.setFillDrawable(drawable);
                    } else {
                        lineDataSet.setFillColor(Color.BLUE);
                    }
                    //lineDataSet.setFillColor(Color.WHITE);
                    lineDataSet.setCircleRadius(5f);
                    lineDataSet.setCircleHoleRadius(4f);
                    lineDataSet.setCircleHoleColor(Color.WHITE);
                    lineDataSet.setCircleColors(Color.BLACK);
                    lineDataSet.setDrawCircleHole(true);
                    lineDataSet.setValueTextSize(10f);
                    lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
                    lineDataSet.setValueFormatter(new DefaultAxisValueFormatter(0));

                    lineData = new LineData(lineDataSet);
                    lineChart.setBackgroundColor(Color.WHITE);
                    lineChart.getDescription().setEnabled(false);
                    lineChart.setDrawGridBackground(false);
                    lineChart.setDragEnabled(true);
                    lineChart.setScaleEnabled(true);
                    lineChart.getAxisRight().setAxisMinimum(0f);
                    lineChart.getAxisLeft().setAxisMinimum(0f);
                    lineChart.getLegend().setEnabled(false);
                    lineChart.animateY(2000);
                    lineChart.setData(lineData);
                    lineChart.setExtraTopOffset(3f);
                    lineChart.setExtraLeftOffset(5f);
                    lineChart.setExtraBottomOffset(5f);
                    lineChart.setExtraRightOffset(5f);
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
                    lineChart.setVisibleYRangeMaximum(60f, lineChart.getAxisLeft().getAxisDependency());
                    lineChart.setVisibleYRangeMinimum(35f, lineChart.getAxisLeft().getAxisDependency());
                    lineChart.setDrawGridBackground(false);
                    lineChart.getAxisLeft().setDrawGridLines(false);
                    lineChart.getAxisLeft().setDrawAxisLine(false);
                    lineChart.getAxisRight().setEnabled(false);
                    lineChart.getAxisLeft().setAxisMaximum(105f);
                    lineChart.getAxisLeft().setTextSize(14f);
                    lineChart.getXAxis().setDrawGridLines(false);
                    lineChart.getXAxis().setAxisMinimum(lineChart.getXAxis().getAxisMinimum() - 4);
                    //lineChart.fitScreen();
                    lineChart.invalidate();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.e("Data ERROR", "Failed to read value.", error.toException());
                }
            };
            recentPostsQuery.addValueEventListener(valueEventListener);
            Button querybtn = view.findViewById(R.id.querybtn);
            querybtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(all==false) {
                        recentPostsQuery.removeEventListener(valueEventListener);
                        allPostsQuery.addValueEventListener(valueEventListener);
                        querybtn.setText("Last 100");
                        all=true;
                    }
                    else{
                        allPostsQuery.removeEventListener(valueEventListener);
                        recentPostsQuery.addValueEventListener(valueEventListener);
                        querybtn.setText("All results");
                        all=false;
                    }
                }
            });
        }



    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
