package com.example.bletesting;

import android.R.string;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private Calendar mCalendar;
    private SimpleDateFormat mdf;
    private String mup,mlow,mpluse,mtime;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private float upperbp,lowerbp,pulserate;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bletesting.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bletesting.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bletesting.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bletesting.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bletesting.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    
    public final static UUID UUID_BLOOD_PRESSURE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.Blood_Pressure_measurement);
    
    
    public final static UUID UUID_BLOOD_PRESSURE_Feature =
            UUID.fromString(SampleGattAttributes.Blood_Pressure_feature);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	
        	//Toast.makeText(BluetoothLeService.this, "servicediscover", Toast.LENGTH_SHORT).show();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        	
            Log.i(TAG, "onCharacteristicRead");
        	//Toast.makeText(BluetoothLeService.this, "status"+status, Toast.LENGTH_SHORT).show();
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Toast.makeText(BluetoothLeService.this, "ACTION_DATA_AVAILABLE", Toast.LENGTH_SHORT).show();
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged");

         //   Toast.makeText(BluetoothLeService.this, "ACTION_DATA_AVAILABLE", Toast.LENGTH_SHORT).show();
        	broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        
        
        Log.i("broadcasrupdate","broadcasrupdate");

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_BLOOD_PRESSURE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            //Toast.makeText(context, text, duration)
            Log.i("Flag"+flag,"Flag");
            int format = BluetoothGattCharacteristic.FORMAT_SFLOAT;
                     
            //final int heartRate = characteristic.getIntValue(format, 1);
            upperbp = characteristic.getFloatValue(format, 1);
            lowerbp = characteristic.getFloatValue(format, 3);
            pulserate = characteristic.getFloatValue(format, 14);
            mlow = Float.toString(lowerbp);
            mup = Float.toString(upperbp);
            mpluse= Float.toString(pulserate );
            new FetchTask().execute();
            
            
           intent.putExtra(EXTRA_DATA, String.valueOf( upperbp+","+lowerbp+","+pulserate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.i(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.i("Trying to use", "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        Toast.makeText(this, "Trying to create a new connection.", Toast.LENGTH_SHORT).show();

        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //Toast.makeText(this, "readcharacteristics", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "readcharacteristics");

        mBluetoothGatt.readCharacteristic(characteristic);
    	//broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_BLOOD_PRESSURE_Feature.equals(characteristic.getUuid())) {

        }
        
        if (UUID_BLOOD_PRESSURE_MEASUREMENT.equals(characteristic.getUuid())) {
        	
           // Toast.makeText(this, "blood_pressure_measurement", Toast.LENGTH_SHORT).show();
            //Toast.makeText(this,"uuid:"+characteristic.getUuid(),Toast.LENGTH_SHORT).show();
            
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            //int permission = descriptor.getPermissions();
            //Toast.makeText(this, "permission"+permission, Toast.LENGTH_SHORT).show();

            
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    
    public class FetchTask extends AsyncTask<Void, Void, JSONArray> {
	    @Override
	    protected JSONArray doInBackground(Void... params) {
	        try {
	            HttpClient httpclient = new DefaultHttpClient();
	            HttpPost httppost = new HttpPost("http://54.254.179.218/fyp2.php");

	            mCalendar = Calendar.getInstance();
	            mdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	            mtime = mdf.format(mCalendar.getTime());
	            // Add your data
	            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
	            nameValuePairs.add(new BasicNameValuePair("date",mtime));
	            nameValuePairs.add(new BasicNameValuePair("up",mup));
	            nameValuePairs.add(new BasicNameValuePair("low",mlow));
	            nameValuePairs.add(new BasicNameValuePair("pulse",mpluse));
	            
	          //  nameValuePairs.add(new BasicNameValuePair("stringdata", "AndDev is Cool!"));
	            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	            // Execute HTTP Post Request
	            HttpResponse response = httpclient.execute(httppost);

	            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "iso-8859-1"), 8);
	            StringBuilder sb = new StringBuilder();
	            sb.append(reader.readLine() + "\n");
	            String line = "0";
	            while ((line = reader.readLine()) != null) {
	                sb.append(line + "\n");
	            }
	            reader.close();
	            String result11 = sb.toString();
/* 		String uri = "http://pc89123.cse.cuhk.edu.hk/~r36/fyp2.php";
		//String uri = "http://www.yoursite.com/script.php";
		 HttpClient httpclient = new DefaultHttpClient();

		HttpPost httpRequest = new HttpPost(uri);
		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		params.add(new BasicNameValuePair("up","122"));
		params.add(new BasicNameValuePair("low", "133"));
		params.add(new BasicNameValuePair("pulse", "111"));*/
	            // parsing data
 	            return new JSONArray(result11);
	        } catch (Exception e) {
	            e.printStackTrace();
	            return null;
	        }
	    }

	    @Override
	    protected void onPostExecute(JSONArray result) {
	        if (result != null) {
	            // do something
	        } else {
	            // error occured
	        }
	    }
	}
}
