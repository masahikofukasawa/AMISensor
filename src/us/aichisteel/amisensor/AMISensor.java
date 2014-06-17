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
//import org.achartengine.model.XYMultipleSeriesDataset;

public abstract class AMISensor {

	protected Physicaloid mSerial;
	protected int iBaudRate;
	protected StringBuilder mText = new StringBuilder();
	protected boolean lastDataIs0x0D = false;
	protected static final String CR = "\r";
	protected String stTransmit = CR;
	protected final static String BR = System.getProperty("line.separator");
	protected boolean mRunningMainLoop;
	protected String stStartCommand;
	protected String stStopCommand;
	protected final static String USB_PERMISSION = "us.aichisteel.amisensor.USB_PERMISSION";

	abstract public void initData();

	abstract public void addData(byte[] rbuf, int len);

//	abstract public void addToDataset(XYMultipleSeriesDataset dataset);

	protected AMISensorInterface sensorListener = null;

	public AMISensor(int baudrate, String start, String stop, Context c) {
		iBaudRate = baudrate;
		stStartCommand = start;
		stStopCommand = stop;
		mRunningMainLoop = false;
		mSerial = new Physicaloid(c);

		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		c.registerReceiver(mUsbReceiver, filter);
	}

	public void initialize(AMISensorInterface listener) {
		if (!mRunningMainLoop) {
			mainloop();
		}
		this.sensorListener = listener;
		openUsbSerial();
	}

	public void finalize(Context c) {
		closeUsbSerial();
		c.unregisterReceiver(mUsbReceiver);
		this.sensorListener = null;
		mRunningMainLoop = false;
	}

	public void startSensor() {
		openUsbSerial();
		if (mSerial.isOpened()) {
			String strWrite = changeEscapeSequence(stStartCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			initData();
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
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				if (sensorListener != null) {
					if (!isReady()) {
						openUsbSerial();
						if (isReady()) {
							sensorListener.attachedSensor();
						}
					}
				}
				if (!mRunningMainLoop) {
					mainloop();
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				closeUsbSerial();
				if (sensorListener != null) {
					sensorListener.detachedSensor();
				}
			} else if (USB_PERMISSION.equals(action)) {
				synchronized (this) {
					if (sensorListener != null) {
						if (!isReady()) {
							openUsbSerial();
							if (isReady()) {
								sensorListener.attachedSensor();
							}
						}
					}
				}
				if (!mRunningMainLoop) {
					mainloop();
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
					setSerialDataToTextView(rbuf, len);
					try {
						sensorListener.dataReady();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(200);
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
		}

		if (!mSerial.isOpened()) {
			if (!mSerial.open()) {
			} else {
				mSerial.setConfig(new UartConfig(iBaudRate, 8, 1, 0, false,
						false));
			}
		}
		if (!mRunningMainLoop) {
			return;
		}
	}

	protected void closeUsbSerial() {
		mSerial.close();
	}

	protected void setSerialDataToTextView(byte[] rbuf, int len) {
		for (int i = 0; i < len; i++) {
			// "\r":CR(0x0D) "\n":LF(0x0A)
			if (rbuf[i] == 0x0D) {
				mText.append(BR);
			} else if (rbuf[i] == 0x0A) {
				// if (iTarget != MENU_TARGET_NTSENSOR) {
				mText.append(BR);
				// }
			} else if ((rbuf[i] == 0x0D) && (rbuf[i + 1] == 0x0A)) {
				mText.append(BR);
				i++;
			} else if (rbuf[i] == 0x0D) {
				// case of rbuf[last] == 0x0D and rbuf[0] == 0x0A
				lastDataIs0x0D = true;
			} else if (lastDataIs0x0D && (rbuf[0] == 0x0A)) {
				mText.append(BR);
				lastDataIs0x0D = false;
			} else if (lastDataIs0x0D && (i != 0)) {
				// only disable flag
				lastDataIs0x0D = false;
				i--;
			} else {
				mText.append((char) rbuf[i]);
			}
		}
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
				// if in unicode, then we're reading unicode
				// values in somehow
				unicode.append(ch);
				if (unicode.length() == 4) {
					// unicode now contains the four hex digits
					// which represents our unicode character
					try {
						int value = Integer.parseInt(unicode.toString(), 16);
						strout.append((char) value);
						unicode.setLength(0);
						inUnicode = false;
						hadSlash = false;
					} catch (NumberFormatException nfe) {
						// throw new
						// NestableRuntimeException("Unable to parse unicode value: "
						// + unicode, nfe);
						throw new IOException("Unable to parse unicode value: "
								+ unicode, nfe);
					}
				}
				continue;
			}
			if (hadSlash) {
				// handle an escaped value
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
					// uh-oh, we're in unicode country....
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
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			strout.append('\\');
		}
		return new String(strout.toString());
	}
}
