package processing.managers;

import responses.ResponseMaps;
import responses.UpdateGameTimeResponse;

public class TimeManager {
	private static final int secondsInHour = 60 * 60;
	private static final int secondsInDay = secondsInHour * 24;
	private static final int inGameCycleSeconds = 6000; // 100 minutes
	private static final float rlToGameRatio = ((float)secondsInDay / inGameCycleSeconds);
	private static int offset = 0; // used only so god can force day or night by changing time.
	private static int secondsPastMidnight = 0;
	private static String prevTime = "";
	private static boolean daytimeChanged = false;
	
	public static void process(int tickId, ResponseMaps responseMaps) {
		final boolean wasDaytime = isDaytime();
		
		final String currentTime = getInGameTime();
		if (!currentTime.equals(prevTime)) {
			responseMaps.addBroadcastResponse(new UpdateGameTimeResponse(currentTime));
			prevTime = currentTime;
		}
		
		final boolean isDaytime = isDaytime();
		
		daytimeChanged = false;
		if (wasDaytime != isDaytime) {
			daytimeChanged = true;
			DepletionManager.removeDaylightFlowers(isDaytime);
			WanderingPetManager.get().rotateWanderingPets();
		}
	}
	
	public static String getInGameTime() {
		secondsPastMidnight = (int) (((System.currentTimeMillis() / 1000L) + offset) % inGameCycleSeconds);
		final int hours = (int)(((float)secondsPastMidnight / inGameCycleSeconds) * 24);
		final int minutes = (int)((secondsPastMidnight * rlToGameRatio) / 60) % 60;
		return String.format("%02d:%02d", hours, minutes);
	}
	
	public static boolean isDaytime() {
		final int hour = (int)(((float)secondsPastMidnight / inGameCycleSeconds) * 24);
		return hour >= 5 && hour < 19;
	}
	
	public static boolean daytimeChanged() {
		return daytimeChanged;
	}
	
	public static void forceDaytimeChange(boolean daytime) {
		// force offset such that hours becomes 19

		// highschool maths boiiis lets go; solve for x and y
		// x is secondsPastMidnight; y is offset
		// hour = (x/inGameCycleSeconds) * 24
		// = x = (hour * inGameCycleSeconds) / 24
		final int x = ((daytime ? 5 : 19) * inGameCycleSeconds) / 24;
		
		// x = sysTime + offset % inGameCycleSeconds
		// = offset = ((sysTime % inGameCycleSeconds) - x) * -1
		offset = (int)(((System.currentTimeMillis() / 1000L) % inGameCycleSeconds) - x) * -1;
	}
}
