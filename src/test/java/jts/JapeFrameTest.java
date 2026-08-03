package jts;

/*
  Tests for the JapeFrame actor tree: the Roster / Recruitable /
  Other actors grouping built from a save game's MERCPROFILE list.
  Exercises the static tree-building and classification logic
  headless, without constructing the AWT frame.
*/

import static org.junit.Assert.*;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

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
	    assertEquals(JapeFrame.ROSTER, JapeFrame.classify(sg, node.index));
	}
	for (int i = 0; i < recruitable.getChildCount(); ++i) {
	    JapeFrame.ActorNode node = (JapeFrame.ActorNode)
		((DefaultMutableTreeNode) recruitable.getChildAt(i)).getUserObject();
	    assertEquals(JapeFrame.RECRUITABLE, JapeFrame.classify(sg, node.index));
	}
    }

    @Test
    public void leafPayloadPointsAtProfile() throws Exception {
	DefaultMutableTreeNode root = JapeFrame.buildActorTree(load(EMPTY));
	DefaultMutableTreeNode recruitable =
	    (DefaultMutableTreeNode) root.getChildAt(1);
	JapeFrame.ActorNode node = (JapeFrame.ActorNode)
	    ((DefaultMutableTreeNode) recruitable.getChildAt(0)).getUserObject();
	assertEquals(0, node.index);
	assertEquals("Barry", node.nickname);
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

	assertEquals("tree column must not collapse after load",
		     treeWidth, actorScroll.getWidth());
	assertEquals("stat column must not collapse after load",
		     statWidth, statPanel.getWidth());
    }
}
