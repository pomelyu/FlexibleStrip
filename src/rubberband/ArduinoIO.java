package rubberband;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import processing.core.PApplet;
import processing.serial.Serial;

public class ArduinoIO {

	private final PApplet pApplet;
	private final Object parent;
	private Serial serial; // The serial port
	private int numOfStrainSensors;
	private int numOfNeoLeds;
	private int numOfTouchSensors;
	private int numOfDummies;
	private double[] gsVal;
	private double[] tsVal;
	
	private int[] val;      // Data received from the serial port
	
	private static int[] mapping = Params.STRIP.strain_gauge_mapping;

	private Method strainGaugeEventMethod;

	public ArduinoIO(final PApplet pApplet, Object anotherParent,
			int numOfStrainSensors, int numOfNeoLeds, int numOfTouchSensors) {
		this.pApplet = pApplet;
		this.numOfStrainSensors = numOfStrainSensors;
		this.numOfNeoLeds = numOfNeoLeds;
		this.numOfTouchSensors = numOfTouchSensors;
		this.numOfDummies = Params.NUM_OF_DUMMY;

		gsVal = new double[numOfStrainSensors];
		tsVal = new double[numOfTouchSensors];

		if (anotherParent == null)
			parent = pApplet;
		else
			parent = anotherParent;

		try {
			strainGaugeEventMethod = parent.getClass().getMethod(
					"strainGaugeEvent", new Class[] { double[].class });

		} catch (Exception e) {
			e.printStackTrace();
		}

		setup();
	}

	public void setup() {

		String[] serialDevices = Serial.list();
		System.out.println(serialDevices.length);
		for (int i = 0; i < serialDevices.length; i++)
			System.out.println(serialDevices[i]);

		//serial = new Serial(pApplet, Serial.list()[6], 9600);
		serial = new Serial(pApplet, "/dev/tty.usbmodem1411", 57600);
		
		val = new int[numOfStrainSensors + numOfTouchSensors + numOfDummies];
		//values = new int[numOfStrainSensors + numOfTouchSensors][width];
	}

	public void setNeoPixels(byte[] data) {
		// for (int i = 0; i < data.length; i++)
		// System.out.print(data[i] + ",");
		// System.out.println("");
		// System.out.println("data.length: " + data.length);

		serial.write(data);
	}
	
	float readFloat(Serial s) {
		return Float.intBitsToFloat(s.read()+(s.read()<<8)+(s.read()<<16)+(s.read()<<24));
	}

	public void update() {
		
		int wantSize = numOfStrainSensors + numOfTouchSensors + numOfDummies;
		while (serial.available() >= 100) {
		    if (serial.read() == 0xff) {
		      if (serial.read() == 0xfe) {
		        
		        for(int i=0; i<wantSize; i++) {
		          val[i] = (int)((serial.read() << 8) | (serial.read()));
		        }
		        
		        ((RubberbandTest) parent).motionEvent(readFloat(serial), readFloat(serial), readFloat(serial));
		      }
		    }
		}
		
		for (int i = 0; i < numOfStrainSensors; i++){
			gsVal[i] = val[2*mapping[i]+1];
		}
		
		for (int i = 0; i < numOfTouchSensors; i++){
			tsVal[i] = val[2*i];
		}
		
		((RubberbandTest) parent).strainGaugeEvent(gsVal);
		((RubberbandTest) parent).touchSensorEvent(tsVal);
		
		
		for(int i = 0; i < gsVal.length; i++) {
			System.out.print(gsVal[i]+" ");
		}
		System.out.println();
		
		
		/*int wantSize = numOfStrainSensors + numOfTouchSensors;

		while (serial.available() > 0) {
			String inData = serial.readStringUntil(10);
			if (inData != null) {
				inData = inData.trim();

				String[] data = inData.split(",");
				int length = data.length;

				if (length != wantSize) {
					System.out.print("[StrainGauge] data unmatch. ");
					System.out.println("should be (" + wantSize + "), but ("
							+ length + "), try config in Arduino part.");
				} else {
					// System.out.println(inData);

					double[] temp = new double[length];
					for (int i = 0; i < length; i++)
						temp[i] = Double.parseDouble(data[i]);

					for (int i = 0; i < gsVal.length; i++)
						gsVal[i] = temp[i];

					((RubberbandTest) parent).strainGaugeEvent(gsVal);

					// try {
					// strainGaugeEventMethod.invoke(parent, gsVal);
					// } catch (IllegalAccessException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// } catch (InvocationTargetException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// } catch (NullPointerException e) {
					// throw new RuntimeException(
					// "You need to implement the gsNailEvent(double[] value) function to handle the hall sensor data.");
					// }
				}
			}
		}*/
	}

}
