package main.responses;

import main.database.SmithableDto;
import main.processing.Player;
import main.requests.Request;

import java.util.ArrayList;

import lombok.Setter;

public class ShowSmithingTableResponse extends Response { 
	@Setter private int oreId;
	@Setter private ArrayList<SmithableDto> smithingOptions;
	@Setter private int storedCoal;
	public ShowSmithingTableResponse() {
		setAction("show_smithing_table");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
