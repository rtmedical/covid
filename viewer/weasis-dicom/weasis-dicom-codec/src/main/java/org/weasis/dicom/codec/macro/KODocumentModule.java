/*******************************************************************************
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.weasis.dicom.codec.macro;

import java.util.Collection;
import java.util.Date;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;

public class KODocumentModule extends Module {

    public KODocumentModule(Attributes dcmItems) {
        super(dcmItems);
    }

    public String getInstanceNumber() {
        return dcmItems.getString(Tag.InstanceNumber);
    }

    public void setInstanceNumber(String s) {
        dcmItems.setString(Tag.InstanceNumber, VR.IS, s);
    }

    public Date getContentDateTime() {
        return dcmItems.getDate(Tag.ContentDateAndTime);
    }

    public void setContentDateTime(Date d) {
        dcmItems.setDate(Tag.ContentDateAndTime, d);
    }

    public Collection<ReferencedRequest> getReferencedRequests() {
        return ReferencedRequest.toReferencedRequestMacros(dcmItems.getSequence(Tag.ReferencedRequestSequence));
    }

    public void setReferencedRequest(Collection<ReferencedRequest> refrqs) {
        updateSequence(Tag.ReferencedRequestSequence, refrqs);
    }

    public Collection<HierachicalSOPInstanceReference> getCurrentRequestedProcedureEvidences() {
        return HierachicalSOPInstanceReference.toHierachicalSOPInstanceReferenceMacros(
            dcmItems.getSequence(Tag.CurrentRequestedProcedureEvidenceSequence));
    }

    public void setCurrentRequestedProcedureEvidences(Collection<HierachicalSOPInstanceReference> refs) {
        updateSequence(Tag.CurrentRequestedProcedureEvidenceSequence, refs);
    }

    public Collection<HierachicalSOPInstanceReference> getIdenticalDocuments() {
        return HierachicalSOPInstanceReference
            .toHierachicalSOPInstanceReferenceMacros(dcmItems.getSequence(Tag.IdenticalDocumentsSequence));
    }

    public void setIdenticalDocuments(Collection<HierachicalSOPInstanceReference> refs) {
        updateSequence(Tag.IdenticalDocumentsSequence, refs);
    }

}
