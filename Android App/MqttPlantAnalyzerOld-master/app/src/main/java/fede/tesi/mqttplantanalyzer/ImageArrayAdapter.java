package fede.tesi.mqttplantanalyzer;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class ImageArrayAdapter extends ArrayAdapter<StorageReference> {
    public ImageArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<StorageReference> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        // Replace text with my own
        String trueName;
        String val=getItem(position).getName();
        if (val.endsWith(".jpg")||val.endsWith(".JPG")) {
            trueName = val.substring(0, val.length() - 4);
        }
        else {
            trueName=val;
        }
        Date date = new Date(Long.valueOf(trueName));
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        view.setText(df.format("yyyy-MM-dd hh:mm:ss a", date));
        return view;
    }
}
