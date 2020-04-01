/*******************************************************************************
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.weasis.launcher;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

public class CustomFileHandler extends FileHandler {
    static {
        new File(System.getProperty("user.home", "") + File.separator + ".weasis" + File.separator + "log").mkdirs(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
    
    public CustomFileHandler() throws IOException {
        super("%h/.weasis/log/boot-%u.log"); //$NON-NLS-1$
    }
}