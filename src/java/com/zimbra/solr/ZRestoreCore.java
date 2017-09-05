package com.zimbra.solr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.DirectoryFactory;
import org.apache.solr.core.DirectoryFactory.DirContext;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.IndexFetcher;
import org.apache.solr.util.PropertiesInputStream;
import org.apache.solr.util.PropertiesOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Greg Solovyev
 * Implementation of https://issues.apache.org/jira/browse/SOLR-7583
 * TODO: remove when SOLR-7583 is released and update ZimbraBackup to use /replication instead of /zbnr
 *
 */
public class ZRestoreCore implements Callable<Boolean> {
    public static final String INDEX_PROPERTIES = "index.properties";
    private static final Logger LOG = LoggerFactory.getLogger(ZRestoreCore.class.getName());

    private final String backupName;
    private final String backupLocation;
    private final SolrCore core;
    private final boolean deleteTmpDir;

    public ZRestoreCore(SolrCore core, String location, String name, boolean deleteTmpDir) {
        this.core = core;
        this.backupLocation = location;
        this.backupName = name;
        this.deleteTmpDir = deleteTmpDir;
    }

    @Override
    public Boolean call() throws Exception {
        return doRestore();
    }

    private boolean doRestore() throws Exception {

        Path backupPath = Paths.get(backupLocation).resolve(backupName);
        String restoreIndexName = "restore." + backupName;
        String restoreIndexPath = core.getDataDir() + restoreIndexName;

        Directory restoreIndexDir = null;
        Directory indexDir = null;
        try (Directory backupDir = FSDirectory.open(backupPath)) {

            restoreIndexDir = core.getDirectoryFactory().get(restoreIndexPath, DirectoryFactory.DirContext.DEFAULT,
                    core.getSolrConfig().indexConfig.lockType);

            // Prefer local copy.
            indexDir = core.getDirectoryFactory().get(core.getIndexDir(), DirectoryFactory.DirContext.DEFAULT,
                    core.getSolrConfig().indexConfig.lockType);

            // Move all files from backupDir to restoreIndexDir
            for (String filename : backupDir.listAll()) {
                checkInterrupted();
                LOG.debug("Copying file {} from {} to {}", filename, backupPath, restoreIndexPath);
                try (IndexInput indexInput = backupDir.openInput(filename, IOContext.READONCE)) {
                    restoreIndexDir.copyFrom(backupDir, filename, filename, IOContext.READONCE);
                } catch (Exception e) {
                    LOG.error("Exception while copying {} from the backup index",filename, e);
                    throw new SolrException(SolrException.ErrorCode.UNKNOWN,
                            "Exception while copying " + filename + " from the backup index", e);
                }
            }
            LOG.debug("Switching directories");
            modifyIndexProps(core, restoreIndexName);

            boolean success;
            try {
                core.getUpdateHandler().newIndexWriter(false);
                openNewSearcher();
                success = true;
                LOG.info("Successfully restored to the backup index");
            } catch (Exception e) {
                // Rollback to the old index directory. Delete the restore index
                // directory and mark the restore as failed.
                LOG.warn("Could not switch to restored index. Rolling back to the current index");
                Directory dir = null;
                try {
                    dir = core.getDirectoryFactory().get(core.getDataDir(), DirectoryFactory.DirContext.META_DATA,
                            core.getSolrConfig().indexConfig.lockType);
                    dir.deleteFile(IndexFetcher.INDEX_PROPERTIES);
                } finally {
                    if (dir != null) {
                        core.getDirectoryFactory().release(dir);
                    }
                }

                core.getDirectoryFactory().doneWithDirectory(restoreIndexDir);
                core.getDirectoryFactory().remove(restoreIndexDir);
                core.getUpdateHandler().newIndexWriter(false);
                openNewSearcher();
                LOG.error("Exception while restoring the backup index", e);
                throw new SolrException(SolrException.ErrorCode.UNKNOWN, "Exception while restoring the backup index", e);
            }
            if (success) {
                core.getDirectoryFactory().doneWithDirectory(indexDir);
                core.getDirectoryFactory().remove(indexDir);
            }

            if (deleteTmpDir) {
                try {
                    core.getDirectoryFactory().remove(backupDir);
                } catch (IllegalArgumentException ioe) {
                    try {
                        FileUtils.deleteDirectory(backupPath.toFile());
                    } catch (Exception e) {
                        LOG.error("Failed to delete temporary upload directory", e);
                    }
                }
            }

            return true;
        } finally {
            if (restoreIndexDir != null) {
                core.getDirectoryFactory().release(restoreIndexDir);
            }
            if (indexDir != null) {
                core.getDirectoryFactory().release(indexDir);
            }
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Stopping restore process. Thread was interrupted.");
        }
    }

    private void openNewSearcher() throws Exception {
        Future[] waitSearcher = new Future[1];
        core.getSearcher(true, false, waitSearcher, true);
        if (waitSearcher[0] != null) {
            waitSearcher[0].get();
        }
    }

    /**
     * If the index is stale by any chance, load index from a different dir in
     * the data dir.
     */
    protected static boolean modifyIndexProps(SolrCore solrCore, String tmpIdxDirName) {
        LOG.info("New index installed. Updating index properties... index=" + tmpIdxDirName);
        Properties p = new Properties();
        Directory dir = null;
        try {
            dir = solrCore.getDirectoryFactory().get(solrCore.getDataDir(), DirContext.META_DATA,
                    solrCore.getSolrConfig().indexConfig.lockType);
            if (slowFileExists(dir, IndexFetcher.INDEX_PROPERTIES)) {
                final IndexInput input = dir.openInput(IndexFetcher.INDEX_PROPERTIES,
                        DirectoryFactory.IOCONTEXT_NO_CACHE);

                final InputStream is = new PropertiesInputStream(input);
                try {
                    p.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                } catch (Exception e) {
                    LOG.error("Unable to load " + IndexFetcher.INDEX_PROPERTIES, e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            try {
                dir.deleteFile(IndexFetcher.INDEX_PROPERTIES);
            } catch (IOException e) {
                // no problem
            }
            final IndexOutput out = dir
                    .createOutput(IndexFetcher.INDEX_PROPERTIES, DirectoryFactory.IOCONTEXT_NO_CACHE);
            p.put("index", tmpIdxDirName);
            Writer os = null;
            try {
                os = new OutputStreamWriter(new PropertiesOutputStream(out), StandardCharsets.UTF_8);
                p.store(os, IndexFetcher.INDEX_PROPERTIES);
                dir.sync(Collections.singleton(INDEX_PROPERTIES));
            } catch (Exception e) {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to write "
                        + IndexFetcher.INDEX_PROPERTIES, e);
            } finally {
                IOUtils.closeQuietly(os);
            }
            return true;

        } catch (IOException e1) {
            throw new RuntimeException(e1);
        } finally {
            if (dir != null) {
                try {
                    solrCore.getDirectoryFactory().release(dir);
                } catch (IOException e) {
                    SolrException.log(LOG, "", e);
                }
            }
        }

    }

    /**
     * Returns true if the file exists (can be opened), false if it cannot be
     * opened, and (unlike Java's File.exists) throws IOException if there's
     * some unexpected error.
     */
    private static boolean slowFileExists(Directory dir, String fileName) throws IOException {
        try {
            dir.openInput(fileName, IOContext.DEFAULT).close();
            return true;
        } catch (NoSuchFileException | FileNotFoundException e) {
            return false;
        }
    }

}
