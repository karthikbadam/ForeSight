package hdi.foresight;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
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
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class StreetActivity extends FragmentActivity implements OnStreetViewPanoramaReadyCallback, GoogleApiClient.ConnectionCallbacks {

    StreetViewPanoramaFragment mStreetView;

    private static final LatLng SAN_FRAN = new LatLng(37.765927, -122.449972);

    private static final LatLng SYDNEY = new LatLng(-33.8, 151);

    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));

    private static final LatLngBounds BOUNDS_SAN_FRAN = new LatLngBounds(
            new LatLng(35.765927, -123.449972), new LatLng(38.765927, -123.449972));


    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = "PLACE FINDER";
    private Context context;

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

    private void setUpMapIfNeeded() {
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

//        Settings.Secure.putString(getContentResolver(),
//                Settings.Secure.ALLOW_MOCK_LOCATION, "0");
    }


    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {

        panorama.setPosition(SYDNEY);

    }

    @Override
    public void onConnected(Bundle bundle) {

        String[] data = new String[1];

        data[0] = "check";

        new GetPlaceTask().execute(data);


        Location newLocation = new Location(LocationManager.GPS_PROVIDER);

        newLocation.setLatitude(SYDNEY.latitude);
        newLocation.setLongitude(SYDNEY.longitude);
        newLocation.setProvider(LocationManager.GPS_PROVIDER);
        newLocation.setAccuracy(1);
        newLocation.setTime(new Date().getTime());
        newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        //LocationServices.FusedLocationApi.setMockLocation(mGoogleApiClient, newLocation);

    }

    @Override
    public void onConnectionSuspended(int i) {

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
