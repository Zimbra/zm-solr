package com.zimbra.solr;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SolrQueryParser;
import org.apache.solr.search.SyntaxError;

public class ZimbraWildcardQParserPlugin extends QParserPlugin {
	public static final String NAME = "zimbrawildcard";
	@Override
	public void init(NamedList args) {
	}

	@Override
	public QParser createParser(String s, SolrParams localParams, SolrParams params,
			SolrQueryRequest req) {
		return new ZimbraWildcardQParser(s, localParams, params, req);
	}
}

class ZimbraWildcardQParser extends QParser {
	WildcardQueryParser parser;
	public ZimbraWildcardQParser(String s, SolrParams localParams, SolrParams params,
			SolrQueryRequest req) {
		super(s, localParams, params, req);
	}

	@Override
	public Query parse() throws SyntaxError {
		String query = getString();
		String defaultField = getParam(CommonParams.DF);
		parser = new WildcardQueryParser(
				defaultField,
				getReq().getSchema().getQueryAnalyzer());
		parser.setFields(localParams.get("fields", defaultField));
		parser.setDefaultParser(new SolrQueryParser(this, defaultField));
		parser.setReader(getReq().getSearcher().getIndexReader());
		String maxExpansions = localParams.get("maxExpansions", null);
		if (maxExpansions != null) {
			parser.setMaxExpansions(Integer.parseInt(maxExpansions));
		} else {
			parser.setMaxExpansions(Integer.MAX_VALUE);
		}

		try {
			return parser.parse(query);
		} catch (ParseException e) {
			throw new SyntaxError(e);
		}
	}
}