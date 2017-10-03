package com.zimbra.solr;

import java.io.Reader;
import java.util.Map;

import org.apache.lucene.analysis.util.CharFilterFactory;

public class ZimbraCharFilterFactory extends CharFilterFactory {

	public ZimbraCharFilterFactory(Map<String, String> args) {
		super(args);
	}

	@Override
	public Reader create(Reader arg0) {
		// TODO Auto-generated method stub
		return new NormalizeCharFilter(arg0);
	}

}
