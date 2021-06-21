package main.utils;

import java.util.Random;

public class RandomUtil {
	public static int getRandom(int min, int max) {
		Random r = new Random();
		return r.nextInt(max - min) + min;
	}
	
	public static boolean chance(int outOf100) {
		return getRandom(0, 100) < outOf100;
	}
}
