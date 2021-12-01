package fede.tesi.mqttplantanalyzer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ChartTypeAdapter extends ArrayAdapter<Chart_Type>{

        Context context;
        int layoutResourceId;
        ArrayList<Chart_Type> data=new ArrayList<Chart_Type>();
        public ChartTypeAdapter(Context context, int layoutResourceId, ArrayList<Chart_Type> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ImageHolder holder = null;

            if(row == null)
            {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new ImageHolder();
                holder.txtTitle = (TextView)row.findViewById(R.id.chartTypeView);
                holder.imgIcon = (ImageView)row.findViewById(R.id.send_icon);
                row.setTag(holder);
            }
            else
            {
                holder = (ImageHolder)row.getTag();
            }

            Chart_Type myImage = data.get(position);
            holder.txtTitle.setText(myImage.getVal());
            int outImage=myImage.getImm();
            holder.imgIcon.setImageResource(outImage);
            return row;

        }

        static class ImageHolder
        {
            ImageView imgIcon;
            TextView txtTitle;
        }

}
