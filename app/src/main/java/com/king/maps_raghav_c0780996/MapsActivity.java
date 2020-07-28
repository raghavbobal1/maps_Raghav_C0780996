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
import java.util.HashMap;
import java.util.List;
import java.util.Vector;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,  GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener {

    private static final int REQUEST_CODE = 1;
    private static final int POLYGON_SIDES = 4;
    Polygon shape;
    List<Marker> markersList = new ArrayList<>();
    List<Marker> distanceMarkers = new ArrayList<>();
    ArrayList<Polyline> polylinesList = new ArrayList<>();

    List<Marker> cityMarkers = new ArrayList<>();
    ArrayList<Character> letterList = new ArrayList<>();

    LocationManager locationManager;
    LocationListener locationListener;
    private GoogleMap mMap;

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
            requestLocationPermission();
        } else {
            startUpdateLocations();
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

                String[] geoData = getAddressInfo(marker.getPosition());
                String title = geoData[0];
                String snippet = geoData[1];

                marker.setTitle(title);
                marker.setSnippet(snippet);

                if (markersList.size() == POLYGON_SIDES) {
                    for(Polyline line: polylinesList){
                        line.remove();
                    }
                    polylinesList.clear();

                    shape.remove();
                    shape = null;

                    for(Marker currMarker: distanceMarkers){
                        currMarker.remove();
                    }
                    distanceMarkers.clear();
                    drawShape();
                }
            }
        });
    }

    private void startUpdateLocations() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);

    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (REQUEST_CODE == requestCode) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
            }
        }
    }
    private String[] getAddressInfo(LatLng latLng){

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
    private void setMarker(LatLng latLng) {
        String[] geoData = getAddressInfo(latLng);
        String title = geoData[0];
        String snippet = geoData[1];

        MarkerOptions options = new MarkerOptions().position(latLng)
                .draggable(true)
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .snippet(snippet);


        if (markersList.size() == POLYGON_SIDES) {
            clearMap();
        }

        Marker mm = mMap.addMarker(options);
        markersList.add(mm);

        if (markersList.size() == POLYGON_SIDES) {
            drawShape();
        }
        addCityLabelMarker(latLng, mm);
    }
    private void addCityLabelMarker(LatLng latLng, Marker locationLabel){

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


    private void drawShape (){
        PolygonOptions options = new PolygonOptions()
                .fillColor(Color.argb(35, 0, 255, 0))
                .strokeColor(Color.RED);

        LatLng[] markersConvex = new LatLng[POLYGON_SIDES];
        for (int i = 0; i < POLYGON_SIDES; i++) {
            markersConvex[i] = new LatLng(markersList.get(i).getPosition().latitude,
                    markersList.get(i).getPosition().longitude);
        }

        Vector<LatLng> sortedLatLong = PointPlotter.convexHull(markersConvex, POLYGON_SIDES);

        Vector<LatLng> sortedLatLong2 =  new Vector<>();

        int l = 0;
        for (int i = 0; i < markersList.size(); i++)
            if (markersList.get(i).getPosition().latitude < markersList.get(l).getPosition().latitude)
                l = i;

        Marker currentMarker = markersList.get(l);
        sortedLatLong2.add(currentMarker.getPosition());
        while(sortedLatLong2.size() != POLYGON_SIDES){
            double minDistance = Double.MAX_VALUE;
            Marker nearestMarker  = null;
            for(Marker marker: markersList){
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
        shape = mMap.addPolygon(options);
        shape.setClickable(true);


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
            polylinesList.add(currentPolyline);
        }
    }

    private void clearMap() {
        for (Marker marker : markersList) {
            marker.remove();
        }
        markersList.clear();

        for(Polyline line: polylinesList){
            line.remove();
        }
        polylinesList.clear();

        shape.remove();
        shape = null;

        for (Marker marker : distanceMarkers) {
            marker.remove();
        }
        distanceMarkers.clear();

        for(Marker marker: cityMarkers){
            marker.remove();
        }
        cityMarkers.clear();
        letterList.clear();

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if(markersList.size() == 0){
            return;
        }
        double minDistance = Double.MAX_VALUE;
        double minCityLabelDistance = Double.MAX_VALUE;

        Marker nearestMarker = null;
        Marker nearestCityMarker = null;

        for(Marker marker: markersList){
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
                            markersList.remove(finalNearestMarker);

                            finalNearestCityMarker.remove();
                            cityMarkers.remove(finalNearestCityMarker);

                            for(Polyline polyline: polylinesList){
                                polyline.remove();
                            }
                            polylinesList.clear();

                            if(shape != null){
                                shape.remove();
                                shape = null;
                            }

                            for(Marker currMarker: distanceMarkers){
                                currMarker.remove();
                            }
                            distanceMarkers.clear();

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
                .icon(displayText(getTotalDistance(polylinesList)));
        distanceMarkers.add(mMap.addMarker(options));
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

        List<LatLng> points = polyline.getPoints();
        LatLng firstPoint = points.remove(0);
        LatLng secondPoint = points.remove(0);

        LatLng center = LatLngBounds.builder().include(firstPoint).include(secondPoint).build().getCenter();
        MarkerOptions options = new MarkerOptions().position(center)
                .draggable(true)
                .icon(displayText(getMarkerDistance(polyline)));
        distanceMarkers.add(mMap.addMarker(options));
    }

    public String getMarkerDistance(Polyline polyline){
        List<LatLng> points = polyline.getPoints();
        LatLng firstPoint = points.remove(0);
        LatLng secondPoint = points.remove(0);


        double distance = distance(firstPoint.latitude,firstPoint.longitude,
                secondPoint.latitude,secondPoint.longitude);
        NumberFormat formatter = new DecimalFormat("#0.0");
        return formatter.format(distance) + " KM";
    }

    public String getTotalDistance(ArrayList<Polyline> polylines){

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
}