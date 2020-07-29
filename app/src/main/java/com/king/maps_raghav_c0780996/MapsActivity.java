package com.king.maps_raghav_c0780996;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,  GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener {

    private static final int req_code = 1;
    private static final int polygone_sides = 4;
    LocationManager locationManager;
    LocationListener locationListener;
    private GoogleMap mMap;
    List<Marker> cityMarkers = new ArrayList<>();
    ArrayList<Character> letterList = new ArrayList<>();
    Polygon design;
    List<Marker> listOfMarkers = new ArrayList<>();
    List<Marker> markerDistance = new ArrayList<>();
    ArrayList<Polyline> listOfPolylines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public BitmapDescriptor displayText(String text) {
        Paint textPaint = new Paint();

        textPaint.setTextSize(48);
        float textWidth = textPaint.measureText(text);
        float textHeight = textPaint.getTextSize();
        int width = (int) (textWidth);
        int height = (int) (textHeight);

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.translate(0, height);

        canvas.drawText(text, 0, 0, textPaint);
        return BitmapDescriptorFactory.fromBitmap(image);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnPolylineClickListener(this);
        mMap.setOnPolygonClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (!hasLocationPermission()) {
            reqLocPermission();
        } else {
            startUpdatingLoc();
            LatLng zoomLocation = new LatLng( 43.651070,-79.347015);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoomLocation, 5));
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                System.out.println("marker Clicked"+marker.isInfoWindowShown());
                if(marker.isInfoWindowShown()){
                    marker.hideInfoWindow();
                }
                else{
                    marker.showInfoWindow();
                }
                return true;
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                String cityMarker  = marker.getTag().toString();
                for(Marker labelMarker : cityMarkers){
                    if(labelMarker.getTag().toString().equals(cityMarker)){
                        labelMarker.setPosition(new LatLng(marker.getPosition().latitude - 0.55, marker.getPosition().longitude));
                    }
                }

                String[] geoData = getAddress(marker.getPosition());
                String title = geoData[0];
                String snippet = geoData[1];

                marker.setTitle(title);
                marker.setSnippet(snippet);

                if (listOfMarkers.size() == polygone_sides) {
                    for(Polyline line: listOfPolylines){
                        line.remove();
                    }
                    listOfPolylines.clear();

                    design.remove();
                    design = null;

                    for(Marker currMarker: markerDistance){
                        currMarker.remove();
                    }
                    markerDistance.clear();
                    drawQuad();
                }
            }
        });
    }

    private void startUpdatingLoc() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);

    }

    private void reqLocPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, req_code);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (req_code == requestCode) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
            }
        }
    }
    private String[] getAddress(LatLng latLng){

        Geocoder geoCoder = new Geocoder(this);
        Address address = null;

        try {
            List<Address> matches = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            address = (matches.isEmpty() ? null : matches.get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String title = "";
        String snippet = "";

        ArrayList<String> titleString = new ArrayList<>();
        ArrayList<String> snippetString = new ArrayList<>();

        if (address != null) {
            if (address.getSubThoroughfare() != null) {
                titleString.add(address.getSubThoroughfare());

            }
            if (address.getThoroughfare() != null) {

                titleString.add(address.getThoroughfare());

            }
            if (address.getPostalCode() != null) {

                titleString.add(address.getPostalCode());

            }
            if (titleString.isEmpty()) {
                titleString.add("Unknown Location");
            }
            if (address.getLocality() != null) {
                snippetString.add(address.getLocality());

            }
            if (address.getAdminArea() != null) {
                snippetString.add(address.getAdminArea());
            }
        }

        title = TextUtils.join(", ", titleString);
        title = (title.equals("") ? "  " : title);

        snippet = TextUtils.join(", ", snippetString);
        String[] result = new String[2];
        result[0] = title;
        result[1] = snippet;
        return result;
    }


    private void addCityLabel(LatLng latLng, Marker locationLabel){

        ArrayList<Character> arr = new ArrayList<>();
        arr.add('A');
        arr.add('B');
        arr.add('C');
        arr.add('D');

        for (Marker marker : cityMarkers) {
            arr.remove((Character) marker.getTag());
        }
        locationLabel.setTag(arr.get(0).toString());

        LatLng labelLatLng = new LatLng(latLng.latitude - 0.55, latLng.longitude);


        MarkerOptions optionsCityLabel = new MarkerOptions().position(labelLatLng)
                .draggable(false)
                .icon(displayText(arr.get(0).toString()));
        Marker letterMarker = mMap.addMarker(optionsCityLabel);

        cityMarkers.add(letterMarker);
        letterList.add(arr.get(0));
        letterMarker.setTag(arr.get(0));
    }


    private void setMarker(LatLng latLng) {
        String[] geoData = getAddress(latLng);
        String title = geoData[0];
        String snippet = geoData[1];

        MarkerOptions options = new MarkerOptions().position(latLng)
                .draggable(true)
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .snippet(snippet);


        if (listOfMarkers.size() == polygone_sides) {
            clearAll();
        }

        Marker mm = mMap.addMarker(options);
        listOfMarkers.add(mm);

        if (listOfMarkers.size() == polygone_sides) {
            drawQuad();
        }
        addCityLabel(latLng, mm);
    }




    private void clearAll() {
        for (Marker marker : listOfMarkers) {
            marker.remove();
        }
        listOfMarkers.clear();

        for(Polyline line: listOfPolylines){
            line.remove();
        }
        listOfPolylines.clear();

        design.remove();
        design = null;

        for (Marker marker : markerDistance) {
            marker.remove();
        }
        markerDistance.clear();

        for(Marker marker: cityMarkers){
            marker.remove();
        }
        cityMarkers.clear();
        letterList.clear();

    }


    private void drawQuad(){
        PolygonOptions options = new PolygonOptions()
                .fillColor(Color.argb(35, 0, 255, 0))
                .strokeColor(Color.RED);

        LatLng[] markersConvex = new LatLng[polygone_sides];
        for (int i = 0; i < polygone_sides; i++) {
            markersConvex[i] = new LatLng(listOfMarkers.get(i).getPosition().latitude,
                    listOfMarkers.get(i).getPosition().longitude);
        }

        Vector<LatLng> sortedLatLong = PointAdder.convexHull(markersConvex, polygone_sides);

        Vector<LatLng> sortedLatLong2 =  new Vector<>();

        int l = 0;
        for (int i = 0; i < listOfMarkers.size(); i++)
            if (listOfMarkers.get(i).getPosition().latitude < listOfMarkers.get(l).getPosition().latitude)
                l = i;

        Marker currentMarker = listOfMarkers.get(l);
        sortedLatLong2.add(currentMarker.getPosition());
        while(sortedLatLong2.size() != polygone_sides){
            double minDistance = Double.MAX_VALUE;
            Marker nearestMarker  = null;
            for(Marker marker: listOfMarkers){
                if(sortedLatLong2.contains(marker.getPosition())){
                    continue;
                }

                double curDistance = distance(currentMarker.getPosition().latitude,
                        currentMarker.getPosition().longitude,
                        marker.getPosition().latitude,
                        marker.getPosition().longitude);

                if(curDistance < minDistance){
                    minDistance = curDistance;
                    nearestMarker = marker;
                }
            }

            if(nearestMarker != null){
                sortedLatLong2.add(nearestMarker.getPosition());
                currentMarker = nearestMarker;
            }
        }
        System.out.println(sortedLatLong);

        options.addAll(sortedLatLong);
        design = mMap.addPolygon(options);
        design.setClickable(true);


        LatLng[] polyLinePoints = new LatLng[sortedLatLong.size() + 1];
        int index = 0;
        for (LatLng x : sortedLatLong) {
            polyLinePoints[index] = x;

            index++;
            if (index == sortedLatLong.size()) {

                polyLinePoints[index] = sortedLatLong.elementAt(0);
            }
        }
        for(int i =0 ; i<polyLinePoints.length -1 ; i++){
            LatLng[] tempArr = {polyLinePoints[i], polyLinePoints[i+1] };
            Polyline currentPolyline =  mMap.addPolyline(new PolylineOptions()
                    .clickable(true)
                    .add(tempArr)
                    .color(Color.RED));
            currentPolyline.setClickable(true);
            listOfPolylines.add(currentPolyline);
        }
    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        if(listOfMarkers.size() == 0){
            return;
        }
        double minDistance = Double.MAX_VALUE;
        double minCityLabelDistance = Double.MAX_VALUE;

        Marker nearestMarker = null;
        Marker nearestCityMarker = null;

        for(Marker marker: listOfMarkers){
            double currDistance = distance(marker.getPosition().latitude,
                    marker.getPosition().longitude,
                    latLng.latitude,
                    latLng.longitude);
            if(currDistance < minDistance){
                minDistance = currDistance;
                nearestMarker = marker;
            }
        }

        for(Marker marker: cityMarkers){
            double currDistance = distance(marker.getPosition().latitude,
                    marker.getPosition().longitude,
                    latLng.latitude,
                    latLng.longitude);
            if(currDistance < minCityLabelDistance){
                minCityLabelDistance = currDistance;
                nearestCityMarker = marker;
            }
        }

        if(nearestMarker != null && nearestCityMarker != null){
            nearestMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_selected));
            final Marker finalNearestMarker = nearestMarker;
            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);

            final Marker finalNearestCityMarker = nearestCityMarker;

            deleteDialog
                    .setTitle("Delete?")
                    .setMessage("Would you like to delete the marker in red?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            finalNearestMarker.remove();
                            listOfMarkers.remove(finalNearestMarker);

                            finalNearestCityMarker.remove();
                            cityMarkers.remove(finalNearestCityMarker);

                            for(Polyline polyline: listOfPolylines){
                                polyline.remove();
                            }
                            listOfPolylines.clear();

                            if(design != null){
                                design.remove();
                                design = null;
                            }

                            for(Marker currMarker: markerDistance){
                                currMarker.remove();
                            }
                            markerDistance.clear();

                        }
                    })

                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finalNearestMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker));

                        }
                    });
            AlertDialog dialog = deleteDialog.create();
            dialog.show();
        }
    }


    public String getTotalDist(ArrayList<Polyline> polylines){

        double totalDistance = 0;
        for(Polyline polyline : polylines){
            List<LatLng> points = polyline.getPoints();
            LatLng firstPoint = points.remove(0);
            LatLng secondPoint = points.remove(0);

            double distance = distance(firstPoint.latitude,firstPoint.longitude,
                    secondPoint.latitude,secondPoint.longitude);
            totalDistance += distance;

        }
        NumberFormat formatter = new DecimalFormat("#0.0");
        return formatter.format(totalDistance) + " KM";
    }

    @Override
    public void onMapClick(LatLng latLng) {
        setMarker(latLng);
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    @Override
    public void onPolygonClick(Polygon polygon) {
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for(LatLng point: polygon.getPoints()){
            builder.include(point);
        }
        LatLng center = builder.build().getCenter();
        MarkerOptions options = new MarkerOptions().position(center)
                .draggable(true)
                .icon(displayText(getTotalDist(listOfPolylines)));
        markerDistance.add(mMap.addMarker(options));
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

        List<LatLng> points = polyline.getPoints();
        LatLng firstPoint = points.remove(0);
        LatLng secondPoint = points.remove(0);

        LatLng center = LatLngBounds.builder().include(firstPoint).include(secondPoint).build().getCenter();
        MarkerOptions options = new MarkerOptions().position(center)
                .draggable(true)
                .icon(displayText(getMarkerDist(polyline)));
        markerDistance.add(mMap.addMarker(options));
    }

    public String getMarkerDist(Polyline polyline){
        List<LatLng> points = polyline.getPoints();
        LatLng firstPoint = points.remove(0);
        LatLng secondPoint = points.remove(0);


        double distance = distance(firstPoint.latitude,firstPoint.longitude,
                secondPoint.latitude,secondPoint.longitude);
        NumberFormat formatter = new DecimalFormat("#0.0");
        return formatter.format(distance) + " KM";
    }

}