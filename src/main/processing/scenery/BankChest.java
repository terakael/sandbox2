package main.processing.scenery;

import java.util.HashSet;

@SuppressWarnings("unused")
public class BankChest {
	private static HashSet<Integer> bankChestTileIds = new HashSet<>();
	
	public static void setupCaches() {
		// when we do a deposit/withdrawal we need to make sure the player is standing next to one of these bankChestTileIds.
		// if they're not then thye cannot access their bank.
	}
}
