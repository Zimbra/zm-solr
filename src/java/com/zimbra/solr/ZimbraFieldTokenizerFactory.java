package com.zimbra.solr;

import java.io.Reader;
import java.util.Map;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

public class ZimbraFieldTokenizerFactory extends TokenizerFactory {

	public ZimbraFieldTokenizerFactory(Map<String, String> args) {
		super(args);
	}

	@Override
	public Tokenizer create(AttributeFactory arg0) {
		return new FieldTokenizer();
	}

}
