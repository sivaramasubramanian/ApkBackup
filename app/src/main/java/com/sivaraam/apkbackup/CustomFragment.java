package com.sivaraam.apkbackup;

/**
 * Created by sivaraam on 19/5/17.
 */


import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomFragment extends Fragment implements SearchView.OnQueryTextListener
{
    private ArrayList<mApplication> mList;
    private final String[] fragmentNames = {"System", "Downloaded"};
    private RecyclerAdapter recyclerAdapter;

    public CustomFragment()
    {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_1, menu);

        SearchManager sm = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.app_bar_search);

        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(sm.getSearchableInfo(getActivity().getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);


        super.onCreateOptionsMenu(menu, inflater);

    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_app_list, container, false);


        RecyclerView recyclerView1 = (RecyclerView) rootView.findViewById(R.id.listview);
        MainActivity m1 = (MainActivity) getActivity();
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView1.setLayoutManager(mLayoutManager);
        int fragmentID = getArguments().getInt("FragmentID");
        mList = m1.getPackInfo(fragmentID);

        recyclerAdapter = new RecyclerAdapter(mList);

        recyclerView1.setAdapter(recyclerAdapter);

        TextView no_results = (TextView) rootView.findViewById(R.id.no_results);
        no_results.setText(no_results.getText() + fragmentNames[fragmentID] + " Apps");
        return rootView;
    }

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        recyclerAdapter.getFilter().filter(query);
        return false;
    }

    public boolean onQueryTextChange(String newText)
    {
        recyclerAdapter.getFilter().filter(newText);
        return true;
    }


}