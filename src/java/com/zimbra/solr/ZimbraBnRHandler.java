package com.zimbra.solr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.RateLimiter;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.FastOutputStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.SnapShooter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.DefaultSolrThreadFactory;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 *
 * @author Greg Solovyev
 * Implementation of https://issues.apache.org/jira/browse/SOLR-7583
 * TODO: remove when SOLR-7583 is released and update ZimbraBackup to use /replication instead of /zbnr
 *
 */
public class ZimbraBnRHandler extends RequestHandlerBase implements SolrCoreAware {

    private static final Logger LOG = LoggerFactory.getLogger(ZimbraBnRHandler.class.getName());
    SolrCore core;


    private static final String LOCATION = "location";

    private static final String SUCCESS = "success";

    private static final String FAILED = "failed";

    private static final String EXCEPTION = "exception";

    public static final String STATUS = "status";

    public static final String COMMAND = "command";

    public static final String CMD_RESTORE = "restore";

    public static final String CMD_RESTORE_STATUS = "restorestatus";

    public static final String CMD_GET_FILE = "filecontent";

    public static final String CMD_GET_BACKUP = "downloadbackup";

    public static final String FILE = "file";

    public static final String MAX_WRITE_PER_SECOND = "maxWriteMBPerSec";

    public static final String FILE_STREAM = "filestream";

    public static final int PACKET_SZ = 1024 * 1024; // 1MB

    public static final String OK_STATUS = "OK";

    private ExecutorService restoreExecutor = new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new DefaultSolrThreadFactory("restoreExecutor"));

    private volatile Future<Boolean> restoreFuture;

    private volatile String currentRestoreName;

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        rsp.setHttpCaching(false);
        final SolrParams solrParams = req.getParams();
        String command = solrParams.get(COMMAND);
        if (command == null) {
            rsp.add(STATUS, OK_STATUS);
            rsp.add("message", "No command");
            return;
        }
        if (command.equalsIgnoreCase(CMD_RESTORE)) {
            restore(new ModifiableSolrParams(solrParams), rsp, req);
            rsp.add(STATUS, OK_STATUS);
        } else if (command.equalsIgnoreCase(CMD_RESTORE_STATUS)) {
            rsp.add(CMD_RESTORE_STATUS, getRestoreStatus());
        } else if (CMD_GET_BACKUP.equalsIgnoreCase(command)) {
            getSnapshot(new ModifiableSolrParams(solrParams), rsp, req);
        }
    }

    /**
     * If requested snapshot is found, zip it up and stream back to the client
     */
    private void getSnapshot(ModifiableSolrParams solrParams, SolrQueryResponse rsp, SolrQueryRequest req)
            throws IOException {
        String name = solrParams.get("name");
        if (name == null) {
            throw new SolrException(ErrorCode.BAD_REQUEST, "Missing mandatory param: name");
        }

        String location = solrParams.get(LOCATION);
        if (location == null) {
            location = core.getDataDir();
        }

        SnapShooter snapShooter = new SnapShooter(core, location, name);

        // check that snapshot exists
        snapShooter.validateDeleteSnapshot();
        Path locationPath = Paths.get(location);

        // archive the snapshot folder so we can stream it back as a single file
        String zipFileName = name + ".zip";
        Directory baseDir = FSDirectory.open(locationPath);

        // zip up snapshot files
        Path zipFilePath = Paths.get(location).resolve(zipFileName);
        OutputStream zipFileOutput = Files.newOutputStream(zipFilePath);
        Path backupPath = locationPath.resolve("snapshot." + name);

        LOG.debug("creating zip file at " + zipFilePath.toAbsolutePath().toString() + " from "
                + backupPath.toAbsolutePath().toString());

        Directory backupDir = FSDirectory.open(backupPath);
        ZipOutputStream zos = null;
        IndexInput is = null;
        try {
            zos = new ZipOutputStream(zipFileOutput);
            for (String filename : backupDir.listAll()) {
                byte[] buf = new byte[1024];
                ZipEntry ze = new ZipEntry(filename);
                zos.putNextEntry(ze);
                is = backupDir.openInput(filename, IOContext.READONCE);
                long filelen = backupDir.fileLength(filename);
                int offset = 0;
                while (offset < filelen) {
                    int bytesRead = (int) Math.min(buf.length, filelen - offset);
                    is.readBytes(buf, 0, bytesRead);
                    zos.write(buf, 0, bytesRead);
                    offset += bytesRead;
                    is.seek(offset);
                }
                is.close();
                zos.flush();
                zos.closeEntry();
            }
        } finally {
            if(zos != null) {
                zos.close();
            }
            if(is != null) {
                is.close();
            }
        }

        // put file name into solrParams, so SnapshotArchiveFileStream can pick
        // it up
        solrParams.set(FILE, zipFileName);
        solrParams.set(LOCATION, location);

        // create a file stream from the archive
        rsp.add(FILE_STREAM, new SnapshotArchiveFileStream(solrParams));
    }

    private boolean processZipUpload(ContentStream stream, String tmpUploadDirName) throws IOException {
        File tmpUploadDir = null;
        tmpUploadDir = new File(core.getDataDir().concat(tmpUploadDirName));
        if (stream != null) {
            // Reader reader = stream.getReader();
            File zippedSnapshotFile = new File(tmpUploadDir, "snapshot.zip");
            FileOutputStream out = FileUtils.openOutputStream(zippedSnapshotFile);
            InputStream in = stream.getStream();
            LOG.debug("uploading zipped snapshot file to " + zippedSnapshotFile.getAbsolutePath());
            try {
                byte[] buffer = new byte[1024];
                int len = 0;
                int bytes = 0;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    bytes+=len;
                }
                LOG.debug("saved {}  bytes into {}",bytes, zippedSnapshotFile.getAbsolutePath());
            } finally {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            }
            if (!zippedSnapshotFile.exists()) {
                LOG.error("Failed to save zipped snapshot file locally");
                return false;
            }

            // unpack downloaded file
            ZipInputStream zis = null;
            try {
                LOG.debug("unpacking zipped snapshot file to " + tmpUploadDir.getAbsolutePath());
                zis = new ZipInputStream(new FileInputStream(zippedSnapshotFile));
                ZipEntry ze = null;
                while ((ze = zis.getNextEntry()) != null) {
                    String zn = ze.getName();
                    Files.copy(zis, tmpUploadDir.toPath().resolve(zn));
                    LOG.debug("Extracting " + zn + " to " + tmpUploadDir.toPath().resolve(zn).toString());
                    zis.closeEntry();
                }
            } finally {
                if (zis != null) {
                    zis.close();
                }
            }

            // delete downloaded file
            Files.delete(zippedSnapshotFile.toPath());
        } else {
            throw new SolrException(ErrorCode.BAD_REQUEST, "NULL stream");
        }

        return true;
    }

    private void restore(SolrParams params, SolrQueryResponse rsp, SolrQueryRequest req) {
        if (restoreFuture != null && !restoreFuture.isDone()) {
            throw new SolrException(ErrorCode.BAD_REQUEST,
                    "Restore in progress. Cannot run multiple restore operations" + "for the same core");
        }
        boolean useUpload = false;
        String tmpUploadDirName = "tmp_upload";
        Iterable<ContentStream> streams = null;
        // If location is not provided then assume that the restore index is
        // present inside the data directory.
        String location = core.getDataDir();
        streams = req.getContentStreams();
        if (streams != null) {
            // restoring from an upload
            Iterator<ContentStream> iter = streams.iterator();
            if (iter.hasNext()) {
                try {
                    if (processZipUpload(iter.next(), tmpUploadDirName)) {
                        useUpload = true;
                    }
                } catch (IOException e) {
                    throw new SolrException(ErrorCode.SERVER_ERROR, "Failed to process uploaded zip file", e);
                }
            } else {
                throw new SolrException(ErrorCode.INVALID_STATE,
                        "Either content streams or location parameter is required to restore from a snapshot");
            }
        }

        // If name is not provided then look for the last unnamed( the ones with
        // the snapshot.timestamp format)
        // snapshot folder since we allow snapshots to be taken without
        // providing a name. Pick the latest timestamp.

        ZRestoreCore restoreCore = new ZRestoreCore(core, location, tmpUploadDirName, useUpload);
        try {
            MDC.put("RestoreCore.core", core.getName());
            MDC.put("RestoreCore.backupLocation", location);
            MDC.put("RestoreCore.backupName", tmpUploadDirName);
            MDC.put("RestoreCore.useUpload", Boolean.toString(useUpload));
            restoreFuture = restoreExecutor.submit(restoreCore);
            currentRestoreName = tmpUploadDirName;
        } finally {
            MDC.remove("RestoreCore.core");
            MDC.remove("RestoreCore.backupLocation");
            MDC.remove("RestoreCore.backupName");
            MDC.remove("RestoreCore.useUpload");
        }
    }

    private NamedList<Object> getRestoreStatus() {
        NamedList<Object> status = new SimpleOrderedMap<>();

        if (restoreFuture == null) {
            status.add(STATUS, "No restore actions in progress");
            return status;
        }

        status.add("snapshotName", currentRestoreName);
        if (restoreFuture.isDone()) {
            try {
                boolean success = restoreFuture.get();
                if (success) {
                    status.add(STATUS, SUCCESS);
                } else {
                    status.add(STATUS, FAILED);
                }
            } catch (Exception e) {
                status.add(STATUS, FAILED);
                status.add(EXCEPTION, e.getMessage());
            }
        } else {
            status.add(STATUS, "In Progress");
        }
        return status;
    }

    @Override
    public String getDescription() {
        return "ZimbraBnRHandler provides API to restore a core via upload and to download a zipped snapshot";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void inform(SolrCore core) {
        this.core = core;
        core.getUpdateHandler().registerSoftCommitCallback(new CommitEventListener(core));
    }

    /**
     * This class is used to beam up archived snapshot files
     */
    private class SnapshotArchiveFileStream implements SolrCore.RawWriter {
        protected SolrParams params;
        protected FastOutputStream fos;
        protected String fileName;
        protected String location;
        private RateLimiter rateLimiter;

        public SnapshotArchiveFileStream(SolrParams solrParams) {
            params = solrParams;
            fileName = params.get(FILE);
            location = params.get(LOCATION);
            // No throttle if MAX_WRITE_PER_SECOND is not specified
            double maxWriteMBPerSec = params.getDouble(MAX_WRITE_PER_SECOND, Double.MAX_VALUE);
            rateLimiter = new RateLimiter.SimpleRateLimiter(maxWriteMBPerSec);
        }

        protected void createOutputStream(OutputStream out) {
            fos = new FastOutputStream(out);
        }

        /**
         * writes the content of this stream to the response.
         * This method is called by {@link org.apache.solr.response.BinaryResponseWriter}
         */
        public void write(OutputStream out) throws IOException {
            createOutputStream(out);
            IndexInput in = null;
            initWrite();

            // if if is a conf file read from config directory
            Path snapShotLocation = Paths.get(location);// .resolve(fileName);
            Directory snapshotDir = FSDirectory.open(snapShotLocation);

            try {
                in = snapshotDir.openInput(fileName, IOContext.READONCE);
                long filelen = snapshotDir.fileLength(fileName);
                long maxBytesBeforePause = 0;
                int offset = 0;

                // write file size
                fos.writeLong(filelen);
                fos.flush();

                // write the file in packets with throttling
                byte[] buf = new byte[PACKET_SZ];
                while (offset < filelen) {
                    int read = (int) Math.min(buf.length, filelen - offset);
                    in.readBytes(buf, 0, read);

                    // write packet size
                    fos.writeInt(read);

                    // write packet content
                    fos.write(buf, 0, read);

                    // flush out the stream
                    fos.flush();

                    LOG.debug("Wrote {} bytes for file {}", offset + read, fileName);

                    // Pause if necessary
                    maxBytesBeforePause += read;
                    if (maxBytesBeforePause >= rateLimiter.getMinPauseCheckBytes()) {
                        rateLimiter.pause(maxBytesBeforePause);
                        maxBytesBeforePause = 0;
                    }
                    if (read != buf.length) {
                        // done reading
                        writeNothingAndFlush();
                        fos.close();
                        break;
                    }
                    offset += read;
                    in.seek(offset);
                }
            } catch (IOException e) {
                LOG.warn("Exception while writing response for params: " + params, e);
            } finally {
                if (in != null) {
                    in.close();
                }
                if (fos != null) {
                    fos.close();
                }
            }

            // delete the zip file after streaming it to the client
            snapshotDir.deleteFile(fileName);
        }

        protected void initWrite() throws IOException {
            if (fileName == null || location == null) {
                // no filename do nothing
                writeNothingAndFlush();
            }
        }

        /**
         * Used to write a marker for EOF
         */
        protected void writeNothingAndFlush() throws IOException {
            fos.writeInt(0);
            fos.flush();
        }
    }
}
