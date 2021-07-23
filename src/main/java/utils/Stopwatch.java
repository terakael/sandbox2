package utils;

import java.util.HashMap;
import java.util.Map;

public class Stopwatch {
	private static HashMap<String, Long> starts = new HashMap<>();
	private static HashMap<String, Long> results = new HashMap<>();
	public static void reset() {
		starts.clear();
		results.clear();
	}
	
	public static void start(String label) {
		starts.put(label, System.nanoTime());
	}
	
	public static void end(String label) {
		if (!starts.containsKey(label)) 
			return;
		
		results.put(label, (System.nanoTime() - starts.get(label)) / 1000000);
	}
	
	public static long getMs(String label) {
		if (!results.containsKey(label))
			return 0;
		return results.get(label);
	}
	
	public static void dump(String label) {
		if (!results.containsKey(label))
			return;
		System.out.println(String.format("%s: %dms", label, results.get(label)));
	}
	
	public static void dump() {
		System.out.println("--------");
		for (Map.Entry<String, Long> entry : results.entrySet())
			System.out.println(String.format("%s: %dms", entry.getKey(), entry.getValue()));
		System.out.println("--------");
	}
}
