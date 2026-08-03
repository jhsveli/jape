package jts;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/

 

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;

public class ItemPanel extends InsetPanel implements DataChangeListener
{
    // Gui Elements
    private Frame parent;
    private GridBagLayout layout = new GridBagLayout();
    private GridBagConstraints constraint = new GridBagConstraints();
    private ItemDetailPanel itemDetailPanel;
    private static final Color SELECTED_BACKGROUND = new Color(255, 255, 153);

    // State machine
    private boolean modified = false;

    // Data sources
    private Actor actor;
    private Mercenary merc;

    // Slot index -> item category the slot accepts; null means any item.
    // First iteration: armor slots only. Headgear/hands/ammo filters will
    // be added to this table in later iterations.
    private static final Integer[] SLOT_CATEGORY = new Integer[Mercenary.ITEM_COUNT];
    static {
	SLOT_CATEGORY[Mercenary.HELMET_INDEX] = Integer.valueOf(ItemExemplar.HELMET_CATEGORY);
	SLOT_CATEGORY[Mercenary.BODY_ARMOR_INDEX] = Integer.valueOf(ItemExemplar.BODY_ARMOR_CATEGORY);
	SLOT_CATEGORY[Mercenary.LEG_ARMOR_INDEX] = Integer.valueOf(ItemExemplar.LEG_ARMOR_CATEGORY);
    }

    private static Integer categoryForSlot(int slotIndex)
    {
	if( slotIndex < 0 || slotIndex >= SLOT_CATEGORY.length ) {
	    return null;
	}
	return SLOT_CATEGORY[slotIndex];
    }

    // Data Views
    private Vector views = new Vector();
    private ItemView currentView;
    private Color defaultBackground;

    // Instance methods
    public ItemPanel(Frame parent)
    {
	super(new Insets(10,10,10,10));
	this.parent = parent;
	this.setLayout(this.layout);

	// Constraints
	this.constraint.anchor = GridBagConstraints.SOUTHWEST;
	this.constraint.fill = GridBagConstraints.BOTH;
	this.constraint.gridx = 0;
	this.constraint.gridheight = 1;
	this.constraint.gridwidth = 1;
	this.constraint.weightx = 1;
	this.constraint.insets = new Insets(0,0,0,0);

	// Add group headings for the equipment slots
	this.newRow();
	this.addHeading(1, "Headgear");
	this.addSpace();
	this.addHeading(1, "Armor");

	// Headgear (first column, rows 1-2)
	this.newRow();
	this.addItem(Mercenary.HEADGEAR_1_INDEX);
	this.addSpace();
	this.addItem(Mercenary.HELMET_INDEX);

	this.newRow();
	this.addItem(Mercenary.HEADGEAR_2_INDEX);
	this.addSpace();
	this.addItem(Mercenary.BODY_ARMOR_INDEX);
	
	// Hands (first column, rows 3-4)
	this.newRow();
	this.addHeading(1, "Hands");
	this.addSpace();
	this.addItem(Mercenary.LEG_ARMOR_INDEX);

	this.newRow();
	this.addItem(Mercenary.RIGHT_HAND_INDEX);

	this.newRow();
	this.addItem(Mercenary.LEFT_HAND_INDEX);

	// Backpack (lower 4 rows by 3 columns)
	this.newRow();
	this.addHeading(3, "Backpack");

	this.newRow();
	this.addItem(Mercenary.BACKPACK_1_1_INDEX);
	this.addItem(Mercenary.BACKPACK_1_2_INDEX);
	this.addItem(Mercenary.BACKPACK_1_3_INDEX);

	this.newRow();
	this.addItem(Mercenary.BACKPACK_2_1_INDEX);
	this.addItem(Mercenary.BACKPACK_2_2_INDEX);
	this.addItem(Mercenary.BACKPACK_2_3_INDEX);

	this.newRow();
	this.addItem(Mercenary.BACKPACK_3_1_INDEX);
	this.addItem(Mercenary.BACKPACK_3_2_INDEX);
	this.addItem(Mercenary.BACKPACK_3_3_INDEX);

	this.newRow();
	this.addItem(Mercenary.BACKPACK_4_1_INDEX);
	this.addItem(Mercenary.BACKPACK_4_2_INDEX);
	this.addItem(Mercenary.BACKPACK_4_3_INDEX);

	// Create item detail panel
	this.newRow();
	this.itemDetailPanel = new ItemDetailPanel(this.parent);
	this.constraint.gridwidth = 3;
	this.constraint.weightx = 1;
	this.constraint.weighty = 1;
	this.add(this.itemDetailPanel, this.constraint);
	this.itemDetailPanel.addDataChangeListener(this);
    }

    private void newRow() 
    {
	// Start a new row
	this.constraint.gridy++;
	this.constraint.gridx = 0;
    }

    private void addComponent(int colwidth, Component component)
    {
	// Add component to panel
	this.constraint.gridwidth = colwidth;
	this.add(component, this.constraint);
	this.constraint.gridx += colwidth;
    }

    private void addSpace() 
    {
	this.constraint.gridx++;
    }

    private void addHeading(int colwidth, String text)
    {
	// Add group heading label
	JLabel label = new JLabel(text, SwingConstants.LEFT);
	this.addComponent(colwidth, label);
    }

    private void addItem(final int index) 
    {
	// Create new button
	final ItemView view = new ItemView(index);
	this.addComponent(1, view);
	this.views.addElement(view);
	view.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doItemSelected(view);
		}});
    }

    public void setActor(Actor actor, Mercenary merc) 
    {
	// Set fields
	this.actor = actor;
	this.merc = merc;

	// Update views from data sources
	for( int idx = 0; idx < this.views.size(); ++idx ) {
	    ItemView view = (ItemView) this.views.elementAt(idx);
	    view.setActor(merc);
	}

	// no.jts.Actor now clean
	this.modified = false;

	// Set detail fields
	this.doItemSelected((ItemView) this.views.elementAt(0));
    }

    public boolean isModified() { return this.itemDetailPanel.isModified(); }
    public void setModified(boolean modified) { 
	this.itemDetailPanel.setModified(modified);
	this.modified = modified; 
    }

    public void doItemSelected(ItemView view) 
    {
	// Get item index
	int index = view.getIndex();
	// Reset the highlight on the previously selected button
	if( this.currentView != null && this.currentView != view ) {
	    this.currentView.setBackground(this.defaultBackground);
	}

	// Save the default button background the first time it is needed,
	// so the highlight can be removed again later
	if( this.defaultBackground == null && this.currentView != view ) {
	    this.defaultBackground = view.getBackground();
	}

	// Highlight the newly selected button
	this.currentView = view;
	view.setBackground(SELECTED_BACKGROUND);

	// Get new item
	Item item = null;
	if( merc != null ) {
	    item = this.merc.items[index];
	}

	// Change to new item
	this.itemDetailPanel.setItem(item, categoryForSlot(index));
    }

    public void dataChanged(DataChangeEvent event) 
    {
	// Extract the control was changed
	FieldView view = (FieldView) event.getSource();
	String oldValue = event.getOldValue();
	String newValue = event.getNewValue();

	// Update views from data sources
	this.currentView.refresh();

	// no.jts.Actor now dirty
	//	System.err.println("no.jts.ItemPanel.doValueChanged(" + view + ",'" + oldValue + "','" + newValue + "')");
	this.modified = true;
	this.fireDataChangeEvent(event);
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
