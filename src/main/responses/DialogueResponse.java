package main.responses;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import main.database.DialogueDao;
import main.database.ItemDao;
import main.database.NPCDao;
import main.database.NpcDialogueDto;
import main.database.NpcDialogueOptionDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.Request;
import main.types.Items;
import main.types.StorageTypes;

@Setter
public class DialogueResponse extends Response {
	private String dialogue = "";
	private String speaker = "";
	
	public DialogueResponse() {
		setAction("dialogue");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		NpcDialogueDto currentDialogue = player.getCurrentDialogue();
		if (currentDialogue == null) {
			// player must have a current dialogue in order to move to the next dialogue.
			// sending an empty dialogue response tells the client to close the window
			responseMaps.addClientOnlyResponse(player, new DialogueResponse());
			return;
		}		
		
		ArrayList<NpcDialogueOptionDto> options = DialogueDao.getDialogueOptionsBySrcDialogueId(currentDialogue.getNpcId(), currentDialogue.getPointId(), currentDialogue.getDialogueId());
		if (!options.isEmpty()) {
			List<NpcDialogueOptionDto> validOptions = new ArrayList<>();// inventory checks etc
			List<Integer> inv = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			for (NpcDialogueOptionDto option : options) {
				if (option.getRequiredItem1() == 0 || inv.contains(option.getRequiredItem1()))
					validOptions.add(option);
			}
			
			DialogueOptionResponse optionResponse = new DialogueOptionResponse();
			optionResponse.setOptions(validOptions);
			responseMaps.addClientOnlyResponse(player, optionResponse);
			return;
		}
		
		NpcDialogueDto nextDialogue = DialogueDao.getDialogue(currentDialogue.getNpcId(), currentDialogue.getPointId(), currentDialogue.getDialogueId() + 1);
		if (nextDialogue == null) {
			// currentDialogue was the last dialogue; close the dialogue window
			responseMaps.addClientOnlyResponse(player, new DialogueResponse());
			return;
		}
		
		player.setCurrentDialogue(nextDialogue);
		dialogue = nextDialogue.getDialogue();
		speaker = NPCDao.getNpcNameById(nextDialogue.getNpcId());
		responseMaps.addClientOnlyResponse(player, this);
		
		if (nextDialogue.getNpcId() == 12 && nextDialogue.getPointId() == 2 && nextDialogue.getDialogueId() == 13) {
			// leo gives the sword to the player
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			if (invItemIds.contains(Items.LEOS_BABY.getValue())) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(Items.LEOS_BABY.getValue()), Items.STEEL_SWORD_III.getValue(), 1, ItemDao.getMaxCharges(Items.STEEL_SWORD_III.getValue()));
				InventoryUpdateResponse.sendUpdate(player, responseMaps);
			}
		}
	}

}
