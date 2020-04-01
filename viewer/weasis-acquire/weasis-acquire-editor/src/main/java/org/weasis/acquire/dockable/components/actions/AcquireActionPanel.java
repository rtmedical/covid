/*******************************************************************************
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.weasis.acquire.dockable.components.actions;

import org.weasis.acquire.explorer.AcquireImageInfo;
import org.weasis.acquire.explorer.AcquireImageValues;
import org.weasis.base.viewer2d.EventManager;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.editor.image.MouseActions;
import org.weasis.core.ui.editor.image.ViewerToolBar;

public interface AcquireActionPanel {
    void initValues(AcquireImageInfo info, AcquireImageValues values);

    String getLastActionCommand();

    void setLastActionCommand(String lastActionCommand);

    default void restoreLastAction() {
        String cmd = getLastActionCommand();
        if (cmd != null) {
            ImageViewerPlugin<ImageElement> container = EventManager.getInstance().getSelectedView2dContainer();
            if (container != null) {
                final ViewerToolBar<?> toolBar = container.getViewerToolBar();
                if (toolBar != null) {
                    setLastActionCommand(null);
                    MouseActions mouseActions = EventManager.getInstance().getMouseActions();
                    mouseActions.setAction(MouseActions.T_LEFT, cmd);
                    container.setMouseActions(mouseActions);
                    toolBar.changeButtonState(MouseActions.T_LEFT, cmd);
                }
            }
        }
    }

}
