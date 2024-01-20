package com.example.clonedstepcounterapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.example.clonedstepcounterapp.*;
import com.example.clonedstepcounterapp.databinding.ActivityMainBinding;
import com.example.clonedstepcounterapp.permissionUtil.AggregatePermission;
import com.example.clonedstepcounterapp.permissionUtil.PermissionManager;
import com.example.clonedstepcounterapp.permissionUtil.Permissions;
import com.example.clonedstepcounterapp.utils.CommonUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements PermissionManager.PermissionListener, OnSuccessListener {

    int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;
    private final String TAG = "MainActivity";
    private FitnessOptions fitnessOptions;
    private FitnessDataResponseModel fitnessDataResponseModel;
    private ActivityMainBinding activityMainBinding;

    private static final String TAG2 = "HeartRateActivity";


    Button heart;
    TextView bpm_display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initialization();
        checkPermissions();
        bpm_display = findViewById(R.id.bpm_display);
        bpm_display.setText("Fetch Heart rate to display");

//        requestForHistory();

//        Calendar targetDate = Calendar.getInstance();
//        targetDate.set(2023, Calendar.JANUARY, 14);
//        requestDayByDayData(targetDate);
    }

    private void initialization() {
        fitnessDataResponseModel = new FitnessDataResponseModel();
    }

    private void checkPermissions() {
        if (!PermissionManager.hasPermissions(this, Permissions.LOCATION_PERMISSION)) {
            PermissionManager.requestPermissions(this, this, "", Permissions.LOCATION_PERMISSION);
        } else {
            checkGoogleFitPermission();
        }
    }



    private void checkGoogleFitPermission() {

        fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_POINTS,FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .build();
        GoogleSignInAccount account = getGoogleAccount();

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    MainActivity.this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions);
        } else {
            startDataReading();
        }

    }



    private void heartRateCall() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            // You have the permission, proceed with subscribing to heart rate data
            Fitness.getRecordingClient(this, getGoogleAccount())
                    .subscribe(DataType.TYPE_HEART_RATE_BPM)
                    .addOnSuccessListener(aVoid -> {
                        // Successfully subscribed to heart rate data
                        // Now, let's read the heart rate data
                        readHeartRateData();
                        Log.e("DATA","Readed data");
                    })
                    .addOnFailureListener(e -> {
                        // Handle subscription failure
                        Log.e("DATA", "Failed to subscribe to heart rate data", e);
                    });

        } else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BODY_SENSORS}, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE);
        }



    }

    private void readHeartRateData() {
        long endTimeMillis = System.currentTimeMillis();
        long startTimeMillis = endTimeMillis - TimeUnit.HOURS.toMillis(24); // Example: Last hour

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .setTimeRange(startTimeMillis, endTimeMillis, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(this, getGoogleAccount())
                .readData(readRequest)
                .addOnSuccessListener(dataReadResponse -> {
                    // Process heart rate data
                    logHeartRateData(dataReadResponse);
                })
                .addOnFailureListener(e -> {
                    // Handle data read failure
                    Log.e("DATA", "Failed to read heart rate data", e);
                });
    }

    private void logHeartRateData(DataReadResponse dataReadResponse) {
        for (DataSet dataSet : dataReadResponse.getDataSets()) {
            for (DataPoint dataPoint : dataSet.getDataPoints()) {
                // Assuming a single value in each DataPointd
                float heartRate = dataPoint.getValue(Field.FIELD_BPM).asFloat();
                long timestamp = dataPoint.getTimestamp(TimeUnit.MILLISECONDS);

                Log.d("DATA", "Heart Rate: " + heartRate + " bpm at " + new Date(timestamp));
                String data = "Heart Rate: " + heartRate + " bpm at " + new Date(timestamp);
                Log.d("DATA",data);
                bpm_display.setText(data);


            }
        }
    }

    private void startDataReading() {

        getTodayData();

        subscribeAndGetRealTimeData(DataType.TYPE_STEP_COUNT_DELTA);

        heart = findViewById(R.id.heart);
        heart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "clicked", Toast.LENGTH_SHORT).show();

                heartRateCall();
            }
        });



    }

    private void subscribeAndGetRealTimeData(DataType dataType) {
        Fitness.getRecordingClient(this, getGoogleAccount())
                .subscribe(dataType)
                .addOnSuccessListener(aVoid -> {
                    Log.e("DATA", "Subscribed");
                })
                .addOnFailureListener(e -> {
                    Log.e("DATA", "Failure " + e.getLocalizedMessage());
                });

        getDataUsingSensor(dataType);

    }

    private void getDataUsingSensor(DataType dataType) {
        Fitness.getSensorsClient(this, getGoogleAccount())
                .add(new SensorRequest.Builder()
                                .setDataType(dataType)
                                .setSamplingRate(1, TimeUnit.SECONDS)  // sample once per minute
                                .build(),
                        new OnDataPointListener() {
                            @Override
                            public void onDataPoint(@NonNull DataPoint dataPoint) {
                                float value = Float.parseFloat(dataPoint.getValue(Field.FIELD_STEPS).toString());
                                fitnessDataResponseModel.steps = Float.parseFloat(new DecimalFormat("#.##").format(value + fitnessDataResponseModel.steps));
                                activityMainBinding.setFitnessData(fitnessDataResponseModel);
                            }
                        }
                );
    }


    private void getTodayData() {

        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(this);
        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_DISTANCE_DELTA)
                .addOnSuccessListener(this);

        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_HEART_POINTS)  // Add heart points data
                .addOnSuccessListener(this);
        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_HEART_RATE_BPM)  // Add heart rate BPM data
                .addOnSuccessListener(this);

    }

//    public void getHeartRate(){
//        DataReadRequest readRequest = new DataReadRequest.Builder()
//                .read(DataType.TYPE_HEART_RATE_BPM)
//                .setTimeRange(startTimeMillis, endTimeMillis, TimeUnit.MILLISECONDS)
//                .build();
//
//        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
//                .readData(readRequest)
//                .addOnSuccessListener(dataReadResponse -> {
//                    // Process heart rate data
//                    // The heart rate data will be available in dataReadResponse.getDataSets()
//                })
//                .addOnFailureListener(e -> {
//                    // Handle data read failure
//                });
//
//    }



//
//    private void requestForHistory() {
//
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(new Date());
//        long endTime = cal.getTimeInMillis();
//
//        cal.set(2021, 2, 5);
//        cal.set(Calendar.HOUR_OF_DAY, 0); //so it get all day and not the current hour
//        cal.set(Calendar.MINUTE, 0); //so it get all day and not the current minute
//        cal.set(Calendar.SECOND, 0); //so it get all day and not the current second
//        long startTime = cal.getTimeInMillis();
//
//
//        DataReadRequest readRequest = new DataReadRequest.Builder()
//                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
//                .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
//                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
//                .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
//                .aggregate(DataType.TYPE_DISTANCE_DELTA)
//                .aggregate(DataType.AGGREGATE_DISTANCE_DELTA)
//                .bucketByTime(1, TimeUnit.HOURS)
//                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
//                .build();
//
//        Fitness.getHistoryClient(this, getGoogleAccount())
//                .readData(readRequest)
//                .addOnSuccessListener(this);
//    }
//
//    private void requestDayByDayData(Calendar targetDate) {
//        long startTime = targetDate.getTimeInMillis();
//        long endTime = targetDate.getTimeInMillis() + TimeUnit.DAYS.toMillis(1); // End time is set to the next day
//
//        DataReadRequest readRequest = new DataReadRequest.Builder()
//                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
//                .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
//                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
//                .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
//                .aggregate(DataType.TYPE_DISTANCE_DELTA)
//                .aggregate(DataType.AGGREGATE_DISTANCE_DELTA)
//                .aggregate(DataType.TYPE_MOVE_MINUTES)
//                .bucketByTime(1, TimeUnit.HOURS) // You can adjust the bucket size based on your requirement
//                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
//                .build();
//
//        Fitness.getHistoryClient(this, getGoogleAccount())
//                .readData(readRequest)
//                .addOnSuccessListener(dataReadResponse -> {
//                    fitnessDataResponseModel.steps = 0f;
//                    fitnessDataResponseModel.distance = 0f;
//                    fitnessDataResponseModel.moveMinutes = 0f;
//
//                    List<Bucket> bucketList = dataReadResponse.getBuckets();
//                    if (bucketList != null && !bucketList.isEmpty()) {
//                        for (Bucket bucket : bucketList) {
//                            DataSet stepsDataSet = bucket.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);
//                            getDataFromDataReadResponse(stepsDataSet);
//                            DataSet distanceDataSet = bucket.getDataSet(DataType.TYPE_DISTANCE_DELTA);
//                            getDataFromDataReadResponse(distanceDataSet);
//                            DataSet moveMinutesDataSet = bucket.getDataSet(DataType.TYPE_MOVE_MINUTES);
//                            getDataFromDataReadResponse(moveMinutesDataSet);
//                        }
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG, "Error reading historical data: " + e.getLocalizedMessage());
//                    System.out.println("ERR: " + e.toString());
//                });
//    }


    private GoogleSignInAccount getGoogleAccount() {
        return GoogleSignIn.getAccountForExtension(MainActivity.this, fitnessOptions);
    }


    @Override
    public void onSuccess(Object o) {
        if (o instanceof DataSet) {
            DataSet dataSet = (DataSet) o;
            if (dataSet != null) {
                getDataFromDataSet(dataSet);
            }
        } else if (o instanceof DataReadResponse) {
            fitnessDataResponseModel.steps = 0f;
            fitnessDataResponseModel.distance = 0f;
            fitnessDataResponseModel.heartRatePoints = 0f; // Initialize heartPoints
//            fitnessDataResponseModel.heartRateBpm = 0f;

            DataReadResponse dataReadResponse = (DataReadResponse) o;
            if (dataReadResponse.getBuckets() != null && !dataReadResponse.getBuckets().isEmpty()) {
                List<Bucket> bucketList = dataReadResponse.getBuckets();

                if (bucketList != null && !bucketList.isEmpty()) {
                    for (Bucket bucket : bucketList) {
                        DataSet stepsDataSet = bucket.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);
                        getDataFromDataReadResponse(stepsDataSet);
                        DataSet distanceDataSet = bucket.getDataSet(DataType.TYPE_DISTANCE_DELTA);
                        getDataFromDataReadResponse(distanceDataSet);

                        DataSet heartPointsDataSet = bucket.getDataSet(DataType.AGGREGATE_HEART_POINTS);
                        getDataFromDataReadResponse(heartPointsDataSet);


                    }
                }
            }
        }

    }

    private void getDataFromDataReadResponse(DataSet dataSet) {

        List<DataPoint> dataPoints = dataSet.getDataPoints();
        for (DataPoint dataPoint : dataPoints) {
            for (Field field : dataPoint.getDataType().getFields()) {
                Log.e("DATA", " data manual history : " + dataPoint.getOriginalDataSource().getStreamName());



                float value = Float.parseFloat(dataPoint.getValue(field).toString());
                Log.e("DATA", " data : " + value);

                if (field.getName().equals(Field.FIELD_STEPS.getName())) {
                    fitnessDataResponseModel.steps = Float.parseFloat(new DecimalFormat("#.##").format(value + fitnessDataResponseModel.steps));
                }else if (field.getName().equals(Field.FIELD_DISTANCE.getName())) {
                    fitnessDataResponseModel.distance = Float.parseFloat(new DecimalFormat("#.##").format(value + fitnessDataResponseModel.distance));
                }
                else if(field.getName().equals(Field.FIELD_BPM.getName())) {
                    fitnessDataResponseModel.heartRatePoints = Float.parseFloat(new DecimalFormat("#.##").format(value + fitnessDataResponseModel.heartRatePoints));
                }

            }
        }
        activityMainBinding.setFitnessData(fitnessDataResponseModel);

    }

    private void getDataFromDataSet(DataSet dataSet) {

        List<DataPoint> dataPoints = dataSet.getDataPoints();
        for (DataPoint dataPoint : dataPoints) {
            Log.e("DATA", " data manual : " + dataPoint.getOriginalDataSource().getStreamName());

            for (Field field : dataPoint.getDataType().getFields()) {

                float value = Float.parseFloat(dataPoint.getValue(field).toString());
                Log.e("DATA", " data : " + value);

                if (field.getName().equals(Field.FIELD_STEPS.getName())) {
                    fitnessDataResponseModel.steps = Float.parseFloat(new DecimalFormat("#.##").format(value));
                }else if (field.getName().equals(Field.FIELD_DISTANCE.getName())) {
                    fitnessDataResponseModel.distance = Float.parseFloat(new DecimalFormat("#.##").format(value));
                }
                else if(field.getName().equals(Field.FIELD_BPM.getName())){
                    System.out.println("field bpm "+value);
                    fitnessDataResponseModel.heartRatePoints = Float.parseFloat(new DecimalFormat("#.##").format(value));
                } else if (field.getName().equals(DataType.AGGREGATE_HEART_POINTS)) {
                    fitnessDataResponseModel.heartRatePoints = Float.parseFloat(new DecimalFormat("#.##").format(value));
                    System.out.println("field points "+value);
                }


            }
        }
        activityMainBinding.setFitnessData(fitnessDataResponseModel);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            startDataReading();
        }
    }

    @Override
    public void onPermissionsGranted(List<String> perms) {
        if (perms != null && perms.size() == Permissions.LOCATION_PERMISSION.length) {
            checkGoogleFitPermission();
        }
    }

    @Override
    public void onPermissionsDenied(List<String> perms) {
        if (perms.size() > 0) {
            PermissionManager.requestPermissions(this, this, "", Permissions.LOCATION_PERMISSION);
        }
    }

    @Override
    public void onPermissionNeverAsked(List<String> perms) {
        CommonUtils.openSettingForPermission(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.onRequestPermissionsResult(this, this, requestCode, permissions, grantResults);
    }

}