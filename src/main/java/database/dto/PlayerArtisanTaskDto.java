package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class PlayerArtisanTaskDto {
	private int playerId;
	private int assignedMasterId;
	private int itemId;
	private int assignedAmount;
	private int handedInAmount;
	private int totalTasks;
	private int totalPoints;
	
	public void reset(int assignedMasterId, int itemId, int assignedAmount) {
		this.itemId = itemId;
		this.assignedMasterId = assignedMasterId;
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
		totalPoints += actualAmountHandedIn;
		return actualAmountHandedIn;
	}
}
