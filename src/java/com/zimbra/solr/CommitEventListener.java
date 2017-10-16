package com.zimbra.solr;

import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;

public class CommitEventListener extends AbstractSolrEventListener {
    public CommitEventListener(SolrCore core) {
        super(core);
    }

    @Override
    public void postCommit() {
        //postCommit is called on hard commit, which means that all pending changes are committed to disk 
        CommitEventCounter.getInstance(this.getCore().getName()).getAndReset();
    }

    @Override
    public void postSoftCommit() {
        //postSoftCommit is called on soft commit, which means that all pending changes are committed to memory  
        CommitEventCounter.getInstance(this.getCore().getName()).getAndReset();
    }
}
