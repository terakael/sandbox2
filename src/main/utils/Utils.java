package main.utils;

import main.processing.PathFinder;

public class Utils {
	public static final int SEGMENT_SIZE = 5; // a segment size is 5x5 tiles
	
	public static boolean areTileIdsWithinRadius(int first, int second, int radius) {
		int firstX = first % PathFinder.LENGTH;
		int firstY = first / PathFinder.LENGTH;
		
		int secondX = second % PathFinder.LENGTH;
		int secondY = second / PathFinder.LENGTH;
		
		return (secondX >= firstX - radius && secondX <= firstX + radius) &&
			   (secondY >= firstY - radius && secondY <= firstY + radius);
	}
	
	// find the greatest common denominator of two numbers
	public static int gcm(int a, int b) {
	    return b == 0 ? a : gcm(b, a % b);
	}
}
