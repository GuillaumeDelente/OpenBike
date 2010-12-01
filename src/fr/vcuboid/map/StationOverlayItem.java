package fr.vcuboid.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import fr.vcuboid.object.Station;

public class StationOverlayItem extends OverlayItem {

	public StationOverlayItem(GeoPoint point, int bikes, int slots) {
		super(point, null, null);
	}
}
