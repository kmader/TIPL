package tipl.ij.volviewer;
/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

import java.awt.Color;

class TextField {
	private String text = "";
	private Color color;

	private double xpos;
	private double ypos;
	private double z;
	private int fontNr;

	public TextField(String text, Color color, int xpos, int ypos, int z) {
		this.setText(text);
		this.setColor(color);
		this.setXpos(xpos);
		this.setYpos(ypos);
		this.setZ(z);
		this.setFontNr(0);
	}

	public TextField(String text, Color color, int xpos, int ypos, int z, int fontNr) {
		this.setText(text);
		this.setColor(color);
		this.setXpos(xpos);
		this.setYpos(ypos);
		this.setZ(z);
		this.setFontNr(fontNr);
	}

	public TextField() {
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setXpos(double xpos) {
		this.xpos = xpos;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public Color getColor() {
		return color;
	}

	public double getZ() {
		return z;
	}

	public int getFontNr() {
		return fontNr;
	}

	public void setFontNr(int fontNr) {
		this.fontNr = fontNr;
	}

	public double getXpos() {
		return xpos;
	}

	public String getText() {
		return text;
	}

	public double getYpos() {
		return ypos;
	}

	public void setYpos(double ypos) {
		this.ypos = ypos;
	}
}
