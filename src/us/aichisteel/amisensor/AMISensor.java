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

import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

public abstract class AMISensor {

	protected Context mContext;
	protected AMISensorInterface sensorListener = null;
	protected Physicaloid mSerial;
	protected int iBaudRate;
	protected static final String CR = "\r";
	protected String stTransmit = CR;
	protected boolean mRunningMainLoop;
	protected String stStartCommand;
	protected String stStopCommand;
	protected boolean isStartSensor = false;

	public final static String USB_PERMISSION = "us.aichisteel.amisensor.USB_PERMISSION";

	abstract protected void initData();

	abstract public void addData(byte[] rbuf, int len);

	abstract public void setOffset();

	abstract public void clearOffset();

	public AMISensor(int baudrate, String start, String stop, Context c,
			AMISensorInterface listener) {
		iBaudRate = baudrate;
		stStartCommand = start;
		stStopCommand = stop;
		mRunningMainLoop = false;
		mSerial = new Physicaloid(c);
		mContext = c;

		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		mContext.registerReceiver(mUsbReceiver, filter);
		this.sensorListener = listener;
	}
	
	public void setStartCommand(String start) {
		stStartCommand = start;
	}
	
	public void initializeSensor() {
		openUsbSerial();
	}

	public void finalizeSensor() {
		closeUsbSerial();
		mContext.unregisterReceiver(mUsbReceiver);
		this.sensorListener = null;
		mRunningMainLoop = false;
	}

	public void startSensor() {
		openUsbSerial();
		if (mSerial.isOpened()) {
			String strWrite = changeEscapeSequence(stStartCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			initData();
			if (!mRunningMainLoop) {
				mainloop();
			}
		}
	}

	public void stopSensor() {
		if (mSerial.isOpened()) {
			String strWrite = changeEscapeSequence(stStopCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			initData();
		}
		closeUsbSerial();
	}

	public boolean isReady() {
		return mSerial.isOpened();
	}
	
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ( UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) || USB_PERMISSION.equals(action) ) {
				if (sensorListener != null) {
					sensorListener.attachedSensor();
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				closeUsbSerial();
				if (sensorListener != null) {
					sensorListener.detachedSensor();
				}
			}
		}
	};

	protected void mainloop() {
		mRunningMainLoop = true;
		new Thread(mLoop).start();

	}

	protected Runnable mLoop = new Runnable() {
		@Override
		public void run() {
			int len;
			byte[] rbuf = new byte[4096];
			for (;;) {

				len = read(rbuf);
				if (len > 0) {
					addData(rbuf, len);
					try {
						sensorListener.dataReady();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!mRunningMainLoop) {
					return;
				}
			}
		}
	};

	protected void openUsbSerial() {
		if (mSerial == null) {
			return;
		}

		if (!mSerial.isOpened()) {
			if (!mSerial.open()) {
			} else {
				mSerial.setConfig(new UartConfig(iBaudRate,
						UartConfig.DATA_BITS8, UartConfig.STOP_BITS1,
						UartConfig.PARITY_NONE, false, false));
				
				
			}
		}
	}

	protected void closeUsbSerial() {
		mSerial.close();
	}

	protected String changeEscapeSequence(String in) {
		String out = new String();
		try {
			out = unescapeJava(in);
		} catch (IOException e) {
			return "";
		}
		out = out + stTransmit;
		return out;
	}

	protected int read(byte[] rbuf) {
		return mSerial.read(rbuf);
	}
	
	public String sendCommand(String s, int sleep_time) {
		String retStr = "";
		if (mSerial.isOpened()) {
			read(new byte[256]);	// clear buffer
			String strWrite = changeEscapeSequence(s);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			try {
				Thread.sleep(sleep_time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			byte[] rbuf = new byte[256];
			int len = read(rbuf);
			if (len > 0) {
				retStr = new String(rbuf,0,len).replaceAll("\n", "").replaceAll("\r", "");
			}
		}
		return retStr;
	}

	protected String unescapeJava(String str) throws IOException {
		if (str == null) {
			return "";
		}
		int sz = str.length();
		StringBuffer unicode = new StringBuffer(4);

		StringBuilder strout = new StringBuilder();
		boolean hadSlash = false;
		boolean inUnicode = false;
		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				unicode.append(ch);
				if (unicode.length() == 4) {
					try {
						int value = Integer.parseInt(unicode.toString(), 16);
						strout.append((char) value);
						unicode.setLength(0);
						inUnicode = false;
						hadSlash = false;
					} catch (NumberFormatException nfe) {
						throw new IOException("Unable to parse unicode value: "
								+ unicode, nfe);
					}
				}
				continue;
			}
			if (hadSlash) {
				hadSlash = false;
				switch (ch) {
				case '\\':
					strout.append('\\');
					break;
				case '\'':
					strout.append('\'');
					break;
				case '\"':
					strout.append('"');
					break;
				case 'r':
					strout.append('\r');
					break;
				case 'f':
					strout.append('\f');
					break;
				case 't':
					strout.append('\t');
					break;
				case 'n':
					strout.append('\n');
					break;
				case 'b':
					strout.append('\b');
					break;
				case 'u': {
					inUnicode = true;
					break;
				}
				default:
					strout.append(ch);
					break;
				}
				continue;
			} else if (ch == '\\') {
				hadSlash = true;
				continue;
			}
			strout.append(ch);
		}
		if (hadSlash) {
			strout.append('\\');
		}
		return new String(strout.toString());
	}
}
