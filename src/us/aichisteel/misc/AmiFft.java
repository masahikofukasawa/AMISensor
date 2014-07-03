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

package us.aichisteel.misc;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sqrt;
import static java.lang.Math.pow;
import static java.lang.Math.log10;

public class AmiFft {

	private List<Double> dFreq = new ArrayList<Double>();
	private List<Double> dLevel = new ArrayList<Double>();
	private int iNum;
	private double dOdr;
	private FFT4g mFft;
	private double[] mCopy;

	public AmiFft(int n, double odr) {
		int power = 0;
		while (pow(2, power) <= n) {
			this.iNum = (int) pow(2, power);
			power++;
		}

		dOdr = odr;
		mFft = new FFT4g(iNum);
		mCopy = new double[iNum];
		// System.out.println("Num=" + iNum);
	}

	public void AmiFftCalc(List<Double> a, boolean isLog) {
		dLevel.clear();
		dFreq.clear();

		// if(a.size()<=iNum) return;

		for (int i = 0; i < iNum; i++) {
			mCopy[i] = a.get(i).doubleValue();
		}
		mFft.rdft(1, mCopy);
		for (int i = 0; i < iNum; i += 2) {
			if (isLog) {
				dLevel.add(10 * log10(sqrt(pow(mCopy[i], 2)
						+ pow(mCopy[i + 1], 2))));
				if (i != 0) {
					dFreq.add(10 * log10((i / 2) * 1 / ((double) iNum * dOdr)) + 10);
				} else {
					dFreq.add(0.0);
				}
			} else {
				dLevel.add(sqrt(pow(mCopy[i], 2) + pow(mCopy[i + 1], 2)));
				dFreq.add((i / 2) * 1 / ((double) iNum * dOdr));
			}
		}
	}

	public int getNum() {
		return iNum / 2;
	}

	public List<Double> getFreq() {
		return dFreq;
	}

	public List<Double> getLevel() {
		return dLevel;
	}

	/*
	 * public double getLogScaleFreq() {
	 *
	 * if (freq == 0) { return 0; } else { return 10 * log10(freq) + 10; } }
	 *
	 * public double getLogScaleLevel(double level) { if (level == 0) { return
	 * 0; } else { return 10 * log10(level); } }
	 */

	public static void main(String args[]) {
		DecimalFormat form = new DecimalFormat("0.00");
		int num = Integer.parseInt(args[0]);
		boolean isLog = Boolean.parseBoolean(args[1]);

		List<Double> a = new ArrayList<Double>();
		AmiFft fft = new AmiFft(num, 0.004);
		try {
			File file = new File(".\\data.txt");
			BufferedReader br = new BufferedReader(new FileReader(file));
			int i = 0;

			String str = br.readLine();
			while (str != null) {
				a.add(Double.parseDouble(str));
				str = br.readLine();
				// System.out.println(i + " " + a[i]);
				i++;
			}

			br.close();

			fft.AmiFftCalc(a, isLog);
			for (i = 0; i < fft.getNum(); i++) {
				System.out.println(i + " " + form.format(fft.getFreq().get(i))
						+ " " + form.format(fft.getLevel().get(i)) + " ");
			}

		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
