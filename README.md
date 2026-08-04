# JAME: A Jagged Alliance 2 Merc Editor

Forked from JAPE by Douglas Greiman
[http://www.duggelz.org/jape](http://www.duggelz.org/jape)

## Table of Contents

1. [About JAPE](#1-about-jape)
2. [Installation](#3-installation)
3. [Running JAME](#4-running-jape)
4. [Support](#5-support)
5. [Changes](#6-changes)

## 1. About JAME

JAME is a save game editor for Jagged Alliance 2. JAME allows you to
view and edit the items and attributes of your mercenaries, or any
other character in the game.

JAME works with the following versions of Jagged Alliance 2:

 * US version 1.03      (Build 99.06.09)
 * US version 1.04      (Build 99.08.11)
 * US version 1.05r     (Build 99.09.15)
 * US version 1.06      (Build 99.11.12)
 * US version 1.07      (Build 00.05.29)
 * US version 1.12      (Build 04.12.-2)
 * US version 1.13-2085 (Build 08.04.27)
 * JA2 Stracciatella     (version 102, Build 04.12.02)

Other versions may work, but are untested.

Only stats editing is possible for version 1.13 save games, not item
editing.


## 3. Installation

Get a release package for your system from the releases section. 

## 4. Running JAPE

### 4.1

Although the application is written in Java, the application packages include the java runtime needed to run the application. No JDK/JRE installation necessary.

### 4.2 Opening a Saved Game

Use the Open button in the toolbar or in the "File" menu, then navigate to the SavedGames
directory and select a saved game file. The saved games will be named
SaveGame01.sav, SaveGame02.sav, etc.

### 4.3 Editing Character Data

The left side of the JAME window contains a list of all the editable
characters in the saved game. If the opened save file is readable, the list will have 3 sections:
"Active", "Recruitable" and "Other actors". 
. Click on the name of a character, and
his/her statistics will be displayed on the right side of the window. Inventory edits only works for mercs recognized as "Active".

The middle of the JAME windows contains detailed information on a
single character. Some information is only present for your currently
hired mercenaries. Most of the fields are editable. Go ahead, give
your custom merc 99 Marksmanship.

The right side of the window show the
selected merc's equipment. Again, this information is only
editable for your currently hired team members.

### 4.4 Save Your Changes

Autosave is enabled by default. Any edits will be committed to the opened file (with a 0,3s debounce).

Alternatively use the Save-button on the toolbar or in the "File" menu.


## 5. Support

JAME comes with NO WARRANTIES, WHETHER EXPRESS OR IMPLIED, INCLUDING,
BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE.

However suggestions, bugs, or feedbacks are welcome in the issues section.

jhsveli@gmail.com

Please put JAPE in the subject line.

## 6. Changes

### 6.1 After fork:

**Changes in 0.5**
* In limited slots (Attachments, Armor, Headgear), filter the item dropdown to allowed items. 
* Add autosave function
* Add toolbar buttons
* Change GUI framework to Swing with FlatLaf Look and feel
* Section the actor list, to get Active/Rostered mercs to the top
* Copy and paste functions
* Support save version used by Ja2 Straciatella

### 6.2 Changes Pre-fork

**Changes in 0.41**

* Stat editing for JA2 version 1.13

**Changes in 0.40**

* Recompiled against a newer Java JVM
* Minor bugfixes in save game format detection

**Changes in 0.32**

* Fixed camouflage kit/boobytrap kit mixup
* Fixed a bug where the 7th and later mercs don't show up as hired

**Changes in 0.31**

* Fixed attachment boxes to allow grenades and detonators

**Changes in 0.30**

* Input boxes silently and automatically ignore bad input
* no.jts.Item editor disables irrelevant fields
* no.jts.Item editor fills in many fields automatically
* Editor is smarter about when save is not needed
* Added ability to edit money amount
