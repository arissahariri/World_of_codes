package com.example.chatapp;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder>{

    Context context;
    List<ModelPost> postList;

    public AdapterPosts(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout row_post.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, parent, false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {

        //get the data //the same from modelpost
        String uid = postList.get(position).getUserid();
        String uName = postList.get(position).getUserName();
        String uEmail = postList.get(position).getUserEmail();
        String uDp = postList.get(position).getUserDp();
        String pId = postList.get(position).getPostId();
        String pTitle = postList.get(position).getPostTitle();
        String pDesc = postList.get(position).getPostDescription();
        String pImage = postList.get(position).getPostImage();
        String pTime = postList.get(position).getPostTime();

        //convert timestamp to dd/m/yyyy
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTime));

        String pDate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        //set data for row post //same name as above
        holder.recEmail.setText(uEmail);
        holder.recTime.setText(pDate);
        holder.recTitle.setText(pTitle);
        holder.recDescription.setText(pDesc);

        // if no image is uploaded, set imageview to hide
        if (pImage.equals("noImage")){

            //hide
            holder.recImage.setVisibility(View.GONE);
        }
        else
        {
            try {
                Picasso.get().load(pImage).into(holder.recImage);
            }

            catch (Exception e){
            }
        }

       //set post image

        try {
            Picasso.get().load(pImage).into(holder.recImage);
        }
        catch (Exception e){

        }



    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //view from row post xml
        ImageView recImage;
        TextView recEmail, recTime, recTitle, recDescription;
        LinearLayout profileLayout;

        public MyHolder(@NonNull View itemView) {

            super(itemView);

            recImage = itemView.findViewById(R.id.pImage);
            recEmail = itemView.findViewById(R.id.uEmail);
            recTime = itemView.findViewById(R.id.pTime);
            recTitle = itemView.findViewById(R.id.pTitle);
            recDescription = itemView.findViewById(R.id.pDescription);
            profileLayout = itemView.findViewById(R.id.profileLayout);

        }
    }
}
