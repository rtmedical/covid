/*******************************************************************************
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.weasis.core.api.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.felix.prefs.BackingStore;
import org.apache.felix.prefs.BackingStoreManager;
import org.apache.felix.prefs.PreferencesDescription;
import org.apache.felix.prefs.PreferencesImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.ClosableURLConnection;
import org.weasis.core.api.util.EscapeChars;
import org.weasis.core.api.util.FileUtil;
import org.weasis.core.api.util.NetworkUtil;
import org.weasis.core.api.util.StringUtil;
import org.weasis.core.api.util.URLParameters;

/**
 * This is an abstract implementation of a backing store which uses streams to read/write the preferences and stores a
 * complete preferences tree in a single stream.
 */
public class StreamBackingStoreImpl implements BackingStore {
    private static final String PREFS_TAG = "preferences"; //$NON-NLS-1$

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamBackingStoreImpl.class);

    /** The bundle context. */
    protected final BundleContext bundleContext;
    private final File prefRootDirectory;

    public StreamBackingStoreImpl(BundleContext context) {
        this.bundleContext = context;
        String prefPath = BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.pref.dir"); //$NON-NLS-1$
        this.prefRootDirectory = new File(prefPath);
        prefRootDirectory.mkdirs();
    }

    /**
     * This method is invoked to check if the backing store is accessible right now.
     *
     * @throws BackingStoreException
     */
    protected void checkAccess() throws BackingStoreException {
        if (prefRootDirectory == null || !prefRootDirectory.exists()) {
            throw new BackingStoreException("Saving of data files to the bundle context is currently not supported."); //$NON-NLS-1$
        }
    }

    /**
     * Get the output stream to write the preferences.
     */
    protected OutputStream getOutputStream(PreferencesDescription desc) throws IOException {
        File file = getFile(desc);
        // Write user folder if not exists
        FileUtil.prepareToWriteFile(file);
        return new FileOutputStream(file);
    }

    @Override
    public Long[] availableBundles() {
        return new Long[0];
    }

    protected PreferencesDescription getDescription(File file) {
        final String fileName = file.getName();
        // parse the file name to get: bundle id, user|system identifier
        if (fileName.endsWith(".xml")) { //$NON-NLS-1$
            final String key = fileName.substring(0, fileName.length() - 4);

            Bundle[] bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(key)) {
                    return new PreferencesDescription(bundle.getBundleId(), AppProperties.WEASIS_USER);
                }
            }
        }
        return null;
    }

    @Override
    public void remove(Long bundleId) throws BackingStoreException {
        // Do nothing, we don't want to delete the preferences file when a bundle is uninstalled
    }

    @Override
    public PreferencesImpl[] loadAll(BackingStoreManager manager, Long bundleId) throws BackingStoreException {
        this.checkAccess();

        PreferencesImpl pref = load(manager, new PreferencesDescription(bundleId, AppProperties.WEASIS_USER));

        return pref == null ? new PreferencesImpl[0] : new PreferencesImpl[] { pref };
    }

    /**
     * Get the file for the preferences tree.
     *
     * @param desc
     * @return the preference file
     */
    protected File getFile(PreferencesDescription desc) {
        Bundle bundle = bundleContext.getBundle(desc.getBundleId());
        if (bundle == null) {
            return null;
        }
        return new File(prefRootDirectory, bundle.getSymbolicName() + ".xml"); //$NON-NLS-1$
    }

    protected String getURL(PreferencesDescription desc, String prefUrl) throws UnsupportedEncodingException {
        Bundle bundle = bundleContext.getBundle(desc.getBundleId());
        if (bundle == null) {
            return null;
        }
        if (StringUtil.hasText(prefUrl)) {

            return String.format("%s?user=%s&profile=%s&module=%s", prefUrl, //$NON-NLS-1$
                BundleTools.getEncodedValue(AppProperties.WEASIS_USER),
                BundleTools.getEncodedValue(AppProperties.WEASIS_PROFILE),
                BundleTools.getEncodedValue(bundle.getSymbolicName()));
        }
        return null;
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#load(org.apache.felix.prefs.BackingStoreManager,
     *      org.apache.felix.prefs.PreferencesDescription)
     */
    @Override
    public PreferencesImpl load(BackingStoreManager manager, PreferencesDescription desc) throws BackingStoreException {
        final PreferencesImpl prefs = loadFromeFile(manager, desc);

        PreferencesImpl remoteprefs = loadFromService(manager, desc);
        if (remoteprefs != null) {
            // merge with saved version
            final PreferencesImpl n = remoteprefs.getOrCreateNode(prefs.absolutePath());
            update(prefs, n);
        }
        return prefs;
    }

    protected static void update(PreferencesImpl prefs, PreferencesImpl update) {
        for (Entry<String, String> entry : update.getProperties().entrySet()) {
            String val = prefs.getProperties().get(entry.getKey());
            if (!Objects.equals(val, entry.getValue())) {
                prefs.put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, PreferencesImpl> children = new HashMap<>();
        for (PreferencesImpl child : prefs.getChildren()) {
            children.put(child.name(), child);
        }

        Collection<PreferencesImpl> uchildren = update.getChildren();

        if (update.getProperties().size() < prefs.getProperties().size() || uchildren.size() < children.size()) {
            // Force the changeset to store remote prefs
            prefs.getChangeSet()
                .propertyChanged(prefs.getProperties().keySet().stream().findFirst().orElseGet(() -> "")); //$NON-NLS-1$
        }

        for (PreferencesImpl child : uchildren) {
            final String name = child.name();
            if (!children.containsKey(name)) {
                // create node
                prefs.node(name);
            }
            update(children.get(name), child);
        }
    }

    protected static void clearAllChangeSet(PreferencesImpl prefs) {
        prefs.getChangeSet().clear();
        for (PreferencesImpl child : prefs.getChildren()) {
            clearAllChangeSet(child);
        }
    }

    protected PreferencesImpl loadFromeFile(BackingStoreManager manager, PreferencesDescription desc)
        throws BackingStoreException {
        this.checkAccess();
        final File file = getFile(desc);
        if (file != null && file.exists()) {
            XMLStreamReader xmler = null;
            try (FileInputStream fileReader = new FileInputStream(file)) {
                final PreferencesImpl root = new PreferencesImpl(desc, manager);
                XMLInputFactory factory = XMLInputFactory.newInstance();
                // disable external entities for security
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
                xmler = factory.createXMLStreamReader(fileReader);
                readStream(root, xmler);
                return readStream(root, xmler);
            } catch (XMLStreamException | IOException e) {
                throw new BackingStoreException("Unable to load preferences.", e); //$NON-NLS-1$
            } finally {
                FileUtil.safeClose(xmler);
            }
        }
        return null;
    }

    private static URLParameters getURLParameters(boolean post) {
        Map<String, String> map = new HashMap<>(BundleTools.SESSION_TAGS_FILE);
        map.put(post ? "Content-Type" : "Accept", "application/xml"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return new URLParameters(map, post);
    }

    protected PreferencesImpl loadFromService(BackingStoreManager manager, PreferencesDescription desc)
        throws BackingStoreException {
        String prefUrl = BundleTools.getServiceUrl();
        if (StringUtil.hasText(prefUrl) && (!BundleTools.isLocalSession() || BundleTools.isStoreLocalSession())) {
            XMLStreamReader xmler = null;
            try (ClosableURLConnection c = NetworkUtil.getUrlConnection(getURL(desc, prefUrl), getURLParameters(false));
                            InputStream fileReader = c.getInputStream()) {
                final PreferencesImpl root = new PreferencesImpl(desc, manager);
                XMLInputFactory factory = XMLInputFactory.newInstance();
                // disable external entities for security
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
                xmler = factory.createXMLStreamReader(fileReader);
                return readStream(root, xmler);
            } catch (XMLStreamException | IOException e) {
                throw new BackingStoreException("Unable to load preferences.", e); //$NON-NLS-1$
            } finally {
                FileUtil.safeClose(xmler);
            }
        }
        return null;
    }

    private PreferencesImpl readStream(PreferencesImpl root, XMLStreamReader xmler) throws XMLStreamException {
        int eventType;
        while (xmler.hasNext()) {
            eventType = xmler.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String key = xmler.getName().getLocalPart();
                if (PREFS_TAG.equals(key)) { // $NON-NLS-1$
                    while (xmler.hasNext()) {
                        eventType = xmler.next();
                        if (eventType == XMLStreamConstants.START_ELEMENT) {
                            // It is a child of the preferences node
                            this.read(root, xmler, xmler.getName().getLocalPart());
                        }
                    }
                }
            }
        }
        root.getChangeSet().clear();
        return root;
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#store(org.apache.felix.prefs.PreferencesImpl)
     */
    @Override
    public void store(PreferencesImpl prefs) throws BackingStoreException {
        // If no change, do not store
        if (!hasChanges(prefs)) {
            return;
        }

        PreferencesImpl rootPrefs = prefs.getRoot();
        try {
            storeInStream(rootPrefs);
        } catch (IOException e) {
            LOGGER.error("Cannot store preference file", e); //$NON-NLS-1$
        }

        String prefUrl = BundleTools.getServiceUrl();
        if (prefUrl != null) {
            try {
                remoteStore(rootPrefs, prefUrl);
            } catch (BackingStoreException | IOException e) {
                LOGGER.error("Cannot store preference file", e); //$NON-NLS-1$
            }
        }
    }

    private void remoteStore(PreferencesImpl prefs, String prefUrl) throws IOException, BackingStoreException {
        if (!BundleTools.isLocalSession() || BundleTools.isStoreLocalSession()) {
            File file = getFile(prefs.getDescription());
            if (file != null && file.exists()) {
                URLParameters urlParams = getURLParameters(true);
                ClosableURLConnection http =
                    NetworkUtil.getUrlConnection(getURL(prefs.getDescription(), prefUrl), urlParams);
                try (OutputStream out = http.getOutputStream()) {
                    writeStream(new FileInputStream(file), out);
                }
                if (http.getUrlConnection() instanceof HttpURLConnection) {
                    NetworkUtil.readResponse((HttpURLConnection) http.getUrlConnection(), urlParams.getHeaders());
                }
            }
        }
    }

    private static void writeStream(InputStream inputStream, OutputStream out) throws IOException {
        try {
            byte[] buf = new byte[FileUtil.FILE_BUFFER];
            int offset;
            while ((offset = inputStream.read(buf)) > 0) {
                out.write(buf, 0, offset);
            }
            out.flush();
        } finally {
            FileUtil.safeClose(inputStream);
        }
    }

    protected void storeInStream(PreferencesImpl prefs) throws BackingStoreException, IOException {
        final PreferencesImpl rootPrefs = prefs.getRoot();
        XMLStreamWriter writer = null;
        try (final OutputStream output = getOutputStream(rootPrefs.getDescription())) {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            writer = factory.createXMLStreamWriter(output, "UTF-8"); //$NON-NLS-1$
            writer.writeStartDocument("UTF-8", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
            writer.writeStartElement(PREFS_TAG); // $NON-NLS-1$
            write(rootPrefs, writer);
            writer.writeEndElement();
            writer.writeEndDocument();
            clearAllChangeSet(rootPrefs);
        } catch (XMLStreamException e) {
            throw new BackingStoreException("Unable to store preferences.", e); //$NON-NLS-1$
        } finally {
            FileUtil.safeClose(writer);
        }

    }

    /**
     * Has the tree changes.
     *
     * @param prefs
     *            the prefs
     * @return true, if successful
     */
    protected boolean hasChanges(PreferencesImpl prefs) {
        if (prefs.getChangeSet().hasChanges()) {
            return true;
        }
        final Iterator<?> i = prefs.getChildren().iterator();
        while (i.hasNext()) {
            final PreferencesImpl current = (PreferencesImpl) i.next();
            if (this.hasChanges(current)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#update(org.apache.felix.prefs.PreferencesImpl)
     */
    @Override
    public void update(PreferencesImpl prefs) throws BackingStoreException {
        // Do nothing, only update when writing
    }

    /**
     * Write the preferences recursively to the output stream.
     *
     * @param prefs
     * @param os
     * @throws IOException
     * @throws XMLStreamException
     */
    protected void write(PreferencesImpl prefs, XMLStreamWriter writer) throws XMLStreamException {
        final int size = prefs.getProperties().size();
        if (size > 0) {
            this.writePreferences(prefs, writer);
        }
        final Collection<?> children = prefs.getChildren();
        final Iterator<?> i = children.iterator();
        while (i.hasNext()) {
            final PreferencesImpl child = (PreferencesImpl) i.next();
            writer.writeStartElement(child.name());
            this.write(child, writer);
            writer.writeEndElement();
        }
        writer.flush();
    }

    protected void read(PreferencesImpl prefs, XMLStreamReader xmler, String startKey) throws XMLStreamException {
        int eventType;
        while (xmler.hasNext()) {
            eventType = xmler.next();
            switch (eventType) {
                // It is a properties of the node
                case XMLStreamConstants.CHARACTERS:
                    prefs.getProperties().put(startKey, xmler.getText());
                    break;
                // It is a child of the node
                case XMLStreamConstants.START_ELEMENT:
                    PreferencesImpl impl = prefs.getOrCreateNode(startKey);
                    this.read(impl, xmler, xmler.getName().getLocalPart());
                    impl.getChangeSet().clear();
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    // In case the tag does not contain values or inner tag
                    if (prefs.getProperties().isEmpty() && prefs.getChildren().isEmpty()) {
                        prefs.getOrCreateNode(startKey);
                    }
                    if (startKey.equals(xmler.getName().getLocalPart())) {
                        return; // Return to the parent tag
                    }
                    break;
                default:
                    break;
            }
        }

    }

    protected void writePreferences(PreferencesImpl prefs, XMLStreamWriter writer) throws XMLStreamException {
        final Iterator<?> i = prefs.getProperties().entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            writer.writeStartElement(entry.getKey().toString());
            writer.writeCharacters(EscapeChars.forXML(entry.getValue().toString()));
            writer.writeEndElement();
        }
        writer.flush();
    }
}
