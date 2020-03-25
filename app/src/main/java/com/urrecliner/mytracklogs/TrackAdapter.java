package com.urrecliner.mytracklogs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;

import static com.urrecliner.mytracklogs.Vars.logLocations;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.decimalComma;
import static com.urrecliner.mytracklogs.Vars.trackActivity;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.utils;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder>  {

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView tvStartFinish, tvMeterMinutes;
        View viewLine;

        TrackViewHolder(View view) {
            super(view);
            this.viewLine = itemView.findViewById(R.id.one_track);
            this.tvStartFinish = (TextView) itemView.findViewById(R.id.startFinishTime);
            this.tvMeterMinutes = (TextView) itemView.findViewById(R.id.metersMinutes);
            this.viewLine.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    LogLocation LogLocation = logLocations.get(position);
                    Intent intent = new Intent(trackActivity, MapActivity.class);
                    intent.putExtra("startTime", LogLocation.getStartTime());
                    intent.putExtra("finishTime", LogLocation.getFinishTime());
                    intent.putExtra("minutes", LogLocation.getMinutes());
                    intent.putExtra("meters", LogLocation.getMeters());
                    intent.putExtra("position", position);
                    trackActivity.startActivity(intent);
                }
            });

            viewLine.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    deleteThisLogOrNot(getAdapterPosition());
                    return true;
                }
            });
        }
    }

    private static void deleteThisLogOrNot(int pos) {
        final int position = pos;
        final LogLocation LogLocation = logLocations.get(pos);
        AlertDialog.Builder builder = new AlertDialog.Builder(trackActivity);
        builder.setTitle("이동 정보 처리");
        String s = utils.long2DateDay(LogLocation.getStartTime())+" "+utils.long2Time(LogLocation.getStartTime())+"~"+
                utils.long2Time(LogLocation.getFinishTime())+"\n"+
                decimalComma.format(LogLocation.getMeters())+"m "+utils.minute2Text(LogLocation.getMinutes());
        builder.setMessage(s);
        builder.setNegativeButton("Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        long fromTime = LogLocation.getStartTime();
                        long toTime = LogLocation.getFinishTime();
                        logLocations.remove(position);
                        trackAdapter.notifyItemRemoved(position);
                        databaseIO.logDeleteFromTo(fromTime, toTime);

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.one_track, viewGroup, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder viewHolder, int position) {

        String s;
        LogLocation LogLocation = logLocations.get(position);
        s = utils.long2DateDay(LogLocation.getStartTime())+"\n"+utils.long2Time(LogLocation.getStartTime())+"~"+
                utils.long2Time(LogLocation.getFinishTime());
        viewHolder.tvStartFinish.setText(s);
        DecimalFormat decimalFormat = new DecimalFormat("##,###,###");
        s = decimalFormat.format(LogLocation.getMeters())+"m\n"+utils.minute2Text(LogLocation.getMinutes());
        viewHolder.tvMeterMinutes.setText(s);
        int grayed = 220 * position / (logLocations.size()+1);
        viewHolder.viewLine.setBackgroundColor(ContextCompat.getColor(trackActivity,R.color.logBackground) - grayed - grayed * 256 - grayed * 256 * 256);
    }

    @Override
    public int getItemCount() {
        return (null != logLocations ? logLocations.size() : 0);
    }

}
