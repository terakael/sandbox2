package main.utils;

import main.processing.PathFinder;
import main.processing.Player;
import main.processing.WorldProcessor;

public class Utils {
	public static String getFriendlyCount(int count) {
		final int THOUSAND = 1000;
		final int HUNDRED_THOUSAND = THOUSAND * 100;
		final int MILLION = THOUSAND * THOUSAND;
		final int TEN_MILLION = MILLION * 10;
		if (count < HUNDRED_THOUSAND) {
			return String.format("%d", count);
		} else if (count >= HUNDRED_THOUSAND && count < TEN_MILLION) {
			return String.format("%dk", count / THOUSAND);
		} else {
			return String.format("%dM", count / MILLION);
		}
	}
	
	public static boolean areTileIdsWithinRadius(int first, int second, int radius) {
		int firstX = first % PathFinder.LENGTH;
		int firstY = first / PathFinder.LENGTH;
		
		int secondX = second % PathFinder.LENGTH;
		int secondY = second / PathFinder.LENGTH;
		
		return (secondX >= firstX - radius && secondX <= firstX + radius) &&
			   (secondY >= firstY - radius && secondY <= firstY + radius);
	}
}
