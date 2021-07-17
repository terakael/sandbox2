package main.responses;

import java.util.List;

import lombok.Setter;
import main.GroundItemManager;
import main.database.dao.DialogueDao;
import main.database.dao.ItemDao;
import main.database.dao.NPCDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.NpcDialogueDto;
import main.database.dto.NpcDialogueOptionDto;
import main.processing.attackable.Player;
import main.requests.DialogueOptionRequest;
import main.requests.Request;
import main.types.Items;
import main.types.StorageTypes;

public class DialogueOptionResponse extends Response {
	@Setter private List<NpcDialogueOptionDto> options;
	public DialogueOptionResponse() {
		setAction("dialogue_option");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		NpcDialogueDto dialogue = player.getCurrentDialogue();
		if (dialogue == null)
			return;
		
		if (!(req instanceof DialogueOptionRequest))
			return;
		
		NpcDialogueOptionDto selectedOption = DialogueDao.getDialogueOption(dialogue.getNpcId(), dialogue.getPointId(), player.getId());
		if (selectedOption == null) {
			// setting to null causes the DialogueResponse to send an empty message to the player, cleaning up the client side
			player.setCurrentDialogue(null);
			new DialogueResponse().process(null, player, responseMaps);
			return;
		}
		
		NpcDialogueDto newDialogue = DialogueDao.getDialogue(dialogue.getNpcId(), dialogue.getPointId(), selectedOption.getDialogueDest());
		// sometimes the chosen dialogue triggers a new conversation point.
		// next time the player talks to the npc, it'll start on that point.
		if (newDialogue.getPointId() != 0)
			DialogueDao.setPlayerNpcDialogueEntryPoint(player.getId(), newDialogue.getNpcId(), selectedOption.getNextPointId());
		
		player.setCurrentDialogue(newDialogue);
		DialogueResponse dialogueResponse = new DialogueResponse();
		dialogueResponse.setDialogue(newDialogue.getDialogue());
		dialogueResponse.setSpeaker(NPCDao.getNpcNameById(newDialogue.getNpcId()));
		responseMaps.addClientOnlyResponse(player, dialogueResponse);
		
		// not sure how to structure this; needs to be in a db table somewhere though.
		if (newDialogue.getNpcId() == 26 && newDialogue.getPointId() == 1 && newDialogue.getDialogueId() == 13) {
			// "here you go" for the cape
			int capeId = 0;
			switch (selectedOption.getOptionId()) {
			case 3: // black
				capeId = Items.BLACK_CAPE.getValue();
				break;
			case 4: // white
				capeId = Items.WHITE_CAPE.getValue();
				break;
			case 5: // red
				capeId = Items.RED_CAPE.getValue();
				break;
			case 6:  // blue
				capeId = Items.BLUE_CAPE.getValue();
				break;
			case 7: // green
				capeId = Items.GREEN_CAPE.getValue();
				break;
			default:
				break;
			}
			
			if (capeId != 0) {
				// if the player doesn't have any inventory space then drop it on the ground below them
				List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
				if (!invItemIds.contains(0)) {
					// drop on ground
					GroundItemManager.add(player.getFloor(), player.getId(), capeId, player.getTileId(), 1, ItemDao.getMaxCharges(capeId));
				} else {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(0), capeId, 1, ItemDao.getMaxCharges(capeId));
				}
				
				InventoryUpdateResponse.sendUpdate(player, responseMaps);
			}
		}
	}
}
