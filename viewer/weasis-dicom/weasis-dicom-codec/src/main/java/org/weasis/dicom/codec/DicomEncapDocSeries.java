/*******************************************************************************
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.weasis.dicom.codec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Tag;
import org.dcm4che3.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.media.MimeInspector;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.FileUtil;
import org.weasis.core.api.util.StringUtil;

public class DicomEncapDocSeries extends Series<DicomEncapDocElement> implements FilesExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DicomEncapDocSeries.class);

    public DicomEncapDocSeries(String subseriesInstanceUID) {
        super(TagW.SubseriesInstanceUID, subseriesInstanceUID, DicomSeries.defaultTagView);
    }

    public DicomEncapDocSeries(DicomSeries dicomSeries) {
        super(TagW.SubseriesInstanceUID, dicomSeries.getTagValue(TagW.SubseriesInstanceUID),
            DicomSeries.defaultTagView);

        Iterator<Entry<TagW, Object>> iter = dicomSeries.getTagEntrySetIterator();
        while (iter.hasNext()) {
            Entry<TagW, Object> e = iter.next();
            setTag(e.getKey(), e.getValue());
        }
    }

    @Override
    public void addMedia(DicomEncapDocElement media) {
        if (media != null && media.getMediaReader() instanceof DicomMediaIO) {
            DicomMediaIO dicomImageLoader = (DicomMediaIO) media.getMediaReader();
            String extension = ".tmp"; //$NON-NLS-1$
            Attributes ds = dicomImageLoader.getDicomObject();
            String mime = ds.getString(Tag.MIMETypeOfEncapsulatedDocument);
            String ext = MimeInspector.getExtensions(mime);
            if (StringUtil.hasText(extension)) {
                extension = "." + ext; //$NON-NLS-1$
            }
            // see http://dicom.nema.org/MEDICAL/Dicom/current/output/chtml/part03/sect_C.24.2.html
            Object data = dicomImageLoader.getDicomObject().getValue(Tag.EncapsulatedDocument);
            if (data instanceof BulkData) {
                BulkData bulkData = (BulkData) data;
                FileInputStream in = null;
                FileOutputStream out = null;
                try {
                    File file = File.createTempFile("encap_", extension, AppProperties.FILE_CACHE_DIR); //$NON-NLS-1$
                    in = new FileInputStream(media.getFile());
                    out = new FileOutputStream(file);
                    StreamUtils.skipFully(in, bulkData.offset());
                    StreamUtils.copy(in, out, bulkData.length());
                    media.setDocument(file);
                    this.add(media);
                } catch (Exception e) {
                    LOGGER.error("Cannot extract encapsulated document", e); //$NON-NLS-1$
                } finally {
                    FileUtil.safeClose(out);
                    FileUtil.safeClose(in);
                }
            }
        }
    }

    @Override
    public String getToolTips() {
        StringBuilder toolTips = new StringBuilder("<html>"); //$NON-NLS-1$
        addToolTipsElement(toolTips, Messages.getString("DicomSeries.pat"), TagD.get(Tag.PatientName)); //$NON-NLS-1$
        addToolTipsElement(toolTips, Messages.getString("DicomSeries.mod"), TagD.get(Tag.Modality)); //$NON-NLS-1$
        addToolTipsElement(toolTips, Messages.getString("DicomSeries.series_nb"), TagD.get(Tag.SeriesNumber)); //$NON-NLS-1$
        addToolTipsElement(toolTips, Messages.getString("DicomSeries.study"), TagD.get(Tag.StudyDescription)); //$NON-NLS-1$
        addToolTipsElement(toolTips, Messages.getString("DicomSeries.series"), TagD.get(Tag.SeriesDescription)); //$NON-NLS-1$
        addToolTipsElement(toolTips, Messages.getString("DicomSeries.date"), TagD.get(Tag.SeriesDate), TagD.get(Tag.SeriesTime)); //$NON-NLS-1$
        toolTips.append("</html>"); //$NON-NLS-1$
        return toolTips.toString();
    }

    @Override
    public String toString() {
        return (String) getTagValue(TagW.SubseriesInstanceUID);
    }

    @Override
    public String getMimeType() {
        return DicomMediaIO.SERIES_ENCAP_DOC_MIMETYPE;
    }

    @Override
    public List<File> getExtractFiles() {
        // Should have only one file as all the DicomEncapDocElement items are split in sub-series
        List<File> files = new ArrayList<>();
        getMedias(null, null).forEach(dcm -> files.add(dcm.getExtractFile())); // Synchronized iteration with forEach
        return files;
    }

}
