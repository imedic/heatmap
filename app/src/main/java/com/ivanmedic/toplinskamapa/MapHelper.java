package com.ivanmedic.toplinskamapa;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MapHelper {
    private GoogleMap map;
    private HeatmapTileProvider provider;
    private TileOverlay overlay;

    public MapHelper(GoogleMap map) {
        this.map = map;
    }

    public void addHeatMap(ArrayList<LatLng> data) {
        int[] colors = {
                Color.rgb(102,187,106),
                Color.rgb(251,140,0),
                Color.rgb(230,81,0),
                Color.rgb(183,28,28)
        };

        float[] startPoints = {
                0.1f, 0.6f, 0.95f, 1f
        };

        Gradient gradient = new Gradient(colors, startPoints);

        try {
            provider = new HeatmapTileProvider.Builder()
                    .data(data)
                    .gradient(gradient)
                    .radius(10)
                    .build();

            overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
            provider.setOpacity(0.6);
        } catch (Exception e) {
            Log.d("Heat map", e.getMessage());
        }
    }

    public void refreshMap(ArrayList<LatLng> data) {
        if(map != null) {
            map.clear();
            addHeatMap(data);
        }
    }

    public void clearMap() {
        if(map != null) {
            map.clear();
        }
    }

    public void moveMapCamera(LatLng coordinates) {
        map.animateCamera(CameraUpdateFactory.newLatLng(coordinates));
    }

    public void takeScreenshot() {
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            public void onMapLoaded() {
                final String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

                map.snapshot(new GoogleMap.SnapshotReadyCallback() {
                    public void onSnapshotReady(Bitmap bitmap) {
                        FileOutputStream out = null;
                        try {
                            File folder = new File(Environment.getExternalStorageDirectory(), "heatmaps");
                            if(!folder.exists()) folder.mkdir();

                            File outputFile = new File(Environment.getExternalStorageDirectory(), "heatmaps/heatmap " + date + ".png");
                            out = new FileOutputStream(outputFile);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                    }
                });
            }
        });
    }
}
