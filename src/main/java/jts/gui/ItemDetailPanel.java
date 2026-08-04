package jts.gui;
/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


import jts.data.Item;
import jts.data.ItemExemplar;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;

public class ItemDetailPanel extends InsetPanel implements DataChangeListener {
    private final GridBagConstraints constraint = new GridBagConstraints();

    // State machine
    private boolean modified = false;

    // Data sources
    private Item item;

    // Data views
    private final Vector<Component> views = new Vector<>();
    private final Vector<Component> ammoComponents = new Vector<>();
    private final Vector<Component> attachmentComponents = new Vector<>();
    private final Vector<Component> moneyComponents = new Vector<>();

    private final JChoiceView idView;
    private final JNumberView quantityView;
    private final JLabel itemPctLabel;
    private final JNumberView[] itemPctViews = new JNumberView[6];
    private final JChoiceView ammoIdView;
    private final JNumberView ammoQuantityView;
    private final JNumberView ammoVarietyView;
    private final JNumberView ammoPctView;
    private final JChoiceView[] attachmentIdViews = new JChoiceView[4];
    private final JNumberView[] attachmentPctViews = new JNumberView[4];

    // Instance methods
    public ItemDetailPanel(Component parent) {
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

        // Field views
        this.newRow();
        this.addText("Item");
        this.idView = addItemView(6, "Item ID", ItemExemplar.nameList);

        this.newRow();
        this.addText("Number of Items");
        this.quantityView = addByteView(1, "Quantity");

        this.newRow();
        this.itemPctLabel = this.addText("Item %");
        for (int idx = 0; idx < 6; ++idx) {
            this.itemPctViews[idx] = addByteView(1, "Item " + (idx + 1) + " %");
        }

        this.newRow();
        JLabel ammoIdLabel = this.addText("Ammo");
        this.ammoComponents.addElement(ammoIdLabel);
        this.ammoIdView = addItemView(6, "Ammo ID", ItemExemplar.ammoNameList);
        this.ammoComponents.addElement(this.ammoIdView);

        this.newRow();
        JLabel ammoQuantityLabel = this.addText("Number of Rounds");
        this.ammoComponents.addElement(ammoQuantityLabel);
        this.ammoQuantityView = addByteView(1, "Ammo Quantity");
        this.ammoComponents.addElement(this.ammoQuantityView);

        this.newRow();
        JLabel ammoVarietyLabel = this.addText("Ammo Variety");
        this.ammoComponents.addElement(ammoVarietyLabel);
        this.ammoVarietyView = addByteView(1, "Ammo Variety");
        this.ammoComponents.addElement(this.ammoVarietyView);

        this.newRow();
        JLabel ammoPctLabel = this.addText("Ammo %");
        this.ammoComponents.addElement(ammoPctLabel);
        this.ammoPctView = addByteView(1, "Ammo %");
        this.ammoComponents.addElement(this.ammoPctView);

        for (int idx = 0; idx < 4; ++idx) {
            String str = "Attachment " + (idx + 1);

            this.newRow();
            JLabel label = this.addText(str);
            this.attachmentComponents.addElement(label);

            JChoiceView view = addItemView(6, str + " ID", ItemExemplar.attachmentNameList);
            this.attachmentIdViews[idx] = view;
            this.attachmentComponents.addElement(view);

            JNumberView pctView = this.addByteView(1, str + " %");
            this.attachmentPctViews[idx] = pctView;
            this.attachmentComponents.addElement(pctView);

            JLabel pctLabel = this.addText("%");
            this.attachmentComponents.addElement(pctLabel);
        }

        this.newRow();
        this.addText("Weight (x 0.1 kg)");
        JNumberView weightView = this.addByteView(1, "Weight");

        this.newRow();
        JLabel moneyValueLabel = this.addText("Money Value");
        this.moneyComponents.addElement(moneyValueLabel);
        JNumberView moneyValueView = this.addShortView(2, "Money Value");
        this.moneyComponents.addElement(moneyValueView);

        //addByteStat("Unknown 1", "Unknown 1");
        //addByteStat("Unknown 4", "Unknown 4");

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

    private JNumberView addNumberView(String statField) {
        JNumberView view = new JNumberView(statField, 3);
        // Input cells take weight and horizontal fill so they shrink
        // gradually when the panel is narrower than preferred, instead of
        // snapping to the tiny JTextField minimum (same fix as StatPanel).
        this.constraint.fill = GridBagConstraints.HORIZONTAL;
        this.constraint.weightx = 1;
        this.addComponent(1, view);
        this.constraint.fill = GridBagConstraints.NONE;
        this.constraint.weightx = 0;
        this.views.addElement(view);
        view.addDataChangeListener(this);
        return view;
    }



    private JLabel addText(String text) {
        // Create new label
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        this.constraint.fill = GridBagConstraints.HORIZONTAL;
        this.addComponent(1, label);
        return label;
    }

    public JNumberView addByteView(int colwidth, String statField) {
        return this.addNumberView(statField);
    }

    public JNumberView addShortView(int colwidth, String statField) {
        return this.addNumberView(statField);
    }

    public JChoiceView addItemView(int colwidth, String statField, Vector<String> statChoices) {
        // Create new view
        JChoiceView view = new JChoiceView(statField, statChoices);
        // Combos get weight and a smaller minimum so they share the shrink
        // with the number cells when the panel is narrow; a combo that keeps
        // its full width (JComboBox min == preferred) would force the rest
        // of its row to snap to minimum. The height is kept at the preferred
        // height so the combo never collapses to invisible.
        this.constraint.fill = GridBagConstraints.HORIZONTAL;
        this.constraint.weightx = 1;
        this.addComponent(colwidth, view);
        this.constraint.fill = GridBagConstraints.NONE;
        this.constraint.weightx = 0;
        view.setMinimumSize(new Dimension(40, view.getPreferredSize().height));
        this.views.addElement(view);
        view.addDataChangeListener(this);
        return view;
    }

    public void setItem(Item item) {
        this.setItem(item, null);
    }

    /**
     * Show an item in the detail panel. When {@code allowedCategory} is
     * non-null the Item ID dropdown is restricted to that category (plus
     * "None"); when null every item is offered.
     */
    public void setItem(Item item, Integer allowedCategory) {
        // Update fields
        this.item = item;
//  	if( this.item != null ) {
//  	    for( int foo = 0; foo < this.item.data.length; ++foo ) {
//  		System.err.println("" + foo + ":\t" + this.item.data[foo]);
//  	    }
//  	}

        // Rebuild the dropdowns for the selected slot: the Item ID dropdown
        // is restricted by the slot's category (armor slots), while the ammo
        // and attachment dropdowns are restricted by the item's category
        this.idView.setChoices(this.buildItemChoices(allowedCategory));
        this.ammoIdView.setChoices(this.buildAmmoChoices());
        for (int idx = 0; idx < this.attachmentIdViews.length; ++idx) {
            this.attachmentIdViews[idx].setChoices(this.buildAttachmentChoices(idx));
        }

        // Update views from data sources
        for (Enumeration<Component> e = this.views.elements(); e.hasMoreElements(); ) {
            FieldView view = (FieldView) e.nextElement();
            view.setStruct(item);
            view.refresh();
        }

        // Decide which views to show
        this.setEnabledAll();

        // Actor now clean
        this.modified = false;
    }

    /**
     * Build the Item ID dropdown entries for the given allowed category
     * (null = every item). "None" is always offered first.
     */
    private Vector<String> buildItemChoices(Integer allowedCategory) {
        if (allowedCategory == null) {
            return ItemExemplar.nameList;
        }

        Vector<String> choices = buildChoices(allowedCategory);
        this.keepCurrentVisible(choices, "Item ID");
        return choices;
    }

    /**
     * Build the Ammo ID dropdown entries: ammo items only.
     */
    private Vector<String> buildAmmoChoices() {
        Vector<String> choices = buildChoices(ItemExemplar.AMMO_CATEGORY);
        this.keepCurrentVisible(choices, "Ammo ID");
        return choices;
    }

    /**
     * Build an Attachment ID dropdown for the given attachment slot.
     * Weapons take weapon attachments; body armor takes armor attachments
     * (ceramic plates); other item types keep the original mixed list
     * (their attachment fields are disabled anyway).
     */
    private Vector<String> buildAttachmentChoices(int attachmentIndex) {
        ItemExemplar exemplar = (this.item == null) ? null : this.item.getExemplar();
        int category;
        if (exemplar != null && exemplar.category == ItemExemplar.WEAPON_CATEGORY) {
            category = ItemExemplar.WEAPON_ATTACHMENT_CATEGORY;
        } else if (exemplar != null && exemplar.category == ItemExemplar.BODY_ARMOR_CATEGORY) {
            category = ItemExemplar.ARMOR_ATTACHMENT_CATEGORY;
        } else {
            Vector<String> choices = copyList(ItemExemplar.attachmentNameList);
            this.keepCurrentVisible(choices, "Attachment " + (attachmentIndex + 1) + " ID");
            return choices;
        }

        Vector<String> choices = buildChoices(category);
        this.keepCurrentVisible(choices, "Attachment " + (attachmentIndex + 1) + " ID");
        return choices;
    }

    /**
     * Build a dropdown list containing "None" and every item of the given
     * category, in the original item order.
     */
    private Vector<String> buildChoices(int category) {
        Vector<String> choices = new Vector<>();
        choices.addElement("None");
        for (int idx = 0; idx < ItemExemplar.nameList.size(); ++idx) {
            String name = (String) ItemExemplar.nameList.elementAt(idx);
            ItemExemplar exemplar = (ItemExemplar) ItemExemplar.exemplarTable.get(name);
            if (exemplar != null && exemplar.category == category) {
                choices.addElement(name);
            }
        }
        return choices;
    }

    /**
     * Copy a shared list so it can be modified (e.g. by keepCurrentVisible).
     */
    private Vector<String> copyList(Vector<String> source) {
        Vector<String> copy = new Vector<>();
        for (int idx = 0; idx < source.size(); ++idx) {
            copy.addElement(source.elementAt(idx));
        }
        return copy;
    }

    /**
     * If the slot already holds a known item that isn't in the filtered
     * list, keep it visible so the UI doesn't lie about what's equipped.
     */
    private void keepCurrentVisible(Vector<String> choices, String fieldName) {
        if (this.item != null) {
            String current = this.item.get(fieldName);
            if (current != null && !current.equals("None") && !choices.contains(current)) {
                choices.addElement(current);
            }
        }
    }

    public void setEnabled(Vector<Component> views, boolean enabled) {
        for (Enumeration<Component> e = views.elements(); e.hasMoreElements(); ) {
            Component view = (Component) e.nextElement();
            view.setEnabled(enabled);
        }
    }

    public void setEnabledAll() {
        // Noop if not showing any item
        if (this.item == null) {
            return;
        }

        // Decide which fields to show based on item type
        // Unknown item ids have no exemplar; treat them like the default case
        ItemExemplar exemplar = this.item.getExemplar();
        int category = (exemplar == null) ? -1 : exemplar.category;
        switch (category) {
            case ItemExemplar.WEAPON_CATEGORY:
                this.setEnabled(this.ammoComponents, true);
                this.setEnabled(this.attachmentComponents, true);
                this.setEnabled(this.moneyComponents, false);
                break;

            case ItemExemplar.BODY_ARMOR_CATEGORY:
                this.setEnabled(this.ammoComponents, false);
                this.setEnabled(this.attachmentComponents, true);
                this.setEnabled(this.moneyComponents, false);
                break;

            case ItemExemplar.MONEY_CATEGORY:
                this.setEnabled(this.ammoComponents, false);
                this.setEnabled(this.attachmentComponents, false);
                this.setEnabled(this.moneyComponents, true);
                break;

            default:
                this.setEnabled(this.ammoComponents, false);
                this.setEnabled(this.attachmentComponents, false);
                this.setEnabled(this.moneyComponents, false);
                break;
        }

        // Show the appropriate number of %'s
        int quantity = this.item.getInt("Quantity");
        for (int idx = 0; idx < 6; ++idx) {
            this.itemPctViews[idx].setEnabled((idx < quantity));
        }
        this.itemPctLabel.setEnabled(quantity != 0);

        // Set label appropriately
        if (category == ItemExemplar.AMMO_CATEGORY) {
            this.itemPctLabel.setText("Number of Rounds");
        } else {
            this.itemPctLabel.setText("Item %");
        }
    }

    public void dataChanged(DataChangeEvent event) {
        // Extract the control was changed
        FieldView view = (FieldView) event.getSource();
        String oldValue = event.getOldValue();
        String newValue = event.getNewValue();

        //System.err.println("doValueChanged(" + view + ",'" + oldValue + "','" + newValue + "')");

        // Do nothing if no item
        if (this.item == null) {
            return;
        }

        // Do control-specific stuff
        if (view == this.idView) {
            //	    System.err.println(" idview");

            // Make the input more palatable
            if ((oldValue == null) || (oldValue.isEmpty())) {
                oldValue = "None";
            }
            if ((newValue == null) || (newValue.isEmpty())) {
                newValue = "None";
            }

            // If switching from none to something, change % to 100 and count to 1
            if (oldValue.equals("None") && (!newValue.equals("None"))) {
                this.quantityView.setText("1");
                this.itemPctViews[0].setEnabled(true);
                this.itemPctViews[0].setText("100");
            }
            // If switching from something to none, change % to 0 and count to 0
            else if (newValue.equals("None")) {
                this.quantityView.setText("0");
                this.itemPctViews[0].setEnabled(true);
                this.itemPctViews[0].setText("0");
            }

            // If switching to weapon, change count to 1
            ItemExemplar exemplar = this.item.getExemplar();
            if (exemplar != null && exemplar.category == ItemExemplar.WEAPON_CATEGORY) {
                this.quantityView.setText("1");
            }
            // If switching to new ammo, set variety
            else if (exemplar != null && exemplar.category == ItemExemplar.AMMO_CATEGORY) {
                int quantity = this.item.getInt("Quantity");
                for (int idx = 0; idx < quantity; ++idx) {
                    this.itemPctViews[idx].setEnabled(true);
                    this.itemPctViews[idx].setText("" + exemplar.ammoCapacity);
                }
            }

            // Rebuild the ammo and attachment dropdowns for the new item
            this.ammoIdView.setChoices(this.buildAmmoChoices());
            for (int idx = 0; idx < this.attachmentIdViews.length; ++idx) {
                this.attachmentIdViews[idx].setChoices(this.buildAttachmentChoices(idx));
            }

            // Decide which fields to show
            this.setEnabledAll();
        } else if (view == this.ammoIdView) {
            //	    System.err.println(" ammoIdView");

            // Make the input more palatable
            if ((oldValue == null) || (oldValue.isEmpty())) {
                oldValue = "None";
            }
            if ((newValue == null) || (newValue.isEmpty())) {
                newValue = "None";
            }

            // If switching to new ammo, set variety
            if ((!newValue.equals("None")) && (!newValue.equals(oldValue))) {
                int ammoId = this.item.getInt("Ammo ID");
                ItemExemplar exemplar = (ItemExemplar) ItemExemplar.exemplarTable.get(ammoId);
                System.err.println("" + exemplar.ammoVariety);
                this.ammoVarietyView.setText("" + exemplar.ammoVariety);
                this.ammoQuantityView.setText("" + exemplar.ammoCapacity);
                this.ammoPctView.setText("100");
            }
        } else if (view == this.quantityView) {
            // Decide which fields to show
            this.setEnabledAll();
        } else {
            for (int idx = 0; idx < this.attachmentIdViews.length; ++idx) {
                if (view == this.attachmentIdViews[idx]) {
                    // Make the input more palatable
                    if ((oldValue == null) || (oldValue.isEmpty())) {
                        oldValue = "None";
                    }
                    if ((newValue == null) || (newValue.isEmpty())) {
                        newValue = "None";
                    }

                    // If switching attachment from none to something
                    if (oldValue.equals("None") && (!newValue.equals("None"))) {
                        this.attachmentPctViews[idx].setText("100");
                    }

                    break;
                }
            }
        }

        // Actor now dirty
        //	System.err.println("ItemDetailPanel.doValueChanged(" + view + ",'" + oldValue + "','" + newValue + "')");
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

