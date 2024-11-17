package ssh2.matss.ph.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
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

        // Set up listeners for the buttons
        binding.btnShare.setOnClickListener(v -> mAdapter.shareLog());
        binding.btnDelete.setOnClickListener(v -> mAdapter.clearLog());

        return binding.getRoot();
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
    public void onItemClick(View view, int position, String logText) {
        // Handle item click
    }

    @Override
    public void onItemLongClick(View view, int position, String logText) {
        // Handle item long click
    }
}
