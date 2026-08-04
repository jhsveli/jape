package jts.data;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


public interface Structure {
    void chain(Structure nextStructure);

    String get(String name);

    int getInt(String name);

    void set(String name, String value);

    void setInt(String name, int value);
}

