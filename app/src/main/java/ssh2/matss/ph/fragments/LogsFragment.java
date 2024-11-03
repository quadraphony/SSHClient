package ssh2.matss.ph.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ssh2.matss.ph.R;
import ssh2.matss.ph.adapter.LogsAdapter;
import ssh2.matss.ph.databinding.FragmentLogsBinding;

public class LogsFragment extends Fragment implements LogsAdapter.OnItemClickListener {

    private FragmentLogsBinding binding;
    private RecyclerView logListView;
    private LogsAdapter mAdapter;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentLogsBinding.inflate(inflater, container, false);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        mAdapter = new LogsAdapter(layoutManager, getActivity());
        mAdapter.setOnItemClickListener(this);

        logListView = binding.rvLogs;

        logListView.setAdapter(mAdapter);
        logListView.setLayoutManager(layoutManager);

        mAdapter.scrollToLastPosition();

        setHasOptionsMenu(true);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle("Logs");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_logs, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu Itens
        int itemId = item.getItemId();
        if (itemId == R.id.deleteLogs) {// Para Android 6.0 Marshmallow e superior
            mAdapter.clearLog();
        } else if (itemId == R.id.shareLogs) {
            mAdapter.shareLog();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(View view, int position, String logText) {

    }

    @Override
    public void onItemLongClick(View view, int position, String logText) {

    }
}