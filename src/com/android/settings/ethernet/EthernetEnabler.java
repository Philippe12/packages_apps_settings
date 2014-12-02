/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.ethernet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.ethernet.IEthernetManager;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDevInfo;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.WirelessSettings;

import java.util.concurrent.atomic.AtomicBoolean;
import android.os.AsyncTask;

public class EthernetEnabler implements CompoundButton.OnCheckedChangeListener  {
    private final Context mContext;
    private Switch mSwitch;
    private AtomicBoolean mConnected = new AtomicBoolean(false);

	private EthernetManager mEthManager;
    private boolean mStateMachineEvent;
    private ConnectivityManager mService;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
			if (EthernetManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
 				final int event = intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE,
												EthernetManager.EVENT_CONFIGURATION_SUCCEEDED);
				switch(event){
					case EthernetManager.EVENT_CONFIGURATION_SUCCEEDED:
						mSwitch.setChecked(true);
						break;
					case EthernetManager.EVENT_CONFIGURATION_FAILED:
						mSwitch.setChecked(false);
						break;
					case EthernetManager.EVENT_DISCONNECTED:
						mSwitch.setChecked(false);
						break;
					default:
						break;
				}
        	} 
        }
    };

    public EthernetEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;

		mService = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		mEthManager = (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
        // The order matters! We really should not depend on this. :(
        mIntentFilter = new IntentFilter(EthernetManager.NETWORK_STATE_CHANGED_ACTION);
    }

    public void resume() {
        // Wi-Fi state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitch.setOnCheckedChangeListener(this);
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);

        final int EthernetState = mEthManager.getState();
        boolean isEnabled = EthernetState == EthernetManager.ETHERNET_STATE_ENABLED;
        boolean isDisabled = EthernetState == EthernetManager.ETHERNET_STATE_DISABLED;
        mSwitch.setChecked(isEnabled);
        mSwitch.setEnabled(isEnabled || isDisabled);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSwitch.setEnabled(isChecked);
        setEthEnabled(isChecked);
    }

	private void setEthEnabled(final boolean enable){

		new AsyncTask<Void, Void, Void>(){

			protected void onPreExecute(){
				//Disable button
				//mEthEnable.setSummary(R.string.eth_toggle_summary_opening);
				mSwitch.setEnabled(false);
			}

			@Override
			protected Void doInBackground(Void... unused){
				try{
					if ((mEthManager.isConfigured() != true) && (enable == true)){
						publishProgress();
					}else{
						mEthManager.setEnabled(enable);
					}
					Thread.sleep(500);
				}catch(Exception e){
				}
				return null;
			}

			/*protected void onProgressUpdate(Void... unused){
				Preference tmpPre = mEthDevices.getPreference(0);
				if( tmpPre instanceof EthPreference){
					EthPreference tmpEthPre = (EthPreference)tmpPre;
					mEthManager.updateDevInfo(tmpEthPre.getConfigure());
					mEthManager.setEnabled(enable);
				}
			}*/

			protected void onPostExecute(Void unused) {
				// Enable button
				mSwitch.setEnabled(true);
			}
		}.execute();
	}

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }

    private void handleStateChanged(@SuppressWarnings("unused") NetworkInfo.DetailedState state) {
        // After the refactoring from a CheckBoxPreference to a Switch, this method is useless since
        // there is nowhere to display a summary.
        // This code is kept in case a future change re-introduces an associated text.
        /*
        // WifiInfo is valid if and only if Wi-Fi is enabled.
        // Here we use the state of the switch as an optimization.
        if (state != null && mSwitch.isChecked()) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            if (info != null) {
                //setSummary(Summary.get(mContext, info.getSSID(), state));
            }
        }
        */
    }
}
