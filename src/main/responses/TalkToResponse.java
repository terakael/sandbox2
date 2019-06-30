package main.responses;

import java.util.ArrayList;

import main.database.DialogueDao;
import main.database.NPCDao;
import main.database.NpcDialogueDto;
import main.database.NpcMessageDao;
import main.processing.FightManager;
import main.processing.NPC;
import main.processing.NPCManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.requests.Request;
import main.requests.TalkToRequest;
import main.utils.RandomUtil;

public class TalkToResponse extends Response {
	private int objectId;
	private String message;
	
	public TalkToResponse() {
		setAction("talk to");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof TalkToRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you're too busy fighting!");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		TalkToRequest request = (TalkToRequest)req;
		NPC npc = NPCManager.get().getNpcByInstanceId(request.getObjectId());// request tileid is the instnace id
		if (npc == null) {
			setRecoAndResponseText(0, "you can't talk to that.");
			return;
		}
		
		if (!PathFinder.isNextTo(npc.getTileId(), player.getTileId())) {
			player.setTarget(npc);	
			player.setSavedRequest(request);
		} else {
			// talk to it
			handleTalkTo(npc, player, responseMaps);
		}
		
	}
	
	private void handleTalkTo(NPC npc, Player player, ResponseMaps responseMaps) {
		NpcDialogueDto initialDialogue = DialogueDao.getEntryDialogueByPlayerIdNpcId(player.getId(), npc.getId());
		if (initialDialogue == null)
			initialDialogue = DialogueDao.getDialogue(npc.getId(), 1, 1);
		
		// if there's no dialogue, maybe there's a simple message from the npc
		if (initialDialogue == null) {
			objectId = npc.getInstanceId();
			ArrayList<String> messages = NpcMessageDao.getMessagesByNpcId(npc.getId()); 
			if (!messages.isEmpty()) {
				message = messages.get(RandomUtil.getRandom(0, messages.size()));
				responseMaps.addLocalResponse(player.getTileId(), this);
			}
			return;
		}
		
		player.setCurrentDialogue(initialDialogue);
		
		DialogueResponse dialogue = new DialogueResponse();
		dialogue.setDialogue(initialDialogue.getDialogue());
		dialogue.setSpeaker(NPCDao.getNpcNameById(npc.getId()));
		responseMaps.addClientOnlyResponse(player, dialogue);
	}
	
}
