package com.example.a1716342.beaconslocalisation2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

public class AdapterReferencePosition extends RecyclerView.Adapter<AdapterReferencePosition.ViewHolder> {

    /*
    this class is the adapter for referencePosition activity
     */
    private static final String TAG = "ARP";
    private static final String FILE_NAME = "reference_position.dat"; //file containing coordinates
    Hashtable<ArrayList<String>,Double> beaconsList;
    ArrayList<ArrayList<String>> keys;
    Context context;

    // Constructor
    public AdapterReferencePosition(Hashtable<ArrayList<String>,Double> beaconsDictionnary, ArrayList<ArrayList<String>> keys, Context context)
    {
        this.beaconsList = beaconsDictionnary;
        this.keys = keys;
        this.context = context;
    }

    /*
       View Holder class to instantiate views
     */
    class ViewHolder extends RecyclerView.ViewHolder{
        //UUID
        private TextView uuid;

        //Major
        private TextView major;

        //Minor
        private TextView minor;

        //Distance
        private TextView distance;

        //X
        private TextView X;

        //Y
        private TextView Y;

        //Z
        private TextView Z;

        //Current Beacon
        private ArrayList<String> currentBeacon;

        //View Holder Class Constructor
        public ViewHolder(final View itemView) {
            super(itemView);
            //Initializing views
            uuid = itemView.findViewById(R.id.uuid2);
            major = itemView.findViewById(R.id.major2);
            minor = itemView.findViewById(R.id.minor2);
            distance = itemView.findViewById(R.id.distance2);
            X = itemView.findViewById(R.id.xCard);
            Y = itemView.findViewById(R.id.yCard);
            Z = itemView.findViewById(R.id.zCard);

            //View to use the alertDialog
            LayoutInflater li = LayoutInflater.from(context);
            final View dialogView = li.inflate(R.layout.alertdialog_coordinates, null);

            //Setting the alertDialog up
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(itemView.getContext());
            alertDialog.setTitle("COORDINATES");
            alertDialog.setMessage("Enter the coordinates");
            alertDialog.setView(dialogView);

            final EditText x = dialogView.findViewById(R.id.xAlert);
            final EditText y = dialogView.findViewById(R.id.yAlert);
            final EditText z = dialogView.findViewById(R.id.zAlert);

            //AlertDialog to input the coordinates of the beacons
            alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    FileOutputStream fos;
                    ObjectOutputStream oos;
                    FileInputStream fis;
                    ObjectInputStream ois;
                    Hashtable<ArrayList<String>,ArrayList<Double>> listBeacons;
                    try {
                        fis = context.openFileInput(FILE_NAME);
                        ois = new ObjectInputStream(fis);
                        listBeacons = (Hashtable<ArrayList<String>,ArrayList<Double>>) ois.readObject();
                        Log.i(TAG, listBeacons.toString());
                        ois.close();
                        fis.close();
                        if(listBeacons.containsKey(currentBeacon)) {
                            ArrayList<Double> newCoordinates = new ArrayList<>(3);
                            newCoordinates.add(Double.parseDouble(x.getText().toString()));
                            newCoordinates.add(Double.parseDouble(y.getText().toString()));
                            newCoordinates.add(Double.parseDouble(z.getText().toString()));
                            listBeacons.replace(currentBeacon, newCoordinates);
                        }
                        else {
                            ArrayList<Double> newCoordinates = new ArrayList<>(3);
                            newCoordinates.add(Double.parseDouble(x.getText().toString()));
                            newCoordinates.add(Double.parseDouble(y.getText().toString()));
                            newCoordinates.add(Double.parseDouble(z.getText().toString()));
                            listBeacons.put(currentBeacon, newCoordinates);
                        }

                        fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
                        oos = new ObjectOutputStream(fos);
                        oos.writeObject(listBeacons);
                        oos.close();
                        fos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
            //Listener to put a beacon in the ignored list
            itemView.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onClick(View view) {
                    alertDialog.show();

                }
            });
        }

        public void display(ArrayList<String> key){
            currentBeacon = key;
            ArrayList<Double> coordinates = readCoordinates();

            //Displaying UUID
            uuid.setText(currentBeacon.get(0));
            //Displaying major
            major.setText(currentBeacon.get(1));
            //Displaying minor
            minor.setText(currentBeacon.get(2));
            //Displaying RSSI
            distance.setText(String.valueOf(beaconsList.get(currentBeacon)));
            //Displaying coordinates
            X.setText(String.valueOf(coordinates.get(0)));
            Y.setText(String.valueOf(coordinates.get(1)));
            Z.setText(String.valueOf(coordinates.get(2)));
        }

        public ArrayList<Double> readCoordinates() {

            /*
            this method allows to reqd coordinates in the file
             */

            FileInputStream fis;
            ObjectInputStream ois;
            ArrayList<Double> coordinates = new ArrayList<Double>(3);
            Hashtable<ArrayList<String>,ArrayList<Double>> hashtable;

            try{
                fis =context.openFileInput(FILE_NAME);
                ois = new ObjectInputStream(fis);
                hashtable = (Hashtable<ArrayList<String>,ArrayList<Double>>) ois.readObject();
                ois.close();
                fis.close();
                if (hashtable.containsKey(currentBeacon)){
                    coordinates = hashtable.get(currentBeacon);
                } else {
                    coordinates.add(0.0);
                    coordinates.add(0.0);
                    coordinates.add(0.0);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            return coordinates;
        }
    }

    @Override
    public AdapterReferencePosition.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_card_reference_position,parent,false));
    }

    @Override
    public void onBindViewHolder(AdapterReferencePosition.ViewHolder holder, int position) {
        //Getting the key in the list within respective position
        ArrayList<String> key = keys.get(position);
        //Displaying
        holder.display(key);
    }
    @Override
    public int getItemCount()
    {
        return keys.size();
    }
}