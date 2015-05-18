package hdi.foresight;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaCameraChangeListener;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaChangeListener;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;



public class StreetActivity extends FragmentActivity implements OnStreetViewPanoramaReadyCallback, GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback, OnStreetViewPanoramaChangeListener, OnStreetViewPanoramaCameraChangeListener {

    StreetViewPanoramaFragment mStreetView;

    //private static final LatLng SAN_FRAN = new LatLng(37.765927, -122.449972);

    //private static final LatLng SYDNEY = new LatLng(-33.8, 151);

    private static final LatLng COLLEGE_PARK = new LatLng(38.987565, -76.941398);

    private static LatLng currentLocation = COLLEGE_PARK;

    private static float currentOrientation = 0;


    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));
//
//    private static final LatLngBounds BOUNDS_SAN_FRAN = new LatLngBounds(
//            new LatLng(35.765927, -123.449972), new LatLng(38.765927, -123.449972));


    private GoogleApiClient mGoogleApiClient;

    private MapFragment mapFragment;

    private static final String TAG = "PLACE FINDER";

    private Context context;

    private MarkerOptions locationMarkerOptions = new MarkerOptions().position(COLLEGE_PARK).rotation(currentOrientation);

    private Marker locationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Some buildings have indoor maps. Center the camera over
        // the building, and a floor picker will automatically appear.
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(COLLEGE_PARK, 15));

        // add marker
        locationMarker = map.addMarker(locationMarkerOptions)
                ;
        ;
    }

    private void setUpMapIfNeeded() {

        if (mapFragment == null) {

            mapFragment = (MapFragment) getFragmentManager()
                    .findFragmentById(R.id.normalmap);

            mapFragment.getMapAsync(this);

        }


        if (mStreetView == null) {

            mStreetView =
                    (StreetViewPanoramaFragment) getFragmentManager()
                            .findFragmentById(R.id.streetmap);

            mStreetView.getStreetViewPanoramaAsync(this);

        }

        if (mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(LocationServices.API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addConnectionCallbacks(this)
                    .build();

        }

        mGoogleApiClient.connect();

        context = this.getApplicationContext();

    }


    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {

        panorama.setPosition(COLLEGE_PARK);

        panorama.setOnStreetViewPanoramaCameraChangeListener(this);

        panorama.setOnStreetViewPanoramaChangeListener(this);

        StreetViewPanoramaCamera camera = panorama.getPanoramaCamera();

        Log.i(TAG, "ORIGINAL ORIENTATION" + camera.getOrientation());

    }

    @Override
    public void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) {

        if (location != null) {

            Log.i(TAG, "LOCATION CHANGED "+ location.position.latitude + ", "+ location.position.longitude );

            currentLocation = new LatLng(location.position.latitude, location.position.longitude);

            locationMarker.setPosition(currentLocation);

        }
    }

    @Override
    public void onStreetViewPanoramaCameraChange(StreetViewPanoramaCamera camera) {

        Log.i(TAG, "ORIENTATION CHANGED" + camera.getOrientation());

        currentOrientation = camera.getOrientation().bearing;

        locationMarker.setRotation(currentOrientation);

    }

    @Override
    public void onConnected(Bundle bundle) {

//        String[] data = new String[1];
//        data[0] = "check";
//        new GetPlaceTask().execute(data);

        new GetYelpData().execute();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    // Getting a list from Yelp API
    private class GetYelpData extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... data) {

            Yelp yelp = Yelp.getYelp(StreetActivity.this);

            String businesses = yelp.search("food", currentLocation.latitude, currentLocation.longitude);

            try {

                return processJson(businesses);

            } catch (JSONException e) {

                return businesses;

            }
        }

        @Override
        protected void onPostExecute(String result) {

            //mSearchResultsText.setText(result);
            Log.i(TAG, result);

            setProgressBarIndeterminateVisibility(false);

        }
    }

    String processJson(String jsonStuff) throws JSONException {

        JSONObject json = new JSONObject(jsonStuff);
        JSONArray businesses = json.getJSONArray("businesses");
        ArrayList<String> businessNames = new ArrayList<String>(businesses.length());
        for (int i = 0; i < businesses.length(); i++) {
            JSONObject business = businesses.getJSONObject(i);
            businessNames.add(business.getString("name"));
        }
        return TextUtils.join("\n", businessNames);

    }




    // Neglecting this part for now
    private class GetPlaceTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String[] data) {

            PendingResult<AutocompletePredictionBuffer> results = Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, "restaurant sydney", BOUNDS_GREATER_SYDNEY, null);

            // This method should have been called off the main UI thread. Block and wait for at most 60s
            // for a result from the API.

            AutocompletePredictionBuffer autocompletePredictions = results
                    .await(60, TimeUnit.SECONDS);

            // Confirm that the query completed successfully, otherwise return null
            final com.google.android.gms.common.api.Status status = autocompletePredictions.getStatus();

            if (!status.isSuccess()) {

//                Toast.makeText(context, "Error contacting API: " + status.toString(),
//                        Toast.LENGTH_SHORT).show();

                Log.e(TAG, "Error getting autocomplete prediction API call: " + status.toString());

                autocompletePredictions.release();

            }

            Log.i(TAG, "Query completed. Received " + autocompletePredictions.getCount()
                    + " predictions.");

            Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();

            ArrayList resultList = new ArrayList<>(autocompletePredictions.getCount());

            while (iterator.hasNext()) {

                AutocompletePrediction prediction = iterator.next();
                // Get the details of this prediction and copy it into a new PlaceAutocomplete object.

                resultList.add(new PlaceAutocomplete(prediction.getPlaceId(),
                        prediction.getDescription()));

                Places.GeoDataApi.getPlaceById(mGoogleApiClient, prediction.getPlaceId())

                        .setResultCallback(new ResultCallback<PlaceBuffer>() {
                            @Override
                            public void onResult(PlaceBuffer places) {
                                if (places.getStatus().isSuccess()) {

                                    final Place myPlace = places.get(0);

                                    List<String> input = new ArrayList<String>();
                                    input.add(myPlace.getId());

                                    PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, new PlaceFilter(false, input));

                                    result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                                        @Override
                                        public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                                            for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                                                Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                                                        placeLikelihood.getPlace().getName(),
                                                        placeLikelihood.getLikelihood()));
                                            }
                                            likelyPlaces.release();
                                        }
                                    });

                                    Log.i(TAG, "Place found: "+ places.getCount() + "; name: " + myPlace.getName());

                                }
                                places.release();
                            }
                        });
            }

            Log.i(TAG, "Results " + resultList.size()
                    + " predictions.");

            autocompletePredictions.release();

            return null;
        }

        class PlaceAutocomplete {

            public CharSequence placeId;
            public CharSequence description;

            PlaceAutocomplete(CharSequence placeId, CharSequence description) {
                this.placeId = placeId;
                this.description = description;
            }

            @Override
            public String toString() {
                return description.toString();
            }
        }
    }
}
