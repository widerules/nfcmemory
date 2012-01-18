/*
 * Copyright (c) 2012 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.imvs.nfc.memory;

public class Card {
	public boolean isTaken;

	private int imageId;

	public Card(int CardImageId) {
		imageId = CardImageId;
		isTaken = false;
	}

	public int getImageId() {
		return imageId;
	}

	public void setCardImageId(int ImageId) {
		imageId = ImageId;
	}
}
