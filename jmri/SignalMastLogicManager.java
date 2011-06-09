package jmri;

import java.util.ArrayList;

import jmri.jmrit.display.layoutEditor.LayoutEditor;

/**
 *
 * <hr>
 * This file is part of JMRI.
 * <P>
 * JMRI is free software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published
 * by the Free Software Foundation. See the "COPYING" file for a copy
 * of this license.
 * <P>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 * <P>
 *
 * @author			Kevin Dickerson Copyright (C) 2011
 * @version			$Revision: 1.3 $
 */
public interface SignalMastLogicManager extends Manager {

    public void addDestinationMastToLogic(SignalMastLogic src, SignalMast destination);

    /**
     * Discover all possible valid source and destination signalmasts past pairs
     * on all layout editor panels.
     * @return A has Hashtable, of each source signalmast, with an arraylist of
     * all the valid destination signalmast.
     */
    public void automaticallyDiscoverSignallingPairs() throws JmriException;

    /**
     * This uses the layout editor to check if the destination signalmast is
     * reachable from the source signalmast
     *
     * @param sourceMast Source SignalMast
     * @param destMast Destination SignalMast
     * @return true if valid, false if not valid.
     */
    public boolean checkValidDest(SignalMast sourceMast, SignalMast destMast) throws JmriException;

    /**
     * Discover valid destination signalmasts for a given source signal on a
     * given layout editor panel.
     * @param source Source SignalMast
     * @param layout Layout Editor panel to check.
     */
    public void discoverSignallingDest(SignalMast source, LayoutEditor layout) throws JmriException;

    public void dispose();

    /**
     * Gather a list of all the signal mast logics, by destination signal mast
     */
    public ArrayList<SignalMastLogic> getLogicsByDestination(SignalMast destination);

    public long getSignalLogicDelay();

    public SignalMastLogic getSignalMastLogic(SignalMast source);

    /**
     * Returns an arraylist of signalmastlogic
     * @return An ArrayList of SignalMast logics
     */
    public ArrayList<SignalMastLogic> getSignalMastLogicList();

    /**
     * Used to initialise all the signalmast logics. primarily used after loading.
     */
    public void initialise();

    public SignalMastLogic newSignalMastLogic(SignalMast source);

    public void removeDestinationMastToLogic(SignalMastLogic src, SignalMast destination);

    /**
     * Remove a destination mast from the signalmast logic
     * @param sml The signalmast logic of the source signal
     * @param dest The destination mast
     */
    public void removeSignalMastLogic(SignalMastLogic sml, SignalMast dest);

    /**
     * Completely remove the signalmast logic.
     */
    public void removeSignalMastLogic(SignalMastLogic sml);

    public void setSignalLogicDelay(long l);

}
