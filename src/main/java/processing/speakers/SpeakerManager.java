package processing.speakers;

import java.util.HashMap;
import java.util.Map;

import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import processing.attackable.Player;
import responses.ResponseMaps;

public class SpeakerManager {
	private static Map<Integer, Speaker> speakers = new HashMap<>();
	static {
		speakers.put(26, new Tybalt());
		speakers.put(58, new Alaina());
		speakers.put(59, new Clothilda());
		speakers.put(62, new Realtor());
	}
	
	public static void preShowDialogue(NpcDialogueDto dto, Player player, ResponseMaps responseMaps) {
		if (speakers.containsKey(dto.getNpcId()))
			speakers.get(dto.getNpcId()).preShowDialogue(dto, player, responseMaps);
	}
	
	public static void postShowDialogue(NpcDialogueDto dto, Player player, ResponseMaps responseMaps) {
		if (speakers.containsKey(dto.getNpcId()))
			speakers.get(dto.getNpcId()).postShowDialogue(dto, player, responseMaps);
	}
	
	public static boolean dialogueOptionMeetsDisplayCriteria(NpcDialogueOptionDto dto, Player player) {
		if (!speakers.containsKey(dto.getNpcId()))
			return true;
		return speakers.get(dto.getNpcId()).dialogueOptionMeetsDisplayCriteria(dto, player);
	}
	
	public static NpcDialogueDto switchDialogue(NpcDialogueDto previousDialogueDto, Player player, ResponseMaps responseMaps) {
		if (previousDialogueDto != null && speakers.containsKey(previousDialogueDto.getNpcId()))
			return speakers.get(previousDialogueDto.getNpcId()).switchDialogue(previousDialogueDto, player, responseMaps);
		return null;
	}
}
