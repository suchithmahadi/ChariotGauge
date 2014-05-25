package com.chariotinstruments.chariotgauge;

import java.util.Random;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.renderer.XYSeriesRenderer;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SingleChartActivity extends Activity implements Runnable {

    private static GraphicalView mChartView;
    private static Thread thread;
    private LineGraphBuilder line = new LineGraphBuilder();
    private XYSeriesRenderer chartOne = new XYSeriesRenderer(); //chart one.
    private XYSeriesRenderer chartVolts = new XYSeriesRenderer();
    private TimeSeries dataSetOne = new TimeSeries("temp");
    private TimeSeries dataSetVolts = new TimeSeries("volts");
    
    ImageButton  btnOne;
    ImageButton  btnTwo;
    ImageButton  btnHome;
    ImageButton  btnDisplay;
    String       currentMsg;
    MultiGauges  multiGauge;
    MultiGauges  multiGaugeVolts;
    float        currentSValue;
    float        voltSValue;
    boolean      paused;
    int          i = 0;
    
    // Key names received from the BluetoothChatService Handler
    public static final String TOAST       = "toast";
    private static final int CURRENT_TOKEN = 1;
    private static final int VOLT_TOKEN    = 0;
    
    BluetoothSerialService mSerialService; 
    private static Handler workerHandler;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        //Setup the environment.
        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chart_layout);
        
        //assign the top label buttons
        btnOne     = (ImageButton) findViewById(R.id.btnOne);
        btnTwo     = (ImageButton) findViewById(R.id.btnTwo);
        btnDisplay = (ImageButton) findViewById(R.id.btnDisplay);
        
        //setup the gauge-calc instances
        multiGauge      = new MultiGauges(this);
        multiGaugeVolts = new MultiGauges(this);
        multiGauge.buildChart(CURRENT_TOKEN);
        multiGaugeVolts.buildChart(VOLT_TOKEN);
        
        //Get the mSerialService object from the UI activity.
        Object obj = PassObject.getObject();
        //Assign it to global mSerialService variable in this activity.
        mSerialService = (BluetoothSerialService) obj;
        
        //Check if the serial service object is null - assign the handler.
        if(mSerialService != null){
            //Update the BluetoothSerialService instance's handler to this activities.
            mSerialService.setHandler(mHandler);
        }
        
        thread = new Thread(SingleChartActivity.this);
        thread.start();
    }
    
  //Handles the data being sent back from the BluetoothSerialService class.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(!paused){
                byte[] readBuf = (byte[]) msg.obj;
                
                // construct a string from the valid bytes in the buffer
                String readMessage;
                try {
                    readMessage = new String(readBuf, 0, msg.arg1);
                } catch (NullPointerException e) {
                    readMessage = "0";
                }
                
                //Redraw the needle to the correct value.
                currentMsg = readMessage;
                Message workerMsg = workerHandler.obtainMessage(1, currentMsg);
                workerMsg.sendToTarget();
            }
        }
    };

    @Override
    public void run(){
        Looper.prepare();
        workerHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                
                //local variables
                double pointX = 0.0f;
                double pointY = 0.0f;
                double pointYVolts = 0.0f;
                
                //Parse latest data.
                parseInput((String)msg.obj);
                
                //Calc data 
                multiGauge.handleSensor(currentSValue);
                multiGaugeVolts.handleSensor(voltSValue);
                pointX = (double)i;
                pointY = (double)multiGauge.getCurrentGaugeValue();
                pointYVolts = (double)multiGaugeVolts.getCurrentGaugeValue();

                //Put latest data on chart.
                Point p = new Point(pointX, pointY);
                Point pVolts = new Point(pointX, pointYVolts);
                line.setXAxisMin(i-30);
                line.setXAxisMax(i+30);
                
                //Add the points to the graph.
                line.addNewPoints(dataSetOne, p); 
                line.addNewPoints(dataSetVolts, pVolts);
                
                if(!paused){
                    try {
                        mChartView.repaint();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                //Increment time value.
                i++;
            }
        };
        Looper.loop();
    }
    
    
    private void parseInput(String sValue){
        String[] tokens=sValue.split(","); //split the input into an array.

        try {
            currentSValue = Float.valueOf(tokens[CURRENT_TOKEN].toString());//Get current token for this gauge activity, cast as float.
            voltSValue = Float.valueOf(tokens[VOLT_TOKEN].toString());//Get volt token value, cast as float.
        } catch (NumberFormatException e) {
            currentSValue = 0f;
            voltSValue = 0f;
        } catch (ArrayIndexOutOfBoundsException e){
            currentSValue = 0f;
            voltSValue = 0f;
        }
    }
    
    protected XYSeriesRenderer buildNewChart(XYSeriesRenderer chartIn, int chartColor){
        chartIn.setColor(chartColor);
        chartIn.setPointStyle(PointStyle.CIRCLE);
        chartIn.setFillPoints(true);
        
        return chartIn;
    }
    
    protected TimeSeries buildNewTimeSeries(TimeSeries dataSetIn, String name){
        dataSetIn.setTitle(name);
        return dataSetIn;
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        //Setup series renderers.
        chartOne = buildNewChart(chartOne, Color.GREEN);
        chartVolts = buildNewChart(chartVolts, Color.RED);
        
        //Setup datasets.
        dataSetOne = buildNewTimeSeries(dataSetOne, "Boost");
        dataSetVolts = buildNewTimeSeries(dataSetVolts, "Volts");
        
        //Setup line-graph view
        line.setYAxisMin(-100);
        line.setYAxisMax(100);
        line.addDataSet(dataSetOne);
        line.addDataSet(dataSetVolts);
        line.addSeries(chartOne);
        line.addSeries(chartVolts);
        mChartView = line.getView(this);
        
        //add it to the chart_layout layout
        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        layout.addView(mChartView); 
    }
    
    //Kills the looper before going back home
    @Override
    public void onBackPressed(){
        paused = true;
        workerHandler.getLooper().quit();
        super.onBackPressed();
    }
    
    //chart/gauge display click handling
    public void buttonDisplayClick(View v){
        paused = true;
        workerHandler.getLooper().quit();
        PassObject.setObject(mSerialService);
        startActivity(new Intent(getApplicationContext(), BoostActivity.class));
    }
    
    //Button one handling.
    public void buttonOneClick(View v){   
        //TODO: reset max value.
        paused = false;
        btnTwo.setBackgroundResource(Color.TRANSPARENT);
        Toast.makeText(getApplicationContext(), "Max value reset.", Toast.LENGTH_SHORT).show();
    }

    //Button two handling.
    public void buttonTwoClick(View v){
        if(!paused){
            paused = true;
            //TODO: set graph to max value OR Pause
            btnTwo.setBackgroundResource(R.drawable.btn_bg_pressed);
        }else{
            paused = false;
            btnTwo.setBackgroundResource(Color.TRANSPARENT);
        }
    }
    
    //Activity transfer handling
    public void goHome(View v){
        PassObject.setObject(mSerialService);
        onBackPressed();
        finish();
    }
    
    @Override
    protected void onPause(){
        super.onPause();
        if(this.isFinishing()){
            PassObject.setObject(mSerialService);
        }
    }
    
    protected int generateRandomData()
    {
        Random random = new Random();
        return random.nextInt(40);
    }

}