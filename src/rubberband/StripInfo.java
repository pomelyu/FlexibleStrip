package rubberband;

public class StripInfo {
	public int[] strain_gauge_mapping;
	public float gap_radius_fix;
	public int[] calibration_in_order_1;
	public String calibration_data_set;
	public String strip_additional_info;
	
	public StripInfo(int[] mapping, float gap_magic, int[] order1, String data_set){
		strain_gauge_mapping = mapping;
		gap_radius_fix = gap_magic;
		calibration_in_order_1 = order1;
		calibration_data_set = data_set;
		strip_additional_info = "No Info";
	}
	
	public void setInfo(String info){ strip_additional_info = info; }
}
