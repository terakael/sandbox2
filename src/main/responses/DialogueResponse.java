package main.responses;

import java.util.ArrayList;

import lombok.Setter;
import main.database.DialogueDao;
import main.database.NpcDialogueDto;
import main.database.NpcDialogueOptionDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.Request;

@Setter
public class DialogueResponse extends Response {
	private String dialogue = "";
	
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
			// send ShowDialogueOptionResponse
			
			ArrayList<NpcDialogueOptionDto> validOptions = new ArrayList<>();// inventory checks etc
			ArrayList<Integer> inv = PlayerStorageDao.getInventoryListByPlayerId(player.getId());
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
		responseMaps.addClientOnlyResponse(player, this);
	}

}
