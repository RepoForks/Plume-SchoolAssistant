package com.pdt.plume;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class TaskAdapter extends ArrayAdapter {

    Context context;
    int layoutResourceId;
    ArrayList<Task> data = null;

    public TaskAdapter(Context context, int layoutResourceId, ArrayList<Task> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.icon = (ImageView)row.findViewById(R.id.task_icon);
            holder.title = (TextView)row.findViewById(R.id.task_title);
            holder.shared = (TextView)row.findViewById(R.id.task_shared);
            holder.description = (TextView)row.findViewById(R.id.task_description);
            holder.attachment = (TextView)row.findViewById(R.id.task_attachment);
            holder.date = (TextView)row.findViewById(R.id.task_date);

            row.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)row.getTag();
        }

        Task task = data.get(position);
        holder.icon.setImageResource(task.taskIcon);
        holder.title.setText(task.taskTitle);
        holder.shared.setText(task.taskShared);
        holder.description.setText(task.taskDescription);
        holder.attachment.setText(task.taskAttachment);
        holder.date.setText("" + task.taskDueDate);

        return row;
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
        TextView shared;
        TextView description;
        TextView attachment;
        TextView date;
    }


}
