package jts.gui;

/*
  Tests for the JapeFrame actor tree: the Roster / Recruitable /
  Other actors grouping built from a save game's MERCPROFILE list.
  Exercises the static tree-building and classification logic
  headless, without constructing the AWT frame.
*/

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import jts.data.Actor;
import jts.data.Item;
import jts.data.Mercenary;
import jts.data.SaveGame;
import org.junit.Test;

public class JapeFrameTest
{
    static {
	System.setProperty("java.awt.headless", "true");
    }

    private static final String EMPTY = "/savegames/version102_empty.sav";
    private static final String FOUR_SQUAD = "/savegames/straciatella_4squad.sav";

    /**
     * The hand-sorted recruitable order: profiles 0..50 (the AIM and
     * MERC rosters, down to and including Bubba; the profile table
     * lists Larry twice), then the extra nicknames.  Profile 64 stores
     * Slay under the nickname "Terry", so it displays as Terry.
     */
    private static final String[] RECRUITABLE_ORDER = {
	"Barry", "Blood", "Lynx", "Grizzly", "Vicki", "Trevor", "Grunty",
	"Ivan", "Steroid", "Igor", "Shadow", "Red", "Reaper", "Fidel",
	"Fox", "Sidney", "Gus", "Buns", "Ice", "Spider", "Cliff", "Bull",
	"Hitman", "Buzz", "Raider", "Raven", "Static", "Len", "Danny",
	"Magic", "Stephen", "Scully", "Malice", "Dr. Q", "Nails", "Thor",
	"Scope", "Wolf", "MD", "Meltdown", "Biff", "Haywire", "Gasket",
	"Razor", "Flo", "Gumpy", "Larry", "Larry", "Cougar", "Numb", "Bubba",
	"Ira", "Dimitri", "Carlos", "Miguel", "Iggy", "Maddog", "Vince",
	"Devin", "Robot", "Hamous", "Dynamo", "Shank", "Terry", "Mike",
	"Conrad"
    };

    private static SaveGame load(String resource) throws Exception {
	SaveGame sg = new SaveGame();
	File sample = new File(JapeFrameTest.class.getResource(resource).toURI());
	sg.load(sample.getPath());
	return sg;
    }

    private static List<String> leafLabels(DefaultMutableTreeNode group) {
	List<String> labels = new ArrayList<String>();
	for (int i = 0; i < group.getChildCount(); ++i) {
	    labels.add(group.getChildAt(i).toString());
	}
	return labels;
    }

    @Test
    public void nullSaveBuildsThreeEmptyGroups() {
	DefaultMutableTreeNode root = JapeFrame.buildActorTree(null);
	assertEquals(3, root.getChildCount());
	for (int i = 0; i < root.getChildCount(); ++i) {
	    assertEquals(0, root.getChildAt(i).getChildCount());
	}
    }

    @Test
    public void treeHasThreeGroupsInOrder() throws Exception {
	DefaultMutableTreeNode root = JapeFrame.buildActorTree(load(EMPTY));
	assertEquals(3, root.getChildCount());
	assertEquals(JapeFrame.ROSTER_GROUP,
		     ((DefaultMutableTreeNode) root.getChildAt(0)).getUserObject());
	assertEquals(JapeFrame.RECRUITABLE_GROUP,
		     ((DefaultMutableTreeNode) root.getChildAt(1)).getUserObject());
	assertEquals(JapeFrame.OTHER_GROUP,
		     ((DefaultMutableTreeNode) root.getChildAt(2)).getUserObject());
    }

    @Test
    public void recruitableGroupFollowsHandSortedOrder() throws Exception {
	DefaultMutableTreeNode root = JapeFrame.buildActorTree(load(EMPTY));
	DefaultMutableTreeNode recruitable =
	    (DefaultMutableTreeNode) root.getChildAt(1);
	assertEquals(RECRUITABLE_ORDER.length, recruitable.getChildCount());
	assertEquals(Arrays.asList(RECRUITABLE_ORDER), leafLabels(recruitable));
    }

    @Test
    public void everyProfileIsClassified() throws Exception {
	SaveGame sg = load(EMPTY);
	int[] counts = new int[3];
	for (int idx = 0; idx < sg.actorCount; ++idx) {
	    counts[JapeFrame.classify(sg, idx)]++;
	}
	assertEquals("empty save has no active mercs", 0, counts[JapeFrame.ROSTER]);
	assertEquals(RECRUITABLE_ORDER.length, counts[JapeFrame.RECRUITABLE]);
	assertEquals(sg.actorCount - RECRUITABLE_ORDER.length,
		     counts[JapeFrame.OTHER]);
    }

    @Test
    public void recruitableListConstantMatchesExtractedList() {
	// 51 profiles down to Bubba (one Larry duplicated in the table)
	// plus 15 hand-picked extras.
	assertEquals(65, JapeFrame.RECRUITABLE_NICKNAMES.length);
    }

    @Test
    public void activeMercsWinOverRecruitableList() throws Exception {
	SaveGame sg = load(FOUR_SQUAD);
	DefaultMutableTreeNode root = JapeFrame.buildActorTree(sg);
	DefaultMutableTreeNode roster =
	    (DefaultMutableTreeNode) root.getChildAt(0);
	DefaultMutableTreeNode recruitable =
	    (DefaultMutableTreeNode) root.getChildAt(1);

	// Roster leaves are exactly the profiles classified ROSTER
	// (Bull, MD, Igor, Grunty, Pacos, Fatima in the sample; the
	// generic SOLDIER records have no matching profile).
	int expectedRoster = 0;
	for (int idx = 0; idx < sg.actorCount; ++idx) {
	    if (JapeFrame.classify(sg, idx) == JapeFrame.ROSTER) {
		expectedRoster++;
	    }
	}
	assertTrue(expectedRoster > 0);
	assertEquals(expectedRoster, roster.getChildCount());

	for (int i = 0; i < roster.getChildCount(); ++i) {
	    JapeFrame.ActorNode node = (JapeFrame.ActorNode)
		((DefaultMutableTreeNode) roster.getChildAt(i)).getUserObject();
	    assertEquals(JapeFrame.ROSTER, JapeFrame.classify(sg, node.index()));
	}
	for (int i = 0; i < recruitable.getChildCount(); ++i) {
	    JapeFrame.ActorNode node = (JapeFrame.ActorNode)
		((DefaultMutableTreeNode) recruitable.getChildAt(i)).getUserObject();
	    assertEquals(JapeFrame.RECRUITABLE, JapeFrame.classify(sg, node.index()));
	}
    }

    @Test
    public void leafPayloadPointsAtProfile() throws Exception {
	DefaultMutableTreeNode root = JapeFrame.buildActorTree(load(EMPTY));
	DefaultMutableTreeNode recruitable =
	    (DefaultMutableTreeNode) root.getChildAt(1);
	JapeFrame.ActorNode node = (JapeFrame.ActorNode)
	    ((DefaultMutableTreeNode) recruitable.getChildAt(0)).getUserObject();
	assertEquals(0, node.index());
	assertEquals("Barry", node.nickname());
    }

    /** Mirrors JapeFrame's body layout (tree scroll | stat | item) and
     * checks that loading a save does not collapse the tree and stat
     * columns: the frame is packed with the empty tree and not resized
     * when a save is loaded. */
    @Test
    public void bodyColumnsKeepWidthWhenSaveLoads() throws Exception {
	JPanel body = new JPanel(new GridBagLayout());

	JTree tree = new JTree(new DefaultTreeModel(JapeFrame.buildActorTree(null)));
	tree.getSelectionModel().setSelectionMode(
	    TreeSelectionModel.SINGLE_TREE_SELECTION);
	tree.setRootVisible(false);
	tree.setShowsRootHandles(true);
	JScrollPane actorScroll = new JScrollPane(tree);
	// Mirrors JapeFrame: edge spacing on the tree column and a floor on
	// its width so it cannot collapse to the tiny default minimum.
	actorScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
	actorScroll.setMinimumSize(new Dimension(135, 0));
	StatPanel statPanel = new StatPanel(null);
	ItemPanel itemPanel = new ItemPanel(null);

	GridBagConstraints c1 = new GridBagConstraints();
	c1.fill = c1.BOTH; c1.gridx = 0; c1.gridy = 0; c1.weighty = 1; c1.weightx = 0;
	body.add(actorScroll, c1);
	GridBagConstraints c2 = new GridBagConstraints();
	c2.fill = c2.BOTH; c2.anchor = c2.NORTHWEST; c2.gridx = 1; c2.gridy = 0;
	c2.weighty = 1; c2.weightx = 1;
	body.add(statPanel, c2);
	GridBagConstraints c3 = new GridBagConstraints();
	c3.fill = c3.BOTH; c3.anchor = c3.NORTHWEST; c3.gridx = 2; c3.gridy = 0;
	c3.weighty = 1; c3.weightx = 1;
	body.add(itemPanel, c3);

	// Constructor: frame packed with the empty tree
	body.setSize(body.getPreferredSize());
	body.doLayout();
	int treeWidth = actorScroll.getWidth();
	int statWidth = statPanel.getWidth();
	assertTrue("tree column should have a usable width", treeWidth > 50);
	assertTrue("stat column should have a usable width", statWidth > 100);

	// Load a save: model swap + bound actors, frame size unchanged
	SaveGame sg = load(FOUR_SQUAD);
	tree.setModel(new DefaultTreeModel(JapeFrame.buildActorTree(sg)));
	Actor actor = sg.getActor(0);
	Mercenary merc = sg.getMercByNick(actor.get("Nickname"));
	statPanel.setActor(actor, merc);
	itemPanel.setActor(actor, merc);
	body.doLayout();

	assertTrue("tree column must not collapse after load",
		   actorScroll.getWidth() >= 100);
	assertTrue("stat column must not collapse after load",
		   statPanel.getWidth() >= 100);
	assertTrue("item column must not collapse after load",
		   itemPanel.getWidth() >= 100);

	// Window narrower than preferred: the tree column floors at its
	// minimum instead of collapsing to ~0, and the stat input fields
	// shrink gradually instead of snapping to their 5px minimum.
	body.setSize(body.getPreferredSize().width - 200, body.getHeight());
	body.doLayout();
	statPanel.doLayout(); // body layout only sizes the panel, not its fields
	assertTrue("tree column must stay usable when window shrinks",
		     actorScroll.getWidth() >= 120);
	assertTrue("stat column must stay usable when window shrinks",
		     statPanel.getWidth() > 100);
	assertTrue("stat input fields must stay usable when window shrinks",
		     statFieldWidth(statPanel, 2) > 8); // Health
    }

    /** Width of the nth JNumberView child of the panel (0-based). */
    private static int statFieldWidth(StatPanel panel, int which)
    {
	int seen = 0;
	for( java.awt.Component c : panel.getComponents() ) {
	    if( c instanceof JNumberView) {
		if( seen == which ) {
		    return c.getWidth();
		}
		++seen;
	    }
	}
	return -1;
    }

    @Test
    public void toolbarHasOpenSaveCopyPasteAndDefaultAutosave() {
	ActionListener noop = new ActionListener() {
	    public void actionPerformed(ActionEvent e) { }
	};
	JapeFrame.ToolbarControls controls =
	    JapeFrame.buildToolBar(noop, noop, noop, noop, noop, noop);
	JToolBar toolBar = controls.toolBar;
	JCheckBox autosave = controls.autosaveCheckbox;

	List<String> labels = new ArrayList<String>();
	for( Component c : toolBar.getComponents() ) {
	    if( c instanceof JButton ) {
		labels.add(((JButton) c).getText());
	    }
	}
	assertEquals(Arrays.asList("Open...", "Save", "Copy Equipped",
				  "Paste Equipped", "Copy All", "Paste All"),
		     labels);
	assertTrue("autosave must be enabled by default", autosave.isSelected());
    }

    @Test
    public void autosaveRuleRequiresCheckboxAndOpenSave() {
	assertTrue(JapeFrame.autosaveActive(true, new SaveGame()));
	assertFalse(JapeFrame.autosaveActive(false, new SaveGame()));
	assertFalse(JapeFrame.autosaveActive(true, null));
    }

    @Test
    public void fileChangedRule() {
	// Same timestamp and length: unchanged
	assertFalse(JapeFrame.fileChanged(1000, 10, 1000, 10));
	// Either a different timestamp or a different length counts
	assertTrue(JapeFrame.fileChanged(1000, 10, 1001, 10));
	assertTrue(JapeFrame.fileChanged(1000, 10, 1000, 11));
    }

    @Test
    public void copyPasteAllItemsReplicatesSlots() throws Exception {
	SaveGame sg = load(FOUR_SQUAD);
	Mercenary source = sg.getMerc(0);
	Mercenary target = sg.getMerc(1);

	byte[][] clip = JapeFrame.snapshotItems(source, JapeFrame.ALL_ITEM_SLOTS);
	// the sample save must contain at least one real item to copy
	boolean foundReal = false;
	for( byte[] data : clip ) {
	    if( data != null && new Item(data).getInt("Item ID") != 0 ) {
		foundReal = true;
	    }
	}
	assertTrue("sample save should contain a real item", foundReal);

	// wipe the target, then paste
	for( int i = 0; i < JapeFrame.ALL_ITEM_SLOTS.length; ++i ) {
	    target.items[i] = new Item(new byte[Item.ITEM_LENGTH]);
	}
	JapeFrame.pasteItems(target, JapeFrame.ALL_ITEM_SLOTS, clip);

	for( int i = 0; i < JapeFrame.ALL_ITEM_SLOTS.length; ++i ) {
	    byte[] expected = (clip[i] == null) ? new byte[Item.ITEM_LENGTH] : clip[i];
	    assertArrayEquals("slot " + i, expected, target.items[i].encode());
	}
    }

    @Test
    public void copyPasteEquippedReplicatesLoadout() throws Exception {
	SaveGame sg = load(FOUR_SQUAD);
	Mercenary source = sg.getMerc(0);
	Mercenary target = sg.getMerc(1);

	byte[][] clip = JapeFrame.snapshotItems(source, JapeFrame.EQUIPPED_SLOTS);
	assertEquals(JapeFrame.EQUIPPED_SLOTS.length, clip.length);
	JapeFrame.pasteItems(target, JapeFrame.EQUIPPED_SLOTS, clip);

	for( int i = 0; i < JapeFrame.EQUIPPED_SLOTS.length; ++i ) {
	    int slot = JapeFrame.EQUIPPED_SLOTS[i];
	    byte[] expected = (clip[i] == null) ? new byte[Item.ITEM_LENGTH] : clip[i];
	    assertArrayEquals("equipped slot " + slot, expected,
			      target.items[slot].encode());
	}
    }
}
