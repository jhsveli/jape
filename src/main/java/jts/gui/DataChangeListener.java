package jts.gui;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


import java.util.EventListener;

public interface DataChangeListener extends EventListener {
    void dataChanged(DataChangeEvent event);
}
