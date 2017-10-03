package com.zimbra.solr;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class ZimbraTimeFilterFactory extends TokenFilterFactory {

	public ZimbraTimeFilterFactory(Map<String, String> args) {
		super(args);
	}

	@Override
	public TokenStream create(TokenStream arg0) {
		return new TimeTokenFilter(arg0);
	}

}
