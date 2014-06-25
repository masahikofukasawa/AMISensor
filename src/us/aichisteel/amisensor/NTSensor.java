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

public class NTSensor extends AMISensor {
	public static final int NTSENSOR_SPS = 250;
	private static final double DEFAULT_OFFSET = 2.3;
	private int mMaxSize = 1000; //
	private double mSensitivity = 4; // 4[V/uT]
	private double mOffset = DEFAULT_OFFSET;
	private List<Double> mSensorData;
	private StringBuilder mText = new StringBuilder();
	private double mLatestVoltage = mOffset;
	private int mDataCounter = 0;

	public NTSensor(Context c) {
		super(115200, "a", "s", c);
		this.mSensorData = new ArrayList<Double>();
	}

	public List<Double> getData() {
		return mSensorData;
	}

	public double getLatestVoltage(){
		return mLatestVoltage;
	}

	public void setMaxTime(double sec) {
		if (sec > 0) {
			mMaxSize = (int) (NTSENSOR_SPS * sec);
		}
	}

	@Override
	public void setOffset() {
		mOffset = mLatestVoltage;
	}
	@Override
	public void clearOffset(){
		mOffset = DEFAULT_OFFSET;
	}

	@Override
	public void initData() {
		mSensorData.clear();
		mText.setLength(0);
		mDataCounter = 0;
	}

	@Override
	public void addData(byte[] rbuf, int len) {
		for (int i = 0; i < len; i++) {
			if (rbuf[i] == 'v') {
				try {
					if (mText.length() != 8) {
						Log.e("AMISENSOR: ", "Wrong Input Stirng1:" + mText
								+ " Count=" + mDataCounter);
					} else {
						mLatestVoltage = Double.parseDouble(mText.toString());
						mSensorData.add(1000 * (mLatestVoltage - mOffset)
								/ mSensitivity);
						mDataCounter++;
					}
				} catch (Exception e) {
					Log.e("AMISENSOR: ", "Wrong Input Stirng2:" + mText);
				}
				mText.setLength(0);

				while (mSensorData.size() > mMaxSize) {
					mSensorData.remove(0);
				}
			} else if (rbuf[i] >= '0' && rbuf[i] <= '9') {
				mText.append((char) rbuf[i]);
			} else if (rbuf[i] == '.') {
				mText.append((char) rbuf[i]);
			}
		}
	}
}
