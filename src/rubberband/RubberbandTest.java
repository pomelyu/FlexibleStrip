package rubberband;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import libsvm.svm;
import libsvm.svm_model;
import math.geom2d.AffineTransform2D;

import processing.core.PApplet;
import processing.core.PImage;

import rubberband.math.LeastSquareFit;
import rubberband.math.Spline;

public class RubberbandTest extends PApplet {

	private static final long serialVersionUID = 1L;
	private static final int HEIGHT = Params.WINDOW_HEIGHT;
	private static final int WIDTH = Params.WINDOW_WIDTH;

	ArrayList<Float> spline_x = new ArrayList<Float>();
	ArrayList<Float> spline_y = new ArrayList<Float>();

	StrainGauge[] ss = new StrainGauge[Params.NUM_STRAIN_SENSORS];

	LeastSquareFit LSF_P;
	LeastSquareFit LSF_N;
	LeastSquareFit invLSF_P;
	LeastSquareFit invLSF_N;
	ArduinoIO arduinoDevices;

	private double[] rawStrainVal, rawRadiusVal;

	private float[] resample_x, resample_y, resample_c, resample_r;
	private float[] resample_cs, resample_c2, resample_dx, resample_dy;

	// offset value of each strain gauge
	private double[] flatValue = new double[Params.NUM_STRAIN_SENSORS];

	// touch state of every touch sensor
	private boolean[] touchState = new boolean[Params.NUM_TOUCH_SENSORS];
	private int[] touchColor = new int[Params.NUM_TOUCH_SENSORS];

	// for debug
	private int sensorInterest = 0;
	private float[] fixRawData = new float[Params.NUM_STRAIN_SENSORS];

	// for record data
	private String curState = "ReadyToRecord";
	private int recordCount = 0 ;
	private FileWriter outFile = null;
	private BufferedWriter writer = null;
	private BufferedReader reader = null;

	// for display control
	private int dx = 0;
	private int dy = 0;
	private int dz = 0;
	private float yaw = 0;
	private float pitch = 0;
	private float roll = 0;

	// for shape recognition
	private int curShape = -1;
	private PImage curShapeImg = null;

	private svm_predict svmp = null;
	private svm_model model = null;
	
	private boolean toLoadImg = true;
	private int targetShape = 4;
	private PImage baseLine = null;
	
	// load saved raw data file
	private String rawDataFile = null;
	
	// ////////////////////////////////////////////////////////////////////////////////
	// Processing event
	// ////////////////////////////////////////////////////////////////////////////////

	public void setup() {

		size(WIDTH, HEIGHT, P3D);
		background(255);
		smooth();
		fill(0);

		for (int i = 0; i < ss.length; i++)
			ss[i] = new StrainGauge(this, Params.GAUGE_LENGTH);

		if (Params.DO_ARDUINO)
			init_Arduino_Sensors();
		init_StrainGague_Mapping();

		for (int i = 0; i < flatValue.length; i++)
			flatValue[i] = 0;

		// To calibrate each strain
		calibrationStrainGauge();

		svmp = new svm_predict(this);

		try 
		{
			model = svm.svm_load_model("train.libsvm.model");
		} catch (IOException e) { e.printStackTrace(); }
		
		if (Params.DATA_FROM_FILE){
			showFileChooser();
			loadDataFromFile();
		}
	  }

	public void update() {
		if (arduinoDevices != null)
			arduinoDevices.update();
	}

	public void draw() {
		update();

		background(200);

		ellipseMode(CENTER);

		//draw response curve
//		drawTransFunction();
		drawInform();

		fill(0);
		textSize(12);
		text(curState, 10, HEIGHT-10);
		textSize(12);
		
		drawAllData(3*WIDTH/4, 0, WIDTH/4, HEIGHT/4);
//		drawCurrentShape(WIDTH-200, HEIGHT-200, 190, 190);
		drawTargetImage(200, 200, 500);

		// ////////////////////////////////////////////////////////////////////////////////
		// Write Data to File
		// ////////////////////////////////////////////////////////////////////////////////
		if (outFile != null){
			if (recordCount < 100){
				writeRawDataToFile();
				recordCount++;
			}
			else{
				recordCount = 0;
				curState = "ReadyToRecord";
				try {
					writer.flush();
					outFile.close();
					writer.close();
				} catch (IOException e) { e.printStackTrace(); }
				outFile = null;
				writer = null;
			}
		}
		// Translation
		translate(WIDTH/4, HEIGHT*3/4);
		translate(0, 0, -Params.ONE_STEP*3);
		translate(dx, dy, dz);

//		rotateY(-radians(yaw));
//		rotateX(-radians(pitch));
//		rotateZ(radians(roll));
//
//		rotateY(radians(-90));
		
		// ////////////////////////////////////////////////////////////////////////////////
		// Simply Draw using openGL functions
		// ////////////////////////////////////////////////////////////////////////////////
		pushMatrix();



		for (int i = 0; i < ss.length; i++) {
			ss[i].draw();

			Point2D.Float nextStart = ss[i].nextStart();
			float nextAngle = ss[i].nextAngle();
			translate(nextStart.x, nextStart.y);
			rotate(nextAngle);
		}

		popMatrix();

		// ////////////////////////////////////////////////////////////////////////////////
		// As previous step, but here, manually compute sample locations w/o
		// openGL functions
		// ////////////////////////////////////////////////////////////////////////////////
		spline_x.clear();
		spline_y.clear();

		AffineTransform2D affine = AffineTransform2D.createTranslation(0, 0);
		//affine = affine.concatenate(AffineTransform2D.createTranslation(200, 200));

		for (int i = 0; i < ss.length; i++) {

			math.geom2d.Point2D vec;
			Point2D.Float[] pts = ss[i].getPoints();
			for (int j = 0; j < pts.length; j++) {

				if (j == 1) {
					vec = new math.geom2d.Point2D(pts[j].x, pts[j].y);
					vec = vec.transform(affine);

					spline_x.add(new Float(vec.x()));
					spline_y.add(new Float(vec.y()));
				}
			}

			Point2D.Float nextStart = ss[i].nextStart();
			float nextAngle = ss[i].nextAngle();

			if (i == ss.length - 1) {
				vec = new math.geom2d.Point2D(nextStart.x, nextStart.y);
				vec = vec.transform(affine);
				spline_x.add(new Float(vec.x()));
				spline_y.add(new Float(vec.y()));
			}

			affine = affine.concatenate(AffineTransform2D.createTranslation(
					nextStart.x, nextStart.y));
			affine = affine.concatenate(AffineTransform2D
					.createRotation(nextAngle));

		}

		// ////////////////////////////////////////////////////////////////////////////////
		// re-Sampling and computing Curvature on the samplings.
		// ////////////////////////////////////////////////////////////////////////////////
		noFill();

		int s = spline_x.size();
		float[] mx = new float[s];
		float[] my = new float[s];

		for (int i = 0; i < s; i++) {
			mx[i] = ((Float) spline_x.get(i)).floatValue();
			my[i] = ((Float) spline_y.get(i)).floatValue();
		}

		Spline xs, ys;
		xs = new Spline(mx);
		ys = new Spline(my);

		int loose_Sample = Params.NUM_NEOPIXELS; // 20
		int dense_Sample = loose_Sample * 5;

		if (resample_x == null) {
			resample_x = new float[loose_Sample];
			resample_y = new float[loose_Sample];
			resample_c = new float[loose_Sample]; // Curvatures
			resample_r = new float[loose_Sample]; // Radius

			resample_cs = new float[loose_Sample]; // Signed Curvatures
			resample_c2 = new float[loose_Sample]; // Re-map to [0-255]

//			resample_dx = new float[loose_Sample]; // store first
//			// derivation x
//			resample_dy = new float[loose_Sample]; // store first
//			// derivation y
		}
		float loose_step = (float) (mx.length - 1) / (loose_Sample - 1);
		float dense_step = (float) (mx.length - 1) / (dense_Sample - 1);

		float dense_x1, dense_x2;
		float dense_y1, dense_y2;
		float dx, dx2, dy, dy2;
		float ddx, ddy;
		for (int t = 0; t < loose_Sample; t++) {

			resample_x[t] = xs.calc(t * loose_step);
			resample_y[t] = ys.calc(t * loose_step);

			dense_x1 = xs.calc(t * loose_step + dense_step);
			dense_x2 = xs.calc(t * loose_step + dense_step * 2);
			dense_y1 = ys.calc(t * loose_step + dense_step);
			dense_y2 = ys.calc(t * loose_step + dense_step * 2);

			dx = dense_x1 - resample_x[t];
			dx2 = dense_x2 - dense_x1;
			dy = dense_y1 - resample_y[t];
			dy2 = dense_y2 - dense_y1;

			ddx = dx2 - dx;
			ddy = dy2 - dy;

			// resample_dx[t] = dx;
			// resample_dy[t] = dy;

			// compute signed curvatures
			resample_cs[t] = (float) ((dx * ddy - dy * ddx) / Math.pow(dx * dx
					+ dy * dy, 1.5));
		}

		// store sign of the curvatures
		for (int i = 0; i < resample_c.length; i++) {
			resample_c[i] = Math.abs(resample_cs[i]);
			resample_r[i] = 1 / resample_c[i];
			if (resample_cs[i] > 0)
				resample_cs[i] = 1;
			else
				resample_cs[i] = -1;
		}

		// re-map curvature to 0-255
		float min = Float.MAX_VALUE;
		float max = Float.MIN_NORMAL;
		for (int i = 0; i < resample_c.length; i++) {
			if (resample_c[i] > max)
				max = resample_c[i];
			if (resample_c[i] < min)
				min = resample_c[i];
		}
		for (int i = 0; i < resample_c.length; i++)
			resample_c2[i] = 255 * (resample_c[i] - min) / (max - min);

		// ////////////////////////////////////////////////////////////////////////////////
		// draw on-edge circle to justify the computing
		// ////////////////////////////////////////////////////////////////////////////////

		// draw curvatures
		// for (int i = 0; i < loose_x.length; i++) {
		// stroke(loose_c2[i], 0, 0); Vector2D vec = new Vector2D(loose_dx[i],
		// loose_dy[i]); vec = vec.normalize(); vec = vec.rotate(loose_cs[i] *
		// Math.PI / 2); vec = vec.times(loose_r[i]);
		//
		// // stroke(0, 255, 0); // Draw circle on samples //
		// ellipse(loose_x[i], loose_y[i], loose_r[i] * 2, loose_r[i] * 2);
		//
		// // Draw circle on edges ellipse(loose_x[i] + (float) vec.x(),
		// loose_y[i] + (float) vec.y(), loose_r[i] * 2, loose_r[i] * 2);
		// }

		// ////////////////////////////////////////////////////////////////////////////////
		// draw Spline
		// ////////////////////////////////////////////////////////////////////////////////
		drawSpline(resample_x, resample_y);

		// ////////////////////////////////////////////////////////////////////////////////
		// send to NeoPixels
		// ////////////////////////////////////////////////////////////////////////////////
		if (Params.DO_ARDUINO) {

			int bufferSize = Params.NUM_NEOPIXELS * 3 + 2;
			ByteBuffer bbuf = ByteBuffer.allocate(bufferSize);
			// set identifier
			bbuf.put((byte) 0xFF);
			bbuf.put((byte) 0xFE);

			for (int i = 0; i < Params.NUM_NEOPIXELS; i++) {
				if (resample_cs[i] > 0) {
					bbuf.put((byte) 0);
					bbuf.put((byte) resample_c2[i]);
					bbuf.put((byte) 0);
				} else {
					bbuf.put((byte) 0);
					bbuf.put((byte) 0);
					bbuf.put((byte) resample_c2[i]);
				}
			}

//			arduinoDevices.setNeoPixels(bbuf.array());
		}
	}

	public void mousePressed() {
	}

	public void keyPressed() {
		switch(key){
		case ' ':
			for (int i = 0; i < ss.length; i++)
				flatValue[i] = ss[i].getCurrentValue();
			break;
		case '9':
			sensorInterest--;
			if (sensorInterest < 0)
				sensorInterest = Params.NUM_STRAIN_SENSORS-1;
			break;
		case '0':
			sensorInterest++;
			if (sensorInterest >= Params.NUM_STRAIN_SENSORS)
				sensorInterest = 0;
		case 'r':
			curState = "Recording";
			recordCount = 0;
			String timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(Calendar.getInstance().getTime());
			try {
				outFile = new FileWriter("Record_" + timeStamp + ".txt");
				writer = new BufferedWriter(outFile);
			} catch (IOException e){
				e.printStackTrace();
			}
			writeFlatValueToFile();
			break;
		case 'z':
			for (int i=0; i < Params.NUM_STRAIN_SENSORS; i++)
				fixRawData[i] = (float) ss[i].getCurrentValue();
			break;
		case 'j':
			dx = dx - Params.ONE_STEP;
			break;
		case 'l':
			dx = dx + Params.ONE_STEP;
			break;
		case 'i':
			dy = dy - Params.ONE_STEP;
			break;
		case 'k':
			dy = dy + Params.ONE_STEP;
			break;
		case 'n':
			dz = dz - Params.ONE_STEP;
			break;
		case 'm':
			dz = dz + Params.ONE_STEP;
			break;
		case 'd':
			targetShape++;
			toLoadImg = true;
			break;
		case 'a':
			targetShape--;
			toLoadImg = true;
			break;
		case ',':
			flatValue[sensorInterest]--;
			break;
		case '.':
			flatValue[sensorInterest]++;
			break;
		}

	}

	// ////////////////////////////////////////////////////////////////////////////////
	// Callback function
	// ////////////////////////////////////////////////////////////////////////////////

	public void strainGaugeEvent(double[] rVal) {
		// System.out.println(rVal[3]);

		if (rawStrainVal == null)
			rawStrainVal = new double[rVal.length];
		if (rawRadiusVal == null)
			rawRadiusVal = new double[rVal.length];

		if (rawStrainVal.length != rVal.length)
			rawStrainVal = new double[rVal.length];

//		for (int i = 0; i < rVal.length; i++)
//			rawStrainVal[i] = rVal[i];

		if (Params.DATA_FROM_FILE){
			for	(int i = 0; i < rVal.length; i++){
				ss[i].update(rawStrainVal[i]);
				rawRadiusVal[i] = getCurveRadius(i, ss[i].getCurrentValue());
				ss[i].setCurvatureRadius((float) rawRadiusVal[i]);
			}
		}
		else{
			for	(int i = 0; i < rVal.length; i++){
				ss[i].update(rVal[i]);
				rawStrainVal[i] = ss[i].getCurrentValue();
				rawRadiusVal[i] = getCurveRadius(i, ss[i].getCurrentValue());
				ss[i].setCurvatureRadius((float) rawRadiusVal[i]);
			}
		}
		// FIXME add rational radius on gap region
		
//		// set by harmonic mean
//		float CHEAT = 5.0f;
//		for (int i = 0; i < rVal.length-1; i++){
//			float r = (float) ((1.0f/rawRadiusVal[i] + 1.0f/rawRadiusVal[i+1])/2);
//			ss[i].setNextRadius(CHEAT * 1.0f/r);
//		}
//		ss[rVal.length-1].setNextRadius(CHEAT * (float)rawRadiusVal[rVal.length-1]);
		
		// set by average in response curve
		float magicNumber = Params.STRIP.gap_radius_fix;
		for (int i = 0; i < rVal.length-1; i++){
			double v1 = invLSF_Evaluate(1.0f/rawRadiusVal[i]);
			double v2 = invLSF_Evaluate(1.0f/rawRadiusVal[i+1]);
			ss[i].setNextRadius( (float)(magicNumber * getCurveRadius(-1, (v1+v2)/2)));
		}
		ss[rVal.length-1].setNextRadius(magicNumber * (float)rawRadiusVal[rVal.length-1]);
	}

	public void touchSensorEvent(double[] rVal){
		// TODO
		for (int i = 0; i < Params.NUM_TOUCH_SENSORS; i++){
			touchColor[i] = touchState[i] ? Params.COLOR_TOUCH : Params.COLOR_UNTOUCH;
		}
	}

	public void motionEvent(float yaw, float pitch, float roll){
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
//		System.out.println(yaw+" "+pitch+" "+roll);				
	}

	
	// ////////////////////////////////////////////////////////////////////////////////
	// Initial device and calibration
	// ////////////////////////////////////////////////////////////////////////////////
	
	public void init_Arduino_Sensors() {
		arduinoDevices = new ArduinoIO(this, null,
				Params.NUM_STRAIN_SENSORS, Params.NUM_NEOPIXELS, Params.NUM_TOUCH_SENSORS);
	}

	public void init_StrainGague_Mapping() {
		int order = 2;
		double zero = 0;
		// XXX Add test data if possible
		// for LSF_Positive	
		double[] input = new double[] { 673.290, 684.805, 710.265, 727.550, 
				742.515, 814.545, 844.690 };
		zero = input[0];
		for (int i = 0; i < input.length; i++)
			input[i] = input[i] - zero;
		double[] response = new double[] { 0, 1.0/56.32, 1.0/27.66, 1.0/18.11, 1.0/13.33, 
				1.0/10.46, 1.0/8.55};
		for	(int i = 0; i < response.length; i++)
			response[i] = response[i] / Params.DISPLAY_RATIO;
		LSF_P = new LeastSquareFit(input, response, order);
		invLSF_P = new LeastSquareFit(response, input, order);

		// for LSF_Negative
		input = new double[] { 705.550, 680.090, 642.130, 616.175, 
				586.685, 507.360, 475.840 };
		zero = input[0];
		for (int i = 0; i < input.length; i++)
			input[i] = input[i] - zero;
		response = new double[] { 0, -1.0/56.32, -1.0/27.66, -1.0/18.11, -1.0/13.33, 
				-1.0/10.46, -1.0/8.55 };
		for	(int i = 0; i < response.length; i++)
			response[i] = response[i] / Params.DISPLAY_RATIO;
		LSF_N = new LeastSquareFit(input, response, order);
		invLSF_N = new LeastSquareFit(response, input, order);
	}

	private double invLSF_Evaluate(double d){
		if (d > 0)
			return invLSF_P.evaluate(d);
		else
			return invLSF_N.evaluate(d);
	}

	private void calibrationStrainGauge(){

		// XXX appropriate weight to (0, 0)
		int zeroWeight = 200;
		int numDataFromFile = 0;
		int numData = 0;
		double[] calCurvature = null;

		// ////////////////////////////////////////////////////////////////////////////////
		// reader header and curvature information from file
		// ////////////////////////////////////////////////////////////////////////////////	
		try {
			reader = new BufferedReader(new FileReader(Params.STRIP.calibration_data_set));
			try {
				String line;
				line = reader.readLine();
				if (line != null){
					String[] inStrArr = line.split(" ");
					numDataFromFile = inStrArr.length;
					numData = zeroWeight + numDataFromFile;
					calCurvature = new double[numDataFromFile];
					for (int i = 0; i < numDataFromFile; i++)
						calCurvature[i] = 1/Double.parseDouble(inStrArr[i]);
				}
				else{
					System.out.println("Empty file!!!");
					return;
				}
			} catch (IOException e) { e.printStackTrace(); }
		} catch (FileNotFoundException e) { e.printStackTrace(); }

		// ////////////////////////////////////////////////////////////////////////////////
		// read remaining data from file
		// ////////////////////////////////////////////////////////////////////////////////
		double[][] calDataFromTest = new double[Params.NUM_STRAIN_SENSORS][numData];

		for (int i = 0; i < zeroWeight; i++){
			for (int j = 0; j < Params.NUM_STRAIN_SENSORS; j++)
				calDataFromTest[j][i] = 0;
		}

		try {
			String line;
			int count = zeroWeight;
			while ((line = reader.readLine()) != null){
				String[] inStrArr = line.split(" ");
				for (int i = 0; i < Params.NUM_STRAIN_SENSORS; i++)
					calDataFromTest[i][count] = Double.parseDouble(inStrArr[i]);
				count++;
			}
		} catch (IOException e) { e.printStackTrace(); }
		try { reader.close(); } catch (IOException e) { e.printStackTrace(); }

		// ////////////////////////////////////////////////////////////////////////////////
		// Baseline data from mapping curve
		// ////////////////////////////////////////////////////////////////////////////////	
		double[] rawDataForCalCurvature = new double[numData];
		for (int i = 0; i < zeroWeight; i++)
			rawDataForCalCurvature[i] = 0;
		for (int i = zeroWeight; i < numData; i++){
			double curvature = calCurvature[i-zeroWeight];
			if (curvature >= 0)
				rawDataForCalCurvature[i] = this.invLSF_P.evaluate(curvature);
			else
				rawDataForCalCurvature[i] = this.invLSF_N.evaluate(curvature);
		}

		// ////////////////////////////////////////////////////////////////////////////////
		// set calibrate LSF regression to each strain gauge
		// ////////////////////////////////////////////////////////////////////////////////
		int[] gaugeInOrder1 = Params.STRIP.calibration_in_order_1;
		int idx = 0;
		for (int i = 0; i < Params.NUM_STRAIN_SENSORS; i++){
			double[] input = calDataFromTest[i];
			LeastSquareFit lsf;
			
			if ( idx < gaugeInOrder1.length && i == gaugeInOrder1[idx]){
				lsf = new LeastSquareFit(input, rawDataForCalCurvature, 1);
				idx++;
			}
			else
				lsf = new LeastSquareFit(input, rawDataForCalCurvature, 2);
			
			ss[i].mapLSF = lsf;
		}
	}




	// ////////////////////////////////////////////////////////////////////////////////
	// function to draw
	// ////////////////////////////////////////////////////////////////////////////////

	void drawSpline(ArrayList x, ArrayList y) {
		int s = x.size();
		float[] mx = new float[s];
		float[] my = new float[s];

		for (int i = 0; i < s; i++) {
			mx[i] = ((Float) x.get(i)).floatValue();
			my[i] = ((Float) y.get(i)).floatValue();
		}

		drawSpline(mx, my);
	}

	void drawSpline(float[] x, float[] y) {
		if (x.length < 1)
			return;

		Spline xs, ys;
		xs = new Spline(x);
		ys = new Spline(y);

		noStroke();
		float space = floor((float)(x.length-1)/(Params.NUM_TOUCH_SENSORS-1) * 100) ;
		space = space / 100;

		for	(float t = 0.0f; t <= space * (Params.NUM_TOUCH_SENSORS-1); t += 0.01f){
			float inter = t/space;
			int c = lerpColor(touchColor[floor(inter)], touchColor[ceil(inter)], inter-floor(inter));
			fill(c);

			beginShape();

			vertex(xs.calc(t), ys.calc(t), 0);
			vertex(xs.calc(t + 0.01f), ys.calc(t + 0.01f), 0);
			vertex(xs.calc(t + 0.01f), ys.calc(t + 0.01f), Params.STRIP_WIDTH);
			vertex(xs.calc(t), ys.calc(t), Params.STRIP_WIDTH);

			endShape();
		}

		stroke(255, 0, 0);

//		for (int i = 0; i < x.length; i++) {
//			ellipse(x[i], y[i], 8, 8);
//		}
	}

	private void drawTransFunction(){
		strokeWeight(2);
		int LEN = 500;
		int[] curve = new int[LEN];
		if (flatValue != null){
			for (int i = 0; i < LEN; i++){
				curve[i] = (int) getCurveRadius(-1, i-LEN/2);
			}
			stroke(200);
			for (int i = 1; i < LEN; i++)
				line(i+(WIDTH-LEN)/2, HEIGHT/2 - curve[i-1], 
						i+1+(WIDTH-LEN)/2, HEIGHT/2 - curve[i]);
			stroke(120);
			line(0, HEIGHT/2, WIDTH, HEIGHT/2);
			line(WIDTH/2, 0, WIDTH/2, HEIGHT);
		}
		strokeWeight(1);
	}


	private void drawInform(){
		for (int i = 0; i < WIDTH-1; i++){
			stroke(Params.COLOR_TOUCH);
			line(WIDTH-i, 1.0f/ss[sensorInterest].radiusWindow.get(i)*2500+HEIGHT/2,
					WIDTH-(i+1), 1.0f/ss[sensorInterest].radiusWindow.get(i+1)*2500+HEIGHT/2);
			stroke(Params.COLOR_UNTOUCH);
			line(WIDTH-i, ss[sensorInterest].rawWindow.get(i)-HEIGHT/2, 
					WIDTH-(i+1), ss[sensorInterest].rawWindow.get(i+1)-HEIGHT/2);
		}

		drawMappedFunction(sensorInterest);

		textSize(20);
		text(sensorInterest, 50, 80);

		for (int i = 0; i < Params.NUM_STRAIN_SENSORS; i++){
			text(i, 50, 100 + 20*i);
			text((float) (ss[i].getCurrentValue() - fixRawData[i]), 90, 100 + 20*i);
			text((float) ss[i].mapLSF.evaluate(ss[i].getCurrentValue()-flatValue[i]), 200, 100 + 20*i);
		}
		textSize(12);

	}

	private void drawMappedFunction(int idx){
		fill(0);
		int LEN = 200;
		for (int i = -LEN; i < LEN-1; i++){
			line(WIDTH/2+i, HEIGHT/2-(int)ss[idx].mapLSF.evaluate(i),
					WIDTH/2+i+1, HEIGHT/2-(int)ss[idx].mapLSF.evaluate(i+1));
		}
	}

	private void drawAllData(int x, int y, int w, int h){
		float move = (float)h*3/(4*(Params.NUM_STRAIN_SENSORS-1));
		for (int i = 0; i < Params.NUM_STRAIN_SENSORS; i++){
			stroke(Params.COLOR_POOL[i]);
			for (int j = 1; j < w; j++){
				line(x + j, y + h/8 + move*i - (int)(ss[i].rawWindow.get(WIDTH-j)-flatValue[i]), 
						x + (j+1), y + h/8 + move*i - (int)(ss[i].rawWindow.get(WIDTH-j-1)-flatValue[i]));
			}
		}
	}

	private void drawCurrentShape(int x, int y, int w, int h){
		strokeWeight(2);
		stroke(0);
		fill(255);
		rect(x, y, w, h);
		int tmpShape = -1;
		try {
			tmpShape = svm_predict.classifyJarLib(model, rawRadiusVal);
		} catch (IOException e1) { e1.printStackTrace(); }

		if (tmpShape != curShape){
			curShape = tmpShape;
			switch (curShape){
			case 5:
				curShapeImg = loadImage("circle.png");
				break;
			case 6:
				curShapeImg = loadImage("square.png");
				break;
			case 7:
				curShapeImg = loadImage("triangle.png");
				break;
			case 8:
				curShapeImg = loadImage("v_shape.png");
				break;
			case 9:
				curShapeImg = loadImage("ok.png");
				break;
			case 10:
				curShapeImg = loadImage("cap.png");
				break;
			case 11:
				curShapeImg = loadImage("w_shape.png");
				break;
			default:
				curShapeImg = loadImage("no.png");
				break;
			}
		}
		image(curShapeImg, x, y, w, h);
		strokeWeight(1);
	}

	private void drawTargetImage(int x, int y, int r){
		pushMatrix();
		translate(0, 0, -100);
		
		if (toLoadImg){
			switch(targetShape){
			case 5:
				baseLine = loadImage("circle.png");
				break;
			case 6:
				baseLine = loadImage("square.png");
				break;
			case 7:
				baseLine = loadImage("triangle.png");
				break;
			case 8:
				baseLine = loadImage("v_shape.png");
				break;
			case 9:
				baseLine = loadImage("ok.png");
				break;
			case 10:
				baseLine = loadImage("cap.png");
				break;
			case 11:
				baseLine = loadImage("w_shape.png");
				break;
			default:
				baseLine = loadImage("no.png");
				break;
			}
		}
		image(baseLine, x, y, r, r);
		toLoadImg = false;
		
		popMatrix();
	}
	
	
	private void showFileChooser(){
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("."));

		chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				return (f.getName().toLowerCase().endsWith(".txt") || f.isDirectory());
			}

			public String getDescription() {
				return "(*,TXT) Raw data File";
			}
		});

		int r = chooser.showOpenDialog(new JFrame());
		if (r == JFileChooser.APPROVE_OPTION) {
			rawDataFile = chooser.getSelectedFile().getAbsolutePath();
			System.out.println(rawDataFile);
		}
	}
	
	
	// ////////////////////////////////////////////////////////////////////////////////
	// Other function
	// ////////////////////////////////////////////////////////////////////////////////
	
	private void writeRawDataToFile(){
		if (outFile != null){
			try {
				for (int i = 0; i < ss.length; i++){
					writer.write( Double.toString( ss[i].getCurrentValue()) );
					writer.write( " " );
				}
				writer.write( "\n" );
			} catch (IOException e) { e.printStackTrace(); }
		} else { System.out.println("output file not create"); }
	}
	
	private void writeFlatValueToFile(){
		if (outFile != null){
			try {
				for (int i = 0; i < ss.length; i++){
					writer.write( Double.toString( flatValue[i]) );
					writer.write( " " );
				}
				writer.write( "\n" );
			} catch (IOException e) { e.printStackTrace(); }
		} else { System.out.println("output file not create"); }
	}
	
	public double getCurveRadius(int idx, double strainValue) {
		double mappedValue;
		if (idx == -1){
			mappedValue = strainValue;
		}
		else{
			mappedValue = strainValue - flatValue[idx];
			mappedValue = ss[idx].mapLSF.evaluate(mappedValue);
		}
		// XXX
		int threshold = 50;
		double base = 1.05;
		if (mappedValue >= 0){
			double r;
			if ( mappedValue < threshold )
				r = 1.0/LSF_P.evaluate( mappedValue ) * Math.pow(base, threshold-mappedValue);
			else
				r = 1.0/LSF_P.evaluate( mappedValue );
			return r;
		}
		else{
			double r;
			if ( mappedValue > -threshold )
				r = 1.0/LSF_N.evaluate( mappedValue ) * Math.pow(base, threshold+mappedValue);
			else
				r = 1.0/LSF_N.evaluate( mappedValue );
			return r;
		}
	}
	
	
	private void loadDataFromFile(){
		double[] sum = new double[Params.NUM_STRAIN_SENSORS];
		int count = 0;
		try {
			reader = new BufferedReader(new FileReader(rawDataFile));
			try {
				String line;
				
				// read flat value from first line of file
				if ( (line = reader.readLine()) != null ){
					String[] inStrArr = line.split(" ");
					for (int i = 0; i < Params.NUM_STRAIN_SENSORS; i++){
						flatValue[i] = Double.parseDouble(inStrArr[i]);
					}
				}
				
				// read remaining data and get average
				while( (line = reader.readLine()) != null){
					String[] inStrArr = line.split(" ");
					for (int i = 0; i < Params.NUM_STRAIN_SENSORS; i++){
						sum[i] = sum[i] + Double.parseDouble(inStrArr[i]);
					}
					count++;
				}
			} catch (IOException e) { e.printStackTrace(); }
		} catch (FileNotFoundException e) { e.printStackTrace(); }
		
		for (int i = 0; i < Params.NUM_STRAIN_SENSORS; i++)
			sum[i] = sum[i] / count;
		
		rawStrainVal = sum;
	}
}
