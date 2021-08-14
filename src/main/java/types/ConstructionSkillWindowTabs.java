package types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ConstructionSkillWindowTabs {
	fires(-1),
	regular(Items.PLANK.getValue()),
	oak(Items.OAK_PLANK.getValue()),
	willow(Items.WILLOW_PLANK.getValue()),
	maple(Items.MAPLE_PLANK.getValue()),
	yew(Items.YEW_PLANK.getValue()),
	magic(Items.MAGIC_PLANK.getValue())
	;
	
	@Getter private final int plankId;
}
