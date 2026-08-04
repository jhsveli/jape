package jts.data;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


public interface Field {
    String get(byte[] data);

    int getInt(byte[] data);

    void set(byte[] data, String value);

    void setInt(byte[] data, int value);
}
