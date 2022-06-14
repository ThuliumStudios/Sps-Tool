package com.sps.bullhorn;

import javafx.scene.layout.GridPane;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BullhornUtility extends GridPane {

	private BullhornAPI bullhorn;
	
	public BullhornUtility(BullhornAPI bullhorn) {
		this.bullhorn = bullhorn;
	}

	public Set<String> getFieldSet(String... fields) {
		Set<String> fieldSet = new HashSet<>();
		fieldSet.addAll(List.of(fields));
		return fieldSet;
	}
	
	public BullhornAPI getBullhorn() {
		return bullhorn;
	}
}
