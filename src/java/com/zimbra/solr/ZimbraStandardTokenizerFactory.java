package com.zimbra.solr;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

public class ZimbraStandardTokenizerFactory extends TokenizerFactory implements ResourceLoaderAware {
	private StopFilterFactory thaiStopwordFactory;

	public ZimbraStandardTokenizerFactory(Map<String, String> args) {
		super(args);
		Map<String, String> stopwordArgs = new HashMap<String, String>();
		stopwordArgs.put("luceneMatchVersion", luceneMatchVersion.toString());
		stopwordArgs.put("words", "lang/stopwords_th.txt");
		stopwordArgs.put("ignoreCase", "true");
		thaiStopwordFactory = new StopFilterFactory(stopwordArgs);
	}

	@Override
	public Tokenizer create(AttributeFactory arg0) {
		return new ZimbraTokenizer(thaiStopwordFactory);
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		thaiStopwordFactory.inform(loader);
	}
}
