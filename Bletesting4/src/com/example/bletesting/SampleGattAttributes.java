package com.example.bletesting;

import java.util.HashMap;


public class SampleGattAttributes {
	
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String Blood_Pressure_feature = "00002a49-0000-1000-8000-00805f9b34fb";
    public static String Blood_Pressure_measurement = "00002a35-0000-1000-8000-00805f9b34fb";  
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        
    	attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service"); 
    	attributes.put("00001810-0000-1000-8000-00805f9b34fb", "Blood Pressure Service");     	
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(Blood_Pressure_feature, "Blood Pressure feature");
        attributes.put(Blood_Pressure_measurement, "Blood Pressure measurement");
        
        
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}