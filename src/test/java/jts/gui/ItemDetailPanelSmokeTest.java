package jts.gui;

/*
  Smoke tests for the Swing item detail panel. These construct the panel
  headless (no display needed) and drive the real data flow: refresh,
  commit-on-edit, invalid-input revert, item switching and per-category
  enable/disable.
*/

import jts.data.Item;
import jts.data.ItemExemplar;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;

public class ItemDetailPanelSmokeTest {
    static {
        // The AWT/Swing components used here are all headless-safe; the
        // panel itself must never require a display in these tests.
        System.setProperty("java.awt.headless", "true");
    }

    private Item makeItem(int id, int quantity) {
        byte[] data = new byte[Item.ITEM_LENGTH];
        Item item = new Item(data);
        item.setInt("Item ID", id);
        item.setInt("Quantity", quantity);
        return item;
    }

    /**
     * The nth JChoiceView child (0-based), in construction order.
     */
    private JChoiceView findChoice(ItemDetailPanel panel, int which) {
        int seen = 0;
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof JChoiceView) {
                if (seen == which) {
                    return (JChoiceView) c;
                }
                ++seen;
            }
        }
        return null;
    }

    /**
     * True if the choice view offers the named entry.
     */
    private boolean hasChoice(JChoiceView view, String name) {
        for (int idx = 0; idx < view.getItemCount(); ++idx) {
            if (name.equals(view.getItemAt(idx))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The nth JNumberView child (0-based), in construction order.
     */
    private JNumberView findNumber(ItemDetailPanel panel, int which) {
        int seen = 0;
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof JNumberView) {
                if (seen == which) {
                    return (JNumberView) c;
                }
                ++seen;
            }
        }
        return null;
    }

    @Test
    public void constructsWithSwingComponents() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        assertTrue("panel should be a Swing container", panel instanceof JPanel);
        assertTrue("panel should have children", panel.getComponentCount() > 0);

        int labels = 0, numbers = 0, choices = 0;
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof JLabel) ++labels;
            if (c instanceof JNumberView) ++numbers;
            if (c instanceof JChoiceView) ++choices;
        }
        assertEquals("expected 18 labels", 18, labels);
        assertEquals("expected 16 number views", 16, numbers);
        assertEquals("expected 6 choice views", 6, choices);
    }

    @Test
    public void refreshShowsItem() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        panel.setItem(makeItem(1, 2)); // Glock 17, qty 2

        assertEquals("Glock 17", findChoice(panel, 0).getSelectedItem());
        assertEquals("2", findNumber(panel, 0).getText());
    }

    @Test
    public void editCommitsToStruct() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        Item item = makeItem(1, 2);
        panel.setItem(item);

        findNumber(panel, 0).setText("5");
        assertEquals(5, item.getInt("Quantity"));
        assertTrue("panel should be marked modified", panel.isModified());
    }

    @Test
    public void invalidInputRevertsToLastValidValue() throws Exception {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        Item item = makeItem(1, 2);
        panel.setItem(item);

        findNumber(panel, 0).setText("5");
        findNumber(panel, 0).setText("abc");

        // The revert is deferred via SwingUtilities.invokeLater; flush the queue
        SwingUtilities.invokeAndWait((Runnable) () -> {
        });

        int shown = Integer.parseInt(findNumber(panel, 0).getText());
        assertEquals("field should show the last valid value", item.getInt("Quantity"), shown);
    }

    @Test
    public void itemSwitchUpdatesDetails() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        Item item = makeItem(1, 2);
        panel.setItem(item);

        findChoice(panel, 0).setSelectedItem("Beretta 92F");
        assertEquals(3, item.getInt("Item ID"));
        assertEquals("weapon quantity should reset to 1", "1", findNumber(panel, 0).getText());
    }

    @Test
    public void categoryEnablesRelevantFields() {
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

    @Test
    public void armorSlotFilterRestrictsItemChoices() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        // Steel Helmet (0x00B0) in the helmet slot
        panel.setItem(makeItem(0x00B0, 1),
                Integer.valueOf(ItemExemplar.HELMET_CATEGORY));

        JChoiceView idView = findChoice(panel, 0);
        assertEquals("Steel Helmet", idView.getSelectedItem());
        assertTrue("None must stay available", hasChoice(idView, "None"));
        assertTrue("helmet items must be offered", hasChoice(idView, "Steel Helmet"));
        assertTrue("helmet items must be offered", hasChoice(idView, "Kevlar Helmet"));
        assertFalse("weapons must be filtered out", hasChoice(idView, "Glock 17"));
        assertFalse("body armor must be filtered out", hasChoice(idView, "Kevlar Vest"));
        assertFalse("leg armor must be filtered out", hasChoice(idView, "Kevlar Leggings"));
    }

    @Test
    public void unfilteredSlotKeepsFullItemList() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        panel.setItem(makeItem(1, 2)); // Glock 17, no category restriction

        JChoiceView idView = findChoice(panel, 0);
        assertTrue("weapons must be offered", hasChoice(idView, "Glock 17"));
        assertTrue("helmet items must be offered", hasChoice(idView, "Steel Helmet"));
    }

    @Test
    public void outOfCategoryItemStaysVisible() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        // Combat Knife (0x0025) in the helmet slot: not a helmet, but must
        // stay visible so the UI doesn't lie about what's equipped
        panel.setItem(makeItem(0x0025, 1),
                Integer.valueOf(ItemExemplar.HELMET_CATEGORY));

        JChoiceView idView = findChoice(panel, 0);
        assertEquals("Combat Knife", idView.getSelectedItem());
        assertTrue("out-of-category item must stay visible", hasChoice(idView, "Combat Knife"));
        assertTrue("helmet items must still be offered", hasChoice(idView, "Steel Helmet"));
    }

    @Test
    public void unknownItemFallsBackToNone() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        // 0x0073 is not in the exemplar table (unknown item id)
        panel.setItem(makeItem(0x0073, 1),
                Integer.valueOf(ItemExemplar.HELMET_CATEGORY));

        JChoiceView idView = findChoice(panel, 0);
        assertEquals("None", idView.getSelectedItem());
    }

    @Test
    public void ammoDropdownIsRestrictedToAmmo() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        panel.setItem(makeItem(1, 2)); // Glock 17

        JChoiceView ammoView = findChoice(panel, 1);
        assertTrue("ammo items must be offered", hasChoice(ammoView, "9mm Pistol Magazine"));
        assertFalse("weapons must be filtered out", hasChoice(ammoView, "Glock 17"));
        assertFalse("weapon attachments must be filtered out", hasChoice(ammoView, "Silencer"));
        assertFalse("armor attachments must be filtered out", hasChoice(ammoView, "Ceramic Plates"));
    }

    @Test
    public void outOfCategoryAmmoStaysVisible() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        Item item = makeItem(1, 2); // Glock 17
        item.setInt("Ammo ID", 0x0083); // Stun Grenade stuffed in the ammo slot
        panel.setItem(item);

        JChoiceView ammoView = findChoice(panel, 1);
        assertEquals("Stun Grenade", ammoView.getSelectedItem());
        assertTrue("out-of-category ammo must stay visible", hasChoice(ammoView, "Stun Grenade"));
        assertTrue("ammo items must still be offered", hasChoice(ammoView, "9mm Pistol Magazine"));
    }

    @Test
    public void weaponAttachmentDropdownIsRestrictedToWeaponAttachments() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        panel.setItem(makeItem(1, 2)); // Glock 17

        JChoiceView attachmentView = findChoice(panel, 2);
        assertTrue("weapon attachments must be offered", hasChoice(attachmentView, "Silencer"));
        assertTrue("weapon attachments must be offered", hasChoice(attachmentView, "Sniper Scope"));
        assertFalse("armor attachments must be filtered out", hasChoice(attachmentView, "Ceramic Plates"));
        assertFalse("ammo must be filtered out", hasChoice(attachmentView, "9mm Pistol Magazine"));
    }

    @Test
    public void armorAttachmentDropdownOffersCeramicPlates() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        panel.setItem(makeItem(0x00A1, 1)); // Flak Jacket (body armor)

        JChoiceView attachmentView = findChoice(panel, 2);
        assertTrue("armor attachments must be offered", hasChoice(attachmentView, "Ceramic Plates"));
        assertFalse("weapon attachments must be filtered out", hasChoice(attachmentView, "Silencer"));
    }

    @Test
    public void itemSwitchRebuildsAttachmentDropdown() {
        ItemDetailPanel panel = new ItemDetailPanel(null);
        panel.setItem(makeItem(1, 2)); // Glock 17 first

        JChoiceView attachmentView = findChoice(panel, 2);
        assertTrue("weapon attachments must be offered", hasChoice(attachmentView, "Silencer"));

        // Switch the item to body armor; the attachment dropdown must follow
        findChoice(panel, 0).setSelectedItem("Flak Jacket");
        assertTrue("armor attachments must be offered", hasChoice(attachmentView, "Ceramic Plates"));
        assertFalse("weapon attachments must be filtered out", hasChoice(attachmentView, "Silencer"));
    }
}
