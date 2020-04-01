/*******************************************************************************
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.weasis.dicom.explorer;

import java.awt.Desktop;
import java.io.File;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.media.MimeInspector;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.ui.editor.MimeSystemAppViewer;
import org.weasis.core.ui.editor.SeriesViewer;
import org.weasis.core.ui.editor.SeriesViewerFactory;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.FilesExtractor;

@org.osgi.service.component.annotations.Component(service = SeriesViewerFactory.class, immediate = false)
public class MimeSystemAppFactory implements SeriesViewerFactory {

    public static final String NAME = Messages.getString("MimeSystemAppViewer.app"); //$NON-NLS-1$
    public static final Icon ICON = new ImageIcon(MimeInspector.class.getResource("/icon/16x16/apps-system.png")); //$NON-NLS-1$
    public static final MimeSystemAppViewer mimeSystemViewer = new MimeSystemAppViewer() {

        @Override
        public String getPluginName() {
            return NAME;
        }

        @Override
        public void addSeries(MediaSeries series) {
            if (series instanceof FilesExtractor) {
                // As SUN JRE supports only Gnome and responds "true" for Desktop.isDesktopSupported()
                // in KDE session, but actually does not support it.
                // http://bugs.sun.com/view_bug.do?bug_id=6486393
                FilesExtractor extractor = (FilesExtractor) series;
                for (File file : extractor.getExtractFiles()) {
                    if (AppProperties.OPERATING_SYSTEM.startsWith("linux")) { //$NON-NLS-1$
                        startAssociatedProgramFromLinux(file);
                    } else if (AppProperties.OPERATING_SYSTEM.startsWith("win")) { //$NON-NLS-1$
                        // Workaround of the bug with mpg file see http://bugs.sun.com/view_bug.do?bug_id=6599987
                        startAssociatedProgramFromWinCMD(file);
                    } else if (Desktop.isDesktopSupported()) {
                        final Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.OPEN)) {
                            startAssociatedProgramFromDesktop(desktop, file);
                        }
                    }
                }
            }
        }

        @Override
        public String getDockableUID() {
            return null;
        }
    };

    public MimeSystemAppFactory() {
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public String getUIName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return NAME;
    }

    @Override
    public boolean canReadMimeType(String mimeType) {
        return DicomMediaIO.SERIES_VIDEO_MIMETYPE.equals(mimeType)
            || DicomMediaIO.SERIES_ENCAP_DOC_MIMETYPE.equals(mimeType);
    }

    @Override
    public boolean isViewerCreatedByThisFactory(SeriesViewer<? extends MediaElement> viewer) {
        return false;
    }

    @Override
    public SeriesViewer<? extends MediaElement> createSeriesViewer(Map<String, Object> properties) {
        return mimeSystemViewer;
    }

    @Override
    public int getLevel() {
        return 100;
    }

    @Override
    public boolean canAddSeries() {
        return false;
    }

    @Override
    public boolean canExternalizeSeries() {
        return false;
    }
}
