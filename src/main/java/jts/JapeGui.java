/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/

package jts;

import com.formdev.flatlaf.FlatDarculaLaf;

public class JapeGui {
    private static final int DEFAULT_WIDTH = 130;
    private static final int DEFAULT_HEIGHT = 188;

    public static void main(String[] args) 
    {
	// Install the FlatLaf Darcula look and feel
	FlatDarculaLaf.setup();

	// Create frame
	JapeFrame japeFrame = new JapeFrame();
	
	// Make frame visible
	japeFrame.setVisible(true);

	// Open a save
	//japeFrame.doOpen();
    }
}
