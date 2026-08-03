package no.jts;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/

 

import java.util.*;

public interface DataChangeListener extends EventListener
{
    public void dataChanged(DataChangeEvent event);
}
