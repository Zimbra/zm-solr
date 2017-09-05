package com.zimbra.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

public class CommitCountHandler extends RequestHandlerBase {
    protected enum ACTIONS {get, increment, reset};

    public CommitCountHandler() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp)
            throws Exception {
        SolrParams params = req.getParams();
        String actionParam = params.get("action");
        ACTIONS action = null;
        if (actionParam == null){
          action = ACTIONS.get;
        } else {
            try {
                action = ACTIONS.valueOf(actionParam.trim());
            } catch (IllegalArgumentException iae) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown action: " + actionParam);
            }
        }

        switch(action){
            case increment:
                rsp.add("count",CommitEventCounter.getInstance(req.getCore().getName()).increment());
                break;
            case reset:
                rsp.add("count",CommitEventCounter.getInstance(req.getCore().getName()).getAndReset());
                break;
            case get:
                default:
                    rsp.add("count",CommitEventCounter.getInstance(req.getCore().getName()).get());
                    break;

        }

    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSource() {
        // TODO Auto-generated method stub
        return null;
    }

}
