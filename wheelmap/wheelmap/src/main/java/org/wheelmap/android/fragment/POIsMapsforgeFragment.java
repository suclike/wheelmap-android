/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.fragment;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapView.OnMoveListener;
import org.mapsforge.android.maps.MapView.OnZoomListener;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.wheelmap.android.activity.MapsforgeMapActivity;
import org.wheelmap.android.app.WheelmapApp;
import org.wheelmap.android.app.WheelmapApp.Capability;
import org.wheelmap.android.fragment.SearchDialogFragment.OnSearchDialogListener;
import org.wheelmap.android.model.Extra;
import org.wheelmap.android.online.R;
import org.wheelmap.android.overlays.MyLocationOverlay;
import org.wheelmap.android.overlays.OnTapListener;
import org.wheelmap.android.overlays.POIsCursorMapsforgeOverlay;
import org.wheelmap.android.ui.mapsforge.ConfigureMapView;
import org.wheelmap.android.utils.ParceableBoundingBox;

import android.app.Activity;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.akquinet.android.androlog.Log;

public class POIsMapsforgeFragment extends SherlockFragment implements
		DisplayFragment, OnMoveListener, OnZoomListener, OnTapListener,
		OnSearchDialogListener, OnExecuteBundle {
	public final static String TAG = POIsMapsforgeFragment.class
			.getSimpleName();

	private WorkerFragment mWorkerFragment;

	private MapView mMapView;
	private MapController mMapController;
	private POIsCursorMapsforgeOverlay mPoisItemizedOverlay;
	private MyLocationOverlay mCurrLocationOverlay;
	private GeoPoint mLastRequestedPosition;

	private Cursor mCursor;

	private boolean isCentered;
	private int oldZoomLevel = 18;
	private static final float SPAN_ENLARGEMENT_FAKTOR = 1.3f;
	private static final byte ZOOMLEVEL_MIN = 16;
	private static final int MAP_ZOOM_DEFAULT = 18; // Zoon 1 is world view
	private GeoPoint mLastGeoPointE6;

	public interface OnPOIsMapsforgeListener {
		public void onShowDetail(long id);

		public void onRefreshing(boolean isRefreshing);

	}

	private OnPOIsMapsforgeListener mListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof OnPOIsMapsforgeListener)
			mListener = (OnPOIsMapsforgeListener) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = (LinearLayout) inflater.inflate(R.layout.fragment_mapsforge,
				container, false);

		System.gc();
		mMapView = (MapView) v.findViewById(R.id.map);

		mMapView.setClickable(true);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setScaleBar(true);

		ConfigureMapView.pickAppropriateMap(getActivity()
				.getApplicationContext(), mMapView);

		mMapController = mMapView.getController();

		// overlays
		mPoisItemizedOverlay = new POIsCursorMapsforgeOverlay(getActivity(),
				this, false);
		mCurrLocationOverlay = new MyLocationOverlay();

		Capability cap = WheelmapApp.getCapabilityLevel();
		if (cap == Capability.DEGRADED_MIN || cap == Capability.DEGRADED_MAX) {
			mPoisItemizedOverlay.enableLowDrawQuality(true);
			mCurrLocationOverlay.enableLowDrawQuality(true);
			mCurrLocationOverlay.enableUseOnlyOneBitmap(true);

		}
		mMapView.getOverlays().add(mPoisItemizedOverlay);
		mMapView.getOverlays().add(mCurrLocationOverlay);
		mMapController.setZoom(oldZoomLevel);
		mMapView.setMoveListener(this);
		mMapView.setZoomListener(this);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getArguments() == null
				|| getArguments().getBoolean(Extra.CREATE_WORKER_FRAGMENT,
						true)) {
			FragmentManager fm = getFragmentManager();
			Fragment fragment = (POIsMapsforgeWorkerFragment) fm
					.findFragmentByTag(POIsMapsforgeWorkerFragment.TAG);
			if (fragment == null) {
				fragment = new POIsMapsforgeWorkerFragment();
				fm.beginTransaction()
						.add(fragment, POIsMapsforgeWorkerFragment.TAG)
						.commit();

			}
			mWorkerFragment = (WorkerFragment) fragment;
			mWorkerFragment.registerDisplayFragment(this);
		}

		if (savedInstanceState != null)
			executeState(savedInstanceState);
		else if (getArguments() != null)
			executeState(getArguments());
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		((MapsforgeMapActivity) getActivity()).destroyMapView(mMapView);
		mWorkerFragment.unregisterDisplayFragment(this);
		WheelmapApp.getSupportManager().cleanReferences();
		System.gc();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void executeBundle(Bundle bundle) {
		bundle.putBoolean(Extra.EXPLICIT_DIRECT_RETRIEVAL, true);
		executeState(bundle);
	}

	private void executeState(Bundle bundle) {
		if (bundle == null)
			return;

		boolean doRequest = false;

		if (bundle.containsKey(Extra.CENTER_MAP)) {
			int lat = bundle.getInt(Extra.LATITUDE);
			int lon = bundle.getInt(Extra.LONGITUDE);
			int zoom = bundle.getInt(Extra.ZOOM_MAP, MAP_ZOOM_DEFAULT);

			GeoPoint gp = new GeoPoint(lat, lon);
			mMapController.setCenter(gp);
			mMapView.setZoomListener(null);
			mMapController.setZoom(zoom);
			mMapView.setZoomListener(this);
			isCentered = true;
			oldZoomLevel = zoom;
			doRequest = true;
		}

		if (bundle.getBoolean(Extra.EXPLICIT_RETRIEVAL, false)) {
			mMapController.setZoom(MAP_ZOOM_DEFAULT);
			oldZoomLevel = MAP_ZOOM_DEFAULT;
			doRequest = true;
		}

		if (doRequest
				&& bundle.getBoolean(Extra.EXPLICIT_DIRECT_RETRIEVAL, false)) {
			requestUpdate();
			return;
		}

		if (doRequest)
			mMapView.getViewTreeObserver().addOnGlobalLayoutListener(
					new OnGlobalLayoutListener() {

						@Override
						public void onGlobalLayout() {
							requestUpdate();
							mMapView.getViewTreeObserver()
									.removeGlobalOnLayoutListener(this);
						}
					});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		GeoPoint gp = mMapView.getMapCenter();
		outState.putBoolean(Extra.CENTER_MAP, true);
		outState.putInt(Extra.LATITUDE, gp.getLatitudeE6());
		outState.putInt(Extra.LONGITUDE, gp.getLongitudeE6());
		outState.putInt(Extra.ZOOM_MAP, mMapView.getZoomLevel());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.ab_map_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
		case R.id.menu_search:
			showSearch();
			return true;
		case R.id.menu_location:
			centerMap(mLastGeoPointE6, true);
			requestUpdate();
			break;
		default:
			// noop
		}

		return false;
	}

	@Override
	public void onMove(float vertical, float horizontal) {
		GeoPoint centerLocation = mMapView.getMapCenter();
		int minimalLatitudeSpan = mMapView.getLatitudeSpan() / 3;
		int minimalLongitudeSpan = mMapView.getLongitudeSpan() / 3;

		if (mLastRequestedPosition != null
				&& (Math.abs(mLastRequestedPosition.getLatitudeE6()
						- centerLocation.getLatitudeE6()) < minimalLatitudeSpan)
				&& (Math.abs(mLastRequestedPosition.getLongitudeE6()
						- centerLocation.getLongitudeE6()) < minimalLongitudeSpan))
			return;

		if (mMapView.getZoomLevel() < ZOOMLEVEL_MIN)
			return;

		requestUpdate();
	}

	@Override
	public void onZoom(byte zoomLevel) {
		boolean isZoomedEnough = true;

		if (zoomLevel < ZOOMLEVEL_MIN) {
			isZoomedEnough = false;
			oldZoomLevel = zoomLevel;
			return;
		}

		if (zoomLevel < oldZoomLevel) {
			isZoomedEnough = false;
		}

		if (isZoomedEnough && zoomLevel >= oldZoomLevel) {
			oldZoomLevel = zoomLevel;
			return;
		}

		requestUpdate();
		isZoomedEnough = true;
		oldZoomLevel = zoomLevel;
	}

	private void requestUpdate() {
		Bundle extras = fillExtrasWithBoundingRect();
		mWorkerFragment.requestUpdate(extras);
	}

	private Bundle fillExtrasWithBoundingRect() {
		Bundle bundle = new Bundle();

		int latSpan = (int) (mMapView.getLatitudeSpan() * SPAN_ENLARGEMENT_FAKTOR);
		int lonSpan = (int) (mMapView.getLongitudeSpan() * SPAN_ENLARGEMENT_FAKTOR);
		GeoPoint center = mMapView.getMapCenter();
		mLastRequestedPosition = center;
		ParceableBoundingBox boundingBox = new ParceableBoundingBox(
				center.getLatitudeE6() + (latSpan / 2), center.getLongitudeE6()
						+ (lonSpan / 2),
				center.getLatitudeE6() - (latSpan / 2), center.getLongitudeE6()
						- (lonSpan / 2));
		bundle.putSerializable(Extra.BOUNDING_BOX, boundingBox);

		return bundle;
	}

	private void centerMap(GeoPoint geoPoint, boolean force) {
		if (!isCentered || force) {
			mMapController.setCenter(geoPoint);
			isCentered = true;
		}

		// we got the first time current position so center map on it
		if (mLastGeoPointE6 == null && !isCentered) {
			mMapController.setCenter(geoPoint);
			isCentered = true;
		}
	}

	private void setCursor(Cursor cursor) {
		Log.d(TAG, "setCursor cursor "
				+ ((cursor != null) ? cursor.hashCode() : "null") + " count = "
				+ ((cursor != null) ? cursor.getCount() : "null")
				+ " isNewCursor = " + (cursor != mCursor));
		if (cursor == mCursor)
			return;

		mCursor = cursor;
		mPoisItemizedOverlay.setCursor(cursor);
	}

	@Override
	public void onTap(OverlayItem item, long poiId) {
		if (mListener != null)
			mListener.onShowDetail(poiId);
	}

	private void showSearch() {
		FragmentManager fm = getActivity().getSupportFragmentManager();
		SearchDialogFragment searchDialog = SearchDialogFragment.newInstance(
				false, true);

		searchDialog.setTargetFragment(this, 0);
		searchDialog.show(fm, SearchDialogFragment.TAG);
	}

	@Override
	public void onSearch(Bundle bundle) {
		Bundle boundingBoxExtras = fillExtrasWithBoundingRect();
		bundle.putAll(boundingBoxExtras);

		mWorkerFragment.requestSearch(bundle);
	}

	@Override
	public void onUpdate(WorkerFragment fragment) {
		setCursor(fragment.getCursor());

		if (mListener != null)
			mListener.onRefreshing(fragment.isRefreshing());
	}

	@Override
	public void setCurrentLocation(GeoPoint point, Location location) {
		mLastGeoPointE6 = point;
		mCurrLocationOverlay.setLocation(mLastGeoPointE6,
				location.getAccuracy());
		centerMap(point, false);
	}

}