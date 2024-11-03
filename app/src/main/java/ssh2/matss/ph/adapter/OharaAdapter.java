package ssh2.matss.ph.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import ssh2.matss.ph.databinding.ItemSpinnerBinding;

public class OharaAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<HashMap<String, String>> config;
    private HashMap<String, String> data;
    private final boolean is;

    public OharaAdapter(
            Context context,
            ArrayList<HashMap<String, String>> configs,
            boolean is)
    {
        this.context = context;
        this.config = configs;
        this.is = is;
    }

    @Override
    public int getCount() {
        return config.size();
    }

    @Override
    public Object getItem(int i) {
        return config.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        @SuppressLint("ViewHolder") ItemSpinnerBinding binding = ItemSpinnerBinding.inflate(LayoutInflater.from(context));
        data = config.get(i);

        TextView icon = binding.ivFlag;
        String str = data.get("mIcon");

        binding.tvName.setText(data.get("mName"));
        binding.tvInfo.setText(data.get("mInfo"));

        if (str != null) setServerIcon(icon, is ? str : "rocket");

        return binding.getRoot();
    }


    private void setServerIcon(TextView textView, String countryCode) {
        String flagEmoji = getFlagEmoji(countryCode);
        textView.setText(flagEmoji);
    }


    private String getFlagEmoji(String countryCode) {
        if (countryCode.length() == 2) { // Country codes are typically 2 letters
            int firstChar = Character.codePointAt(countryCode.toUpperCase(), 0) - 0x41 + 0x1F1E6;
            int secondChar = Character.codePointAt(countryCode.toUpperCase(), 1) - 0x41 + 0x1F1E6;
            return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
        } else {
            return "🚀";
        }
    }
}
