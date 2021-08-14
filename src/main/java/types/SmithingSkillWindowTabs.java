package types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SmithingSkillWindowTabs {
	copper(Items.COPPER_BAR.getValue()),
	iron(Items.IRON_BAR.getValue()),
	steel(Items.STEEL_BAR.getValue()),
	mithril(Items.MITHRIL_BAR.getValue()),
	addy(Items.ADDY_BAR.getValue()),
	gold(Items.GOLD_BAR.getValue()),
	rune(Items.RUNITE_BAR.getValue());
	
	private final int barId;
}
