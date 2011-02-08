package fr.vcuboid.filter;

import fr.vcuboid.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
	private SeekBar mBar;
    private Context mContext;
    private static SharedPreferences sp;
    private static String OPT_SEEKBAR_KEY;
    private static final int OPT_SEEKBAR_DEF = 30;
    private static final int LAYOUT_PADDING = 10;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        OPT_SEEKBAR_KEY = context.getString(R.string.distance_filter);
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        // TODO Auto-generated method stub
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setPadding(LAYOUT_PADDING, LAYOUT_PADDING, LAYOUT_PADDING, LAYOUT_PADDING);
        mBar = new SeekBar(mContext);
        mBar.setMax(2000);
        mBar.setOnSeekBarChangeListener(this);
        mBar.setProgress(getValue());
        layout.addView(mBar, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            setValue(mBar.getProgress());
        }
    }

    private void setValue(int value) {
        Editor ed = sp.edit();
        ed.putInt(OPT_SEEKBAR_KEY, value);
        ed.commit();
    }

    private int getValue() {
        return sp.getInt(OPT_SEEKBAR_KEY, OPT_SEEKBAR_DEF);
    }
}