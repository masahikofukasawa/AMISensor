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

public class Ami30xEvk extends AMISensor {
	private final static int DEFAULT_ODR = 100;	// [Hz]
	private int mOdr = DEFAULT_ODR;
	private static final double DEFAULT_OFFSET = 0;
	private int mMaxSize = 1000; //
	private double[] mOffset = {DEFAULT_OFFSET,DEFAULT_OFFSET,DEFAULT_OFFSET};
	private List<Double[]> mSensorData;
	private StringBuilder mText = new StringBuilder();
	private double[] mLatestVoltage = mOffset.clone();
	private int mAxisCounter = 0; // 0:X 1:Y 2:Z
	private Double[] m3AxisData = new Double[3];
	
	public class AmiInterference {
		public double xy;
		public double xz;
		public double yx;
		public double yz;
		public double zx;
		public double zy;
	}
	public class AmiParam {
		public double[] fine_output;
		public double[] offset;
		public double[] sens;
		public AmiInterference interference;
		public AmiParam(){
			fine_output = new double[3];
			offset		= new double[3];
			sens		= new double[3];
			interference= new AmiInterference();
		}
	}
	
	private AmiParam mParam = new AmiParam();
	
	public Ami30xEvk(Context c,AMISensorInterface listener) {
		super(115200, "mes 0 " + String.valueOf(1000/DEFAULT_ODR), "mes 1", c,listener);
		this.mSensorData  = new ArrayList<Double[]>();
	}
	
	public int getOdr(){
		return mOdr;
	}
	public void setOdr(int odr){
		mOdr = odr;
		setStartCommand("mes 0 " + String.valueOf(1000/mOdr));
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
	
	private boolean getParameter() {
		boolean ret = false;
		sendCommand("act 0",100);	// Set force active for AMI306
		// read parameter from EVK
		String[] str = sendCommand("cag", 200).replace("cag|OK:","").split(",");
		try{
			mParam.fine_output[0] = Double.parseDouble(str[0]);
			mParam.fine_output[1] = Double.parseDouble(str[1]);
			mParam.fine_output[2] = Double.parseDouble(str[2]);
			mParam.sens[0] = Double.parseDouble(str[3]);
			mParam.sens[1] = Double.parseDouble(str[4]);
			mParam.sens[2] = Double.parseDouble(str[5]);
			mParam.interference.xy = Double.parseDouble(str[6]);
			mParam.interference.xz = Double.parseDouble(str[7]);
			mParam.interference.yx = Double.parseDouble(str[8]);
			mParam.interference.yz = Double.parseDouble(str[9]);
			mParam.interference.zx = Double.parseDouble(str[10]);
			mParam.interference.zy = Double.parseDouble(str[11]);
			Log.i("Ami30xEvk: ",
					"FineOutput=(" +
					String.valueOf(mParam.fine_output[0]) + "," + 
					String.valueOf(mParam.fine_output[1]) + "," + 
					String.valueOf(mParam.fine_output[2]) + ")" +
					" Sens=(" +
					String.valueOf(mParam.sens[0]) + "," + 
					String.valueOf(mParam.sens[1]) + "," + 
					String.valueOf(mParam.sens[2]) + ")"
					);
		} catch (Exception e) {
			Log.i("Ami30xEvk: ERROR ",
					"FineOutput=(" +
					String.valueOf(mParam.fine_output[0]) + "," + 
					String.valueOf(mParam.fine_output[1]) + "," + 
					String.valueOf(mParam.fine_output[2]) + ")" +
					" Sens=(" +
					String.valueOf(mParam.sens[0]) + "," + 
					String.valueOf(mParam.sens[1]) + "," + 
					String.valueOf(mParam.sens[2]) + ")"
					);
			
			ret = true;
		}
		
		// read one data for offset value
		str = sendCommand("mea", 100).replace("mea|","").split(",");
		try{
			mOffset[0] = Double.parseDouble(str[0])-2048;
			mOffset[1] = Double.parseDouble(str[1])-2048;
			mOffset[2] = Double.parseDouble(str[2])-2048;
			Log.i("Ami30xEvk: ",
					"Offset= (" +
					String.valueOf(mOffset[0]) + "," + 
					String.valueOf(mOffset[1]) + "," + 
					String.valueOf(mOffset[2]) + ")"
					);			
		} catch (Exception e) {
			Log.i("Ami30xEvk: ",
					"ERROR " +
					str[0] + " " + 
					str[1] + " " + 
					str[2] + ""
					);						
			ret = true;
		}
		return ret;
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
			if (rbuf[i] == '\r') {
				mAxisCounter=0;
				mText.setLength(0);					
				while (mSensorData.size() > mMaxSize) {
					mSensorData.remove(0);
				}					
				
			} else if (rbuf[i] == ',') {
				if(mAxisCounter<3){
					try {
						mLatestVoltage[mAxisCounter] = Double.parseDouble(mText.toString())-2048.0;
						m3AxisData[mAxisCounter] = 1000.0 * (mLatestVoltage[mAxisCounter]-mOffset[mAxisCounter])/mParam.sens[mAxisCounter];
					} catch (Exception e) {
						Log.e("Ami30xEvk: ", "Wrong Input Stirng:" + mText);
					}
					if(mAxisCounter==2){
						mSensorData.add(m3AxisData.clone());	// クローンを渡す。
					}
					mAxisCounter++;
				}
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
		if (mSerial.isOpened()) {
			if (!mRunningMainLoop) {
				if(getParameter()){
					// Error:
					
				}
			}
			initData();
			String strWrite = changeEscapeSequence(stStartCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			Log.i("Ami30xEvk: ", stStartCommand + " Called.");
			if (!mRunningMainLoop) {
				mainloop();
			}
		}
	}
}
