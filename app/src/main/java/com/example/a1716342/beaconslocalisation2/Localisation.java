package com.example.a1716342.beaconslocalisation2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

public class Localisation extends Activity implements BeaconConsumer {

    /*
    This class is the one where the localisation is using
    It uses Triangulation with distance as data to predict areas
    */

    private static final String TAG = "Localisation";
    private static final String FILE_NAME = "ignored_beacons_list2.dat";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int NUMBER_BEACONS = 4;

    //Layout components
    TextView resultText;
    TabLayout bar;
    Button scan, stopScan;

    //Beacon Manager
    BeaconManager beaconManager;

    //HashTable containing beacons' information
    //KEY : {UUID, Major, Minor}
    //VALUE : RSSI
    Hashtable<ArrayList<String>, Double> beaconsList;

    Triangulation3D triangulation3D;
    Mapmaker map;
    ArrayList<Double> coordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localisation);

        //Checking the state of the required permissions
        //See : https://altbeacon.github.io/android-beacon-library/requesting_permission.html
        //See : https://developer.android.com/training/permissions/requesting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check...
            //... for coarse location
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }

        //getting beaconManager instance (object) for Main Activity class
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconsList = new Hashtable<ArrayList<String>, Double>();

        //Setting the navigate bar
        bar = findViewById(R.id.bar);
        bar.addTab(bar.newTab().setText("Localisation"));
        bar.addTab(bar.newTab().setText("Beacons"));
        bar.addTab(bar.newTab().setText("ReferencePosition"));
        bar.getTabAt(0).select();

        bar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                beaconManager.unbind(Localisation.this);
                if (bar.getSelectedTabPosition()==1){
                    Intent selection = new Intent(Localisation.this, Beacons.class);
                    startActivity(selection);
                }
                else if (bar.getSelectedTabPosition()==2){
                    Intent position = new Intent(Localisation.this, ReferencePosition.class);
                    startActivity(position);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //Setting components
        scan = findViewById(R.id.scan_localisation);
        stopScan = findViewById(R.id.stopScan_localisation);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Binding activity to the BeaconService
                beaconManager.bind(Localisation.this);
                stopScan.setEnabled(true);
                scan.setEnabled(false);
            }
        });
        stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Unbinding activity to the BeaconService
                beaconManager.unbind(Localisation.this);
                scan.setEnabled(true);
                stopScan.setEnabled(false);
            }
        });

        resultText = findViewById(R.id.result);

        //Class for triangulation
        triangulation3D = new Triangulation3D(this);

        map = new Mapmaker();

        coordinates = new ArrayList<>();
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        //Specifies a class that should be called each time the BeaconService gets ranging data,
        // which is nominally once per second when beacons are detected.
        beaconManager.addRangeNotifier(new RangeNotifier() {
            /*
               This Override method tells us all the collections of beacons and their details that
               are detected within the range by device
             */
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                Log.i(TAG, "Current activity : " + this.toString());

                //Updating the hashtable
                if (beacons.size() > 0) {
                    // Iterating through all Beacons from Collection of Beacons
                    for (Beacon b : beacons) {
                        ArrayList<String> beaconID = new ArrayList<String>();
                        beaconID.add(b.getId1().toString());
                        beaconID.add(b.getId2().toString());
                        beaconID.add(b.getId3().toString());

                        if (!isIgnored(beaconID)) {
                            //If the beacon is not in the hashtable, then adding it
                            if (beaconsList.get(beaconID) == null) {
                                beaconsList.put(beaconID, b.getDistance());
                            }
                            //Else change his RSSI in the hashtable
                            else {
                                beaconsList.replace(beaconID, b.getDistance());
                            }
                        }
                    }
                    if (beaconsList.size() >= NUMBER_BEACONS) { //This number has to be change
                        coordinates = triangulation3D.compute(beaconsList, 3, 2, 1, 0);
                        try {
                            Localisation.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resultText.setText(Integer.toString(map.getArea(coordinates.get(0), coordinates.get(1), coordinates.get(2))));
                                }
                            });
                        } catch (Exception e) { }
                    }
                }
            }
        });
        try {
            //Tells the BeaconService to start looking for beacons that match the passed Region object.
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            Log.e(TAG, "ERROR : " + e.getMessage());
        }
    }

    public boolean isIgnored(ArrayList<String> beacon) {
        /*
        this method allows to know if a beacon is ignored (see the activity Selection)
         */
        FileInputStream fis;
        ObjectInputStream ois;
        ArrayList<ArrayList<String>> listBeacons;
        try {
            fis = openFileInput(FILE_NAME);
            ois = new ObjectInputStream(fis);
            listBeacons = (ArrayList<ArrayList<String>>) ois.readObject();
            ois.close();
            fis.close();
            if (listBeacons.contains(beacon)) return true;
            else return false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Unbinds an Android Activity or Service to the BeaconService to avoid leak.
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this)) {
            beaconManager.unbind(this);
            beaconManager.setBackgroundMode(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this)) {
            beaconManager.setBackgroundMode(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                break;
            }
        }
    }
}
