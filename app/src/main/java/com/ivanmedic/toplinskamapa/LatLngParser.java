package com.ivanmedic.toplinskamapa;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

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

public class LatLngParser {
    private Context context;

    public LatLngParser(Context context) {
        this.context = context;
    }


    public ArrayList<LatLng> parseGpx(Uri gpxFileUri) {
        ArrayList<LatLng> inputData = new ArrayList<>();
        String[] gpxTypes = {"wpt", "trkpt"};
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            InputStream gpxFileStream = context.getContentResolver().openInputStream(gpxFileUri);

            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(gpxFileStream);
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

            gpxFileStream.close();
        }
        catch (Exception e) {
            Toast.makeText(context, "Dogodila se pogreška", Toast.LENGTH_LONG).show();
        }

        return inputData;
    }



    public ArrayList<LatLng> parseJsonResource(int resource) throws JSONException {

        ArrayList<LatLng> list = new ArrayList<>();

        InputStream inputStream = context.getResources().openRawResource(resource);

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
