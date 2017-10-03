/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.solr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * {@code image/jpeg} becomes {@code image/jpeg} and {@code image}
 *
 * @author ysasaki
 */
public final class MimeTypeTokenizer extends Tokenizer {
    private static final int MIN_TOKEN_LEN = 3;
    private static final int MAX_TOKEN_LEN = 256;
    private static final int MAX_TOKEN_COUNT = 100;

    private final List<String> tokens = new LinkedList<String>();
    private Iterator<String> itr;
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    
    public MimeTypeTokenizer() {
    	super();
    }

    private void add(String src) {
        if (tokens.size() >= MAX_TOKEN_COUNT) {
            return;
        }
        String token = src.trim();
        if (token.length() < MIN_TOKEN_LEN || token.length() > MAX_TOKEN_LEN) {
            return;
        }
        token = token.toLowerCase();
        tokens.add(token);
        // extract primary of primary/sub
        int delim = token.indexOf('/');
        if (delim > 0) {
            String primary = token.substring(0, delim).trim();
            if (primary.length() >= MIN_TOKEN_LEN) {
                tokens.add(primary);
            }
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
    	clearAttributes();
    	if (itr.hasNext()) {	
            termAttr.setEmpty().append(itr.next());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
		BufferedReader reader = new BufferedReader(input);
		StringBuilder s = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				s.append(line);
			}
		} catch (IOException e1) {
			return;
		}
	    add(s.toString());
	    tokens.add(tokens.isEmpty() ? "none" : "any");
	    itr = tokens.iterator();
    }

    @Override
    public void close() throws IOException {
    	super.close();
        tokens.clear();
    }

}
