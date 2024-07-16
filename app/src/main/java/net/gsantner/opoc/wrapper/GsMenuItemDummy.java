/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2022-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.wrapper;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

public class GsMenuItemDummy implements MenuItem {
    private final int _itemId;

    public GsMenuItemDummy(final int itemId) {
        _itemId = itemId;
    }

    @Override
    public int getItemId() {
        return _itemId;
    }

    @Override
    public int getGroupId() {
        return 0;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        return null;
    }

    @Override
    public MenuItem setTitle(int title) {
        return null;
    }

    @Override
    public CharSequence getTitle() {
        return null;
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        return null;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return null;
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        return null;
    }

    @Override
    public MenuItem setIcon(int iconRes) {
        return null;
    }

    @Override
    public Drawable getIcon() {
        return null;
    }

    @Override
    public MenuItem setIntent(Intent intent) {
        return null;
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        return null;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        return null;
    }

    @Override
    public char getNumericShortcut() {
        return 0;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        return null;
    }

    @Override
    public char getAlphabeticShortcut() {
        return 0;
    }

    @Override
    public MenuItem setCheckable(boolean checkable) {
        return null;
    }

    @Override
    public boolean isCheckable() {
        return false;
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        return null;
    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public MenuItem setVisible(boolean visible) {
        return null;
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean hasSubMenu() {
        return false;
    }

    @Override
    public SubMenu getSubMenu() {
        return null;
    }

    @Override
    public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        return null;
    }

    @Override
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return null;
    }

    @Override
    public void setShowAsAction(int actionEnum) {
    }

    @Override
    public MenuItem setShowAsActionFlags(int actionEnum) {
        return null;
    }

    @Override
    public MenuItem setActionView(View view) {
        return null;
    }

    @Override
    public MenuItem setActionView(int resId) {
        return null;
    }

    @Override
    public View getActionView() {
        return null;
    }

    @Override
    public MenuItem setActionProvider(ActionProvider actionProvider) {
        return null;
    }

    @Override
    public ActionProvider getActionProvider() {
        return null;
    }

    @Override
    public boolean expandActionView() {
        return false;
    }

    @Override
    public boolean collapseActionView() {
        return false;
    }

    @Override
    public boolean isActionViewExpanded() {
        return false;
    }

    @Override
    public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
        return null;
    }


    public static class Menu implements android.view.Menu {
        @Override
        public MenuItem add(CharSequence title) {
            return add(0, 0, 0, "");
        }

        @Override
        public MenuItem add(int titleRes) {
            return add(0, 0, 0, "");
        }

        @Override
        public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
            return new GsMenuItemDummy(itemId);
        }

        @Override
        public MenuItem add(int groupId, int itemId, int order, int titleRes) {
            return add(0, 0, 0, "");
        }

        @Override
        public SubMenu addSubMenu(CharSequence title) {
            return null;
        }

        @Override
        public SubMenu addSubMenu(int titleRes) {
            return null;
        }

        @Override
        public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
            return null;
        }

        @Override
        public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
            return null;
        }

        @Override
        public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems) {
            return 0;
        }

        @Override
        public void removeItem(int id) {
        }

        @Override
        public void removeGroup(int groupId) {
        }

        @Override
        public void clear() {
        }

        @Override
        public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
        }

        @Override
        public void setGroupVisible(int group, boolean visible) {
        }

        @Override
        public void setGroupEnabled(int group, boolean enabled) {
        }

        @Override
        public boolean hasVisibleItems() {
            return false;
        }

        @Override
        public MenuItem findItem(int id) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public MenuItem getItem(int index) {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
            return false;
        }

        @Override
        public boolean isShortcutKey(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean performIdentifierAction(int id, int flags) {
            return false;
        }

        @Override
        public void setQwertyMode(boolean isQwerty) {
        }
    }
}
