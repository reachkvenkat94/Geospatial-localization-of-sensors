package com.example.mylocationapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

public class MainActivity extends AppCompatActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int MY_PERMISSIONS_REQUEST = 1;
    LocationManager locationManager;

    private MqttClientConnector mqttClient;

    private Double phoneLatitude;
    private Double phoneLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mqttClient = new MqttClientConnector(this);
    }

    public void onToggle(View view)
    {
        Button btnToggle = findViewById(R.id.btnToggle);
        if(btnToggle.getText() == getString(R.string.btn_get))
        {
            checkPermissions();
            btnToggle.setText(getString(R.string.btn_pause));
        }
        else
        {
            locationManager.removeUpdates(locationListener);
            mqttClient.disconnect();
            btnToggle.setText(getString(R.string.btn_get));
        }
    }

    private void checkPermissions()
    {
        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                //Log.e("testing", "Permission is granted");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                mqttClient.connect();

            } else {
                //Log.e("testing", "Permission is revoked");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST);

            }
        }
        else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            mqttClient.connect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    checkPermissions();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    // Define a listener that responds to location updates
    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            TextView txtLat = (TextView) findViewById(R.id.textLatitudeValue);
            txtLat.setText(Double.toString(lat));

            TextView txtLon = (TextView) findViewById(R.id.textLongitudeValue);
            txtLon.setText(Double.toString(lon));

            phoneLatitude = lat;
            phoneLongitude = lon;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    };

    private Location getLastKnownLocation()
    {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
        return null;
    }

    private class MqttClientConnector {
        private Gson gson;
        private MqttAndroidClient mqttAndroidClient;
        private MqttConnectOptions mqttConnectOptions;
        private List<SensorData> sensors;

        final String serverUri = "tcp://192.168.1.198:1883";
        String clientId = "WSNAndroidClient";
        final String subscriptionTopic = "WSNAndroidTopic";

        public MqttClientConnector(Context context)
        {
            gson = new Gson();
            sensors = new ArrayList<>();

            mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
            mqttAndroidClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("mqtt", "Connection Lost.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.i("mqtt", "New message: " + new String(message.getPayload()));
                    receiveData(new String(message.getPayload()));

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            clientId = clientId + System.currentTimeMillis();

            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);
        }

        private void receiveData(String payload)
        {
            SensorData newReading;
            newReading = gson.fromJson(payload, SensorData.class);

            if(phoneLatitude != null && phoneLongitude != null){
                newReading.setDistance(calcDistance(newReading.getLatitude(), phoneLatitude, newReading.getLongitude(), phoneLongitude));
                Log.i("reading", newReading.toString());
                boolean match = false;
                for(int i=0; i< sensors.size(); i++) {
                    if (sensors.get(i).getId().equals(newReading.getId())) {
                        sensors.get(i).setTemperature(newReading.getTemperature());
                        match = true;
                        break;
                    }
                }

                if(!match)
                {
                    sensors.add(newReading);
                }

                int nearestIndex = -1;
                double nearestDistance = -1;
                for(int i = 0; i< sensors.size(); i++)
                {
                    if(sensors.get(i).getDistance() < nearestDistance || nearestDistance == -1)
                    {
                        nearestIndex = i;
                    }
                }

                if(nearestIndex != -1)
                {
                    TextView txtSensorLatitudeValue = findViewById(R.id.textSensorLatitudeValue);
                    txtSensorLatitudeValue.setText(Double.toString(sensors.get(nearestIndex).getLatitude()));

                    TextView txtSensorLongitudeValue = findViewById(R.id.textSensorLongitudeValue);
                    txtSensorLongitudeValue.setText(Double.toString(sensors.get(nearestIndex).getLongitude()));

                    TextView txtTemperatureValue = findViewById(R.id.textTemperatureValue);
                    txtTemperatureValue.setText(Double.toString(sensors.get(nearestIndex).getTemperature()));

                    TextView txtDistanceValue = findViewById(R.id.textDistanceValue);
                    txtDistanceValue.setText(Double.toString(sensors.get(nearestIndex).getDistance()));
                }
            }
            else
            {
                Location loc = getLastKnownLocation();
                if(loc != null)
                {
                    phoneLatitude = loc.getLatitude();
                    phoneLongitude = loc.getLongitude();
                }
            }
        }

        private double calcDistance(double lat1, double lat2, double lon1, double lon2)
        {
            double R = 6371000; //radius of earth in meters
            double phi1 = Math.toRadians(lat1);
            double phi2 = Math.toRadians(lat2);
            double deltaPhi = Math.toRadians(lat2 - lat1);
            double deltaLambda = Math.toRadians(lon2 - lon1);

            double a = Math.sin(deltaPhi/2) * Math.sin(deltaPhi/2)
                    + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda/2) * Math.sin(deltaLambda/2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

            double d = R * c;

            return d;
        }

        public void connect(){
            try{
                mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener(){
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken){
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        subscribeToTopic();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("mqtt", "Failed to connect");
                    }
                });
            }
            catch(MqttException ex){
                ex.printStackTrace();
            }
        }

        public void disconnect()
        {
            try {
                mqttAndroidClient.disconnect();
            }
            catch(MqttException ex)
            {
                ex.printStackTrace();
            }
        }

        private void subscribeToTopic() {
            try {
                mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i("mqtt", "Subscribed!");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("mqtt", "Failed to subscribe");
                    }
                });

            } catch (MqttException ex) {
                System.err.println("Exception whilst subscribing");
                ex.printStackTrace();
            }
        }
    }

    private class SensorData
    {
        private String id;

        private double latitude;

        private double longitude;

        private double temperature;

        private double distance;

        public String getId()
        {
            return id;
        }
        public void setId(String id)
        {
            this.id = id;
        }

        public double getLatitude()
        {
            return latitude;
        }
        public void setLatitude(double latitude)
        {
            this.latitude = latitude;
        }

        public double getLongitude()
        {
            return longitude;
        }
        public void setLongitude(double longitude)
        {
            this.longitude = longitude;
        }

        public double getTemperature()
        {
            return temperature;
        }
        public void setTemperature(double temperature)
        {
            this.temperature = temperature;
        }

        public double getDistance()
        {
            return distance;
        }
        public void setDistance(double distance)
        {
            this.distance = distance;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Id ").append(getId()).append("\n")
                    .append("Latitude ").append(getLatitude()).append("\n")
                    .append("Longitude ").append(getLongitude()).append("\n")
                    .append("Temperature ").append(getTemperature()).append("\n")
                    .append("Distance ").append(getDistance());

            return sb.toString();
        }
    }
}
