/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.futurice.lwuitrssreader;

import com.sun.lwuit.events.DataChangedListener;
import com.sun.lwuit.list.DefaultListModel;

/**
 *
 * @author tsaa
 */
public class ListModel extends DefaultListModel implements DataChangedListener {

    private ListForm listForm;

    public ListModel(ListForm listForm) {
        this.listForm = listForm;
        addDataChangedListener(this);
    }

    public void dataChanged(int type, int index) {
        listForm.repaint();
    }

    public void repaint() {
        listForm.repaint();
    }
}