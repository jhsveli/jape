package jts;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/

 

import com.formdev.flatlaf.FlatDarculaLaf;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.tree.*;
import java.util.HashSet;
import java.util.Set;

public class JapeFrame extends JFrame implements DataChangeListener
{
    private static final int DEFAULT_WIDTH = 450;
    private static final int DEFAULT_HEIGHT = 450;

    // Major GUI elements
    private GridBagLayout layout = new GridBagLayout();
    private JTree     actorTree;
    private StatPanel statPanel;
    private ItemPanel itemPanel;

    // Menu
    private JMenuBar  menuBar;
    private JMenu     fileMenu;
    private JMenuItem openItem;
    private JMenuItem saveItem;
    private JMenuItem closeItem;
    private JMenuItem quitItem;
    private JMenu     helpMenu;
    private JMenuItem aboutItem;

    // The save game engine
    private SaveGame saveGame;
    private boolean saveGameModified;
    private Actor currentActor;
    private Mercenary currentMerc;
    private String currentDir = System.getProperty("user.dir");

    public JapeFrame() 
    {
	super("JAPE");

	// Menu
	this.createMenuBar();

	// Create body panel
	JPanel body = new JPanel();
	body.setLayout(this.layout);
	this.setContentPane(body);

	// Create actor tree
	this.actorTree = new JTree(new DefaultTreeModel(buildActorTree(null)));
	this.actorTree.getSelectionModel().setSelectionMode(
	    TreeSelectionModel.SINGLE_TREE_SELECTION);
	this.actorTree.setRootVisible(false);
	this.actorTree.setShowsRootHandles(true);
	this.actorTree.addTreeSelectionListener(new TreeSelectionListener() {
		public void valueChanged(TreeSelectionEvent e) {
		    doSelectActor();
		}});
	JScrollPane actorScroll = new JScrollPane(this.actorTree);
	// Match the 10px edge spacing of the stat/item panels on the
	// window's left, top, and bottom; the right side stays 0 so the
	// gap to the stat panel is unchanged.
	actorScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
	// Floor the tree column width so it never collapses to its tiny
	// default minimum (~22px) when the window is narrower than the
	// preferred width; nicknames stay readable even at small sizes.
	actorScroll.setMinimumSize(new Dimension(135, 0));
	GridBagConstraints c1 = new GridBagConstraints();
	c1.fill = c1.BOTH;
	c1.gridx = 0;
	c1.gridy = 0;
	c1.weighty = 1;
	c1.weightx = 0;
	body.add(actorScroll, c1);

	// Create stat panel
	this.statPanel = new StatPanel(this);
	this.statPanel.addDataChangeListener(this);
	GridBagConstraints c2 = new GridBagConstraints();
	c2.fill = c2.BOTH;
	c2.anchor = c2.NORTHWEST;
	c2.gridx = 1;
	c2.gridy = 0;
	c2.weighty = 1;
	c2.weightx = 1;
	body.add(this.statPanel, c2);

	// Create item panel
	this.itemPanel = new ItemPanel(this);
	this.itemPanel.addDataChangeListener(this);
	GridBagConstraints c3 = new GridBagConstraints();
	c3.fill = c3.BOTH;
	c3.anchor = c3.NORTHWEST;
	c3.gridx = 2;
	c3.gridy = 0;
	c3.weighty = 1;
	c3.weightx = 1;
	body.add(this.itemPanel, c3);

	// Handle window events
	this.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    doQuit();
		}});

	// Size the frame
	this.addNotify();
	//	Insets frameInsets = this.getInsets();
	//	int width = frameInsets.left + frameInsets.right + DEFAULT_WIDTH;
	//	int height = frameInsets.top + frameInsets.bottom + DEFAULT_HEIGHT;
	//	this.setSize(width, height);
	//	this.setLocation(50,50);

	// Init the panels
	this.populateActorList();

	// Show
	this.pack();
	this.repaint();
    }
    
    private void createMenuBar() {
	// Create menu bar
	this.menuBar = new JMenuBar();

	// File menu
	this.fileMenu = new JMenu("File");
	this.menuBar.add(this.fileMenu);

	this.openItem = new JMenuItem("Open...");
	this.openItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doOpen();
		}});
	this.fileMenu.add(this.openItem);

	this.saveItem = new JMenuItem("Save");
	this.saveItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doSave();
		}});
	this.fileMenu.add(this.saveItem);
	this.saveItem.setEnabled(false);

	this.closeItem = new JMenuItem("Close");
	this.closeItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doClose();
		}});
	this.fileMenu.add(this.closeItem);
	this.closeItem.setEnabled(false);

	this.fileMenu.addSeparator();

	this.quitItem = new JMenuItem("Quit");
	this.quitItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doQuit();
		}});
	this.fileMenu.add(this.quitItem);

	// About menu
	this.helpMenu = new JMenu("Help");
	this.menuBar.add(this.helpMenu);

	this.aboutItem = new JMenuItem("About...");
	this.aboutItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    doAbout();
		}});
	this.helpMenu.add(this.aboutItem);
	
	// Attach menubar to frame
	this.setJMenuBar(this.menuBar);
    }

    public boolean doAbout() {
	new JapeAbout(this).setVisible(true);
	return true;
    }

    public boolean doClose() {
	// Do we currently have a save game open?
	if ( (this.saveGame != null) && (this.saveGameModified) )
	{
	    // Prompt user for save
	    String shortName = new File(this.saveGame.filename).getName();
	    int option = OptionDialog.showConfirmDialog(
		this, 
		"The file " + shortName + " has been modifed.\n" +
		"Do you want to save the changes?",
		this.getTitle(),
		OptionDialog.YES_NO_CANCEL_OPTION,
		OptionDialog.WARNING_MESSAGE);
	    if ( option == OptionDialog.CANCEL_OPTION || 
		 option == OptionDialog.CLOSED_OPTION )
	    {
		return false;
	    } else if ( option == OptionDialog.YES_OPTION )
	    {
		boolean success = doSave();
		if ( success == false ) {
		    return false;
		}
	    }
	}

	// Disable relevant menu items
	this.saveItem.setEnabled(false);
	this.closeItem.setEnabled(false);

	// Close current save
	this.saveGame = null;
	this.saveGameModified = false;
	this.populateActorList();
	return true;
    }

    public boolean doOpen() {
	// Close any existing save
	boolean success = this.doClose();
	if ( ! success ) {
	    return false;
	}

	// Create open file chooser
	JFileChooser fileChooser = new JFileChooser();
	if( this.currentDir != null ) {
	    fileChooser.setCurrentDirectory(new File(this.currentDir));
	}
	fileChooser.setFileFilter(new FileNameExtensionFilter("Save games", "sav"));

	// Show chooser
	int result = fileChooser.showOpenDialog(this);

	// What did the user choose?
	if( result != JFileChooser.APPROVE_OPTION ) {
	    return false;
	}
	File file = fileChooser.getSelectedFile();
	this.currentDir = file.getParent();
	String filename = file.getPath();

	// Open the selected file
	SaveGame saveGame = new SaveGame();
	try {
	    saveGame.load(filename);
	} catch( EOFException e )
	{
	    // Display error message
	    OptionDialog.showMessageDialog(
		this, 
		"Unable to load file.\n" +
		"Unexpected end of file reached.",
		this.getTitle(),
		OptionDialog.ERROR_MESSAGE);
	    return false;
	} catch( FileNotFoundException e )
	{
	    // Display error message
	    OptionDialog.showMessageDialog(
		this, 
		"Unable to load file.\n" +
		"The system cannot find the file specified.",
		this.getTitle(),
		OptionDialog.ERROR_MESSAGE);
	    return false;
	} catch( IOException e )
	{
	    // Display error message
	    String errorMessage = e.getMessage();
	    if( e.getMessage() == null )
	    {
		errorMessage = "An unknown error occurred.";
	    }
	    OptionDialog.showMessageDialog(
		this, 
		"Unable to load file.\n" +
		errorMessage,
		this.getTitle(),
		OptionDialog.ERROR_MESSAGE);
	    return false;
	} 
	catch( SaveGame.FormatException e )
	{ 
	    // Display error message
	    //e.printStackTrace(System.err);
	    OptionDialog.showMessageDialog(
		this, 
		"Unable to load file.\n" +
		"The save game format was not recognized.\n" +
		e.getMessage(),
		this.getTitle(),
		OptionDialog.ERROR_MESSAGE);
	    return false;
	}

	// Enable relevant menu items
	this.saveItem.setEnabled(true);
	this.closeItem.setEnabled(true);

	// Display the file
	this.saveGame = saveGame;

	// Select the first actor
	this.populateActorList();
	DefaultMutableTreeNode root =
	    (DefaultMutableTreeNode) this.actorTree.getModel().getRoot();
	DefaultMutableTreeNode first = findActorNode(root, 0);
	if ( first != null ) {
	    this.actorTree.setSelectionPath(new TreePath(first.getPath()));
	}
	this.doSelectActor();
	this.saveGameModified = false;

	return true;
    }

    public boolean doSave() {
	// Do we have a save game open
	if ( this.saveGame == null )
	{
	    return true;
	}

	// Save the game
	try {
	    this.saveGame.save();
	} catch( IOException e )
	{
	    // Display error message
	    String errorMessage = e.getMessage();
	    if( e.getMessage() == null )
	    {
		errorMessage = "An unknown error occurred.";
	    }
	    OptionDialog.showMessageDialog(
		this, 
		"Unable to save file.\n" +
		errorMessage,
		this.getTitle(),
		OptionDialog.ERROR_MESSAGE);
	    return false;
	}

	// Reset modified flag
	this.saveGameModified = false;
	this.statPanel.setModified(false);
	this.itemPanel.setModified(false);

	return true;
    }

    public boolean doQuit() 
    {
	// Close any existing save
	boolean success = this.doClose();
	if ( ! success ) {
	    return false;
	}

	// Close window
	this.setVisible(false);
	this.dispose();
	System.exit(0);
	return true;
    }

    private boolean doSelectActor()
    {
	// Which node did the user select
	Actor actor = null;
	Mercenary merc = null;
	Object selected = this.actorTree.getLastSelectedPathComponent();
	if ( selected instanceof DefaultMutableTreeNode )
	{
	    Object payload =
		((DefaultMutableTreeNode) selected).getUserObject();
	    if ( payload instanceof ActorNode )
	    {
		ActorNode node = (ActorNode) payload;
		// Get actor and/or merc
		actor = this.saveGame.getActor(node.index);
		String nickname = actor.get("Nickname");
		merc = this.saveGame.getMercByNick(nickname);
	    }
	}

	// Do it
	this.doSetActor(actor, merc);
	return true;
    }

    private void doSetActor(Actor actor, Mercenary merc) 
    {
	this.currentActor = actor;
	this.currentMerc = merc;

	// Redisplay the gui
	this.statPanel.setActor(this.currentActor, this.currentMerc);
	this.itemPanel.setActor(this.currentActor, this.currentMerc);
    }

    // == Actor tree grouping ==

    /** Classification of a profile in the actor tree. */
    static final int ROSTER = 0;
    static final int RECRUITABLE = 1;
    static final int OTHER = 2;

    static final String ROSTER_GROUP = "Active";
    static final String RECRUITABLE_GROUP = "Recruitable";
    static final String OTHER_GROUP = "Other actors";

    /**
     * Profiles considered recruitable: the first 51 MERCPROFILE entries
     * (the AIM and MERC agency rosters, down to and including Bubba at
     * profile index 50), followed by hand-picked rebel and special RPC
     * nicknames.  Extracted from the MERCPROFILESTRUCT list of a save
     * game and kept as nicknames so the list survives across saves.
     */
    static final String[] RECRUITABLE_NICKNAMES = {
	// AIM + MERC rosters, in profile order, through Bubba (index 50)
	"Barry", "Blood", "Lynx", "Grizzly", "Vicki", "Trevor", "Grunty",
	"Ivan", "Steroid", "Igor", "Shadow", "Red", "Reaper", "Fidel",
	"Fox", "Sidney", "Gus", "Buns", "Ice", "Spider", "Cliff", "Bull",
	"Hitman", "Buzz", "Raider", "Raven", "Static", "Len", "Danny",
	"Magic", "Stephen", "Scully", "Malice", "Dr. Q", "Nails", "Thor",
	"Scope", "Wolf", "MD", "Meltdown", "Biff", "Haywire", "Gasket",
	"Razor", "Flo", "Gumpy", "Larry", "Cougar", "Numb", "Bubba",
	// Hand-picked rebel / special RPCs
	"Ira", "Dimitri", "Carlos", "Miguel", "Iggy", "Maddog", "Vince",
	"Devin", "Robot", "Hamous", "Dynamo", "Shank", "Slay", "Mike",
	"Conrad"
    };

    private static Set<String> recruitableLookup;

    private static Set<String> recruitableLookup() {
	if ( recruitableLookup == null ) {
	    recruitableLookup = new HashSet<String>();
	    for( String nickname : RECRUITABLE_NICKNAMES ) {
		recruitableLookup.add(nickname);
	    }
	}
	return recruitableLookup;
    }

    /** Is this profile on the recruitable list?  Matches on nickname,
     * and also on the full name for profiles that store the well-known
     * name there (e.g. Slay is stored as nickname "Terry"). */
    static boolean isRecruitable(Actor actor) {
	String nickname = actor.get("Nickname").trim();
	if ( recruitableLookup().contains(nickname) ) {
	    return true;
	}
	String name = actor.get("Name").trim();
	return recruitableLookup().contains(name);
    }

    /** Classify a profile: ROSTER (has an active Mercenary record),
     * RECRUITABLE, or OTHER. */
    static int classify(SaveGame saveGame, int idx) {
	Actor actor = saveGame.getActor(idx);
	String nickname = actor.get("Nickname");
	if ( saveGame.getMercByNick(nickname) != null ) {
	    return ROSTER;
	}
	if ( isRecruitable(actor) ) {
	    return RECRUITABLE;
	}
	return OTHER;
    }

    /** Build the actor tree: Active / Recruitable / Other actors.
     *  The Recruitable group follows RECRUITABLE_NICKNAMES order so the
     *  hand-sorted list order is preserved; the other groups follow
     *  profile order. */
    static DefaultMutableTreeNode buildActorTree(SaveGame saveGame) {
	DefaultMutableTreeNode root = new DefaultMutableTreeNode("Actors");
	DefaultMutableTreeNode roster =
	    new DefaultMutableTreeNode(ROSTER_GROUP);
	DefaultMutableTreeNode recruitable =
	    new DefaultMutableTreeNode(RECRUITABLE_GROUP);
	DefaultMutableTreeNode other =
	    new DefaultMutableTreeNode(OTHER_GROUP);
	root.add(roster);
	root.add(recruitable);
	root.add(other);

	if ( saveGame == null ) {
	    return root;
	}

	// Claimed profiles, to avoid adding a profile to two groups
	boolean[] claimed = new boolean[saveGame.actorCount];

	// Roster first: active mercs win over the recruitable list
	for( int idx = 0; idx < saveGame.actorCount; ++idx )
	{
	    if ( classify(saveGame, idx) == ROSTER ) {
		roster.add(actorNode(saveGame, idx));
		claimed[idx] = true;
	    }
	}

	// Recruitable, in the hand-sorted list order
	for( String entry : RECRUITABLE_NICKNAMES )
	{
	    for( int idx = 0; idx < saveGame.actorCount; ++idx )
	    {
		if ( claimed[idx] ) {
		    continue;
		}
		Actor actor = saveGame.getActor(idx);
		if ( entry.equals(actor.get("Nickname").trim()) ||
		     entry.equals(actor.get("Name").trim()) ) {
		    recruitable.add(actorNode(saveGame, idx));
		    claimed[idx] = true;
		}
	    }
	}

	// Everything else, in profile order
	for( int idx = 0; idx < saveGame.actorCount; ++idx )
	{
	    if ( ! claimed[idx] ) {
		other.add(actorNode(saveGame, idx));
	    }
	}
	return root;
    }

    private static DefaultMutableTreeNode actorNode(SaveGame saveGame, int idx)
    {
	Actor actor = saveGame.getActor(idx);
	return new DefaultMutableTreeNode(
	    new ActorNode(idx, actor.get("Nickname")));
    }

    /** First leaf with the given profile index (depth first). */
    private static DefaultMutableTreeNode findActorNode(
	DefaultMutableTreeNode node, int index)
    {
	Object payload = node.getUserObject();
	if ( payload instanceof ActorNode ) {
	    return (((ActorNode) payload).index == index) ? node : null;
	}
	for( int i = 0; i < node.getChildCount(); ++i ) {
	    DefaultMutableTreeNode found = findActorNode(
		(DefaultMutableTreeNode) node.getChildAt(i), index);
	    if ( found != null ) {
		return found;
	    }
	}
	return null;
    }

    /** Leaf payload: profile index (for lookup) and nickname (display). */
    static class ActorNode {
	final int index;
	final String nickname;
	ActorNode(int index, String nickname) {
	    this.index = index;
	    this.nickname = nickname;
	}
	public String toString() {
	    return nickname;
	}
    }

    private void populateActorList()
    {
	// Rebuild the tree from the current save game
	this.actorTree.setModel(
	    new DefaultTreeModel(buildActorTree(this.saveGame)));
	this.doSetActor(null, null);

	// Game loaded?
	if ( this.saveGame == null ) {
	    return;
	}

	// Roster and Recruitable start expanded; Other actors collapsed
	DefaultMutableTreeNode root =
	    (DefaultMutableTreeNode) this.actorTree.getModel().getRoot();
	for( int idx = 0; idx < root.getChildCount(); ++idx )
	{
	    DefaultMutableTreeNode group =
		(DefaultMutableTreeNode) root.getChildAt(idx);
	    if ( ! OTHER_GROUP.equals(group.getUserObject()) ) {
		this.actorTree.expandPath(new TreePath(group.getPath()));
	    }
	}
    }

    public void dataChanged(DataChangeEvent event) {
	this.saveGameModified = true;
    }

    // Class methods
    public static void main(String[] args) 
    {
	// Install the FlatLaf Darcula look and feel
	FlatDarculaLaf.setup();

	// Create frame
	JapeFrame japeFrame = new JapeFrame();
	
	// Make frame visible
	japeFrame.setVisible(true);

	// Open a save
	//japeFrame.doOpen();
    }
}

// Local Variables:
// tab-width: 8
// End:
