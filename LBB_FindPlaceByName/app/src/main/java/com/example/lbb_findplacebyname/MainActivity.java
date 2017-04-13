package com.example.lbb_findplacebyname;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Text;

import java.nio.charset.Charset;

public class MainActivity extends FragmentActivity implements OnConnectionFailedListener {

    private static final int REQUEST_RESOLVE_CODE = 101;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 102;
    private static final String DIALOG_ERROR_TAG = "dialogError";
    private static final String STATE_RESOLVING_TAG = "resolvingError";
    private static final LatLng DELHI_LATLNG = new LatLng(28.613924, 77.209003);
    private boolean resolvingError = false;
    private GoogleApiClient placesGoogleApiClient;
    private PlaceAutocompleteFragment placeAutocompleteFragment;
    private GoogleMap map;
    private ViewFlipper viewFlipper;
    private Place curSelectedPlace;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);

        placesGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).enableAutoManage(this,this).build();
        resolvingError = false;

        placeAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener(){
            @Override
            public void onPlaceSelected(Place place)
            {

                //Log.d("onPlaceSelected: ","Place" + place.getName());
                //TODO get place details
                curSelectedPlace = place;

                viewFlipper.showNext();

                CharSequence name = curSelectedPlace.getName();
                CharSequence address = curSelectedPlace.getAddress();
                CharSequence phone = curSelectedPlace.getPhoneNumber();
                Uri webUri = curSelectedPlace.getWebsiteUri();
                CharSequence website = null;

                if(webUri!=null)
                    website = webUri.toString();
                //PLACE NAME
                TextView placeName = (TextView) findViewById(R.id.placeName);
                if(name!=null)
                    placeName.setText(name);
                else
                    placeName.setText(" ");
                //MAP
                MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        map = googleMap;
                        googleMap.clear();
                        LatLng curPlace = curSelectedPlace.getLatLng();
                        if(curPlace!=null)
                        {
                            googleMap.addMarker(new MarkerOptions().position(curPlace).title(curSelectedPlace.getName().toString()));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(curPlace));
                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
                        }
                        else
                        {
                            googleMap.addMarker(new MarkerOptions().position(DELHI_LATLNG).title("Delhi"));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(DELHI_LATLNG));
                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(10.0f));
                        }
                    }
                });

                //ADDRESS
                TextView addressView = (TextView) findViewById(R.id.address);
                if(address!=null)
                    addressView.setText(address);
                else
                    addressView.setText(" ");

                //PHONE NO.
                TextView phoneView = (TextView) findViewById(R.id.phoneNo);
                if(phone!=null)
                    phoneView.setText(phone);
                else
                    phoneView.setText(" ");


                //WEBSITE
                TextView websiteView = (TextView) findViewById(R.id.website);
                if(website!=null)
                    websiteView.setText(website);
                else
                    websiteView.setText(" ");

            }
            @Override
            public void onError(Status status)
            {
                //TODO Handle the error
                //Log.d("onPlaceSelected","An error occurred: " + status);
            }
        });

        //restricting results to India - unnecessary, but done as a precaution
        AutocompleteFilter countryFilter = new AutocompleteFilter.Builder().setCountry("IN").build();
        placeAutocompleteFragment.setFilter(countryFilter);

        //restricting bound to the area around Delhi
        LatLng southwest = new LatLng(28.407354, 76.839045);
        LatLng northeast = new LatLng(28.888083, 77.405863);
        placeAutocompleteFragment.setBoundsBias(new LatLngBounds(southwest,northeast));
    }

    public void goBack(View view)
    {
        viewFlipper.showPrevious();
    }
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_TAG,resolvingError);
    }

    protected void onRestoreInstanceState(Bundle savedState)
    {
        super.onRestoreInstanceState(savedState);
        resolvingError = savedState.getBoolean(STATE_RESOLVING_TAG);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == REQUEST_RESOLVE_CODE)
        {
            resolvingError = false;
            if(resultCode == RESULT_OK)
            {
                if(!placesGoogleApiClient.isConnecting() && !placesGoogleApiClient.isConnected())
                {
                    placesGoogleApiClient.connect();
                }
            }
        }
    }

    public void onConnectionFailed(ConnectionResult result)
    {
        //TODO
        if(resolvingError)
        {
            return;
        }
        else if(result.hasResolution())
        {
            try
            {
                resolvingError = true;
                result.startResolutionForResult(this,REQUEST_RESOLVE_CODE);
            }
            catch(IntentSender.SendIntentException e)
            {
                //Some issue with resolution intent, try again!
                placesGoogleApiClient.connect();
            }
        }
        else
        {
            showErrorDialog(result.getErrorCode());
            resolvingError = true;
        }
    }

    private void showErrorDialog(int errorCode)
    {
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR_TAG,errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(),"errordialog");
    }
    public void onDialogDismissed()
    {
        resolvingError = false;
    }
    public static class ErrorDialogFragment extends DialogFragment{
        public ErrorDialogFragment(){
        }
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            int errCode = this.getArguments().getInt(DIALOG_ERROR_TAG);
            return GoogleApiAvailability.getInstance().getErrorDialog(this.getActivity(),errCode,REQUEST_RESOLVE_CODE);
        }

        public void onDismiss(DialogInterface dialog)
        {
            ((MainActivity)getActivity()).onDialogDismissed();
        }
    }
}
