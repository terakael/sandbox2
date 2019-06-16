package main.utils;

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
}
