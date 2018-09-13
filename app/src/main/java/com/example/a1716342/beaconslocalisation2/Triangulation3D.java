package com.example.a1716342.beaconslocalisation2;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Hashtable;


public class Triangulation3D {
    private static final String TAG = "triangulation";
    private static final String FILE_NAME = "reference_position.dat";


    private Context context;

    //Distances given by the application, with all the beacons
    private Hashtable<ArrayList<String>, Double> distances;

    //Distances of the selected beacons
    private Hashtable<ArrayList<String>, Double> selectedDistances;

    //Coordinates of all the beacons
    private Hashtable<ArrayList<String>, ArrayList<Double>> references;

    //Coordinates of the user's position (result)
    private ArrayList<Double> coordinates;

    //Values for the computation
    private double a, b, c, d, ap, bp, cp, dp;
    private double A, B;
    private double alpha, beta, gamma, delta;
    private double correctionParameter;

    //Constructors
    public Triangulation3D(Context context, double correctionParameter){
        this.context = context;
        this.correctionParameter = correctionParameter;
    }

    public Triangulation3D(Context context){
        this.context = context;
        this.correctionParameter = 0.02;
    }


    public ArrayList<Double> compute(Hashtable<ArrayList<String>, Double> distances, int index1, int index2, int index3, int index4){

        /*
        This is the main method, it has to be called by the class which uses triangulation
         */
        double x, y, z;

        this.coordinates = new ArrayList<>();
        this.setDistances(distances);
        this.readReferences();
        this.selectedDistances = selectBeacons((Hashtable<ArrayList<String>,Double>)distances.clone());
        logDistance();
        this.correction();
        logDistance();
        logCoordinates();
        this.firstStep(index1,index2,index3);

        if (this.c != 0 && this.cp != 0) {
            this.secondStep1();
            this.thirdStep1(index1);
            coordinates = solution1(index4);
            logVariable();
        }
        else if (this.c == 0 && this.cp != 0){
            if (this.b != 0) {
                this.secondStep2();
                this.thirdStep2(index1);
                coordinates = solution2(index4);
                logVariable();
            }
            else {
                this.secondStep3(index1);
                coordinates = solution3(index4);
                logVariable();
            }
        }
        else if (this.c != 0 && this.cp == 0){
            if (this.bp != 0) {
                this.secondStep4();
                this.thirdStep4(index1);
                coordinates = solution4(index4);

                logVariable();
            }
            else {
                this.secondStep5(index1);
                coordinates = solution5(index4);

                logVariable();
            }
        }
        else {
            if (this.b != 0){
                this.secondStep6();
                this.thirdStep6(index1);
                coordinates = solution6(index4);

                logVariable();
            }
            else {
                this.secondStep7();
                this.thirdStep7(index1);
                coordinates = solution7(index4);

                logVariable();
            }
        }

        return this.coordinates;
    }

    public void setDistances(Hashtable<ArrayList<String>, Double> distances){
        this.distances = distances;
    }


    private void readReferences(){
        //Read the positions of the beacons in a file
        FileInputStream fis;
        ObjectInputStream ois;
        references = new Hashtable<ArrayList<String>,ArrayList<Double>>();
        try{
            fis = context.openFileInput(FILE_NAME);
            ois = new ObjectInputStream(fis);
            references = (Hashtable<ArrayList<String>,ArrayList<Double>>) ois.readObject();
            ois.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private Hashtable<ArrayList<String>,Double> selectBeacons(Hashtable<ArrayList<String>,Double> distances) {
        //Select the 4 closest beacons
        Hashtable<ArrayList<String>,Double> selectedDistances = new Hashtable<ArrayList<String>,Double>();
        ArrayList<String> min;
        while (selectedDistances.size()<4){
            min = minimum(distances);
            selectedDistances.put(min, distances.get(min));
            distances.remove(min);
        }
        return selectedDistances;
    }

    private ArrayList<String> minimum(Hashtable<ArrayList<String>, Double> distances){
        //Find the closest beacon
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(distances.keySet());
        ArrayList<String> min = keys.get(0);
        for (ArrayList<String> i : keys){
            if (distances.get(i)<distances.get(min)){
                min = i;
            }
        }
        return min;
    }

    private void correction(){
        //Ensure that a solution exists
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(selectedDistances.keySet());
        double d1,d2;
        for(int i = 0; i < keys.size(); i++){
            for (int j = i+1; j < keys.size(); j++){
                while( selectedDistances.get(keys.get(i))+selectedDistances.get(keys.get(j))<distance(references.get(keys.get(i)), references.get(keys.get(j)))){
                    //Log.i(TAG, selectedDistances.get(keys.get(i))+" ; "+selectedDistances.get(keys.get(j))+" ; "+distance(references.get(keys.get(i)), references.get(keys.get(j))));
                    d1 = selectedDistances.get(keys.get(i));
                    selectedDistances.remove(keys.get(i));
                    selectedDistances.put(keys.get(i), d1+correctionParameter);

                    d2 = selectedDistances.get(keys.get(j));
                    selectedDistances.remove(keys.get(j));
                    selectedDistances.put(keys.get(j), d2+correctionParameter);
                }
            }
        }
    }

    private double distance(ArrayList<Double> A, ArrayList<Double> B){
        //Give the distance between 2 positions
        double d = (A.get(0)-B.get(0))*(A.get(0)-B.get(0)) + (A.get(1)-B.get(1))*(A.get(1)-B.get(1)) + (A.get(2)-B.get(2))*(A.get(2)-B.get(2));
        return Math.sqrt(d);
    }

    private double distance(int index){
        //Give the distance between the user and a beacon (among the selected beacons
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(selectedDistances.keySet());
        return selectedDistances.get(keys.get(index));
    }

    private double cooX(int index){
        //Give the X value of the coordinates of a beacon (among the selected beacons)
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(selectedDistances.keySet());
        return references.get(keys.get(index)).get(0);
    }

    private double cooY(int index){
        //Give the Y value of the coordinates of a beacon (among the selected beacons)
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(selectedDistances.keySet());
        return references.get(keys.get(index)).get(1);
    }

    private double cooZ(int index){
        //Give the Z value of the coordinates of a beacon (among the selected beacons)
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(selectedDistances.keySet());
        return references.get(keys.get(index)).get(2);
    }

    private ArrayList<Double> getBeaconCoordinates(int index){
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(selectedDistances.keySet());
        return references.get(keys.get(index));
    }

    private void firstStep(int index1, int index2, int index3){
        // First step of the computation
        // Compute a, b, c, d, ap, bp, cp, dp
        this.a = setScale(2*cooX(index2)-2*cooX(index1));
        this.b = setScale(2*cooY(index2)-2*cooY(index1));
        this.c = setScale(2*cooZ(index2)-2*cooZ(index1));
        this.d = setScale(cooX(index1)*cooX(index1)-cooX(index2)*cooX(index2)
                +cooY(index1)*cooY(index1)-cooY(index2)*cooY(index2)
                +cooZ(index1)*cooZ(index1)-cooZ(index2)*cooZ(index2)
                +distance(index2)*distance(index2)-distance(index1)*distance(index1));

        this.ap = setScale(2*cooX(index3)-2*cooX(index1));
        this.bp = setScale(2*cooY(index3)-2*cooY(index1));
        this.cp = setScale(2*cooZ(index3)-2*cooZ(index1));
        this.dp = setScale(cooX(index1)*cooX(index1)-cooX(index3)*cooX(index3)
                +cooY(index1)*cooY(index1)-cooY(index3)*cooY(index3)
                +cooZ(index1)*cooZ(index1)-cooZ(index3)*cooZ(index3)
                +distance(index3)*distance(index3)-distance(index1)*distance(index1));
    }

    private void logVariable(){
        Log.i(TAG, "a : "+a);
        Log.i(TAG, "b : "+b);
        Log.i(TAG, "c : "+c);
        Log.i(TAG, "d : "+d);

        Log.i(TAG, "ap : "+ap);
        Log.i(TAG, "bp : "+bp);
        Log.i(TAG, "cp : "+cp);
        Log.i(TAG, "dp : "+dp);

        Log.i(TAG, "A : "+A);
        Log.i(TAG, "B : "+B);

        Log.i(TAG, "alpha : "+alpha);
        Log.i(TAG, "beta : "+beta);
        Log.i(TAG, "gamma : "+gamma);
        Log.i(TAG, "delta : "+delta);
    }

    private void logDistance(){
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(selectedDistances.keySet());
        for (ArrayList<String> i : keys){
            Log.i(TAG, i.toString()+" : "+selectedDistances.get(i));
        }
    }

    private void logCoordinates(){
        ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(references.keySet());
        for (ArrayList<String> i : keys) {
            Log.i(TAG, i.toString() + " : " + references.get(i));
        }
    }

    private double setScale(double d){
        return ((double)Math.round(d*1000))/1000;
    }

    //methods below compute different variables (each versions corresponds with a case)

    private void secondStep1(){
        this.A =  (this.ap/this.cp-this.a/this.c)/(this.b/this.c-this.bp/this.cp);
        this.B = (this.dp/this.cp-this.d/this.c)/(this.b/this.c-this.bp/this.cp);
    }

    private void thirdStep1(int index1){
        this.alpha = 1 + this.A*this.A + (this.a/this.c)*(this.a/this.c) + (this.b*this.A/this.c)*(this.b*this.A/this.c) + 2*this.a*this.b*this.A/(this.c*this.c);
        this.beta = -2*cooX(index1) - 2*cooY(index1)*this.A + 2*this.A*this.B - 2*cooZ(index1)*this.a/this.c - 2*cooZ(index1)*this.b*this.A/this.c + 2*this.A*this.B*this.b*this.b/(this.c*this.c)
                +2*this.a*this.b*this.B/(this.c*this.c) + 2*this.a*this.d/(this.c*this.c) + 2*this.b*this.d*this.A/(this.c*this.c);
        this.gamma = cooX(index1)*cooX(index1) + cooY(index1)*cooY(index1) - 2*cooY(index1)*this.B + this.B*this.B + cooZ(index1)*cooZ(index1) - 2*cooZ(index1)*this.b*this.B/this.c - 2*cooZ(index1)*this.d/this.c
                + (this.b*this.B/this.c)*(this.b*this.B/this.c) + (this.d/this.c)*(this.d/this.c) + 2*this.b*this.B*this.d/(this.c*this.c) - distance(index1)*distance(index1);
    }

    private void secondStep2(){
        this.A =  -this.a/this.b;
        this.B = -this.d/this.b;
    }

    private void thirdStep2(int index1){
        this.alpha = 1 + this.A*this.A + (this.ap/this.cp)*(this.ap/this.cp) + (this.bp*this.A/this.cp)*(this.bp*this.A/this.cp) + 2*this.ap*this.bp*this.A/(this.cp*this.cp);
        this.beta = -2*cooX(index1) - 2*cooY(index1)*this.A + 2*this.A*this.B - 2*cooZ(index1)*this.ap/this.cp - 2*cooZ(index1)*this.bp*this.A/this.cp + 2*this.A*this.B*this.bp*this.bp/(this.cp*this.cp)
                +2*this.ap*this.bp*this.B/(this.cp*this.cp) + 2*this.ap*this.dp/(this.cp*this.cp) + 2*this.bp*this.dp*this.A/(this.cp*this.cp);
        this.gamma = cooX(index1)*cooX(index1) + cooY(index1)*cooY(index1) - 2*cooY(index1)*this.B + this.B*this.B + cooZ(index1)*cooZ(index1) + 2*cooZ(index1)*this.bp*this.B/this.cp + 2*cooZ(index1)*this.dp/this.cp
                + (this.bp*this.B/this.cp)*(this.bp*this.B/this.cp) + (this.dp/this.cp)*(this.dp/this.cp) + 2*this.bp*this.B*this.dp/(this.cp*this.cp) - distance(index1)*distance(index1);
    }

    private void secondStep3(int index1){
        this.alpha = 1 + (this.bp/this.cp)*(this.bp/this.cp);
        this.beta = 2*cooY(index1) + 2*cooZ(index1)*this.bp/this.cp - 2*this.ap*this.d*this.bp/(this.a*this.cp*this.cp) + 2*this.bp*this.dp/(this.cp*this.cp);
        this.gamma = cooX(index1)*cooX(index1) + 2*cooX(index1)*this.d/this.a + (this.d/this.a)*(this.d/this.a) + cooY(index1)*cooY(index1) + cooZ(index1)*cooZ(index1) - 2*cooZ(index1)*this.ap*this.d/(this.a*this.cp)
                + 2*cooZ(index1)*this.dp/this.cp + (this.ap*this.d/(this.a*this.cp))*(this.ap*this.d/(this.a*this.cp)) + (this.dp/this.cp)*(this.dp/this.cp) - 2*this.ap*this.d*this.dp/(this.a*this.cp*this.cp) - distance(index1)*distance(index1);
    }

    private void secondStep4(){
        this.A =  -this.ap/this.bp;
        this.B = -this.dp/this.bp;
    }

    private void thirdStep4(int index1){
        this.alpha = 1 + this.A*this.A + (this.a/this.c)*(this.a/this.c) + (this.b*this.A/this.c)*(this.b*this.A/this.c) + 2*this.a*this.b*this.A/(this.c*this.c);
        this.beta = -2*cooX(index1) - 2*cooY(index1)*this.A + 2*this.A*this.B - 2*cooZ(index1)*this.a/this.c - 2*cooZ(index1)*this.b*this.A/this.c + 2*this.A*this.B*this.b*this.b/(this.c*this.c)
                +2*this.a*this.b*this.B/(this.c*this.c) + 2*this.a*this.d/(this.c*this.c) + 2*this.b*this.d*this.A/(this.c*this.c);
        this.gamma = cooX(index1)*cooX(index1) + cooY(index1)*cooY(index1) - 2*cooY(index1)*this.B + this.B*this.B + cooZ(index1)*cooZ(index1) - 2*cooZ(index1)*this.b*this.B/this.c - 2*cooZ(index1)*this.d/this.c
                + (this.b*this.B/this.c)*(this.b*this.B/this.c) + (this.d/this.c)*(this.d/this.c) + 2*this.b*this.B*this.d/(this.c*this.c) - distance(index1)*distance(index1);
    }

    private void secondStep5(int index1){
        this.alpha = 1 + (this.b/this.c)*(this.b/this.c);
        this.beta = 2*cooY(index1) + 2*cooZ(index1)*this.b/this.c - 2*this.a*this.dp*this.b/(this.ap*this.c) + 2*this.b*this.d/this.c;
        this.gamma = cooX(index1)*cooX(index1) + 2*cooX(index1)*this.dp/this.ap + (this.dp/this.ap)*(this.dp/this.ap) + cooY(index1)*cooY(index1) + cooZ(index1)*cooZ(index1) - 2*cooZ(index1)*this.a*this.dp/(this.ap*this.c)
                + 2*cooZ(index1)*this.d/this.c + (this.a*this.dp/(this.ap*this.c))*(this.a*this.dp/(this.ap*this.c)) + (this.d/this.c)*(this.d/this.c) - 2*this.a*this.dp*this.d/(this.ap*this.c) - distance(index1)*distance(index1);
    }

    private void secondStep6(){
        this.A =  ((this.d*this.bp)/this.b-this.dp)/(this.ap-this.a*this.bp/this.b);
        this.B = (-this.d-this.A*this.a)/this.b;
    }

    private void thirdStep6(int index1){
        this.alpha = 1;
        this.beta = -2*cooZ(index1);
        this.gamma = (cooX(index1)-this.A)*(cooX(index1)-this.A) + (cooY(index1)-this.B)*(cooY(index1)-this.B) + cooZ(index1)*cooZ(index1)-distance(index1)*distance(index1);
    }

    private void secondStep7(){
        this.A =  -this.d/this.a;
        this.B = (-this.d*this.ap  )/(this.a*this.bp)-this.dp/this.bp;
    }

    private void thirdStep7(int index1){

        this.alpha = 1;
        this.beta = -2*cooZ(index1);
        this.gamma = (cooX(index1)-this.A)*(cooX(index1)-this.A) + (cooY(index1)-this.B)*(cooY(index1)-this.B) + (cooZ(index1)*cooZ(index1)) -distance(index1)*distance(index1);
    }

    /*
    All the methods below return a list containing coordinates
    */

    private ArrayList<Double> solution1(int index4){
        ArrayList<Double> s1, s2;
        s1 = new ArrayList<>();
        s2 = new ArrayList<>();
        double x1, x2;
        this.delta = this.beta*this.beta - 4*this.alpha*this.gamma;

        x1 = setScale((-this.beta - Math.sqrt(this.delta))/(2*this.alpha));
        x2 = setScale((-this.beta + Math.sqrt(this.delta))/(2*this.alpha));
        s1.add(x1);
        s1.add(this.A*x1+this.B);
        s1.add(-this.a*x1/this.c - this.b*s1.get(1)/this.c - this.d/this.c);
        s2.add(x2);
        s2.add(this.A*x2+this.B);
        s2.add(-this.a*x2/this.c - this.b*s2.get(1)/this.c - this.d/this.c);

        Log.i(TAG, s1.toString()+" ; "+s2.toString());

        Log.i(TAG, distance(s1,getBeaconCoordinates(index4))+" ; "+distance(s2,getBeaconCoordinates(index4))+" ; "+distance(index4));

        if (((distance(s1,getBeaconCoordinates(index4))-distance(index4))*(distance(s1,getBeaconCoordinates(index4))-distance(index4))) < ((distance(s2,getBeaconCoordinates(index4))-distance(index4))*(distance(s2,getBeaconCoordinates(index4))-distance(index4)))) {
            return s1;
        } else {
            return s2;
        }
    }

    private ArrayList<Double> solution2(int index4){
        ArrayList<Double> s1, s2;
        s1 = new ArrayList<>();
        s2 = new ArrayList<>();
        double x1,y1, z1, x2, y2, z2;
        this.delta = this.beta*this.beta - 4*this.alpha*this.gamma;

        x1 = setScale((-this.beta - Math.sqrt(this.delta))/(2*this.alpha));
        x2 = setScale((-this.beta + Math.sqrt(this.delta))/(2*this.alpha));
        y1 = setScale(this.A*x1+this.B);
        z1 = setScale(-this.ap*x1/this.cp - this.bp*y1/this.cp - this.dp/this.cp);
        y2 = setScale(this.A*x1+this.B);
        z2 = setScale(-this.ap*x1/this.cp - this.bp*y1/this.cp - this.dp/this.cp);

        s1.add(x1);
        s1.add(y1);
        s1.add(z1);
        s2.add(x2);
        s2.add(y2);
        s2.add(z2);

        Log.i(TAG, s1.toString()+" ; "+s2.toString());

        Log.i(TAG, distance(s1,getBeaconCoordinates(index4))+" ; "+distance(s2,getBeaconCoordinates(index4))+" ; "+distance(index4));

        if (((distance(s1,getBeaconCoordinates(index4))-distance(index4))*(distance(s1,getBeaconCoordinates(index4))-distance(index4))) < ((distance(s2,getBeaconCoordinates(index4))-distance(index4))*(distance(s2,getBeaconCoordinates(index4))-distance(index4)))) {
            return s1;
        } else {
            return s2;
        }
    }

    private ArrayList<Double> solution3(int index4){
        ArrayList<Double> s1, s2;
        s1 = new ArrayList<>();
        s2 = new ArrayList<>();
        double x1,y1, z1, x2, y2, z2;
        this.delta = this.beta*this.beta - 4*this.alpha*this.gamma;

        y1 = setScale((-this.beta - Math.sqrt(this.delta))/(2*this.alpha));
        y2 = setScale((-this.beta + Math.sqrt(this.delta))/(2*this.alpha));
        x1 = setScale(-this.d/this.a);
        z1 = setScale(-this.ap*x1/this.cp - this.bp*y1/this.cp - this.dp/this.cp);
        x2 = setScale(-this.d/this.a);
        z2 = setScale(-this.ap*x2/this.cp - this.bp*y2/this.cp - this.dp/this.cp);

        s1.add(x1);
        s1.add(y1);
        s1.add(z1);
        s2.add(x2);
        s2.add(y2);
        s2.add(z2);

        Log.i(TAG, s1.toString()+" ; "+s2.toString());

        Log.i(TAG, distance(s1,getBeaconCoordinates(index4))+" ; "+distance(s2,getBeaconCoordinates(index4))+" ; "+distance(index4));

        if (((distance(s1,getBeaconCoordinates(index4))-distance(index4))*(distance(s1,getBeaconCoordinates(index4))-distance(index4))) < ((distance(s2,getBeaconCoordinates(index4))-distance(index4))*(distance(s2,getBeaconCoordinates(index4))-distance(index4)))) {
            return s1;
        } else {
            return s2;
        }
    }

    private ArrayList<Double> solution4(int index4){
        ArrayList<Double> s1, s2;
        s1 = new ArrayList<>();
        s2 = new ArrayList<>();
        double x1,y1, z1, x2, y2, z2;
        this.delta = this.beta*this.beta - 4*this.alpha*this.gamma;

        x1 = setScale((-this.beta - Math.sqrt(this.delta))/(2*this.alpha));
        y1 = setScale(this.A*x1+this.B);
        z1 = setScale(-this.a*x1/this.c - this.b*y1/this.c - this.d/this.c);
        x2 = setScale((-this.beta + Math.sqrt(this.delta))/(2*this.alpha));
        y2 = setScale(this.A*x2+this.B);
        z2 = setScale(-this.a*x2/this.c - this.b*y2/this.c - this.d/this.c);
        s1.add(x1);
        s1.add(y1);
        s1.add(z1);
        s2.add(x2);
        s2.add(y2);
        s2.add(z2);

        Log.i(TAG, s1.toString()+" ; "+s2.toString());

        Log.i(TAG, distance(s1,getBeaconCoordinates(index4))+" ; "+distance(s2,getBeaconCoordinates(index4))+" ; "+distance(index4));

        if (((distance(s1,getBeaconCoordinates(index4))-distance(index4))*(distance(s1,getBeaconCoordinates(index4))-distance(index4))) < ((distance(s2,getBeaconCoordinates(index4))-distance(index4))*(distance(s2,getBeaconCoordinates(index4))-distance(index4)))) {
            return s1;
        } else {
            return s2;
        }
    }

    private ArrayList<Double> solution5(int index4){
        ArrayList<Double> s1, s2;
        s1 = new ArrayList<>();
        s2 = new ArrayList<>();
        double x1,y1, z1, x2, y2, z2;
        this.delta = this.beta*this.beta - 4*this.alpha*this.gamma;

        y1 = setScale((-this.beta - Math.sqrt(this.delta))/(2*this.alpha));
        y2 = setScale((-this.beta + Math.sqrt(this.delta))/(2*this.alpha));
        x1 = setScale(-this.dp/this.ap);
        z1 = setScale(-this.a*x1/this.c - this.b*y1/this.c - this.d/this.c);
        x2 = setScale(-this.dp/this.ap);
        z2 = setScale(-this.a*x2/this.c - this.b*y2/this.c - this.d/this.c);
        s1.add(x1);
        s1.add(y1);
        s1.add(z1);
        s2.add(x2);
        s2.add(y2);
        s2.add(z2);

        Log.i(TAG, s1.toString()+" ; "+s2.toString());

        Log.i(TAG, distance(s1,getBeaconCoordinates(index4))+" ; "+distance(s2,getBeaconCoordinates(index4))+" ; "+distance(index4));

        if (((distance(s1,getBeaconCoordinates(index4))-distance(index4))*(distance(s1,getBeaconCoordinates(index4))-distance(index4))) < ((distance(s2,getBeaconCoordinates(index4))-distance(index4))*(distance(s2,getBeaconCoordinates(index4))-distance(index4)))) {
            return s1;
        } else {
            return s2;
        }
    }

    private ArrayList<Double> solution6(int index4){
        ArrayList<Double> s1, s2;
        s1 = new ArrayList<>();
        s2 = new ArrayList<>();
        double x1,   y1, z1, x2, y2, z2;
        this.delta = this.beta*this.beta - 4*this.alpha*this.gamma;

        x1 = setScale(this.A);
        y1 = setScale(this.B);
        z1 = setScale((-this.beta - Math.sqrt(this.delta))/(2*this.alpha));
        x2 = setScale(this.A);
        y2 = setScale(this.B);
        z2 = setScale((-this.beta + Math.sqrt(this.delta))/(2*this.alpha));

        s1.add(x1);
        s1.add(y1);
        s1.add(z1);
        s2.add(x2);
        s2.add(y2);
        s2.add(z2);

        Log.i(TAG, s1.toString()+" ; "+s2.toString());

        Log.i(TAG, distance(s1,getBeaconCoordinates(index4))+" ; "+distance(s2,getBeaconCoordinates(index4))+" ; "+distance(index4));

        if (((distance(s1,getBeaconCoordinates(index4))-distance(index4))*(distance(s1,getBeaconCoordinates(index4))-distance(index4))) <= ((distance(s2,getBeaconCoordinates(index4))-distance(index4))*(distance(s2,getBeaconCoordinates(index4))-distance(index4)))) {
            return s1;
        } else {
            return s2;
        }
    }

    private ArrayList<Double> solution7(int index4){
        ArrayList<Double> s1, s2;
        s1 = new ArrayList<>();
        s2 = new ArrayList<>();
        double x1,y1, z1, x2, y2, z2;
        this.delta = this.beta*this.beta - 4*this.alpha*this.gamma;

        x1 = this.A;
        y1 = this.B;
        z1 = setScale((-this.beta - Math.sqrt(this.delta))/(2*this.alpha));
        x2 = this.A;
        y2 = this.B;
        z2 = setScale((-this.beta + Math.sqrt(this.delta))/(2*this.alpha));

        s1.add(x1);
        s1.add(y1);
        s1.add(z1);
        s2.add(x2);
        s2.add(y2);
        s2.add(z2);

        Log.i(TAG, s1.toString()+" ; "+s2.toString());

        Log.i(TAG, distance(s1,getBeaconCoordinates(index4))+" ; "+distance(s2,getBeaconCoordinates(index4))+" ; "+distance(index4));

        if (((distance(s1,getBeaconCoordinates(index4))-distance(index4))*(distance(s1,getBeaconCoordinates(index4))-distance(index4))) < ((distance(s2,getBeaconCoordinates(index4))-distance(index4))*(distance(s2,getBeaconCoordinates(index4))-distance(index4)))) {
            return s1;
        } else {
            return s2;
        }
    }
}
