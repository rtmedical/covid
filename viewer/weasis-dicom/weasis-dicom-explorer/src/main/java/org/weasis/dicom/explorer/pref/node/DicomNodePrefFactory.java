/*******************************************************************************
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.weasis.dicom.explorer.pref.node;

import java.util.Hashtable;

import org.weasis.core.api.gui.Insertable;
import org.weasis.core.api.gui.Insertable.Type;
import org.weasis.core.api.gui.PreferencesPageFactory;
import org.weasis.core.api.gui.util.AbstractItemDialogPage;

@org.osgi.service.component.annotations.Component(service = PreferencesPageFactory.class, immediate = false)
public class DicomNodePrefFactory implements PreferencesPageFactory {

    @Override
    public AbstractItemDialogPage createInstance(Hashtable<String, Object> properties) {
        return new DicomNodeListView();
    }

    @Override
    public void dispose(Insertable component) {
    }

    @Override
    public boolean isComponentCreatedByThisFactory(Insertable component) {
        return component instanceof DicomNodeListView;
    }

    @Override
    public Type getType() {
        return Type.PREFERENCES;
    }

}
