package jts;

/*
  Smoke tests for the item slot panel. Constructs the panel headless and
  verifies the slot-button structure plus the selection highlight behavior
  (highlight follows the selected slot, previous highlight is reset).
*/

import static org.junit.Assert.*;

import java.awt.Color;

import javax.swing.JLabel;

import org.junit.Test;

public class ItemPanelSmokeTest
{
    static {
	System.setProperty("java.awt.headless", "true");
    }

    private static final Color HIGHLIGHT = new Color(255, 255, 153);

    private ItemView findView(ItemPanel panel, int which)
    {
	int seen = 0;
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof ItemView ) {
		if( seen == which ) {
		    return (ItemView) c;
		}
		++seen;
	    }
	}
	return null;
    }

    @Test
    public void constructsWithExpectedStructure()
    {
	ItemPanel panel = new ItemPanel(null);

	int views = 0, headings = 0, details = 0;
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof ItemView ) ++views;
	    if( c instanceof JLabel ) ++headings;
	    if( c instanceof ItemDetailPanel ) ++details;
	}
	assertEquals("expected 19 item slot buttons", 19, views);
	assertEquals("expected 4 group headings", 4, headings);
	assertEquals("expected 1 detail panel", 1, details);
    }

    @Test
    public void selectionHighlightsTheClickedSlot()
    {
	ItemPanel panel = new ItemPanel(null);
	ItemView first = findView(panel, 0);
	ItemView second = findView(panel, 1);

	panel.doItemSelected(first);
	assertEquals("clicked slot should be highlighted", HIGHLIGHT, first.getBackground());

	panel.doItemSelected(second);
	assertEquals("new slot should be highlighted", HIGHLIGHT, second.getBackground());
	assertNotEquals("previous highlight must be reset", HIGHLIGHT, first.getBackground());
    }

    @Test
    public void setActorHighlightsFirstSlot()
    {
	ItemPanel panel = new ItemPanel(null);
	ItemView first = findView(panel, 0);

	panel.setActor(null, null);
	assertEquals("first slot should be highlighted after setActor", HIGHLIGHT, first.getBackground());
    }
}
