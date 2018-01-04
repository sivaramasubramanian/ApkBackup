package com.sivaraam.apkbackup;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by sivaraam on 17/6/17.
 */

//This class implements Filterable to facilitate Search in the Appbar Menu
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder>
        implements Filterable
{

    private static boolean isFirstRun = true;
    private final ArrayList<mApplication> Applist;
    private ArrayList<mApplication> filteredList;
    private RecyclerAdapterFilter filter;

    public RecyclerAdapter(ArrayList<mApplication> Applist)
    {

        this.Applist = Applist;
        this.filteredList = Applist;

        if (isFirstRun)
        {
            clearList();
            isFirstRun = false;
        }

        getFilter();
    }

    @Override
    public Filter getFilter()
    {
        if (filter == null)
        {
            filter = new RecyclerAdapterFilter();
        }
        return filter;
    }

    private void clearList()
    {
        for (int i = 0; i < MainActivity.noOfApps; i++)
        {
            MainActivity.selectedlist.add(i, false);
        }
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_row, parent, false);
        final ViewHolder holder = new ViewHolder(itemView);
        //Log.d("debug","onCreateViewHolder()");

        //------------------------  IMPORTANT  ---------------------------------
        //                 DO NOT MODIFY THE FOLLOWING
        //----------------------------------------------------------------------

        holder.holderCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                int pos = holder.getAdapterPosition();
                final int id = filteredList.get(pos).id;

                if (compoundButton.isChecked())
                {

                    if (!MainActivity.selectedlist.get(id))
                    {
                        MainActivity.selectedAppList.add(holder.appInfo.id);
                        MainActivity.sizeOfSelected += holder.appInfo.fileSizeBytes;
                    }
                    MainActivity.selectedlist.set(id, true);

                } else
                {

                    if (MainActivity.selectedlist.get(id))
                    {
                        MainActivity.selectedAppList.remove((Integer) holder.appInfo.id);
                        MainActivity.sizeOfSelected -= holder.appInfo.fileSizeBytes;
                    }
                    MainActivity.selectedlist.set(id, false);

                }

            }
        });


        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        mApplication pack = filteredList.get(position);
        holder.appInfo = pack;
        final int id = pack.id;
        holder.holderTextViewAppName.setText(pack.label);
        holder.holderTextViewVersion.setText(pack.version);
        holder.holderTextViewSize.setText(pack.size);
        holder.holderImageViewAppIcon.setImageDrawable(pack.icon);
        holder.holderCheckBox.setChecked(MainActivity.selectedlist.get(id));
        //Log.d("debug","onBindViewHolder()");
    }

    @Override

    public int getItemCount()
    {
        return filteredList.size();

    }

    private class RecyclerAdapterFilter extends Filter
    {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence)
        {
            FilterResults filterResults = new FilterResults();

            if (charSequence != null && charSequence.length() > 0)
            {
                ArrayList<mApplication> templist = new ArrayList<>();

                for (mApplication appln : Applist)
                {
                    if (appln.label.toLowerCase().contains(charSequence.toString().toLowerCase()))
                    {

                        templist.add(appln);
                    }

                }
                filterResults.count = templist.size();
                filterResults.values = templist;
            } else
            {
                filterResults.count = Applist.size();
                filterResults.values = Applist;
            }


            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults)
        {

            filteredList = (ArrayList<mApplication>) filterResults.values;

            notifyDataSetChanged();


        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        public final TextView holderTextViewAppName;
        public final TextView holderTextViewVersion;
        public final TextView holderTextViewSize;
        public final ImageView holderImageViewAppIcon;
        public final CheckBox holderCheckBox;
        public mApplication appInfo;

        public ViewHolder(final View rowView)
        {
            super(rowView);
            holderTextViewAppName = (TextView) rowView.findViewById(R.id.textView1);
            holderTextViewVersion = (TextView) rowView.findViewById(R.id.textView_v);
            holderTextViewSize = (TextView) rowView.findViewById(R.id.textView_s);
            holderImageViewAppIcon = (ImageView) rowView.findViewById(R.id.imageView1);
            holderCheckBox = (CheckBox) rowView.findViewById(R.id.checkBox1);

            //---------------------------------------------------------------------------------
            //  This is inefficient but I'm bored so I won't override GESTURE_DETECTOR now
            //  and build a custom ClickListener to make it efficient. Its for another day.
            //---------------------------------------------------------------------------------

            rowView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    holderCheckBox.toggle();
                }
            });


        }
    }
}
