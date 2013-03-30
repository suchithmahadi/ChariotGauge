package com.chariotinstruments.chariotgauge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.ViewManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class CustomActivity extends Activity{
	GaugeBuilder analogGauge1;
	GaugeBuilder analogGauge2;
	MultiGauges  multiGauge1;
	MultiGauges  multiGauge2;
    ImageButton  btnOne;
    ImageButton	 btnTwo;
    Typeface	 typeFaceDigital;
	
    float 	     flt;
    int			 minValue; //gauge min.
    int			 maxValue; //gauge max.
    double		 sensorMinValue; //the lowest value that has been sent from the arduino.
    double		 sensorMaxValue; //the highest value that has been sent from the arduino.
    boolean		 paused;
    Context		 context;
    float		 boostSValue;
    float		 wbSValue;
    float		 tempSValue;
    float		 oilSValue;
    
    //Prefs vars
    View		 root;
    boolean		 showAnalog; //Display the analog gauge or not.
    boolean		 showDigital; //Display the digital gauge or not.
    boolean		 showNightMode; //Change background to black.
    String		 gaugeOnePref;
    String		 gaugeTwoPref;
    int			 currentTokenOne = 1;
    int			 currentTokenTwo = 2;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;
    
    //Test
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST       = "toast";
    public static final double KPA_TO_PSI  = 0.14503773773020923;
    public static final double ATMOSPHERIC = 101.325;
    public static final double KPA_TO_INHG = 0.295299830714;
    private static final int BOOST_TOKEN = 1;
    private static final int WIDEBAND_TOKEN = 2;
    private static final int TEMP_TOKEN = 3;
    private static final int OIL_TOKEN = 4;
    
    BluetoothSerialService mSerialService;
     
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout_2);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    prefsInit(); //Load up the preferences.
	    context = this;
	    
	    //Instantiate the GaugeBuilder.
	    analogGauge1	= (GaugeBuilder) findViewById(R.id.analogGauge);
	    analogGauge2	= (GaugeBuilder) findViewById(R.id.analogGauge2);
	    multiGauge1 	= new MultiGauges(context);
	    multiGauge2 	= new MultiGauges(context);
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);     
   	    
        //Setup gauge 1
        multiGauge1.setAnalogGauge(analogGauge1);
        multiGauge1.buildGauge(currentTokenOne);
        
        //Setup gauge 2
        multiGauge2.setAnalogGauge(analogGauge2);
        multiGauge2.buildGauge(currentTokenTwo);
	  
	    //Get the mSerialService object from the UI activity.
	    Object obj = PassObject.getObject();
	    //Assign it to global mSerialService variable in this activity.
	    mSerialService = (BluetoothSerialService) obj;
	    //Update the BluetoothSerialService instance's handler to this activities.
	    mSerialService.setHandler(mHandler);
	   
	    if(!showAnalog){
	    	((ViewManager)analogGauge1.getParent()).removeView(analogGauge1); //Remove analog gauge
	    }
	    if(showNightMode){
	    	root = btnOne.getRootView(); //Get root layer view.
	        root.setBackgroundColor(getResources().getColor(R.color.black)); //Set background color to black.
	    }

	}
    
    //Handles the data being sent back from the BluetoothSerialService class.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	case MESSAGE_READ:
            		
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
					//update/process the inbound data.
					updateGauges(readMessage);
            		break;
            	case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                                   Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    
    public void updateGauges(String msg){
    	if(!paused){
	    	parseInput(msg);
	    	switch(currentTokenOne){
	    	case 1:
	    		multiGauge1.handleSensor(boostSValue);
	    		break;
	    	case 2:
	    		multiGauge1.handleSensor(wbSValue);
	    		break;
	    	case 3:
	    		multiGauge1.handleSensor(tempSValue);
	    		break;
	    	case 4:
	    		multiGauge1.handleSensor(oilSValue);
	    		break;
	    	default:
	    		break;	
	    	}
			
			multiGauge2.handleSensor(wbSValue);
			analogGauge1.setValue(multiGauge1.getCurrentGaugeValue());
			analogGauge2.setValue(multiGauge2.getCurrentGaugeValue());
    	}
    }
    
    private void parseInput(String sValue){
    	String[] tokens=sValue.split(","); //split the input into an array.
    	
    	try {
    		//Get current tokens for this gauge activity, cast as float.
    		boostSValue = new Float(tokens[BOOST_TOKEN].toString());
    		wbSValue 	= new Float(tokens[WIDEBAND_TOKEN].toString());
    		tempSValue 	= new Float(tokens[TEMP_TOKEN].toString());
    		oilSValue 	= new Float(tokens[OIL_TOKEN].toString());
		} catch (NumberFormatException e) {
			boostSValue = 0;
			wbSValue 	= 0;
			tempSValue 	= 0;
			oilSValue	= 0;
		} catch (ArrayIndexOutOfBoundsException e){
			boostSValue = 0;
			wbSValue 	= 0;
			tempSValue 	= 0;
			oilSValue	= 0;
		}
    }
    
    //Activity transfer
    public void goHome(View v){
    	PassObject.setObject(mSerialService);
    	onBackPressed();
		finish();
	}
    
    //Button one handling.
    public void buttonOneClick(View v){   
    	//Reset the max value.
    	multiGauge1.setSensorMaxValue(multiGauge1.getMinValue());
    	multiGauge2.setSensorMaxValue(multiGauge2.getMinValue());
    	Toast.makeText(getApplicationContext(), "Max value reset", Toast.LENGTH_SHORT).show();
	}
    
    //Button two handling.
    public void buttonTwoClick(View v){
    	if(!paused){
    		paused = true;
    		//set the gauge/digital to the max value captured so far for two seconds.
    		analogGauge1.setValue((float)multiGauge1.getSensorMaxValue());
    		analogGauge2.setValue((float)multiGauge2.getSensorMaxValue());
        	btnTwo.setBackgroundResource(R.drawable.btn_bg_pressed);
    	}else{
    		paused = false;
    		btnTwo.setBackgroundResource(Color.TRANSPARENT);
    	}
	}
    
    protected void onPause(){
    	super.onPause();
    }
    
    protected void onResume(){
    	super.onResume();
    	analogGauge1.invalidate();
    	analogGauge2.invalidate();
    }
       
    public void prefsInit(){
    	SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
    	showAnalog = sp.getBoolean("showAnalog", true);
    	showDigital = sp.getBoolean("showDigital", true);
    	showNightMode = sp.getBoolean("showNightMode", false);
    	gaugeOnePref = sp.getString("multiGaugeOne", "boost");
    	gaugeTwoPref = sp.getString("multiGaugeTwo", "wb");
    	
    	if(gaugeOnePref.equals("Boost")){currentTokenOne = BOOST_TOKEN;}else 
    	if(gaugeOnePref.equals("Wideband O2")){currentTokenOne = WIDEBAND_TOKEN;}else 
    	if(gaugeOnePref.equals("Temperature")){currentTokenOne = TEMP_TOKEN;}else 
    	if(gaugeOnePref.equals("Oil Pressure")){currentTokenOne = OIL_TOKEN;}
    	if(gaugeTwoPref.equals("Boost")){currentTokenTwo = BOOST_TOKEN;}else
    	if(gaugeTwoPref.equals("Wideband O2")){currentTokenTwo = WIDEBAND_TOKEN;}else
    	if(gaugeTwoPref.equals("Temperature")){currentTokenTwo = TEMP_TOKEN;}else
    	if(gaugeTwoPref.equals("Oil Pressure")){currentTokenTwo = OIL_TOKEN;}
    }
}
