package fr.vcuboid.map;

import java.util.ArrayList;
import java.util.ListIterator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import fr.vcuboid.R;
import fr.vcuboid.object.Station;

public class StationsOverlay extends Overlay {

	static private Bitmap mMarker;
	static int mMarkerShift = 0;
	private Context mContext;
	private MapView mMapView;
	private ArrayList<Station> mStations;

	public StationsOverlay(ArrayList<Station> stations, Context context,
			MapView mapView) {
		super();
		mContext = context;
		mStations = stations;
		mMapView = mapView;
		mMarker = BitmapFactory.decodeResource(mContext.getResources(),
				R.drawable.v3);
		mMarkerShift = mMarker.getHeight();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow == false) {
			Log.e("Vcuboid", "draw markers");
			Projection projection = mapView.getProjection();
			ListIterator<Station> it = mStations.listIterator();
			Point out = new Point();
			Paint p1 = new Paint();
			p1.setAntiAlias(true);
			p1.setTextSize(10);
			p1.setColor(Color.WHITE);
			p1.setTypeface(Typeface.DEFAULT_BOLD);
			while (it.hasNext()) {
				Station s = it.next();
				projection.toPixels(new GeoPoint(s.getLatitude(), s
						.getLongitude()), out);
				canvas.drawBitmap(mMarker, out.x, out.y - mMarkerShift, null);
				canvas.drawText(String.valueOf(s.getAvailableBikes()), out.x + 10, out.y + 20 - mMarkerShift, p1);
				canvas.drawText("Vélos", out.x + 25, out.y + 20 - mMarkerShift, p1);
				canvas.drawText(String.valueOf(s.getFreeSlots()), out.x + 10,
						out.y + 35 - mMarkerShift, p1);
				canvas.drawText("Places", out.x + 25, out.y + 35 - mMarkerShift, p1);
			}
		}
	}
}
