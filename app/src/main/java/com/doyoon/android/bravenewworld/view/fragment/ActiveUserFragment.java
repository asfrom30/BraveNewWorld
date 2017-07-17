package com.doyoon.android.bravenewworld.view.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.doyoon.android.bravenewworld.R;
import com.doyoon.android.bravenewworld.domain.firebase.geovalue.ActiveUser;
import com.doyoon.android.bravenewworld.domain.firebase.value.UserProfile;
import com.doyoon.android.bravenewworld.presenter.Presenter;
import com.doyoon.android.bravenewworld.presenter.base.fragment.PermissionFragment;
import com.doyoon.android.bravenewworld.presenter.dialog.PickmeRequestSendingDialog;
import com.doyoon.android.bravenewworld.presenter.interfaces.ActiveUserListUIController;
import com.doyoon.android.bravenewworld.presenter.interfaces.ActiveUserMapController;
import com.doyoon.android.bravenewworld.util.Const;
import com.doyoon.android.bravenewworld.util.LogUtil;
import com.doyoon.android.bravenewworld.view.UserSelectFragmentView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by DOYOON on 7/10/2017.
 */

public class ActiveUserFragment extends PermissionFragment implements OnMapReadyCallback, ActiveUserMapController, ActiveUserListUIController {

    private static String TAG = ActiveUserFragment.class.getSimpleName();
    private static int linkRes = R.layout.fragment_user_select_map;

    /* View */
    private UserSelectFragmentView mActiveUserListView;
    private MapView mMapView;
    private GoogleMap mGoogleMap;

    private List<UserProfile> displayUserList = new ArrayList();

    /* Shared Preference */
    private double SEARCH_DISTANCE_KM = 100;
    private float CURRENT_CAMERA_ZOOM = Const.DEFAULT_CAMERA_ZOOM;

    public static ActiveUserFragment newInstance() {

        Bundle args = new Bundle();

        ActiveUserFragment fragment = new ActiveUserFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private View baseView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.logLifeCycle(TAG, "on Create");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LogUtil.logLifeCycle(TAG, "onCreateView()");

        /* Link to Presenter */
        Presenter.getInstance().setActiveUserListUIController(this);
        Presenter.getInstance().setActiveUserMapController(this);

        /* Layout Inflating */
        baseView = inflater.inflate(R.layout.fragment_user_select_map, container, false);
        this.mActiveUserListView = new UserSelectFragmentView(this, getContext(), baseView);

        /* Get Default Setting  */
        SEARCH_DISTANCE_KM = Const.DEFAULT_SEARCH_DISTANCE_KM;

        /* Map View Dependency */
        // Gets the MapView from the XML layout and creates it
        mMapView = (MapView) baseView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        // Gets to GoogleMap from the MapView and does initialization stuff
        mMapView.getMapAsync(this);

        if (displayUserList.size() != 0) {
            notifySetChanged();
        }

        Log.e(TAG, "display user list size is ... " + displayUserList.size());


        // todo move to pre Select Fragment
        checkRuntimePermission(new Callback() {
            @Override
            public void runWithPermission() {

            }

            @Override
            public void runWithoutPermission() {

            }
        });

        return baseView;
    }

    public void onActiveUserItemClicked(UserProfile clickedUserProfile) {
        int userType = Presenter.getInstance().getUserType();
        DialogFragment dialogFragment = new PickmeRequestSendingDialog( userType
                , clickedUserProfile
                , new PickmeRequestSendingDialog.Callback() {
                    /* Show Detail Profile */

                    /* Sending */
        });

        dialogFragment.show(getFragmentManager(), null);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;
        setDefaultMapSetting(mGoogleMap);

        //noinspection MissingPermission
        mGoogleMap.setMyLocationEnabled(true);
        setFocusMyLatlng();

        if (Presenter.getInstance().getActiveUserMap() != null) {
            resetMarker(Presenter.getInstance().getActiveUserMap());
        }
    }

    private void setFocusMyLatlng(){
        if (mGoogleMap == null) {
            Log.i(TAG, "mGoogleMap is null, Can't focus Last Lat Lng");
            return;
        }

        Presenter.getInstance().getLastLocation(new Presenter.LocationCallback() {
            @Override
            public void execute(LatLng latLng) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, CURRENT_CAMERA_ZOOM);
                mGoogleMap.moveCamera(cameraUpdate);
                Log.i(TAG, "Set Focus at my last location" + latLng.toString());
            }
        });
    }

    private void setDefaultMapSetting(GoogleMap googleMap){
        // googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);

    }

    private int getMapPinResId(){
        int userType = Presenter.getInstance().getUserType();

        if (userType == Const.UserType.Taker) {
            return Const.MAP_SETTING.GIVER_MAP_PIN_RES_ID;
        } else if (userType == Const.UserType.Giver) {
            return Const.MAP_SETTING.TAKER_MAP_PIN_RES_ID;
        } else {
            throw new IllegalStateException("Presenter User Type is not declared, can't get Map Pin Resource ID`");
        }
    }

    /* Activity Life Cycler */
    @Override
    public void onResume() {
        LogUtil.logLifeCycle(TAG, "on Resume");
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        LogUtil.logLifeCycle(TAG, "onPause");
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onStop() {
        LogUtil.logLifeCycle(TAG, "onStop");
        super.onStop();
    }



    @Override
    public void onDestroy() {
        LogUtil.logLifeCycle(TAG, "onDestroy");
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    /* Getter and Setter */
    public List<UserProfile> getDataList() {
        return this.displayUserList;
    }

    /* Interface for presenter */
    @Override
    public void addActiveUser(UserProfile userProfile) {
        displayUserList.add(userProfile);
    }

    @Override
    public void notifySetChanged() {
        mActiveUserListView.notifyDataListChanged();
    }

    @Override
    public void resetMarker (Map<String, ActiveUser> activeUserMap){
        if (this.mGoogleMap == null) {
            return;
        }

        this.mGoogleMap .clear();
        for (Map.Entry<String, ActiveUser> entry : activeUserMap.entrySet()) {
            ActiveUser activeUser = entry.getValue();

            if(activeUser.isActive()){
                this.mGoogleMap.addMarker(new MarkerOptions()
                        .position(activeUser.getLatLng())
                        .alpha(Const.MAP_SETTING.MARKER_ALPHA)
                        .icon(BitmapDescriptorFactory.fromResource(getMapPinResId())));
            }
        }
    }
}




