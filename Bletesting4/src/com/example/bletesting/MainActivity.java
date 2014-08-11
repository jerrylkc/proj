package com.example.bletesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
 
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Entity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;



public class MainActivity extends ListActivity {

	
	private boolean mScanning;
	private Handler mHandler;
	private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 10000;
    ListView list;
	
    
    final Runnable runnable = new Runnable()
    {
        public void run() 
        {
 	        scanLeDevice(true);
	        mHandler.postDelayed(this, 5000);

        }
        };
    
    
    
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mHandler = new Handler();		
		list = (ListView)findViewById(android.R.id.list);
				
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE does not support", Toast.LENGTH_SHORT).show();
            finish();
        }
		
		else
            Toast.makeText(this, "BLE support", Toast.LENGTH_SHORT).show();

		
		
		 final BluetoothManager bluetoothManager =
	                (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
	        mBluetoothAdapter = bluetoothManager.getAdapter();

	        // Checks if Bluetooth is supported on the device.
	        if (mBluetoothAdapter == null) {
	            Toast.makeText(this, "No bluetooth", Toast.LENGTH_SHORT).show();
	            finish();
	            return;
	        }
		
		
	        mLeDeviceListAdapter = new LeDeviceListAdapter();
	        list.setAdapter(mLeDeviceListAdapter);
	        mHandler.postDelayed(runnable, 5000);
  	         scanLeDevice(true);
 
//	        	Toast.makeText(this, "posted!", Toast.LENGTH_SHORT).show();
	        
	        IntentFilter filter = new IntentFilter();
	        filter.addAction("resume");
	        
	        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
	        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
	            public void onReceive(Context context, Intent intent) {
	             
	            	//mHandler.postDelayed(runnable, 5000);
	            	
 	            }
	        };
	        localBroadcastManager.registerReceiver(broadcastReceiver, filter);    
	        
	        
	        

	}
	
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                   Toast.makeText(MainActivity.this, device.getName()+" "+device.getAddress()+" ", Toast.LENGTH_SHORT).show();
       	           mLeDeviceListAdapter = new LeDeviceListAdapter();
                   mLeDeviceListAdapter.addDevice(device);
                   Toast.makeText(MainActivity.this, device.getName()+" "+device.getAddress()+" ", Toast.LENGTH_SHORT).show();
                   
                   
                   if(device.getAddress().equals("78:C5:E5:99:F6:07")){
                	   
                   //mHandler.removeCallbacks(runnable); 	   
                   final  Intent intent = new Intent(MainActivity.this, DeviceControlActivity.class);
                   
                   intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                   intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                   
                   if (mScanning) {
                       mBluetoothAdapter.stopLeScan(mLeScanCallback);
                       mScanning = false;
                   }
                   //Toast.makeText(MainActivity.this, "our device!!!! ", Toast.LENGTH_SHORT).show();
                   startActivity(intent);}
                   
                   list.setAdapter(mLeDeviceListAdapter);
                }
            });
        }
    };
	
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        
         //Toast.makeText(this, "ListItemClick!", Toast.LENGTH_LONG).show();
        //Toast.makeText(MainActivity.this,"hithithit", Toast.LENGTH_SHORT).show();

        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
        
    }
    
    
	private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }
	
	
	
    
    
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }
	

	
	
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
//                post();
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.	
	      getMenuInflater().inflate(R.menu.main, menu);
	        if (!mScanning) {
	            menu.findItem(R.id.menu_stop).setVisible(false);
	            menu.findItem(R.id.menu_scan).setVisible(true);
	            menu.findItem(R.id.menu_refresh).setActionView(null);
	        } else {
	            menu.findItem(R.id.menu_stop).setVisible(true);
	            menu.findItem(R.id.menu_scan).setVisible(false);
	            menu.findItem(R.id.menu_refresh).setActionView(
	                    R.layout.actionbar_indeterminate_progress);
	        }
	        return true;
	        	        
	}
	
	
	protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }

        // Initializes list view adapter.
//        Toast.makeText(this, "setlistadapter", Toast.LENGTH_LONG).show();

        
        scanLeDevice(true);
    }
	
	public class FetchTask extends AsyncTask<Void, Void, JSONArray> {
	    @Override
	    protected JSONArray doInBackground(Void... params) {
	        try {
	            HttpClient httpclient = new DefaultHttpClient();
	            HttpPost httppost = new HttpPost("http://pc89123.cse.cuhk.edu.hk/~r36/fyp2.php");

	            // Add your data
	            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
	            nameValuePairs.add(new BasicNameValuePair("up","1--1"));
	            nameValuePairs.add(new BasicNameValuePair("low","04440"));
	            nameValuePairs.add(new BasicNameValuePair("pulse","lololol"));
	            
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

	
	
	private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            //mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            mInflator = (LayoutInflater)MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // General ListView optimization code.
            if (view == null) {

                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0){
                viewHolder.deviceName.setText(deviceName);
                Log.i("done","done");
            }
            else{
                viewHolder.deviceName.setText("unknow");
                Log.i("done","done");}

            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
	}
        
        static class ViewHolder {
            TextView deviceName;
            TextView deviceAddress;
        }
}
