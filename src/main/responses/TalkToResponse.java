package main.responses;

import java.util.ArrayList;

import main.database.dao.DialogueDao;
import main.database.dao.NPCDao;
import main.database.dao.NpcMessageDao;
import main.database.dto.NpcDialogueDto;
import main.processing.PathFinder;
import main.processing.attackable.NPC;
import main.processing.attackable.Player;
import main.processing.managers.FightManager;
import main.processing.managers.NPCManager;
import main.requests.Request;
import main.requests.TalkToRequest;
import main.utils.RandomUtil;

@SuppressWarnings("unused")
public class TalkToResponse extends Response {
	private int objectId;
	private String message = "";
	
	public TalkToResponse() {
		setAction("talk to");
	}
	
	public TalkToResponse(int objectId, String message) {
		setAction("talk to");
		this.objectId = objectId;
		this.message = message;
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
		NPC npc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getObjectId());// request tileid is the instnace id
		if (npc == null) {
			setRecoAndResponseText(0, "you can't talk to that.");
			return;
		}
		
		if (!PathFinder.isNextTo(player.getFloor(), npc.getTileId(), player.getTileId())) {
			player.setTarget(npc);	
			player.setSavedRequest(request);
		} else {
			// talk to it
			player.faceDirection(npc.getTileId(), responseMaps);
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
				responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), this);
			} else {
				setRecoAndResponseText(0, "they don't seem interested in talking.");
				responseMaps.addClientOnlyResponse(player, this);
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
