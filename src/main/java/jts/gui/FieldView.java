package jts.gui;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


import jts.data.Structure;

public interface FieldView {
    String getFieldName();

    String getValue();

    boolean isModified();

    void setStruct(Structure struct);

    void refresh();

    void addDataChangeListener(DataChangeListener l);

    void removeDataChangeListener(DataChangeListener l);

    void fireDataChangeEvent(DataChangeEvent e);
}
