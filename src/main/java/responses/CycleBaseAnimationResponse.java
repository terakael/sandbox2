package responses;

import java.util.Collections;

import database.dao.AnimationDao;
import database.dao.BaseAnimationsDao;
import database.dto.AnimationDto;
import database.dto.PlayerAnimationDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.CycleBaseAnimationRequest;
import requests.Request;
import types.PlayerPartType;

public class CycleBaseAnimationResponse extends Response {
	private PlayerPartType type = null;
	private PlayerAnimationDto animation = null;
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof CycleBaseAnimationRequest))
			return;
		
		final CycleBaseAnimationRequest request = (CycleBaseAnimationRequest)req;
		
		try {
			type = PlayerPartType.valueOf(request.getPart().toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			// if the user fucks with the request then this can throw an exception, so just bail silently
			return;
		}
		
		if (!BaseAnimationsDao.getCustomizableParts().contains(type))
			return;
		
		AnimationDto currentAnimationDto = AnimationDao.getAnimationDtoByUpId(request.getCurrentlyDisplayedUpId());
		if (currentAnimationDto == null)
			return;
		
		AnimationDto animationDto = request.getDirection().equals("next")
				? AnimationDao.getAnimationDtoById(BaseAnimationsDao.getNextAnimationId(type, currentAnimationDto.getId()))
				: AnimationDao.getAnimationDtoById(BaseAnimationsDao.getPreviousAnimationId(type, currentAnimationDto.getId()));
		if (animationDto == null)
			return;
		
		animation = new PlayerAnimationDto(
				animationDto.getUpId(), 
				animationDto.getDownId(),
				animationDto.getLeftId(),
				animationDto.getRightId(),
				animationDto.getAttackLeftId(),
				animationDto.getAttackRightId(),
				request.getColor());
		
		ClientResourceManager.addAnimationDtos(player, Collections.singleton(animation));
		
		responseMaps.addClientOnlyResponse(player, this);
	}
}
