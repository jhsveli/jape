package jts.data;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


import java.util.Hashtable;

public class ChoiceField implements Field {
    private final Field baseField;

    // TODO: Fix hashtable two-way lookup pattern
    private final Hashtable table;

    // TODO: Fix hashtable two-way lookup pattern
    public ChoiceField(Field baseField, Hashtable table) {
        this.baseField = baseField;
        this.table = table;
    }

    public String get(byte[] data) {
        int value = this.baseField.getInt(data);
        // TODO: Fix hashtable two-way lookup pattern
        return (String) this.table.get(value);
    }

    public int getInt(byte[] data) {
        return this.baseField.getInt(data);
    }

    public void set(byte[] data, String str) throws NumberFormatException {
        // TODO: Fix hashtable two-way lookup pattern
        Integer val = (Integer) this.table.get(str);
        if (val == null) {
            return;
        }
        int value = val;
        this.setInt(data, value);
    }

    public void setInt(byte[] data, int value) throws NumberFormatException {
        this.baseField.setInt(data, value);
    }
}
    
