package com.ivanmedic.toplinskamapa;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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

    LatLngParser gpsDataParser;

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

        gpsDataParser = new LatLngParser(this);

        // Uzima SupportMapFragment i obavještava kad je mapa spremna za upotrebu.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Programski dohvat tipaka sa tipa prikaza:
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

        //Kreiranje namjere za primanje ažuriranja o lokaciji:
        serviceIntent = new Intent(this, LocationService.class);  //Namjera se šalje servisu LocationService
        IntentFilter filter = new IntentFilter(); //definiramo svojstva
        filter.addAction("LOCATION_UPDATE");     // namjere
        LocationReceiver receiver = new LocationReceiver(); //Kreira se instanca primatelja prijenosa
        registerReceiver(receiver, filter);  //Registrira se primaetlj prijenosa
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

            //Ukoliko id pritisnute tipke odgovara case id-u izvrši sljedeći kod:
            case R.id.main_activity_start_recording: {
                //Pokretanje servisa
                startService(serviceIntent);

                //Pojedine tipke se onesposobe tako da se ne mogu pritisnuti
                stopRecordingButton.setEnabled(true);
                startRecordingButton.setEnabled(false);
                insertMockDataButton.setEnabled(false);
                deleteRecordsButton.setEnabled(false);
                insertGpxButton.setEnabled(false);

                //Pritiskom na tipku prikazat će se tekst u oblačicu
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
                    gpsData.addAll(gpsDataParser.parseJsonResource(R.raw.mock));

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_GPX_FILE) {
            if (resultCode == RESULT_OK) {
                Uri dataUri;

                if(data.getData() == null) {
                    ClipData dataItems = data.getClipData();

                    for (int i = 0; i < dataItems.getItemCount(); i++) {
                        try {
                            dataUri = dataItems.getItemAt(i).getUri();

                            gpsData.addAll(gpsDataParser.parseGpx(dataUri));

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
                        dataUri = data.getData();

                        gpsData.addAll(gpsDataParser.parseGpx(dataUri));

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
}