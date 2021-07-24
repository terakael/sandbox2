package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerArtisanTaskItemDto {
	private int playerId;
	private int itemId;
	private int assignedAmount;
	private int handedInAmount;
	
	public void reset(int itemId, int assignedAmount) {
		this.itemId = itemId;
		this.assignedAmount = assignedAmount;
		this.handedInAmount = 0;
	}
	
	public int updateAmountToHandIn(int amountToHandIn) {
		if (handedInAmount + amountToHandIn <= assignedAmount) {
			handedInAmount += amountToHandIn;
			return amountToHandIn;
		}
		
		final int actualAmountHandedIn = assignedAmount - handedInAmount;
		handedInAmount = assignedAmount;
		return actualAmountHandedIn;
	}
}
