package rubberband;


public class Params {
	
	// for display
	public static float DISPLAY_RATIO = 3.0f;
	public static int WINDOW_HEIGHT = 720;
	public static int WINDOW_WIDTH = 1240;
	public static int ONE_STEP = 20;
	public static int STRIP_WIDTH = 50;
	
	
	// FLAG
	// -- draw auxiliary circle and show curvature in strip
	public static boolean DEBUG = true;
	// -- enable closed form constraint
	public static boolean DO_CLOSE_SHAPE = true;
	// -- read data from file instead from arduino
	public static boolean DATA_FROM_FILE = true;
	
	// For each strip
	private static StripInfo STRIP_WITHOUT_NINIJA = new StripInfo(
			new int[]{14, 4, 9, 7, 11, 3, 12, 6, 13, 1, 10, 8, 15, 5, 0, 2},
			7.0f, 
			new int[]{15},
			"testData1_3.txt");
	
	private static StripInfo STRIP_WITH_NINIJA = new StripInfo(
			new int[]{4, 13, 5, 0, 1, 14, 3, 10, 2, 9, 7, 12, 8, 11, 6, 15}, 
			7.0f, 
			new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, 
			"testData2_1.txt");
	
	// which strip
	public static StripInfo STRIP = STRIP_WITHOUT_NINIJA;
	
	// for strain gauge parameter
	public static float GAUGE_SPACING = 15 * DISPLAY_RATIO; // 15
	public static float GAUGE_LENGTH = 4 * DISPLAY_RATIO;  // 4
	
	// for display touch
	public static int COLOR_TOUCH = color(0, 0, 255, 255);
	public static int COLOR_UNTOUCH = color(255, 0, 0, 255);
	
	// for number of sensor
	public static int NUM_STRAIN_SENSORS = 16;
	public static int NUM_NEOPIXELS = 16;
	public static int NUM_TOUCH_SENSORS = 12;
	public static int NUM_OF_DUMMY = 4;
	
	public static int DATA_WINDOW_SIZE = 8;
	
	public static boolean DO_ARDUINO = true;
	
	public static int[] COLOR_POOL = new int[]{
		color(0, 0, 166, 255), color(0, 113, 188, 255),
		color(0, 174, 239, 255), color(0, 255, 0, 255), 
		color(0, 166, 0, 255), color(140, 198, 0, 255), 
		color(255, 242, 0, 255), color(255, 190, 0, 255), 
		color(255, 138, 0, 255), color(255, 138, 0, 255), 
		color(255, 0, 0, 255), color(255, 0, 151, 255), 
		color(145, 0, 145, 255), color(0, 0, 0, 255), 
		color(64, 64, 64, 255), color(128, 128, 128, 255)		
		};	
	
	static public int color(int r, int g, int b, int alpha){
		int result = (alpha << 24) + (r << 16) + (g << 8) + b;
		return result;
	}
}
