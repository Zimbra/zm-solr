package com.zimbra.solr;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.DateTools;

public final class TimeTokenFilter extends TokenFilter {
	private CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
	
	protected TimeTokenFilter(TokenStream input) {
		super(input);
	}

	@Override
	public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }
        try {
	        String date = DateTools.timeToString(Long.parseLong(termAttr.toString()), DateTools.Resolution.MILLISECOND);
	        termAttr.setEmpty().append(date);
        } catch (NumberFormatException ignore) {
        }
        return true;
	}
}
