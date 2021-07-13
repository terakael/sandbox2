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
	
	public static boolean tileIdWithinRect(int checkTileId, int topLeftTileId, int bottomRightTileId) {
		final int x1 = topLeftTileId % PathFinder.LENGTH;
		final int x2 = bottomRightTileId % PathFinder.LENGTH;
		
		final int y1 = topLeftTileId / PathFinder.LENGTH;
		final int y2 = bottomRightTileId / PathFinder.LENGTH;
		
		final int tileX = checkTileId % PathFinder.LENGTH;
		final int tileY = checkTileId / PathFinder.LENGTH;
		 
		return tileX >= x1 && tileX <= x2 && tileY >= y1 && tileY <= y2;
	}
	
	// find the greatest common denominator of two numbers
	public static int gcm(int a, int b) {
	    return b == 0 ? a : gcm(b, a % b);
	}
}
