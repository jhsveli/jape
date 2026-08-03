package jts;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/

 

import java.awt.*;
import javax.swing.*;

/** A panel with specified insets on all four sides. */
public class InsetPanel extends JPanel
{
  public InsetPanel( Insets insets ) {
    super();
    setBorder(BorderFactory.createEmptyBorder(
        insets.top, insets.left, insets.bottom, insets.right));
  }

  public InsetPanel( int top, int left, int bottom, int right ) {
    super();
    setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
  }
}
