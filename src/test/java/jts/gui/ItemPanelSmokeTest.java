package jts.gui;

/*
  Smoke tests for the item slot panel. Constructs the panel headless and
  verifies the slot-button structure plus the selection highlight behavior
  (highlight follows the selected slot, previous highlight is reset).
*/

import static org.junit.Assert.*;

import java.awt.Color;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import jts.data.Actor;
import jts.data.Item;
import jts.data.Mercenary;
import jts.data.SaveGame;
import org.junit.Test;

public class ItemPanelSmokeTest
{
    static {
	System.setProperty("java.awt.headless", "true");
    }

    private static final Color HIGHLIGHT = selectionHighlight();

    private static Color selectionHighlight()
    {
	Color color = UIManager.getColor("List.selectionBackground");
	return (color != null) ? color : new Color(75, 110, 175);
    }

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

    private ItemDetailPanel findDetailPanel(ItemPanel panel)
    {
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof ItemDetailPanel ) {
		return (ItemDetailPanel) c;
	    }
	}
	return null;
    }

    private JChoiceView findItemChoice(ItemDetailPanel panel)
    {
	// The Item ID combobox is the first JChoiceView in the detail panel
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof JChoiceView ) {
		return (JChoiceView) c;
	    }
	}
	return null;
    }

    private boolean hasChoice(JChoiceView view, String name)
    {
	for( int idx = 0; idx < view.getItemCount(); ++idx ) {
	    if( name.equals(view.getItemAt(idx)) ) {
		return true;
	    }
	}
	return false;
    }

    @Test
    public void constructsWithExpectedStructure()
    {
	ItemPanel panel = new ItemPanel(null);
	assertTrue("panel should be a Swing container", panel instanceof JPanel);

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

    @Test
    public void armorSlotClickFiltersDetailItemChoice()
    {
	ItemPanel panel = new ItemPanel(null);
	ItemDetailPanel detail = findDetailPanel(panel);
	JChoiceView idChoice = findItemChoice(detail);

	// Headgear slot 1 (view 0 -> Mercenary.HEADGEAR_1_INDEX): only headgear
	panel.doItemSelected(findView(panel, 0));
	assertTrue("headgear items must be offered", hasChoice(idChoice, "Gas Mask"));
	assertFalse("weapons must be filtered out", hasChoice(idChoice, "Glock 17"));

	// Headgear slot 2 (view 2 -> Mercenary.HEADGEAR_2_INDEX): only headgear
	panel.doItemSelected(findView(panel, 2));
	assertTrue("headgear items must be offered", hasChoice(idChoice, "Gas Mask"));
	assertFalse("weapons must be filtered out", hasChoice(idChoice, "Glock 17"));

	// Helmet slot (view 1 -> Mercenary.HELMET_INDEX): only helmets offered
	panel.doItemSelected(findView(panel, 1));
	assertTrue("helmet items must be offered", hasChoice(idChoice, "Steel Helmet"));
	assertFalse("weapons must be filtered out", hasChoice(idChoice, "Glock 17"));

	// Leg armor slot (view 4 -> Mercenary.LEG_ARMOR_INDEX): only leggings offered
	panel.doItemSelected(findView(panel, 4));
	assertTrue("leg armor items must be offered", hasChoice(idChoice, "Kevlar Leggings"));
	assertFalse("weapons must be filtered out", hasChoice(idChoice, "Glock 17"));

	// Right hand slot (view 5 -> Mercenary.RIGHT_HAND_INDEX): unfiltered
	panel.doItemSelected(findView(panel, 5));
	assertTrue("unfiltered slots keep the full list", hasChoice(idChoice, "Glock 17"));
    }

    /** The detail number fields must shrink gradually when the panel is
     * narrower than preferred, not snap to their tiny minimum (the
     * regression that made them look empty). */
    @Test
    public void detailNumberFieldsStayUsableWhenPanelNarrows() throws Exception
    {
	SaveGame sg = new SaveGame();
	File sample = new File(ItemPanelSmokeTest.class.getResource(
	    "/savegames/straciatella_4squad.sav").toURI());
	sg.load(sample.getPath());
	Mercenary merc = sg.getMerc(0);
	Actor actor = sg.getActorByNick(merc.get("Nickname"));

	ItemPanel panel = new ItemPanel(null);
	panel.setActor(actor, merc);

	// Select a slot that holds a real item
	ItemView target = null;
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof ItemView ) {
		ItemView v = (ItemView) c;
		Item it = merc.items[v.getIndex()];
		if( it != null && it.getInt("Item ID") != 0 ) {
		    target = v;
		    break;
		}
	    }
	}
	assertNotNull("sample save should contain a real item", target);
	panel.doItemSelected(target);

	ItemDetailPanel detail = findDetailPanel(panel);
	panel.setSize(panel.getPreferredSize());
	panel.doLayout();
	detail.doLayout();

	// Narrow the panel moderately; every number field must stay usable
	panel.setSize(panel.getWidth() - 50, panel.getHeight());
	panel.doLayout();
	detail.doLayout();
	for( java.awt.Component c : detail.getComponents() ) {
	    if( c instanceof JNumberView) {
		assertTrue("number field '" + ((JNumberView) c).getFieldName()
			+ "' must not snap to minimum", c.getWidth() >= 20);
	    }
	}
    }
}
