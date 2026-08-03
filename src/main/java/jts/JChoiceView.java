package jts;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/

 

import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;

/** A Swing choice field view. Swing replacement for ChoiceView. */
public class JChoiceView extends JComboBox implements FieldView, ItemListener
{
    private boolean suppressEvents = false;
    private String fieldName;
    private boolean modified = false;
    private String oldValue;
    private Structure struct;

    public JChoiceView(String fieldName, Vector choices) 
    {
	super();
	this.fieldName = fieldName;
	for( int idx = 0; idx < choices.size(); ++idx ) {
	    this.addItem((String) choices.elementAt(idx));
	}
	// Match AWT Choice behavior: the first item is initially selected
	this.setSelectedIndex(0);
	this.addItemListener(this);
    }

    public String getFieldName() { return this.fieldName; }
    public boolean isModified() { return this.modified; }
    public String getValue() { return (String) this.getSelectedItem(); }
    public void setStruct(Structure struct) { this.struct = struct; }

    /** Replace the dropdown contents without firing change events, keeping
     * the current value selected when it is still offered. */
    public void setChoices(Vector choices) 
    {
	try {
	    this.suppressEvents = true;
	    this.removeAllItems();
	    for( int idx = 0; idx < choices.size(); ++idx ) {
		this.addItem((String) choices.elementAt(idx));
	    }
	    String value = (this.struct == null) ? null : this.struct.get(this.fieldName);
	    if( value != null ) {
		this.selectValue(value);
	    } else {
		this.setSelectedIndex(0);
	    }
	} finally {
	    this.suppressEvents = false;
	}
    }

    public void itemStateChanged(ItemEvent event)
    {
	// Ignore programmatic changes
	if( this.suppressEvents == true ) {
	    return;
	}

	// Get new value
	String newValue = (String) this.getSelectedItem();

	// Attempt to save value
	this.commit(newValue);

	// Success. Save new value and notify listeners
	this.oldValue = newValue;
    }	

    public void commit(String newValue) 
    {
	// Look for the all structs with named field
	if( this.struct != null )
	{
	    String oldValue = struct.get(this.fieldName);
	    if( (oldValue == null && (newValue != null)) || 
		(oldValue != null && (! oldValue.equals(newValue))) ) 
	    {
		this.modified = true;
		struct.set(this.fieldName, newValue);
		this.fireDataChangeEvent(new DataChangeEvent(this, this.oldValue, newValue));
	    }
	}
    }

    public synchronized void refresh() 
    {
	try {
	    this.suppressEvents = true;

	    // Look for the first struct with named field
	    String value = null;
	    if( this.struct != null ) 
	    {
		value = struct.get(this.fieldName);
	    }
	    
	    // Update gui
	    if( value != null ) {
		this.selectValue(value);
	    } else {
		this.setSelectedIndex(0);
	    }
	    this.oldValue = value;
	    
	    // Update state machine
	    this.modified = false;
	} finally {
	    this.suppressEvents = false;
	}
    }

    private void selectValue(String value)
    {
	// Select the matching item, or fall back to the first item
	for( int idx = 0; idx < this.getItemCount(); ++idx ) {
	    if( value.equals(this.getItemAt(idx)) ) {
		this.setSelectedIndex(idx);
		return;
	    }
	}
	this.setSelectedIndex(0);
    }

    // Data Change event processing
    private DataChangeMixin mixin = new DataChangeMixin();

    public void addDataChangeListener(DataChangeListener l) {
	this.mixin.addDataChangeListener(l);
    }

    public void removeDataChangeListener(DataChangeListener l) {
	this.mixin.removeDataChangeListener(l);
    }

    public void fireDataChangeEvent(DataChangeEvent e) {
	this.mixin.fireDataChangeEvent(e);
    }
}
