/*
 Copyright (c) 2013 Nokia Corporation. All rights reserved.
 Nokia and Nokia Connecting People are registered trademarks of Nokia Corporation.
 Oracle and Java are trademarks or registered trademarks of Oracle and/or its
 affiliates. Other product and company names mentioned herein may be trademarks
 or trade names of their respective owners.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package org.tantalum.formrssreader;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStoreFullException;
import org.tantalum.jme.RMSUtils;
import org.tantalum.storage.FlashDatabaseException;
import org.tantalum.util.L;

/**
 * Simple settings form for setting the RSS Feed URL
 *
 * @author ssaa
 */
public final class SettingsForm extends TextBox implements CommandListener {

    private final FormRSSReader midlet;
    private final Command saveCommand = new Command("Save", Command.OK, 0);
    private final Command backCommand = new Command("Back", Command.BACK, 0);

    public SettingsForm(final FormRSSReader midlet) {
        super("RSS Feed URL", "", 256, TextField.URL & TextField.NON_PREDICTIVE);
        setInitialInputMode("UCB_BASIC_LATIN");
        this.midlet = midlet;

        addCommand(saveCommand);
        addCommand(backCommand);
        setCommandListener(this);
    }

    public void setUrlValue(String url) {
        setString(url);
    }

    public String getUrlValue() {
        return getString();
    }

    public void commandAction(Command command, Displayable displayable) {
        try {
            if (command == saveCommand) {
                try {
                    RMSUtils.getInstance().write("settings", getString().getBytes());
                } catch (Exception ex) {
                    L.e("Can not write settings, attempting to delete settings", "", ex);
                    try {
                        RMSUtils.getInstance().delete("settings");
                        RMSUtils.getInstance().write("settings", getString().getBytes());
                    } catch (Exception ex1) {
                        L.e("Can not write settings", "", ex);
                    }
                }
                midlet.switchDisplayable(null, midlet.getList());
                midlet.getList().reload(true);
            } else if (command == backCommand) {
                midlet.switchDisplayable(null, midlet.getList());
            }
        } catch (FlashDatabaseException e) {
            //#debug
            L.e("Can not handle commandAction()", "command=" + command, e);
        }
    }
}
