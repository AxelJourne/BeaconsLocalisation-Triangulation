package com.example.a1716342.beaconslocalisation2;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

public class AdapterBeacons extends RecyclerView.Adapter<AdapterBeacons.ViewHolder> {

    /*
    This class is the adapter for Beacons activity
    */
    private static final String FILE_NAME = "ignored_beacons_list2.dat";
    private static final String TAG = "AdapterSelection";
    Hashtable<ArrayList<String>,Double> beaconsList;
    ArrayList<ArrayList<String>> keys;

    // Constructor
    public AdapterBeacons(Hashtable<ArrayList<String>,Double> beaconsDictionnary, ArrayList<ArrayList<String>> keys)
    {
        this.beaconsList = beaconsDictionnary;
        this.keys = keys;
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

        //CheckBox
        private CheckBox checkBox;

        private ArrayList<String> currentBeacon;

        //View Holder Class Constructor
        public ViewHolder(View itemView)
        {
            super(itemView);

            //Initializing views
            uuid = itemView.findViewById(R.id.uuid);
            major = itemView.findViewById(R.id.major);
            minor = itemView.findViewById(R.id.minor);
            distance = itemView.findViewById(R.id.distance);
            checkBox = itemView.findViewById(R.id.notIgnored);

            //listener to put a beacon in the ignored list
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FileOutputStream fos;
                    ObjectOutputStream oos;
                    FileInputStream fis;
                    ObjectInputStream ois;
                    ArrayList<ArrayList<String>> listBeacons;
                    if (!checkBox.isChecked()){
                        try {
                            fis = view.getContext().openFileInput(FILE_NAME);
                            ois = new ObjectInputStream(fis);
                            listBeacons = (ArrayList<ArrayList<String>>) ois.readObject();
                            ois.close();
                            fis.close();
                            if (!listBeacons.contains(currentBeacon)) {
                                listBeacons.add(currentBeacon);
                                fos = view.getContext().openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
                                oos = new ObjectOutputStream(fos);
                                oos.writeObject(listBeacons);
                                oos.close();
                                fos.close();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            fis = view.getContext().openFileInput(FILE_NAME);
                            ois = new ObjectInputStream(fis);
                            listBeacons = (ArrayList<ArrayList<String>>) ois.readObject();
                            ois.close();
                            fis.close();
                            if (listBeacons.contains(currentBeacon)) {
                                listBeacons.remove(currentBeacon);
                                fos = view.getContext().openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
                                oos = new ObjectOutputStream(fos);
                                oos.writeObject(listBeacons);
                                oos.close();
                                fos.close();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }

            });
        }

        public void display(ArrayList<String> key){
            currentBeacon = key;
            //Displaying UUID
            uuid.setText(currentBeacon.get(0));
            //Displaying major
            major.setText(currentBeacon.get(1));
            //Displaying minor
            minor.setText(currentBeacon.get(2));
            //Displaying RSSI
            distance.setText(String.valueOf(beaconsList.get(currentBeacon)));
            //Checking or unchecking the checkbox
            if (isIgnored(currentBeacon)) checkBox.setChecked(false);
        }

        public boolean isIgnored(ArrayList<String> beacon){
            FileInputStream fis;
            ObjectInputStream ois;
            ArrayList<ArrayList<String>> listBeacons;
            try {
                fis = itemView.getContext().openFileInput(FILE_NAME);
                ois = new ObjectInputStream(fis);
                listBeacons = (ArrayList<ArrayList<String>>) ois.readObject();
                ois.close();
                fis.close();
                if  (listBeacons.contains(beacon)) return true;
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

    }

    @Override
    public AdapterBeacons.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_card_selection,parent,false));
    }

    @Override
    public void onBindViewHolder(AdapterBeacons.ViewHolder holder, int position) { holder.display(keys.get(position)); }

    @Override
    public int getItemCount()
    {
        return keys.size();
    }
}
