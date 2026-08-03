package jts;

/*
  Smoke tests for the Swing item detail panel. These construct the panel
  headless (no display needed) and drive the real data flow: refresh,
  commit-on-edit, invalid-input revert, item switching and per-category
  enable/disable.
*/

import static org.junit.Assert.*;

import javax.swing.*;

import org.junit.Test;

public class ItemDetailPanelSmokeTest
{
    static {
	// The AWT/Swing components used here are all headless-safe; the
	// panel itself must never require a display in these tests.
	System.setProperty("java.awt.headless", "true");
    }

    private Item makeItem(int id, int quantity)
    {
	byte[] data = new byte[Item.ITEM_LENGTH];
	Item item = new Item(data);
	item.setInt("Item ID", id);
	item.setInt("Quantity", quantity);
	return item;
    }

    /** The nth JChoiceView child (0-based), in construction order. */
    private JChoiceView findChoice(ItemDetailPanel panel, int which)
    {
	int seen = 0;
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof JChoiceView ) {
		if( seen == which ) {
		    return (JChoiceView) c;
		}
		++seen;
	    }
	}
	return null;
    }

    /** The nth JNumberView child (0-based), in construction order. */
    private JNumberView findNumber(ItemDetailPanel panel, int which)
    {
	int seen = 0;
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof JNumberView ) {
		if( seen == which ) {
		    return (JNumberView) c;
		}
		++seen;
	    }
	}
	return null;
    }

    @Test
    public void constructsWithSwingComponents()
    {
	ItemDetailPanel panel = new ItemDetailPanel(null);
	assertTrue("panel should be a Swing container", panel instanceof JPanel);
	assertTrue("panel should have children", panel.getComponentCount() > 0);

	int labels = 0, numbers = 0, choices = 0;
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof JLabel ) ++labels;
	    if( c instanceof JNumberView ) ++numbers;
	    if( c instanceof JChoiceView ) ++choices;
	}
	assertEquals("expected 18 labels", 18, labels);
	assertEquals("expected 16 number views", 16, numbers);
	assertEquals("expected 6 choice views", 6, choices);
    }

    @Test
    public void refreshShowsItem()
    {
	ItemDetailPanel panel = new ItemDetailPanel(null);
	panel.setItem(makeItem(1, 2)); // Glock 17, qty 2

	assertEquals("Glock 17", findChoice(panel, 0).getSelectedItem());
	assertEquals("2", findNumber(panel, 0).getText());
    }

    @Test
    public void editCommitsToStruct()
    {
	ItemDetailPanel panel = new ItemDetailPanel(null);
	Item item = makeItem(1, 2);
	panel.setItem(item);

	findNumber(panel, 0).setText("5");
	assertEquals(5, item.getInt("Quantity"));
	assertTrue("panel should be marked modified", panel.isModified());
    }

    @Test
    public void invalidInputRevertsToLastValidValue() throws Exception
    {
	ItemDetailPanel panel = new ItemDetailPanel(null);
	Item item = makeItem(1, 2);
	panel.setItem(item);

	findNumber(panel, 0).setText("5");
	findNumber(panel, 0).setText("abc");

	// The revert is deferred via SwingUtilities.invokeLater; flush the queue
	SwingUtilities.invokeAndWait(new Runnable() { public void run() {} });

	int shown = Integer.parseInt(findNumber(panel, 0).getText());
	assertEquals("field should show the last valid value", item.getInt("Quantity"), shown);
    }

    @Test
    public void itemSwitchUpdatesDetails()
    {
	ItemDetailPanel panel = new ItemDetailPanel(null);
	Item item = makeItem(1, 2);
	panel.setItem(item);

	findChoice(panel, 0).setSelectedItem("Beretta 92F");
	assertEquals(3, item.getInt("Item ID"));
	assertEquals("weapon quantity should reset to 1", "1", findNumber(panel, 0).getText());
    }

    @Test
    public void categoryEnablesRelevantFields()
    {
	ItemDetailPanel panel = new ItemDetailPanel(null);

	// Weapon: ammo fields enabled, money disabled
	panel.setItem(makeItem(1, 2));
	assertTrue(findNumber(panel, 7).isEnabled());  // Ammo Quantity
	assertFalse(findNumber(panel, 15).isEnabled()); // Money Value

	// Money: money enabled, ammo disabled
	panel.setItem(makeItem(0x00DB, 1));
	assertTrue(findNumber(panel, 15).isEnabled());
	assertFalse(findNumber(panel, 7).isEnabled());
    }
}
