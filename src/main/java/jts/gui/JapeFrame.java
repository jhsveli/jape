package jts.gui;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


import com.formdev.flatlaf.FlatDarculaLaf;
import jts.data.Actor;
import jts.data.Item;
import jts.data.Mercenary;
import jts.data.SaveGame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JapeFrame extends JFrame implements DataChangeListener {
    private static final int DEFAULT_WIDTH = 450;
    private static final int DEFAULT_HEIGHT = 450;
    /**
     * Autosave debounce: milliseconds to wait after the last change.
     */
    private static final int AUTOSAVE_DELAY = 300;
    /**
     * File monitor poll interval in milliseconds.
     */
    private static final int FILE_WATCH_INTERVAL = 1000;

    /**
     * Equipped slots: headgear, armor, and hands.
     */
    static final int[] EQUIPPED_SLOTS = {
            Mercenary.HEADGEAR_1_INDEX, Mercenary.HEADGEAR_2_INDEX,
            Mercenary.HELMET_INDEX, Mercenary.BODY_ARMOR_INDEX,
            Mercenary.LEG_ARMOR_INDEX,
            Mercenary.RIGHT_HAND_INDEX, Mercenary.LEFT_HAND_INDEX
    };

    /**
     * Every item slot.
     */
    static final int[] ALL_ITEM_SLOTS = allItemSlots();

    private static int[] allItemSlots() {
        int[] slots = new int[Mercenary.ITEM_COUNT];
        for (int i = 0; i < slots.length; ++i) {
            slots[i] = i;
        }
        return slots;
    }

    private final JTree actorTree;
    private final StatPanel statPanel;
    private final ItemPanel itemPanel;

    private JMenuItem saveItem;
    private JMenuItem closeItem;

    // Toolbar
    private JToolBar toolBar;
    private JButton toolbarSaveItem;
    private JCheckBox autosaveCheckbox;
    private JButton copyEquippedButton;
    private JButton pasteEquippedButton;
    private JButton copyAllButton;
    private JButton pasteAllButton;
    private final Timer autosaveTimer;
    private long fileLastModified;
    private long fileLength;

    // Item copy/paste clipboards (raw item data per slot)
    private byte[][] equippedClipboard;
    private byte[][] allClipboard;

    // The save game engine
    private SaveGame saveGame;
    private boolean saveGameModified;
    private Actor currentActor;
    private Mercenary currentMerc;
    private String currentDir = System.getProperty("user.dir");

    public JapeFrame() {
        super("JAME");

        // Menu
        this.createMenuBar();

        // Toolbar (Open/Save/autosave) above the panels
        this.createToolBar();

        // Debounced autosave: writes once, 300ms after the last change
        this.autosaveTimer = new Timer(AUTOSAVE_DELAY, e -> {
            // Re-check at fire time: the checkbox or the save game may
            // have changed during the debounce window
            if (autosaveActive((autosaveCheckbox != null) &&
                            autosaveCheckbox.isSelected(),
                    saveGame)) {
                doSave();
            }
        });
        this.autosaveTimer.setRepeats(false);

        // File monitor: poll for external changes to the open save file
        Timer fileWatchTimer = new Timer(FILE_WATCH_INTERVAL, e -> checkFileChanged());
        fileWatchTimer.start();

        // Create body panel
        JPanel body = new JPanel();
        // Major GUI elements
        GridBagLayout layout = new GridBagLayout();
        body.setLayout(layout);
        this.getContentPane().add(this.toolBar, BorderLayout.NORTH);
        this.getContentPane().add(body, BorderLayout.CENTER);

        // Create actor tree
        this.actorTree = new JTree(new DefaultTreeModel(buildActorTree(null)));
        this.actorTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.actorTree.setRootVisible(false);
        this.actorTree.setShowsRootHandles(true);
        this.actorTree.addTreeSelectionListener(e -> doSelectActor());
        JScrollPane actorScroll = createActorScroll();
        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.BOTH;
        c1.gridx = 0;
        c1.gridy = 0;
        c1.weighty = 1;
        c1.weightx = 0;
        body.add(actorScroll, c1);

        // Create stat panel
        this.statPanel = new StatPanel(this);
        this.statPanel.addDataChangeListener(this);
        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.BOTH;
        c2.anchor = GridBagConstraints.NORTHWEST;
        c2.gridx = 1;
        c2.gridy = 0;
        c2.weighty = 1;
        c2.weightx = 1;
        body.add(this.statPanel, c2);

        // Create item panel
        this.itemPanel = new ItemPanel(this);
        this.itemPanel.addDataChangeListener(this);
        GridBagConstraints c3 = new GridBagConstraints();
        c3.fill = GridBagConstraints.BOTH;
        c3.anchor = GridBagConstraints.NORTHWEST;
        c3.gridx = 2;
        c3.gridy = 0;
        c3.weighty = 1;
        c3.weightx = 1;
        body.add(this.itemPanel, c3);

        // Handle window events
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                doQuit();
            }
        });

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

    private JScrollPane createActorScroll() {
        JScrollPane actorScroll = new JScrollPane(this.actorTree);
        // Match the 10px edge spacing of the stat/item panels on the
        // window's left, top, and bottom; the right side stays 0 so the
        // gap to the stat panel is unchanged.
        actorScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        // Floor the tree column width so it never collapses to its tiny
        // default minimum (~22px) when the window is narrower than the
        // preferred width; nicknames stay readable even at small sizes.
        actorScroll.setMinimumSize(new Dimension(135, 0));
        return actorScroll;
    }

    private void createMenuBar() {
        // Create menu bar
        // Menu
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem openItem = new JMenuItem("Open...");
        openItem.addActionListener(e -> doOpen());
        fileMenu.add(openItem);

        this.saveItem = new JMenuItem("Save");
        this.saveItem.addActionListener(e -> doSave());
        fileMenu.add(this.saveItem);
        this.saveItem.setEnabled(false);

        this.closeItem = new JMenuItem("Close");
        this.closeItem.addActionListener(e -> doClose());
        fileMenu.add(this.closeItem);
        this.closeItem.setEnabled(false);

        fileMenu.addSeparator();

        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.addActionListener(e -> doQuit());
        fileMenu.add(quitItem);

        // About menu
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem aboutItem = new JMenuItem("About...");
        aboutItem.addActionListener(e -> doAbout());
        helpMenu.add(aboutItem);

        // Attach menubar to frame
        this.setJMenuBar(menuBar);
    }

    private void createToolBar() {
        ToolbarControls controls = buildToolBar(e -> doOpen(), e -> doSave(), e -> doCopyEquipped(), e -> doPasteEquipped(), e -> doCopyAll(), e -> doPasteAll());
        this.toolBar = controls.toolBar;
        this.toolbarSaveItem = controls.saveButton;
        this.autosaveCheckbox = controls.autosaveCheckbox;
        this.copyEquippedButton = controls.copyEquippedButton;
        this.pasteEquippedButton = controls.pasteEquippedButton;
        this.copyAllButton = controls.copyAllButton;
        this.pasteAllButton = controls.pasteAllButton;
        this.toolbarSaveItem.setEnabled(false);
        this.copyEquippedButton.setEnabled(false);
        this.pasteEquippedButton.setEnabled(false);
        this.copyAllButton.setEnabled(false);
        this.pasteAllButton.setEnabled(false);
    }

    /**
     * The toolbar controls, collected so the frame can wire them up.
     */
    static class ToolbarControls {
        JToolBar toolBar;
        JButton saveButton;
        JCheckBox autosaveCheckbox;
        JButton copyEquippedButton;
        JButton pasteEquippedButton;
        JButton copyAllButton;
        JButton pasteAllButton;
    }

    /**
     * Create the toolbar controls: Open.../Save, the item copy/paste
     * buttons, and the default-enabled Autosave checkbox.
     */
    static ToolbarControls buildToolBar(ActionListener openListener,
                                        ActionListener saveListener,
                                        ActionListener copyEquippedListener,
                                        ActionListener pasteEquippedListener,
                                        ActionListener copyAllListener,
                                        ActionListener pasteAllListener) {
        ToolbarControls controls = new ToolbarControls();
        controls.toolBar = new JToolBar();
        controls.toolBar.setFloatable(false);

        JButton openButton = new JButton("Open...");
        openButton.addActionListener(openListener);
        controls.toolBar.add(openButton);

        controls.saveButton = new JButton("Save");
        controls.saveButton.addActionListener(saveListener);
        controls.toolBar.add(controls.saveButton);

        controls.toolBar.addSeparator();

        controls.copyEquippedButton = new JButton("Copy Equipped");
        controls.copyEquippedButton.addActionListener(copyEquippedListener);
        controls.toolBar.add(controls.copyEquippedButton);

        controls.pasteEquippedButton = new JButton("Paste Equipped");
        controls.pasteEquippedButton.addActionListener(pasteEquippedListener);
        controls.toolBar.add(controls.pasteEquippedButton);

        controls.copyAllButton = new JButton("Copy All");
        controls.copyAllButton.addActionListener(copyAllListener);
        controls.toolBar.add(controls.copyAllButton);

        controls.pasteAllButton = new JButton("Paste All");
        controls.pasteAllButton.addActionListener(pasteAllListener);
        controls.toolBar.add(controls.pasteAllButton);

        controls.toolBar.addSeparator();

        controls.autosaveCheckbox = new JCheckBox("Autosave", true);
        controls.toolBar.add(controls.autosaveCheckbox);

        return controls;
    }

    /**
     * Autosave rule: persist immediately when the checkbox is on and a
     * save game is open.
     */
    static boolean autosaveActive(boolean selected, SaveGame saveGame) {
        return selected && saveGame != null;
    }

    /**
     * Snapshot the given slots' item data (null entries for empty slots).
     */
    static byte[][] snapshotItems(Mercenary merc, int[] slots) {
        if (merc == null) {
            return null;
        }
        byte[][] data = new byte[slots.length][];
        for (int i = 0; i < slots.length; ++i) {
            Item item = merc.items[slots[i]];
            data[i] = (item == null) ? null : item.encode().clone();
        }
        return data;
    }

    /**
     * Write the clipboard into the given slots.  Empty clipboard entries
     * clear the target slot (a zeroed "None" item), so pasting fully
     * replicates the copied loadout.
     */
    static void pasteItems(Mercenary merc, int[] slots, byte[][] clipboard) {
        if (merc == null || clipboard == null) {
            return;
        }
        for (int i = 0; i < slots.length; ++i) {
            if (clipboard[i] != null) {
                merc.items[slots[i]] = new Item(clipboard[i].clone());
            } else {
                merc.items[slots[i]] = new Item(new byte[Item.ITEM_LENGTH]);
            }
        }
    }

    private void doCopyEquipped() {
        this.equippedClipboard = snapshotItems(this.currentMerc, EQUIPPED_SLOTS);
        this.updateToolbarStates();
    }

    private void doCopyAll() {
        this.allClipboard = snapshotItems(this.currentMerc, ALL_ITEM_SLOTS);
        this.updateToolbarStates();
    }

    private void doPasteEquipped() {
        this.pasteToCurrent(EQUIPPED_SLOTS, this.equippedClipboard);
    }

    private void doPasteAll() {
        this.pasteToCurrent(ALL_ITEM_SLOTS, this.allClipboard);
    }

    private void pasteToCurrent(int[] slots, byte[][] clipboard) {
        if (this.currentMerc == null || clipboard == null) {
            return;
        }
        pasteItems(this.currentMerc, slots, clipboard);

        // Refresh the item panel and record the change (autosave included)
        this.itemPanel.setActor(this.currentActor, this.currentMerc);
        this.markModified();
    }

    /**
     * Record a change and schedule the debounced autosave when enabled.
     */
    private void markModified() {
        this.saveGameModified = true;
        if (autosaveActive((this.autosaveCheckbox != null) &&
                        this.autosaveCheckbox.isSelected(),
                this.saveGame)) {
            this.autosaveTimer.restart();
        }
    }

    private void updateToolbarStates() {
        boolean hasMerc = this.currentMerc != null;
        this.copyEquippedButton.setEnabled(hasMerc);
        this.copyAllButton.setEnabled(hasMerc);
        this.pasteEquippedButton.setEnabled(hasMerc && this.equippedClipboard != null);
        this.pasteAllButton.setEnabled(hasMerc && this.allClipboard != null);
    }

    public void doAbout() {
        new JapeAbout(this).setVisible(true);
    }

    public boolean doClose() {
        // Do we currently have a save game open?
        if ((this.saveGame != null) && (this.saveGameModified)) {
            // Prompt user for save
            String shortName = new File(this.saveGame.filename).getName();
            int option = OptionDialog.showConfirmDialog(
                    this,
                    "The file " + shortName + " has been modifed.\n" +
                            "Do you want to save the changes?",
                    this.getTitle(),
                    OptionDialog.YES_NO_CANCEL_OPTION,
                    OptionDialog.WARNING_MESSAGE);
            if (option == OptionDialog.CANCEL_OPTION ||
                    option == OptionDialog.CLOSED_OPTION) {
                return false;
            } else if (option == OptionDialog.YES_OPTION) {
                boolean success = doSave();
                if (!success) {
                    return false;
                }
            }
        }

        // Disable relevant menu items
        this.saveItem.setEnabled(false);
        this.closeItem.setEnabled(false);
        this.toolbarSaveItem.setEnabled(false);

        // Cancel any pending autosave
        this.autosaveTimer.stop();

        // Close current save
        this.saveGame = null;
        this.saveGameModified = false;
        this.populateActorList();
        return true;
    }

    public void doOpen() {
        // Close any existing save
        boolean success = this.doClose();
        if (!success) {
            return;
        }

        // Create open file chooser
        JFileChooser fileChooser = new JFileChooser();
        if (this.currentDir != null) {
            fileChooser.setCurrentDirectory(new File(this.currentDir));
        }
        fileChooser.setFileFilter(new FileNameExtensionFilter("Save games", "sav"));

        // Show chooser
        int result = fileChooser.showOpenDialog(this);

        // What did the user choose?
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        this.currentDir = file.getParent();
        String filename = file.getPath();

        // Open the selected file
        this.loadSaveFile(filename);
    }

    /**
     * Load the given save file into the editor.  On success replaces the
     * current save game and refreshes the panels; on failure the current
     * save game is left untouched.
     */
    private void loadSaveFile(String filename) {
        // Open the selected file
        SaveGame saveGame = new SaveGame();
        try {
            saveGame.load(filename);
        } catch (EOFException e) {
            // Display error message
            OptionDialog.showMessageDialog(
                    this,
                    "Unable to load file.\n" +
                            "Unexpected end of file reached.",
                    this.getTitle(),
                    OptionDialog.ERROR_MESSAGE);
            return;
        } catch (FileNotFoundException e) {
            // Display error message
            OptionDialog.showMessageDialog(
                    this,
                    "Unable to load file.\n" +
                            "The system cannot find the file specified.",
                    this.getTitle(),
                    OptionDialog.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            // Display error message
            String errorMessage = e.getMessage();
            if (e.getMessage() == null) {
                errorMessage = "An unknown error occurred.";
            }
            OptionDialog.showMessageDialog(
                    this,
                    "Unable to load file.\n" +
                            errorMessage,
                    this.getTitle(),
                    OptionDialog.ERROR_MESSAGE);
            return;
        } catch (SaveGame.FormatException e) {
            // Display error message
            //e.printStackTrace(System.err);
            OptionDialog.showMessageDialog(
                    this,
                    "Unable to load file.\n" +
                            "The save game format was not recognized.\n" +
                            e.getMessage(),
                    this.getTitle(),
                    OptionDialog.ERROR_MESSAGE);
            return;
        }

        // Enable relevant menu items
        this.saveItem.setEnabled(true);
        this.closeItem.setEnabled(true);
        this.toolbarSaveItem.setEnabled(true);

        // Display the file
        this.saveGame = saveGame;

        // Select the first actor
        this.populateActorList();
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode) this.actorTree.getModel().getRoot();
        DefaultMutableTreeNode first = findActorNode(root, 0);
        if (first != null) {
            this.actorTree.setSelectionPath(new TreePath(first.getPath()));
        }
        this.doSelectActor();
        this.saveGameModified = false;

        // Remember the on-disk state for the file monitor
        this.updateFileSnapshot();

    }

    /**
     * True when the file's current state differs from the snapshot.
     */
    static boolean fileChanged(long snapshotLastModified, long snapshotLength,
                               long currentLastModified, long currentLength) {
        return (currentLastModified != snapshotLastModified) ||
                (currentLength != snapshotLength);
    }

    /**
     * Remember the current on-disk state of the open save file.
     */
    private void updateFileSnapshot() {
        if (this.saveGame == null) {
            return;
        }
        File file = new File(this.saveGame.filename);
        if (file.exists()) {
            this.fileLastModified = file.lastModified();
            this.fileLength = file.length();
        }
    }

    /**
     * Poll the open save file; prompt the user if it changed on disk.
     */
    private void checkFileChanged() {
        if (this.saveGame == null) {
            return;
        }
        File file = new File(this.saveGame.filename);
        if (!file.exists()) {
            return;
        }
        long lastModified = file.lastModified();
        long length = file.length();
        if (!fileChanged(this.fileLastModified, this.fileLength,
                lastModified, length)) {
            return;
        }

        // Acknowledge the change before prompting so a timer tick during the
        // modal dialog cannot re-enter and stack another prompt.
        this.fileLastModified = lastModified;
        this.fileLength = length;

        // The file changed on disk since we last loaded or saved it
        String message = "The file has changed on disk.\n";
        if (this.saveGameModified) {
            message += "You have unsaved changes that would be lost.\n";
        }
        message += "Reload it from disk?";
        int option = OptionDialog.showConfirmDialog(
                this, message, this.getTitle(),
                OptionDialog.YES_NO_OPTION, OptionDialog.WARNING_MESSAGE);
        if (option == OptionDialog.YES_OPTION) {
            this.loadSaveFile(this.saveGame.filename);
            // loadSaveFile refreshes the snapshot on success; on failure the
            // snapshot above already reflects the on-disk state, so the same
            // change is not reported again on the next poll.
        }
    }

    public boolean doSave() {
        // Do we have a save game open
        if (this.saveGame == null) {
            return true;
        }

        // Save the game
        try {
            this.saveGame.save();
        } catch (IOException e) {
            // Display error message
            String errorMessage = e.getMessage();
            if (e.getMessage() == null) {
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

        // Our own write is the new on-disk state for the file monitor
        this.updateFileSnapshot();

        return true;
    }

    public void doQuit() {
        // Close any existing save
        boolean success = this.doClose();
        if (!success) {
            return;
        }

        // Close window
        this.setVisible(false);
        this.dispose();
        System.exit(0);
    }

    private void doSelectActor() {
        // Which node did the user select
        Actor actor = null;
        Mercenary merc = null;
        Object selected = this.actorTree.getLastSelectedPathComponent();
        if (selected instanceof DefaultMutableTreeNode) {
            Object payload =
                    ((DefaultMutableTreeNode) selected).getUserObject();
            if (payload instanceof ActorNode node) {
                // Get actor and/or merc
                actor = this.saveGame.getActor(node.index);
                String nickname = actor.get("Nickname");
                merc = this.saveGame.getMercByNick(nickname);
            }
        }

        // Do it
        this.doSetActor(actor, merc);
    }

    private void doSetActor(Actor actor, Mercenary merc) {
        this.currentActor = actor;
        this.currentMerc = merc;

        // Redisplay the gui
        this.statPanel.setActor(this.currentActor, this.currentMerc);
        this.itemPanel.setActor(this.currentActor, this.currentMerc);
        this.updateToolbarStates();
    }

    // == Actor tree grouping ==

    /**
     * Classification of a profile in the actor tree.
     */
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
        if (recruitableLookup == null) {
            recruitableLookup = new HashSet<>();
            Collections.addAll(recruitableLookup, RECRUITABLE_NICKNAMES);
        }
        return recruitableLookup;
    }

    /**
     * Is this profile on the recruitable list?  Matches on nickname,
     * and also on the full name for profiles that store the well-known
     * name there (e.g. Slay is stored as nickname "Terry").
     */
    static boolean isRecruitable(Actor actor) {
        String nickname = actor.get("Nickname").trim();
        if (recruitableLookup().contains(nickname)) {
            return true;
        }
        String name = actor.get("Name").trim();
        return recruitableLookup().contains(name);
    }

    /**
     * Classify a profile: ROSTER (has an active Mercenary record),
     * RECRUITABLE, or OTHER.
     */
    static int classify(SaveGame saveGame, int idx) {
        Actor actor = saveGame.getActor(idx);
        String nickname = actor.get("Nickname");
        if (saveGame.getMercByNick(nickname) != null) {
            return ROSTER;
        }
        if (isRecruitable(actor)) {
            return RECRUITABLE;
        }
        return OTHER;
    }

    /**
     * Build the actor tree: Active / Recruitable / Other actors.
     * The Recruitable group follows RECRUITABLE_NICKNAMES order so the
     * hand-sorted list order is preserved; the other groups follow
     * profile order.
     */
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

        if (saveGame == null) {
            return root;
        }

        // Claimed profiles, to avoid adding a profile to two groups
        boolean[] claimed = new boolean[saveGame.actorCount];

        // Roster first: active mercs win over the recruitable list
        for (int idx = 0; idx < saveGame.actorCount; ++idx) {
            if (classify(saveGame, idx) == ROSTER) {
                roster.add(actorNode(saveGame, idx));
                claimed[idx] = true;
            }
        }

        // Recruitable, in the hand-sorted list order
        for (String entry : RECRUITABLE_NICKNAMES) {
            for (int idx = 0; idx < saveGame.actorCount; ++idx) {
                if (claimed[idx]) {
                    continue;
                }
                Actor actor = saveGame.getActor(idx);
                if (entry.equals(actor.get("Nickname").trim()) ||
                        entry.equals(actor.get("Name").trim())) {
                    recruitable.add(actorNode(saveGame, idx));
                    claimed[idx] = true;
                }
            }
        }

        // Everything else, in profile order
        for (int idx = 0; idx < saveGame.actorCount; ++idx) {
            if (!claimed[idx]) {
                other.add(actorNode(saveGame, idx));
            }
        }
        return root;
    }

    private static DefaultMutableTreeNode actorNode(SaveGame saveGame, int idx) {
        Actor actor = saveGame.getActor(idx);
        return new DefaultMutableTreeNode(
                new ActorNode(idx, actor.get("Nickname")));
    }

    /**
     * First leaf with the given profile index (depth first).
     */
    private static DefaultMutableTreeNode findActorNode(
            DefaultMutableTreeNode node, int index) {
        Object payload = node.getUserObject();
        if (payload instanceof ActorNode) {
            return (((ActorNode) payload).index == index) ? node : null;
        }
        for (int i = 0; i < node.getChildCount(); ++i) {
            DefaultMutableTreeNode found = findActorNode(
                    (DefaultMutableTreeNode) node.getChildAt(i), index);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

	/**
	 * Leaf payload: profile index (for lookup) and nickname (display).
	 */
	record ActorNode(int index, String nickname) {

		public String toString() {
			return nickname;
		}
	}

    private void populateActorList() {
        // Rebuild the tree from the current save game
        this.actorTree.setModel(
                new DefaultTreeModel(buildActorTree(this.saveGame)));
        this.doSetActor(null, null);

        // Game loaded?
        if (this.saveGame == null) {
            return;
        }

        // Roster and Recruitable start expanded; Other actors collapsed
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode) this.actorTree.getModel().getRoot();
        for (int idx = 0; idx < root.getChildCount(); ++idx) {
            DefaultMutableTreeNode group =
                    (DefaultMutableTreeNode) root.getChildAt(idx);
            if (!OTHER_GROUP.equals(group.getUserObject())) {
                this.actorTree.expandPath(new TreePath(group.getPath()));
            }
        }
    }

    public void dataChanged(DataChangeEvent event) {
        this.markModified();
    }

    // Class methods
    public static void main(String[] args) {
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
