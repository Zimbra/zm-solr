package com.zimbra.solr;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;

import com.zimbra.common.service.ServiceException;

public class ZimbraCommitListener extends AbstractSolrEventListener {

    public ZimbraCommitListener(SolrCore core) {
        super(core);
    }

    @Override
    public void postCommit() {
      CommitLock.getInstance().setCommitFinished();
    }

    /* This is the synchronization mechanism that ensures that a commit has finished
     * before a unit test can continue. Since it is a singleton,
     * it's vital that requests are processed serially on the EmbeddedSolrServer.
     */
    public static class CommitLock {
        private final Semaphore semaphore = new Semaphore(0);
        private static ReentrantLock accessLock = new ReentrantLock(true);
        private static CommitLock instance = null;

        private CommitLock() {}

        public static CommitLock getInstance() {
            //we don't want two threads to instantiate the semaphore
            accessLock.lock();
            try {
                if (instance == null) {
                    instance = new CommitLock();
                }
                return instance;
            } finally {
                accessLock.unlock();
            }
        }

        public void setCommitFinished() {
            //when a commit finishes, the semaphore gets cleared, so that a waiting thread can acquire it and continue
            semaphore.release();
        }

        public void waitUntilCommitFinished(int numCommits) throws ServiceException {
            //blocks until a call to setCommitFinished() releases the specified number of permits
            boolean acquired;
            try {
                acquired = semaphore.tryAcquire(numCommits, 60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw ServiceException.FAILURE("waiting for postCommit event interrupted", e);
            }
            if (!acquired) {
                throw ServiceException.FAILURE("cannot acquire postCommit permit within 60 seconds", new Throwable());
            }
        }

        public void reset() {
            semaphore.drainPermits();
        }
    }
}
