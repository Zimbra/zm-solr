package com.zimbra.solr;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import com.google.common.base.CharMatcher;
import com.zimbra.solr.ZimbraTokenizer;

public class ClassicTokenFilter extends TokenFilter {
    private CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private TypeAttribute typeAttr = addAttribute(TypeAttribute.class);

    ClassicTokenFilter(TokenStream in) {
        super(in);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }
        String type = typeAttr.type();
        if (type == ZimbraTokenizer.TokenType.APOSTROPHE.name()) {
            // endsWith "'s"
            int len = termAttr.length();
            if (len >= 2 && termAttr.charAt(len - 1) == 's' && termAttr.charAt(len - 2) == '\'') {
                // remove 's from possessions
                termAttr.setLength(len - 2);
            }
        } else if (type == ZimbraTokenizer.TokenType.ACRONYM.name()) {
            // remove dots from acronyms
            String replace = CharMatcher.is('.').removeFrom(termAttr);
            termAttr.setEmpty().append(replace);
        }
        return true;
    }
}