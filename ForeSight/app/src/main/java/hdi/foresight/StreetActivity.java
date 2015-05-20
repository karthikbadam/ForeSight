package hdi.foresight;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;



public class StreetActivity extends FragmentActivity implements OnStreetViewPanoramaReadyCallback, GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback, OnStreetViewPanoramaChangeListener, OnStreetViewPanoramaCameraChangeListener {

    StreetViewPanoramaFragment mStreetView;

    //private static final LatLng SAN_FRAN = new LatLng(37.765927, -122.449972);

    //private static final LatLng SYDNEY = new LatLng(-33.8, 151);

    private static final LatLng COLLEGE_PARK = new LatLng(38.987565, -76.941398);

    private static final LatLng DC = new LatLng(38.900349, -77.044812);

    private static LatLng currentLocation = DC;

    private static float currentOrientation = 0;


    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));

    private GoogleApiClient mGoogleApiClient;

    private MapFragment mapFragment;

    private static final String TAG = "PLACE FINDER";

    private Context context;

    private MarkerOptions locationMarkerOptions = new MarkerOptions().position(DC).rotation(currentOrientation);

    private Marker locationMarker;

    private HashMap<String, LatLng> mCache = new HashMap<String, LatLng>();

    private LinearLayout debugLayout;

    private DebugCanvas debugCanvas;


    // thread for bluetooth message passing
    RemoteClientThread bluetoothThread;

    // Data to be passed

    private int leftCount = 1;
    private int rightCount = 1;
    private int frontCount = 1;

    private float leftDistance = (float) 0.5;
    private float rightDistance = (float) 0.5;
    private float frontDistance = (float) 0.5;

    private int connectionTrials = 0;
    long startTrialTime = 0;

    public class RemoteClientThread extends Thread{
        public RemoteClientThread(){

        }

        long prevTime = -1;

        @SuppressLint("NewApi")
        public void run(){

            startTrialTime = System.currentTimeMillis();

            if(arduinoDev!=null){
                while(running){
                    try{
                        if(remoteConnected){
                            if(remoteSocket!=null){
                                if(!remoteSocket.isConnected()){
                                    remoteConnected = false;
                                    continue;
                                }

                                String msg = leftCount + "," + rightCount + "," + frontCount;
                                remoteOutStream.write(msg.getBytes("UTF-8"));
                                remoteOutStream.flush();
                                //remoteOutStream.write((byte) 0);
                                //remoteOutStream.flush();

                                Log.i("BLUETOOTH TAG", "Data sent...");

                                debugCanvas.postInvalidate();

//                                leftCount = (leftCount + 1)%10;
//                                rightCount = (leftCount + 3)%10;
//                                frontCount = (leftCount + 2)%10;

                                Thread.sleep(3000);
                            }

                        } else {

                            //if(remoteSocket!=null) remoteSocket.close();
                            remoteSocket = arduinoDev.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID_REMOTE));
                            bluetoothStatus = "Waiting..." + connectionTrials;
                            connectionTrials++;

                            Log.i("BLUETOOTH TAG", "Waiting...");

                            debugCanvas.postInvalidate();
                            remoteSocket.connect();

                            if(remoteSocket.isConnected()){
                                bluetoothStatus = "Connected!!";
                                remoteInStream = remoteSocket.getInputStream();
                                remoteOutStream = remoteSocket.getOutputStream();
                                br_remote = new BufferedReader(new InputStreamReader(remoteInStream));
                                bw_remote = new BufferedWriter(new OutputStreamWriter(remoteOutStream));
                                remoteConnected = true;
                                connectionTrials = 0;
                            }
                        }
                        debugCanvas.postInvalidate();

                    } catch (Exception e){
                        e.printStackTrace();
                        //remoteConnected = false;
                    }
                }
            } else {
                bluetoothStatus = "no device detected";
                debugCanvas.postInvalidate();
            }
        }

        public boolean running = true;
        public void stopRun(){

        }
    }


    class DebugCanvas extends View {

        Paint bgPaint = new Paint();
        Paint msgPaint = new Paint();

        public DebugCanvas(Context context) {
            super(context);

            bgPaint.setColor(Color.TRANSPARENT);
            msgPaint.setColor(Color.BLACK);
            msgPaint.setTextSize(70);
        }

        public void onDraw(Canvas canvas){

            canvas.drawRect(0, 0, 900, 470, bgPaint);

            canvas.drawText("BTC:", 100, 60, msgPaint);
            canvas.drawText(bluetoothStatus, 300, 60, msgPaint);

            canvas.drawText("MSG:", 100, 150, msgPaint);
            canvas.drawText("[L] " + leftCount + " [R] " + rightCount + " [F] " + frontCount, 300, 150, msgPaint);

        }
    }

    // bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothDevice arduinoDev;

    private BluetoothServerSocket remoteServerSocket;

    private BluetoothSocket remoteSocket = null;

    private InputStream remoteInStream;

    OutputStream remoteOutStream;

    BufferedWriter bw_remote;

    BufferedReader br_remote;

    String MY_UUID_REMOTE = "00001101-0000-1000-8000-00805f9b34fb";

    boolean remoteConnected = false;

    String bluetoothStatus = "NC";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street);
        setUpMapIfNeeded();

        // set up bluetooth
        setUpBluetoothIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setUpBluetoothIfNeeded();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void setUpBluetoothIfNeeded() {

        if (debugLayout == null) {

            debugLayout = (LinearLayout) findViewById(R.id.debugconsole);
            debugCanvas = new DebugCanvas(this);

            debugLayout.addView(debugCanvas);
        }

        //if (mBluetoothAdapter == null) {

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    if (deviceName.startsWith("RNBT")) arduinoDev = device;
                }
            }

            bluetoothThread = new RemoteClientThread();
            bluetoothThread.start();

        //}

    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Some buildings have indoor maps. Center the camera over
        // the building, and a floor picker will automatically appear.
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DC, 14));

        // add marker
        locationMarker = map.addMarker(locationMarkerOptions);
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

        panorama.setPosition(DC);

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

            new GetYelpData().execute();

            //redrawMap();

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


    public void redrawMap () {

        GoogleMap map = mapFragment.getMap();

        map.clear();

        locationMarker = map.addMarker(locationMarkerOptions);

        locationMarker.setPosition(currentLocation);

        locationMarker.setRotation(currentOrientation);

        Set<String> n = mCache.keySet();

        Iterator<String> it = n.iterator();

        while (it.hasNext()) {

            String name = it.next();

            LatLng l = mCache.get(name);

            map.addCircle(new CircleOptions().center(l).fillColor(Color.DKGRAY).strokeColor(Color.DKGRAY)).setRadius(10);

        }
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

            StreetActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    redrawMap();
                }
            });

            setProgressBarIndeterminateVisibility(false);

        }
    }

    String processJson(String jsonStuff) throws JSONException {

        JSONObject json = new JSONObject(jsonStuff);
        JSONArray businesses = json.getJSONArray("businesses");


        ArrayList<String> businessNames = new ArrayList<String>(businesses.length());

        mCache.clear();

        leftCount = 0;
        rightCount = 0;
        frontCount = 0;

        for (int i = 0; i < businesses.length(); i++) {

            JSONObject business = businesses.getJSONObject(i);

            String businessName = business.getString("name");

            businessNames.add(business.getString("name"));

            JSONObject location = business.getJSONObject("location").getJSONObject("coordinate");

            if (location != null) {

                float lat = Float.parseFloat(location.getString("latitude"));
                float lon = Float.parseFloat(location.getString("longitude"));

                LatLng hotelLocation = new LatLng(lat, lon);

                double angle = findAngle (currentLocation, hotelLocation);

                Log.i(TAG, "FOUND: " + businessName +  ", " + angle);

                mCache.put(businessName, hotelLocation);

                // left front right calculation

                if ( (angle > currentOrientation && angle < currentOrientation + 45) || (angle > currentOrientation - 45 && angle < currentOrientation)) {
                    //front
                    frontCount++;
                }

                if (angle > currentOrientation + 45 && angle < currentOrientation + 120) {
                    //right
                    rightCount++;
                }

                if (angle > currentOrientation + 180 && angle < currentOrientation + 255) {
                    //left
                    leftCount++;
                }

            }

            debugCanvas.postInvalidate();

        }

        return TextUtils.join("\n", businessNames);

    }


    double findDistance (LatLng source, LatLng destination) {

        double R = 3963.1676;

        //Source
        double lat1 = source.latitude;
        double lng1 = source.longitude;

        // destination
        double lat2 = destination.latitude;
        double lng2 = destination.longitude;

        double dLon = lng2 - lng1;
        double dLat = lat2 - lat1;

        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(lat1) * Math.cos(lat2) *
                                Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;

        return d; // Distance in miles
    }

    double findAngle (LatLng source, LatLng destination) {

        //Source
        double lat1 = source.latitude;
        double lng1 = source.longitude;

        // destination
        double lat2 = destination.latitude;
        double lng2 = destination.longitude;

        double dLon = (lng2-lng1);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.toDegrees((Math.atan2(y, x)));
        brng = (360 - ((brng + 360) % 360));

        return brng;
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
