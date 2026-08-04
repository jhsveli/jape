package jts.gui;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


import jts.data.Actor;
import jts.data.Mercenary;
import jts.data.Skill;
import jts.data.Structure;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;

public class StatPanel extends InsetPanel implements DataChangeListener {
    private final GridBagConstraints constraint = new GridBagConstraints();

    // State machine
    private boolean modified = false;

    // Data sources
    private Actor actor;

    // Data Views
    private final Vector<FieldView> views = new Vector<>();

    // Instance methods
    public StatPanel(Component parent) {
        super(new Insets(10, 10, 10, 10));
        // Gui Elements
        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        // Constraints
        this.constraint.anchor = GridBagConstraints.SOUTHWEST;
        this.constraint.fill = GridBagConstraints.NONE;
        this.constraint.gridx = 0;
        this.constraint.gridheight = 1;
        this.constraint.gridwidth = 1;
        this.constraint.insets = new Insets(0, 0, 0, 0);

        // Name and nickname (18 chars instead of 30 so the panel's
        // preferred width stays modest and the window packs narrower)
        this.addStringStat("Name", "Name", 18);
        this.addStringStat("Nickname", "Nickname", 18);

        // Health, current max and inc
        this.newRow();
        this.addText("Health", SwingConstants.LEFT);
        this.addNumberView(1, "Health", 3);
        this.addText("/", SwingConstants.CENTER);
        this.addNumberView(1, "Max Health", 3);
        this.addText("(+", SwingConstants.RIGHT);
        this.addNumberView(1, "Health Inc", 3);
        this.addText(")", SwingConstants.LEFT);

        // Ability scores
        this.addByteStat("Energy", "Energy");
        this.addByteStat("Morale", "Morale");
        this.addIncStat("Agility", "Agility", "Agility Inc");
        this.addIncStat("Dexterity", "Dexterity", "Dexterity Inc");
        this.addIncStat("Strength", "Strength", "Strength Inc");
        this.addIncStat("Leadership", "Leadership", "Leadership Inc");
        this.addIncStat("Wisdom", "Wisdom", "Wisdom Inc");
        this.addIncStat("Marksmanship", "Marksmanship", "Marksmanship Inc");
        this.addIncStat("Explosives", "Explosives", "Explosives Inc");
        this.addIncStat("Mechanical", "Mechanical", "Mechanical Inc");
        this.addIncStat("Medical", "Medical", "Medical Inc");
        this.addIncStat("Exp Lvl", "Level", "Level Inc");

        // Skills
        this.addChoiceStat("Skill1", "Skill1", Skill.list);
        this.addChoiceStat("Skill2", "Skill2", Skill.list);

        // Statistics
        this.addShortStat("Kills", "Kills");
        this.addShortStat("Assists", "Assists");
        this.addShortStat("Shots Fired", "Shots Fired");
        this.addShortStat("Shots Hit", "Shots Hit");
        this.addShortStat("Battles", "Battles");
        this.addShortStat("Times Wounded", "Wounds");

        // Alignment hack
        // Throw in extra label to make it look right
        this.newRow();
        this.constraint.fill = GridBagConstraints.BOTH;
        this.constraint.weightx = 1;
        this.constraint.weighty = 1;
        this.addComponent(GridBagConstraints.REMAINDER, new JLabel());
    }

    private void newRow() {
        // Start a new row
        this.constraint.gridy++;
        this.constraint.gridx = 0;
    }

    private void addComponent(int colwidth, Component component) {
        // Add component to panel
        this.constraint.gridwidth = colwidth;
        this.add(component, this.constraint);
        this.constraint.gridx += colwidth;
    }

    private void addNumberView(int colwidth, String statField, int charWidth) {
        JNumberView view = new JNumberView(statField, charWidth);
        // Input cells take weight and horizontal fill so they shrink
        // gradually when the panel is narrower than preferred, instead of
        // snapping to the 5px JTextField minimum the moment any deficit
        // appears (GridBagLayout crushes non-filling weight-0 cells).
        this.constraint.fill = GridBagConstraints.HORIZONTAL;
        this.constraint.weightx = 1;
        this.addComponent(colwidth, view);
        this.constraint.fill = GridBagConstraints.NONE;
        this.constraint.weightx = 0;
        this.views.addElement(view);
        view.addDataChangeListener(this);
    }

    private void addText(String text, int align) {
        // Create new label
        JLabel label = new JLabel(text, align);
        this.addComponent(1, label);
    }

    public void addByteStat(String statTitle, String statField) {
        this.newRow();
        this.addText(statTitle, SwingConstants.LEFT);
        this.addNumberView(1, statField, 3);
    }

    public void addIncStat(String statTitle, String statField, String incStatField) {
        this.newRow();
        this.addText(statTitle, SwingConstants.LEFT);
        this.addNumberView(1, statField, 3);
        this.addText("(+", SwingConstants.RIGHT);
        this.addNumberView(1, incStatField, 3);
        this.addText(")", SwingConstants.LEFT);
    }

    public void addShortStat(String statTitle, String statField) {
        this.addByteStat(statTitle, statField);
    }

    public void addStringStat(String statTitle, String statField, int charWidth) {
        this.newRow();
        this.addText(statTitle, SwingConstants.LEFT);
        this.addNumberView(GridBagConstraints.REMAINDER, statField, charWidth);
    }

    public void addChoiceStat(String statTitle, String statField, Vector<String> statChoices) {
        this.newRow();
        this.addText(statTitle, SwingConstants.LEFT);
        JChoiceView view = new JChoiceView(statField, statChoices);
        // Input cells take weight and horizontal fill (see addNumberView).
        this.constraint.fill = GridBagConstraints.HORIZONTAL;
        this.constraint.weightx = 1;
        this.addComponent(GridBagConstraints.REMAINDER, view);
        this.constraint.fill = GridBagConstraints.NONE;
        this.constraint.weightx = 0;
        this.views.addElement(view);
        view.addDataChangeListener(this);
    }

    public void setActor(Actor actor, Mercenary merc) {
        // Update fields
        this.actor = actor;

        Structure struct;
        if (merc != null) {
            struct = merc;
            struct.chain(this.actor);
        } else {
            struct = this.actor;
        }

        // Update views from data sources
        for (Enumeration<FieldView> e = this.views.elements(); e.hasMoreElements(); ) {
            FieldView view = e.nextElement();
            view.setStruct(struct);
            view.refresh();
        }

        // no.jts.Actor now clean
        this.modified = false;
    }

    public void dataChanged(DataChangeEvent event) {
        // Extract the control was changed
        FieldView view = (FieldView) event.getSource();
        String oldValue = event.getOldValue();
        String newValue = event.getNewValue();

        //System.err.println("doValueChanged(" + view + ",'" + oldValue + "','" + newValue + "')");

        // Do nothing if no item
        if (this.actor == null) {
            return;
        }

        // no.jts.Actor now dirty
        //	System.err.println("no.jts.StatPanel.doValueChanged(" + view + ",'" + oldValue + "','" + newValue + "')");
        this.modified = true;
        this.fireDataChangeEvent(event);
    }

    public boolean isModified() {
        return this.modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    // Data Change event processing
    private final DataChangeMixin mixin = new DataChangeMixin();

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

