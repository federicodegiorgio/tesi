package fede.tesi.mqttplantanalyzer;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;

import fede.tesi.mqttplantanalyzer.databinding.FragmentChartListBinding;
import fede.tesi.mqttplantanalyzer.databinding.FragmentSecondBinding;

public class ChartListFragment extends Fragment {
    private FragmentChartListBinding binding;
    ListView listView;
    TextView textView;
    String[] listItem;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentChartListBinding.inflate(inflater, container, false);

        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView=(ListView)view.findViewById(R.id.chartList);
        textView=(TextView)view.findViewById(R.id.chartTypeView);
        ImageView immV =(ImageView)view.findViewById(R.id.send_icon);
        listItem = getResources().getStringArray(R.array.array_charts);
        ArrayList<Chart_Type> provMisc=new ArrayList<>();

        provMisc.add(new Chart_Type(R.drawable.ic_baseline_brightness_high_24,"Luminosity"));
        provMisc.add(new Chart_Type(R.drawable.ic_baseline_cloud_24,"Humidity"));
        provMisc.add(new Chart_Type(R.drawable.ic_baseline_ac_unit_24,"Temperature"));
        provMisc.add(new Chart_Type(R.drawable.ic_baseline_landscape_24,"Moisture"));
        provMisc.add(new Chart_Type(R.drawable.ic_baseline_photo_camera_24,"Image"));

        final ChartTypeAdapter add=new ChartTypeAdapter(this.getContext(), R.layout.sensor_row,provMisc);
        final ArrayAdapter<Chart_Type> adapter = new ArrayAdapter<Chart_Type>(this.getActivity().getBaseContext(),
                android.R.layout.simple_list_item_2, android.R.id.text1, provMisc);
        listView.setAdapter(add);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO Auto-generated method stub
                String value=adapter.getItem(position).getVal();
                switch (value){
                    case "Luminosity":
                        NavHostFragment.findNavController(ChartListFragment.this)
                                .navigate(R.id.action_SecondFragment_to_LuminosityFragment);
                        break;
                    case "Humidity":
                        NavHostFragment.findNavController(ChartListFragment.this)
                                .navigate(R.id.action_SecondFragment_to_HumidityFragment);
                        break;
                    case "Moisture":
                        NavHostFragment.findNavController(ChartListFragment.this)
                                .navigate(R.id.action_SecondFragment_to_MoistureFragment);
                        break;
                    case "Temperature":
                        NavHostFragment.findNavController(ChartListFragment.this)
                                .navigate(R.id.action_SecondFragment_to_TemperatureFragment);
                        break;

                    case "Image":
                        NavHostFragment.findNavController(ChartListFragment.this)
                                .navigate(R.id.action_SecondFragment_to_ImageFragment);
                        break;
                    default:
                        break;
                }

            }
        });


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
