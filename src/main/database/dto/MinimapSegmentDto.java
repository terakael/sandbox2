package main.database.dto;

import lombok.AllArgsConstructor;

@SuppressWarnings("unused")
@AllArgsConstructor
public class MinimapSegmentDto {
	private int floor;
	private int segment;
	private String dataBase64;
}