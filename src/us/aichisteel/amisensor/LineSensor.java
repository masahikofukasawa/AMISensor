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

import android.content.Context;

public class LineSensor extends AMISensor {
	private static final String TAG = LineSensor.class.getSimpleName();

	public class LineSensorData {
		private double[][] mag = new double[3][16];

		public LineSensorData() {
			super();
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 16; j++) {
					mag[i][j] = 0.0;
				}
			}
		}

		public LineSensorData(LineSensorData s) {
			super();
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 16; j++) {
					mag[i][j] = s.mag[i][j];
				}
			}
		}

		public double getMag(int ch, int axis) {
			if (ch < 1 || ch > 16)
				return 0;
			return mag[axis][ch - 1];
		}

		public void setMag(int ch, int axis, double val) {
			mag[axis][ch - 1] = val;
		}

		public int calcPow(int ch) {
			if (ch < 1 || ch > 16)
				return 0;
			return (int) Math.sqrt(mag[0][ch - 1] * mag[0][ch - 1]
					+ mag[1][ch - 1] * mag[1][ch - 1] + mag[2][ch - 1]
					* mag[2][ch - 1]);
		}
	}

	private LineSensorData mSensorData;
	private LineSensorData mOffsetData;
	private LineSensorData mLastValue;
	private StringBuilder mText = new StringBuilder();
	private int mChIndex = 0;
	private int mAxisIndex = 0;
	public final static int AXIS_ID_POWER = 0;
	public final static int AXIS_ID_X = 1;
	public final static int AXIS_ID_Y = 2;
	public final static int AXIS_ID_Z = 3;
	private int mAxisId = AXIS_ID_POWER;

	public LineSensor(Context c, AMISensorInterface listener) {
		super(1250000, "mes 0 200", "mes 1", c, listener);
		mSensorData = new LineSensorData();
		mOffsetData = new LineSensorData();
		mLastValue = new LineSensorData();
	}

	public void setSensorAxis(int id) {
		mAxisId = id;
	}

	public int getSensorAxis() {
		return mAxisId;
	}

	public double getData(int ch, int axis) {
		return mSensorData.getMag(ch, axis) - mOffsetData.getMag(ch, axis);
	}

	public double getPower(int ch) {
		LineSensorData pow = new LineSensorData();
		for (int axis = 0; axis < 3; axis++) {
			pow.setMag(ch, axis,
					mSensorData.getMag(ch, axis) - mOffsetData.getMag(ch, axis));
		}
		return pow.calcPow(ch);
	}

	@Override
	public void clearOffset() {
		for (int axis = 0; axis < 3; axis++) {
			for (int ch = 1; ch <= 16; ch++) {
				mOffsetData.setMag(ch, axis, 0);
			}
		}
	}

	@Override
	public void setOffset() {
		for (int axis = 0; axis < 3; axis++) {
			for (int ch = 1; ch <= 16; ch++) {
				mOffsetData.setMag(ch, axis, mSensorData.getMag(ch, axis));
			}
		}
	}

	@Override
	protected void initData() {
		mText.setLength(0);
	}

	@Override
	public void addData(byte[] rbuf, int len) {
		for (int i = 0; i < len; i++) {
			if (rbuf[i] >= '0' && rbuf[i] <= '9') {
				mText.append((char) rbuf[i]);
			} else if (rbuf[i] == '-') {
				mText.append((char) rbuf[i]);
			} else if ((rbuf[i] == ',')
					| (rbuf[i] == '\r' && mChIndex == 15 && mAxisIndex == 2)) {
				mLastValue.mag[mAxisIndex][mChIndex] = (double) Double
						.parseDouble(mText.toString());
				mText.setLength(0);

				mAxisIndex++;
				if (mAxisIndex > 2) {
					mAxisIndex = 0;
					mChIndex++;
					if (mChIndex > 15) {
						mChIndex = 0;
					}
				}
			} else if (rbuf[i] == '|') {
				mSensorData = new LineSensorData(mLastValue);
				mChIndex = 0;
				mAxisIndex = 0;
				mText.setLength(0);
			}
		}
	}
}
