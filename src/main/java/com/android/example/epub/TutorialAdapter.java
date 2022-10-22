package com.android.example.epub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.MyViewHolder> {

    ArrayList<String> gestureTitle;
    ArrayList<Integer> gestureImages;
    ArrayList<String> gestureUsage;

    Context context;

    public TutorialAdapter(Context context, ArrayList<String> gestureTitle, ArrayList<String> gestureUsage, ArrayList<Integer> gestureImages) {
        this.context = context;
        this.gestureTitle = gestureTitle;
        this.gestureImages = gestureImages;
        this.gestureUsage = gestureUsage;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // infalte the item Layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rowlayout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        MyViewHolder vh = new MyViewHolder(v); // pass the view to View Holder
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        try {
            holder.setData(gestureTitle.get(position), gestureUsage.get(position), gestureImages.get(position));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return gestureTitle.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        // init the item view's
        TextView name;
        TextView usage;

        ImageView image;

        public MyViewHolder(View itemView) {
            super(itemView);

            // get the reference of item view's
            name = (TextView) itemView.findViewById(R.id.title);
            image = (ImageView) itemView.findViewById(R.id.image);
            usage = (TextView) itemView.findViewById(R.id.usage);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show();
                }

            });

        }

        public void setData(String input_name, String input_usage, Integer input_image) throws ParseException {
            name.setText(input_name);
            image.setImageResource(input_image);
            usage.setText(input_usage);
        }


    }

}
