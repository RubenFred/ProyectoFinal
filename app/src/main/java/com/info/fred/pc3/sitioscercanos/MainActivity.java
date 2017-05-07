package com.info.fred.pc3.sitioscercanos;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;


import org.json.JSONException;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback {

    private GoogleApiClient googleApiClient;
    private GoogleMap mMap;

    private TextView latitudTextView;
    private TextView longitudTextView;

    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;

    private double coorLatitud = 0.0;
    private double coorLongitud = 0.0;

    private Button miPosicionButton;

    // Capas de datos
    private GeoJsonLayer layerSalud;
    private GeoJsonLayer layerBancos;
    private RadioButton radioButton;

    // Tipo mapa base
    private SwitchCompat tipoSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // latitudTextView = (TextView) findViewById(R.id.latitudTextView);
        // longitudTextView = (TextView) findViewById(R.id.longitudTextView);

        miPosicionButton = (Button) findViewById(R.id.btn_miPosicion);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();

        crearSolicitudLocalizacion();
        crearSolicitudConfiguracion();
        verificarConfiguracionParaLocalizacion();

        tipoSwitch =(SwitchCompat) findViewById(R.id.tipoSwitch);

        tipoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }else{
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            detenerObtencionUbicaciones();
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("MIAPP", "Localización: " + location.getLatitude() + ", " + location.getLatitude());

        if (location.getLatitude() != 0.0 && location.getLongitude() != 0.0) {
            coorLatitud = (double) location.getLatitude();
            coorLongitud = (double) location.getLongitude();
            Toast.makeText(MainActivity.this, "Coordenadas Obtenidas", Toast.LENGTH_LONG).show();
            detenerObtencionUbicaciones();
            miPosicionButton.setEnabled(true);
        }

        Log.i("ubic=", "lat:" + coorLatitud);
        Log.i("ubic", "Long: " + coorLongitud);

    }

    // Métodos crear

    private void crearSolicitudLocalizacion() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void crearSolicitudConfiguracion() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        locationSettingsRequest = builder.build();
    }

    // Iniciar y detener el proceso

    private void iniciarObtenerUbicaciones() {
        if (tenemosPermisoUbicacion()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } else {
            manejarPermisoDenegado();
        }
    }

    private void detenerObtencionUbicaciones() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    // Configuraciones de localización

    private void verificarConfiguracionParaLocalizacion() {
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequest);
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                Status status = result.getStatus();

                if (status.getStatusCode() == LocationSettingsStatusCodes.SUCCESS) {
                    Log.d("MIAPP", "Todo configurado para obtener ubicaciones");
                    iniciarObtenerUbicaciones();
                } else if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        Log.d("MIAPP", "No satisface lo necesario, abrimos diálogo");
                        status.startResolutionForResult(MainActivity.this, 999);
                    } catch (IntentSender.SendIntentException e) {
                        Log.d("MIAPP", "No podemos abrir díalogo");
                    }
                } else if (status.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                    Log.d("MIAPP", "No podemos abrir de nuevo el diálogo de configuraciones, activalo manualmente");
                    Toast.makeText(MainActivity.this, "No podemos abrir de nuevo el diálogo de configuraciones, activalo manualmente", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 999) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("MIAPP", "Permitió el cambio de configuraciones de ubicación");
                iniciarObtenerUbicaciones();

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("MIAPP", "No permitió el cambio de configuraciones de ubicación");
                Toast.makeText(this, "No permitió el cambio de configuraciones de ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Permisos de ubicación

    private boolean tenemosPermisoUbicacion() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void manejarPermisoDenegado() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Ya rechazaste anteriormente la solictud, debes activar en configuraciones", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 666);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 666) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarObtenerUbicaciones();
            } else {
                Toast.makeText(this, "Permisos no cedidos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //*************  mapa
    @Override

    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Add a marker in Bolivia and move the camera
        LatLng bolivia = new LatLng(-15.888318, -65.067404);

       // mMap.moveCamera(CameraUpdateFactory.newLatLng(bolivia));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bolivia,5));

        // Para add  capas de datos  salud y banca

        // para ptos Salud
        try {
            layerSalud = new GeoJsonLayer(getMap(), R.raw.salud, this);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // para ptos Banco

        try {
            layerBancos = new GeoJsonLayer(getMap(), R.raw.banca1, this);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public void irMiPosicion(View view) {
        LatLng miLugar = new LatLng(coorLatitud, coorLongitud);

        // marca la posicion en el mapa
        mMap.addMarker(new MarkerOptions()
                .position(miLugar)
                .title("Posicion Actual")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.male)));

        CircleOptions circulo= new CircleOptions()
                .strokeColor(ContextCompat.getColor(this, R.color.colorAccent))
                .strokeWidth(5)
                .center(miLugar)
                .radius(70);
        mMap.addCircle(circulo);

        UiSettings uiSettings=mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);

        //mMap.addMarker(new MarkerOptions().position(miLugar).title("Posicion Actual"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miLugar, 15));

    }

    // Para mostrar las capas de informacion  Salud y Bancos *****************
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radio_salud:
                if (checked)
                    layerBancos.removeLayerFromMap();
                    //Toast.makeText(this, "Borrando capa bancos", Toast.LENGTH_SHORT).show();

                //  Marcador Ptos Salud
                for (GeoJsonFeature feature : layerSalud.getFeatures()) {
                    // Check if the magnitude property exists
                    if (feature.getProperty("gml_id") != null) {

                        String tipo = feature.getProperty("ni_at_i");

                        // Create a new point style
                        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();

                        // def  tipo de establecimiento
                        if ("C.S.".equals(tipo)) {
                            //.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon_pointer)));
                            pointStyle.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_centro_salud));
                            //pointStyle.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                        } else if ("Clnica".equals(tipo)) {
                            pointStyle.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_clinica));
                        }
                        else if ("Policlnico".equals(tipo)) {
                            pointStyle.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ico_policlinica));
                        }
                        else if ("Hospital".equals(tipo)) {
                            pointStyle.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ico_hospital));
                        }
                        else {
                            pointStyle.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        }

                        pointStyle.setTitle("" + feature.getProperty("establecim"));
                        pointStyle.setSnippet("" + feature.getProperty("tipo")+" : "+feature.getProperty("direccion"));

                        // Assign the point style to the feature
                        feature.setPointStyle(pointStyle);
                    }


                } // fin for
                layerSalud.addLayerToMap();
                break;


            case R.id.radio_bancos:
                if (checked)
                    layerSalud.removeLayerFromMap();
                //Toast.makeText(this, "Borrando capa salud", Toast.LENGTH_SHORT).show();

                //  Marcador Ptos Bancos
                for (GeoJsonFeature feature : layerBancos.getFeatures()) {
                    // Check if the magnitude property exists
                    if (feature.getProperty("Entidad") != null) {
                        String tipo_oficina = feature.getProperty("TipoOficin");

                        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();

                        // dif  tipo de establecimiento
                        if ("Oficina Central".equals(tipo_oficina)) {
                            pointStyle.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ico_banco_central));
                        } else if ("Agencia Fija".equals(tipo_oficina)) {
                            pointStyle.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ico_banco_agencia));
                        }
                        else if ("Punto de Atencion Corresponsal No Financiero".equals(tipo_oficina)) {
                            pointStyle.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ico_banco_atencion));
                        }

                        else {  // cajeros
                            pointStyle.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ico_banco_cajero));
                        }


                        pointStyle.setTitle("" + feature.getProperty("Entidad"));
                        pointStyle.setSnippet("" + tipo_oficina);

                        // Assign the point style to the feature
                        feature.setPointStyle(pointStyle);
                    }


                } // fin for
                layerBancos.addLayerToMap();
                break;
        }
    }

    public GoogleMap getMap() {
        return mMap;
    }
} // fin de la class
