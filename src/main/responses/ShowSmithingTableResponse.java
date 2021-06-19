package main.responses;

import java.util.List;

import lombok.Setter;
import main.database.dto.SmithableDto;
import main.processing.Player;
import main.requests.Request;

public class ShowSmithingTableResponse extends Response { 
	@Setter private List<SmithableDto> smithingOptions;
	public ShowSmithingTableResponse() {
		setAction("show_smithing_table");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
