package fede.tesi.mqttplantanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Set;

import fede.tesi.mqttplantanalyzer.databinding.FragmentBoardListBinding;

public class BoardSelectionFragment extends Fragment{
        private FragmentBoardListBinding binding;
        ListView listView;
        TextView textView;
        String[] listItem;
        private FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences sharedPref;
        View fragView;

    @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState
        ) {
            binding = FragmentBoardListBinding.inflate(inflater, container, false);

            return binding.getRoot();

        }

        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            fragView=view;
            listView=(ListView)view.findViewById(R.id.boardList);
            textView=(TextView)view.findViewById(R.id.chartTypeView);
            sharedPref = this.getActivity().getSharedPreferences(user.getUid(), Context.MODE_PRIVATE);
            ImageView immV =(ImageView)view.findViewById(R.id.send_icon);
            listItem = getResources().getStringArray(R.array.array_charts);
            ArrayList<Chart_Type> provMisc=new ArrayList<>();
            Set<String> boards;
            if(sharedPref.getStringSet(user.getUid(), null)!=null) {
                boards = sharedPref.getStringSet(user.getUid(), null);
                for (String s : boards) {
                    String myName=sharedPref.getString(s,"");
                    if (myName!=null)
                    provMisc.add(new Chart_Type(R.drawable.ic_baseline_developer_board_24, myName));
                    else
                        provMisc.add(new Chart_Type(R.drawable.ic_baseline_developer_board_24, s));
                }
            }

            final ChartTypeAdapter add=new ChartTypeAdapter(this.getContext(), R.layout.sensor_row,provMisc);
            final ArrayAdapter<Chart_Type> adapter = new ArrayAdapter<Chart_Type>(this.getActivity().getBaseContext(),
                    android.R.layout.simple_list_item_2, android.R.id.text1, provMisc);
            listView.setAdapter(add);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    // TODO Auto-generated method stub
                    String value=adapter.getItem(position).getVal();
                    Log.e("Value",value);

                    String realName=sharedPref.getString(value,"");
                    Log.e("Value2",realName);

                    SharedPreferences.Editor editor=sharedPref.edit();
                    editor.putString("CurrentBoard",realName);
                    editor.apply();
                    Log.e("schedasel",sharedPref.getString("CurrentBoard",""));
                    Navigation.findNavController(fragView).navigate(R.id.action_BoardSelection_to_SecondFragment);

                }
            });


        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            binding = null;
        }
    }
