/*******************************************************************************
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.weasis.dicom.rt;

import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.DicomSpecialElement;
import org.weasis.dicom.codec.DicomSpecialElementFactory;

/**
 * 
 * @author Tomas Skripcak
 * @author Nicolas Roduit
 */
@org.osgi.service.component.annotations.Component(service = DicomSpecialElementFactory.class, immediate = false)
public class RTElementFactory implements DicomSpecialElementFactory {

    public static final String SERIES_RT_MIMETYPE = "rt/dicom"; //$NON-NLS-1$

    private static final String[] modalities = { "RTSTRUCT", "RTPLAN", "RTDOSE" }; //$NON-NLS-1$

    @Override
    public String getSeriesMimeType() {
        return SERIES_RT_MIMETYPE;
    }

    @Override
    public String[] getModalities() {
        return modalities;
    }

    @Override
    public DicomSpecialElement buildDicomSpecialElement(DicomMediaIO mediaIO) {
        return new RtSpecialElement(mediaIO);
    }

}
