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
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {
	private SeekBar mBar;
	private TextView mDistanceTextView;
	private Context mContext;
	private static SharedPreferences mSharedPreferences;
	private static String DISTANCE_FILTER;
	private static final int DEFAULT_DISTANCE = 1000;
	private static final int LAYOUT_PADDING = 10;

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		DISTANCE_FILTER = context.getString(R.string.distance_filter);
		setDialogLayoutResource(R.layout.distance_preference_layout);
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		mDistanceTextView.setText(" " + progress + "m.");
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}

	@Override
    protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
        mDistanceTextView = (TextView) view.findViewById(R.id.distance_preference_meters);
        int distance = getValue();
        mDistanceTextView.setText(" " + distance + "m.");
        mBar = (SeekBar) view.findViewById(R.id.distance_seekBar);
        mBar.setMax(2000);
        mBar.setOnSeekBarChangeListener(this);
        mBar.setProgress(distance);
        return view;
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
		Editor ed = mSharedPreferences.edit();
		ed.putInt(DISTANCE_FILTER, value);
		ed.commit();
	}

	private int getValue() {
		return mSharedPreferences.getInt(DISTANCE_FILTER, DEFAULT_DISTANCE);
	}
}