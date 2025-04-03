package utils;

import java.util.HashSet;
import java.util.Set;

import processing.PathFinder;

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

	public static Set<Integer> getLocalTiles(int tileId, int radius) {
		int topLeft = tileId - radius - (radius * PathFinder.LENGTH);

		Set<Integer> localTiles = new HashSet<>();

		// (radius * 2) + 1 because the centre tile as well
		// e.g. radius of 2 gives us a 5x5 grid (2 left tiles, centre tile, 2 right
		// tiles)
		for (int y = 0; y < (radius * 2) + 1; ++y) {
			for (int x = 0; x < (radius * 2) + 1; ++x)
				localTiles.add(topLeft + (y * PathFinder.LENGTH) + x);
		}

		return localTiles;
	}

	public static int[] getRelativeXY(int topLeftTileId, int relativeTileId) {
		return new int[] {
				relativeTileId % PathFinder.LENGTH - topLeftTileId % PathFinder.LENGTH,
				relativeTileId / PathFinder.LENGTH - topLeftTileId / PathFinder.LENGTH
		};
	}

	public static int getTileIdFromRelativeXY(int topLeftTileId, int relX, int relY) {
		return topLeftTileId + (relY * PathFinder.LENGTH) + relX;
	}
}
