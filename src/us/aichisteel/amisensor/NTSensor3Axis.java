/*
 * Copyright (C) 2014 Aichi Micro Intelligent Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * Distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package us.aichisteel.amisensor;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class NTSensor3Axis extends AMISensor {
	private static final String TAG = NTSensor3Axis.class.getSimpleName();

	private int mOdr = 125;
	private static final double DEFAULT_OFFSET = 2.6;
	private int mMaxSize = 1000; //
	private double[] mSensitivity = {1,1,1};
	private double[] mOffset = {DEFAULT_OFFSET,DEFAULT_OFFSET,DEFAULT_OFFSET};
	private List<Double[]> mSensorData;
	private StringBuilder mText = new StringBuilder();
	private double[] mLatestVoltage = mOffset.clone();
	private int mAxisCounter = 0; // 0:X 1:Y 2:Z
	private Double[] m3AxisData = new Double[3];
	
	public NTSensor3Axis(Context c,AMISensorInterface listener) {
		super(115200, "a", "s", c,listener);
		this.mSensorData  = new ArrayList<Double[]>();
	}
	
	public int getOdr(){
		return mOdr;
	}
	public void setOdr(int odr){
		mOdr = odr;
	}
	
	public List<Double[]> getData() {
		return mSensorData;
	}

	public List<Double> getData(int axis) {
		List<Double> retData = new ArrayList<Double>();
		for (Double[] data : mSensorData) {
			retData.add(data.clone()[axis]);	// クローンを渡す
		}
		return retData;
	}

	private double calcPower(double x, double y, double z){
		double ret;
		ret = Math.sqrt(Math.pow(x,2)+Math.pow(y,2) + Math.pow(z, 2));
		return ret;
	}
	
	public List<Double> getPowerData() {
		List<Double> retData = new ArrayList<Double>();
		for (Double[] data : mSensorData) {
			retData.add(calcPower(data.clone()[0],data.clone()[1],data.clone()[2]));	// クローンを渡す
		}
		return retData;
	}

	public double[] getLatestVoltage(){
		return mLatestVoltage;
	}

	public int getMaxSize(){
		return mMaxSize;
	}
	
	public void setMaxTime(double sec) {
		if (sec > 0) {
			mMaxSize = (int) (mOdr * sec);
		}
	}

	public void setSensitivity(double sensx,double sensy,double sensz){
		mSensitivity[0] = sensx;
		mSensitivity[1] = sensy;
		mSensitivity[2] = sensz;
	}
	
	@Override
	public void setOffset() {
		mOffset = mLatestVoltage.clone();
	}
	@Override
	public void clearOffset(){
		mOffset[0] = DEFAULT_OFFSET;
		mOffset[1] = DEFAULT_OFFSET;
		mOffset[2] = DEFAULT_OFFSET;
	}

	@Override
	protected void initData() {
		mSensorData.clear();
		mText.setLength(0);
		mAxisCounter = 0;
	}

	@Override
	public void addData(byte[] rbuf, int len) {
		for (int i = 0; i < len; i++) {
			if (rbuf[i] == '\n') {
				try {
					mLatestVoltage[mAxisCounter] = Double.parseDouble(mText.toString());
					m3AxisData[mAxisCounter] = 1000 * (mLatestVoltage[mAxisCounter]-mOffset[mAxisCounter])/mSensitivity[mAxisCounter];
					mSensorData.add(m3AxisData.clone());	// クローンを渡す。
				} catch (Exception e) {
					Log.e("AMISENSOR: ", "Wrong Input Stirng1:" + mText);
				}
				mAxisCounter=0;
				mText.setLength(0);
				while (mSensorData.size() > mMaxSize) {
					mSensorData.remove(0);
				}
			} else if (rbuf[i] == ',') {
				try {
					mLatestVoltage[mAxisCounter] = Double.parseDouble(mText.toString());
					m3AxisData[mAxisCounter] = 1000 * (mLatestVoltage[mAxisCounter]-mOffset[mAxisCounter])/mSensitivity[mAxisCounter];
				} catch (Exception e) {
					Log.e("AMISENSOR: ", "Wrong Input Stirng2:" + mText);
				}
				mAxisCounter++;
				mText.setLength(0);
			} else if (rbuf[i] >= '0' && rbuf[i] <= '9') {
				mText.append((char) rbuf[i]);
			} else if (rbuf[i] == '.') {
				mText.append((char) rbuf[i]);
			} else if (rbuf[i] == '-') {
				mText.append((char) rbuf[i]);
			}
		}
	}
	
	@Override
	public void startSensor() {
		openUsbSerial();
		String ret = "";
		if (mSerial.isOpened()) {
			if (!mRunningMainLoop) {
				try {
					ret = sendCommand("get odr", 100);
					setOdr( Integer.parseInt(ret) );
					Log.e("AMISENSOR: ", "Succeeded Set ODR---" + String.valueOf(getOdr()));
				} catch (Exception e) {
					Log.e("AMISENSOR: ", "Failed getting ODR---" + ret + "_length=" + ret.length());
				}
			}
			String strWrite = changeEscapeSequence(stStartCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			initData();
			if (!mRunningMainLoop) {
				mainloop();
			}
		}
	}
}
