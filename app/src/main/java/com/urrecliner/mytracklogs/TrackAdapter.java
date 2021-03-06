package com.urrecliner.mytracklogs;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;

import static com.urrecliner.mytracklogs.Vars.dummyMap;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.decimalComma;
import static com.urrecliner.mytracklogs.Vars.trackActivity;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.trackPosition;
import static com.urrecliner.mytracklogs.Vars.utils;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder>  {

    static class TrackViewHolder extends RecyclerView.ViewHolder {

        TextView tvStartFinish, tvMeterMinutes, tvPlaceName;
        ImageView ivBitmap;
        View viewLine;

        TrackViewHolder(View view) {
            super(view);
            this.viewLine = itemView.findViewById(R.id.one_track);
            this.tvStartFinish = itemView.findViewById(R.id.startFinishTime);
            this.tvMeterMinutes = itemView.findViewById(R.id.metersMinutes);
            this.ivBitmap = itemView.findViewById(R.id.trackMap);
            this.tvPlaceName = itemView.findViewById(R.id.placeName);
            this.viewLine.setOnClickListener(view1 -> {
                trackPosition = getAdapterPosition();
                TrackLog trackLog = trackLogs.get(trackPosition);
                Intent intent = new Intent(trackActivity, MapActivity.class);
                intent.putExtra("trackLog", trackLog);
                trackActivity.startActivity(intent);
            });
            viewLine.setOnLongClickListener(view12 -> {
                deleteThisLogOrNot(getAdapterPosition());
                return true;
            });
        }
    }

    private static void deleteThisLogOrNot(int pos) {
        final int position = pos;
        final TrackLog TrackLog = trackLogs.get(pos);
        AlertDialog.Builder builder = new AlertDialog.Builder(trackActivity);
        builder.setTitle("이동 정보 처리");
        String s = utils.long2DateDay(TrackLog.getStartTime())+" "+utils.long2Time(TrackLog.getStartTime())+"~"+
                utils.long2Time(TrackLog.getFinishTime())+"\n"+
                decimalComma.format(TrackLog.getMeters())+"m "+utils.minute2Text(TrackLog.getMinutes());
        builder.setMessage(s);
        builder.setNegativeButton("Delete", (dialog, which) -> {
                    long fromTime = TrackLog.getStartTime();
                    long toTime = TrackLog.getFinishTime();
                    databaseIO.trackDelete(fromTime);
                    databaseIO.logDeleteFromTo(fromTime, toTime);
                    trackLogs.remove(position);
                    trackAdapter.notifyItemRemoved(position);
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.track_log_line, viewGroup, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder viewHolder, int position) {

        String s;
        TrackLog trackLog = trackLogs.get(position);
        s = utils.long2DateDay(trackLog.getStartTime())+
                "\n"+utils.long2Time(trackLog.getStartTime())+"~" + utils.long2Time(trackLog.getFinishTime());
        viewHolder.tvStartFinish.setText(s);
        DecimalFormat decimalFormat = new DecimalFormat("##,###,###");
        s = decimalFormat.format(trackLog.getMeters())+ "m" +
                "\n"+utils.minute2Text(trackLog.getMinutes()) + ((trackLog.getWalkDrive() == 0) ? " Wlk":" Drv");
        viewHolder.tvMeterMinutes.setText(s);
        viewHolder.tvPlaceName.setText(trackLog.getPlaceName());

//        int grayed = 240 * position / (trackLogs.size()+1)
        int grayed = (position % 4) * 10;
        int backColor = ContextCompat.getColor(trackActivity,R.color.logBackground)
                - grayed - grayed *256 - grayed *256*256;
        viewHolder.viewLine.setBackgroundColor(backColor);
        backColor ^= 0xAAAAAA;
        viewHolder.tvStartFinish.setTextColor(backColor);
        viewHolder.tvMeterMinutes.setTextColor(backColor);
        viewHolder.tvPlaceName.setTextColor(backColor);
        Bitmap bitmap = trackLog.getBitMap();
        if (bitmap.sameAs(dummyMap))
            viewHolder.ivBitmap.setImageResource(R.mipmap.my_track_log);
        else
            viewHolder.ivBitmap.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        return (null != trackLogs ? trackLogs.size() : 0);
    }

}
