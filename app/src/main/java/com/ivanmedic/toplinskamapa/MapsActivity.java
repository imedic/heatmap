package com.ivanmedic.toplinskamapa;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    MapHelper mapHelper;

    private ArrayList<LatLng> gpsData = new ArrayList<>();

    Boolean isFirstLocationReading = true;

    Intent serviceIntent;

    static final int PICK_GPX_FILE = 1;

    public Button startRecordingButton;
    public Button stopRecordingButton;
    public Button deleteRecordsButton;
    public Button insertMockDataButton;
    public Button insertGpxButton;
    public Button takeScreenshotButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        startRecordingButton = (Button) findViewById(R.id.main_activity_start_recording);
        startRecordingButton.setOnClickListener(this);

        stopRecordingButton = (Button) findViewById(R.id.main_activity_stop_recording);
        stopRecordingButton.setOnClickListener(this);
        stopRecordingButton.setEnabled(false);

        deleteRecordsButton = (Button) findViewById(R.id.main_activity_delete_recording);
        deleteRecordsButton.setOnClickListener(this);

        insertMockDataButton = (Button) findViewById(R.id.main_activity_mock_data);
        insertMockDataButton.setOnClickListener(this);

        insertGpxButton = (Button) findViewById(R.id.main_activity_gpx);
        insertGpxButton.setOnClickListener(this);

        takeScreenshotButton = (Button) findViewById(R.id.main_activity_take_screenshot);
        takeScreenshotButton.setOnClickListener(this);


        serviceIntent = new Intent(this, LocationService.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction("LOCATION_UPDATE");
        LocationReceiver receiver = new LocationReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(serviceIntent);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapHelper = new MapHelper(googleMap);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_activity_start_recording: {
                startService(serviceIntent);

                stopRecordingButton.setEnabled(true);
                startRecordingButton.setEnabled(false);
                insertMockDataButton.setEnabled(false);
                deleteRecordsButton.setEnabled(false);
                insertGpxButton.setEnabled(false);

                Toast.makeText(this, "Očitavanje lokacije", Toast.LENGTH_SHORT).show();

                break;
            }

            case R.id.main_activity_stop_recording: {
                stopService(serviceIntent);

                startRecordingButton.setEnabled(true);
                insertMockDataButton.setEnabled(true);
                deleteRecordsButton.setEnabled(true);
                insertGpxButton.setEnabled(true);
                stopRecordingButton.setEnabled(false);

                isFirstLocationReading = true;

                Toast.makeText(this, "Praćenje lokacije ugašeno", Toast.LENGTH_SHORT).show();

                break;
            }

            case R.id.main_activity_delete_recording: {
                gpsData.clear();
                mapHelper.clearMap();
                break;
            }

            case R.id.main_activity_mock_data: {
                try {
                    gpsData.addAll(getLatLngFromJSON(R.raw.mock));

                    mapHelper.addHeatMap(gpsData);

                    mapHelper.moveMapCamera(gpsData.get(gpsData.size() - 1));
                } catch (JSONException e) {
                    Toast.makeText(this, "Dogodila se nekakva pogreška.", Toast.LENGTH_LONG).show();
                }

                break;
            }

            case R.id.main_activity_gpx: {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                intent.setType("*/*");

                startActivityForResult(Intent.createChooser(intent, "Select GPX file"), PICK_GPX_FILE);

                break;
            }

            case R.id.main_activity_take_screenshot: {
                mapHelper.takeScreenshot();
                Toast.makeText(this, "Mapa uslikana", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    public class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Double lat = intent.getDoubleExtra("Latitude", 0);
            Double lng = intent.getDoubleExtra("Longitude", 0);

            if(isFirstLocationReading) {
                mapHelper.moveMapCamera(new LatLng(lat, lng));
                isFirstLocationReading = false;
            }

            gpsData.add(new LatLng(lat, lng));

            Toast.makeText(context, lat + " " + lng, Toast.LENGTH_SHORT).show();

            mapHelper.refreshMap(gpsData);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_GPX_FILE) {
            if (resultCode == RESULT_OK) {
                InputStream is;

                if(data.getData() == null) {
                    ClipData dataItems = data.getClipData();

                    for (int i = 0; i < dataItems.getItemCount(); i++) {
                        try {
                            is = getContentResolver().openInputStream(dataItems.getItemAt(i).getUri());

                            gpsData.addAll(getLatLngFromGpx(is));

                            if (gpsData != null) {
                                mapHelper.refreshMap(gpsData);
                                mapHelper.moveMapCamera(gpsData.get(gpsData.size() - 1));
                            }

                        } catch (Exception e) {
                            Toast.makeText(this, "Dogodila se pogreška pri očitavanju datoteke", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    try {
                        is = getContentResolver().openInputStream(data.getData());

                        gpsData.addAll(getLatLngFromGpx(is));

                        if (gpsData != null) {
                            mapHelper.refreshMap(gpsData);
                            mapHelper.moveMapCamera(gpsData.get(gpsData.size() - 1));
                        }

                    } catch (Exception e) {
                        Toast.makeText(this, "Dogodila se pogreška pri očitavanju datoteke", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private ArrayList<LatLng> getLatLngFromGpx(InputStream gpxFile) {
        ArrayList<LatLng> inputData = new ArrayList<>();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        String[] gpxTypes = {"wpt", "trkpt"};

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(gpxFile);
            Element elementRoot = document.getDocumentElement();

            for (String gpxType : gpxTypes) {
                NodeList nodelist_trkpt = elementRoot.getElementsByTagName(gpxType);

                for (int j = 0; j < nodelist_trkpt.getLength(); j++) {

                    Node node = nodelist_trkpt.item(j);
                    NamedNodeMap attributes = node.getAttributes();

                    String lat = attributes.getNamedItem("lat").getTextContent();
                    Double lat_double = Double.parseDouble(lat);

                    String lng = attributes.getNamedItem("lon").getTextContent();
                    Double lng_double = Double.parseDouble(lng);

                    LatLng newLocation = new LatLng(lat_double, lng_double);

                    inputData.add(newLocation);
                }
            }
        }
        catch (Exception e) {
            Toast.makeText(this, "Dogodila se pogreška", Toast.LENGTH_LONG).show();
        }

        return inputData;
    }

    private ArrayList<LatLng> getLatLngFromJSON(int resource) throws JSONException {

        ArrayList<LatLng> list = new ArrayList<>();

        InputStream inputStream = getResources().openRawResource(resource);

        String json = new Scanner(inputStream).useDelimiter("\\A").next();

        JSONArray array = new JSONArray(json);

        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new LatLng(lat, lng));
        }

        return list;
    }
}