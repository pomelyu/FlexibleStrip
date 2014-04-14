package rubberband;

import java.awt.geom.Point2D;
import java.util.*;

import math.geom2d.Vector2D;

import processing.core.PApplet;

import rubberband.math.*;

public class StrainGauge {

	private float x, y;
	private float gaugeLength;
	private float radius;
	private float nextRadius;
	private Point2D.Float nextStart;
	private final PApplet pApplet;
	private double value;
	private Vector<Double> dataWindow;
	
	// for debug
	public Vector<Float> radiusWindow;
	public Vector<Float> rawWindow;
	
	// for calibration
	public LeastSquareFit mapLSF;
	
	private static int DATA_WINDOW_SIZE = Params.DATA_WINDOW_SIZE;
	private static int RADIUS_WINDOW_SIZE = Params.WINDOW_WIDTH;

	public StrainGauge(final PApplet pApplet, float gaugeLength) {
		this.pApplet = pApplet;
		this.x = 0;
		this.y = 0;
		this.gaugeLength = gaugeLength;
		this.radius = getCurvatureRadius();
		value = 0;
		dataWindow = new Vector<Double>();
		for(int i = 0; i < DATA_WINDOW_SIZE; i++)
			dataWindow.add((double) 0);
		radiusWindow = new Vector<Float>();
		for (int i = 0; i < RADIUS_WINDOW_SIZE; i++)
			radiusWindow.add(0.0f);
		rawWindow = new Vector<Float>();
		for (int i = 0; i < RADIUS_WINDOW_SIZE; i++)
			rawWindow.add(0.0f);
	}
	
	public void update(double rVal){
		if (!dataWindow.isEmpty()){
			dataWindow.remove(0);
			dataWindow.add(rVal);
			double sum = 0;
			for (int i = 0; i < DATA_WINDOW_SIZE; i++)
				sum = sum + dataWindow.get(i);
			value = sum / DATA_WINDOW_SIZE;
			rawWindow.remove(0);
			rawWindow.add((float)value);
		}
		else
			System.out.println("Err! dataWindow empty");
	}
	
	public double getCurrentValue(){ return value; }

	public void setLocation(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void setCurvatureRadius(float radius) {
		this.radius = radius;
		radiusWindow.remove(0);
		radiusWindow.add(radius);
	}
	
	public void setNextRadius(float r){ nextRadius = r; }
	
	private float getCurvatureRadius() {
		// return (float) (Math.random() * 2 - 1.0) * 20;
		// in mm
		int min = -20;
		int max = 20;
		int random = min + (int) (Math.random() * ((max - min) + 1));

		return (float) random;
	}

	public Point2D.Float[] getPoints() {
		int steps = 4;
		Point2D.Float[] pts = new Point2D.Float[steps];

		// positive sign == downward
		float sign = (radius > 0) ? 1.0f : -1.0f;
		float radiusAbs = Math.abs(radius);

		float gaugeInRadius = (float) (gaugeLength / radiusAbs);

		float dRadius = 0;
		for (int i = 0; i < steps; i++) {
			dRadius = i * (gaugeInRadius / steps);
			pts[i] = new Point2D.Float();
			pts[i].x = (float) Math.sin(dRadius) * radiusAbs;
			pts[i].y = (float) (sign * (radiusAbs - Math.cos(dRadius)
					* radiusAbs));

			// System.out.println(i + ", " + pts[i].x + ", " + pts[i].y);
		}

		return pts;
	}

	public void draw() {
		pApplet.stroke(0);
		pApplet.fill(255, 0, 0);
		Point2D.Float[] pts = getPoints();
//		for (int i = 0; i < pts.length; i++) {
//			pApplet.ellipse(pts[i].x, pts[i].y, 2, 2);
//		}
		pApplet.strokeWeight(2);
		pApplet.stroke(0);
		pApplet.line(pts[0].x, pts[1].y, 0, pts[0].x, pts[1].y, Params.STRIP_WIDTH);
		pApplet.line(pts[pts.length-1].x, pts[pts.length-1].y, 0, 
				pts[pts.length-1].x, pts[pts.length-1].y, Params.STRIP_WIDTH);
		pApplet.strokeWeight(1);
		
//		Point2D.Float last = pts[pts.length - 1];
//		if (Params.DEBUG)
//			pApplet.text((int) radius, last.x, last.y-5);

//		Point2D.Float next = nextStart();
		// TODO draw arc install of line
//		pApplet.line(last.x, last.y, next.x, next.y);

		if (Params.DEBUG) {
			pApplet.stroke(0, 0, 255);
			pApplet.noFill();
			if (radius < 200 && radius > -200)
				pApplet.ellipse(0, radius, radius * 2, radius * 2);
		}
	}

	public Point2D.Float nextStart() {
		Point2D.Float[] pts = getPoints();
		Point2D.Float last = pts[pts.length - 1];
	
		Point2D.Float p2 = displaceByRadius(nextRadius);
		
		nextStart = new Point2D.Float();
		nextStart.x = last.x + p2.x;
		nextStart.y = last.y + p2.y;
		
		return nextStart;
	}
	
	private Point2D.Float displaceByRadius(float r){
		
		float absR1 = Math.abs(radius);
		float absR2 = Math.abs(r);
		float t1 = gaugeLength/absR1;
		float t2 = Params.GAUGE_SPACING/absR2;
		
		Point2D.Float displace = new Point2D.Float();
		
		if (radius > 0 && r > 0 ){
			displace.x = (float)( absR2 * Math.sin(t2) * Math.cos(t1) 
					- absR2 * (1-Math.cos(t2)) * Math.sin(t1) );
			displace.y = (float)( absR2 * Math.sin(t2) * Math.sin(t1)
					+ absR2 * (1-Math.cos(t2)) * Math.cos(t1) );
		}
		else if (radius > 0 && r < 0){
			displace.x = (float)( absR2 * Math.sin(t2) * Math.cos(t1) 
					+ absR2 * (1-Math.cos(t2)) * Math.sin(t1) );
			displace.y = (float)( absR2 * Math.sin(t2) * Math.sin(t1)
					- absR2 * (1-Math.cos(t2)) * Math.cos(t1) );
		}
		else if (radius < 0 && r > 0){
			displace.x = (float)( absR2 * Math.sin(t2) * Math.cos(t1) 
					+ absR2 * (1-Math.cos(t2)) * Math.sin(t1) );
			displace.y = (float)( - absR2 * Math.sin(t2) * Math.sin(t1)
					+ absR2 * (1-Math.cos(t2)) * Math.cos(t1) );
		}
		else if (radius < 0 && r < 0){
			displace.x = (float)( absR2 * Math.sin(t2) * Math.cos(t1) 
					- absR2 * (1-Math.cos(t2)) * Math.sin(t1) );
			displace.y = (float)( - absR2 * Math.sin(t2) * Math.sin(t1)
					- absR2 * (1-Math.cos(t2)) * Math.cos(t1) );
		}
		else{
			displace.x = 0;
			displace.y = 0;
			System.out.println("Curvature zero!!!");
		}
		return displace;
		
	}
	
	public float nextAngle() {
		return (float)(gaugeLength/radius + Params.GAUGE_SPACING/nextRadius);
		
//		float angle = 0;
//		float t1 = gaugeLength/Math.abs(radius);
//		float t2 = Params.GAUGE_SPACING/nextRadius;
//		
//		if (radius > 0 && nextRadius > 0){
//			angle = t1 + t2;
//		}
//		else if (radius > 0 && nextRadius < 0){
//			angle = t1 - t2;
//		}
//		else if (radius < 0 && nextRadius > 0){
//			angle = -t1 + t2;
//		}
//		else if (radius < 0 && nextRadius < 0){
//			angle = -t1 - t2;
//		}
//		else{
//			System.out.println("Curvature zero!!!");
//			angle = 0;
//		}
//		return angle;
	}
}
