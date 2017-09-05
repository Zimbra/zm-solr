/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013 Zimbra Software, LLC.
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.th.ThaiTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;

import com.google.common.base.Joiner;
import com.zimbra.common.util.Pair;

/**
 * A grammar-based tokenizer using JFlex.
 * <p>
 * The implementation is based on {@code StandardAnalyzer} extending an ability
 * of tokenizing CJK unicode blocks where bigram tokenization is applied.
 *
 * @author ysasaki
 */
public final class ZimbraTokenizer extends Tokenizer {

    enum TokenType {
        ALNUM, APOSTROPHE, ACRONYM, COMPANY, EMAIL, HOST, NUM, CJK, THAI, SOUTHEAST_ASIAN, PUNC;
    }

    private final ZimbraLexer lexer;
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAttr = addAttribute(TypeAttribute.class);
    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncAttr = addAttribute(PositionIncrementAttribute.class);
    private int cjk = -1;
    private boolean thai = false;
    private boolean email = false;
    private boolean host = false;
    private TokenStream thaiTokenizer = null;
    private StopFilterFactory thaiStopFilterFactory;
    private TokenType bufferedToken = null;
    private LinkedList<Pair<String, Integer>> bufferedEmailTokens = new LinkedList<Pair<String, Integer>>();
    private LinkedList<Pair<String, Integer>> bufferedHostTokens = new LinkedList<Pair<String, Integer>>();
    boolean first = true;

    public ZimbraTokenizer() {
    	super();
		lexer = new ZimbraLexer();
    }

    public ZimbraTokenizer(StopFilterFactory thaiStopwordFactory) {
    	super();
		lexer = new ZimbraLexer();
		this.thaiStopFilterFactory = thaiStopwordFactory;
	}

	private boolean incrementThaiToken() throws IOException {
    	if (thaiTokenizer.incrementToken()) {
    		AttributeSource src = thaiTokenizer.cloneAttributes();
    		CharTermAttribute thaiTerm = src.getAttribute(CharTermAttribute.class);
    		OffsetAttribute thaiOffset = src.getAttribute(OffsetAttribute.class);
    		PositionIncrementAttribute thaiPosInc = src.getAttribute(PositionIncrementAttribute.class);
    		termAttr.append(thaiTerm.toString());
    		typeAttr.setType(TokenType.THAI.name());
    		int curOffset = lexer.yychar();
    		offsetAttr.setOffset(curOffset + thaiOffset.startOffset(), curOffset + thaiOffset.endOffset());
    		posIncAttr.setPositionIncrement(thaiPosInc.getPositionIncrement());
    		return true;
    	} else {
    		thaiTokenizer.close();
    		return false;
    	}
    }

    private boolean incrementEmailToken() {
		if (bufferedEmailTokens.isEmpty()) {
			return false;
		}
		Pair<String, Integer> token = bufferedEmailTokens.pop();
		String tokenString = token.getFirst();
		Integer relativeOffset = token.getSecond();
		typeAttr.setType(TokenType.EMAIL.name());
		posIncAttr.setPositionIncrement(0);
		offsetAttr.setOffset(relativeOffset, relativeOffset + tokenString.length());
		termAttr.append(tokenString);
		return true;
	}


	private boolean incrementHostToken() {
		if (bufferedHostTokens.isEmpty()) {
			return false;
		}
		Pair<String, Integer> token = bufferedHostTokens.pop();
		String tokenString = token.getFirst();
		Integer relativeOffset = token.getSecond();
		typeAttr.setType(TokenType.HOST.name());
		posIncAttr.setPositionIncrement(0);
		offsetAttr.setOffset(relativeOffset, relativeOffset + tokenString.length());
		termAttr.append(tokenString);
		return true;
	}

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();

        if (thai) {
        	 if (incrementThaiToken()) {
        		 first = false;
        		 return true;
        	 } else {
        		 thai = false;
        	 }
        }
        if (email) {
        	if (incrementEmailToken()) {
        		return true;
        	} else {
        		email = false;
        	}
        }
        if (host) {
        	if (incrementHostToken()) {
        		return true;
        	} else {
        		host = false;
        	}
        }
        if (cjk >= 0) { // more to process CJK
            if (cjk + 1 < lexer.yylength()) {
                lexer.getTerm(termAttr, cjk, 2); // bigram
                setOffset(lexer.yychar() + cjk, 2);
                posIncAttr.setPositionIncrement(1);
                typeAttr.setType(TokenType.CJK.name());
                cjk++;
                first = false;
                return true;
            } else { // end of CJK
                cjk = -1;
            }
        }

        while (true) {
        	TokenType type;
        	if (bufferedToken != null) {
        		type = bufferedToken;
        		this.bufferedToken = null;
        	} else {
        		type = lexer.next();
        	}
            if (type == null) { // EOF
                return false;
            }

            if (type == TokenType.CJK) {
                if (lexer.yylength() == 1) {
                    lexer.getTerm(termAttr, 0, 1);
                } else {
                    lexer.getTerm(termAttr, 0, 2);
                    cjk = 1;
                }
                setOffset(lexer.yychar(), termAttr.length());
                posIncAttr.setPositionIncrement(1);
                typeAttr.setType(type.name());
            } else if (type == TokenType.THAI) {
            	thai = true;
            	//buffer as many thai tokens as possible
            	List<String> thaiTerms = new LinkedList<String>();
            	while (type == TokenType.THAI) {
            		lexer.getTerm(termAttr);
	            	thaiTerms.add(termAttr.toString());
            		type = lexer.next();
            	}
            	//the token that broke above loop should be stored so the next
            	//call to incrementToken() doesn't skip it
            	bufferedToken = type;
            	thaiTokenizer = getThaiTokenizer(Joiner.on(" ").join(thaiTerms));
            	clearAttributes();
            	incrementThaiToken();
            } else if (type == TokenType.EMAIL) {
            	lexer.getTerm(termAttr);
            	setOffset(lexer.yychar(), termAttr.length());
            	posIncAttr.setPositionIncrement(1);
            	typeAttr.setType(type.toString());
            	email = true;
            	bufferEmailTokens(termAttr.toString(), lexer.yychar());
            } else if (type == TokenType.HOST) {
            	lexer.getTerm(termAttr);
            	setOffset(lexer.yychar(), termAttr.length());
            	posIncAttr.setPositionIncrement(1);
            	typeAttr.setType(type.toString());
            	host = true;
            	bufferHostTokens(termAttr.toString(), lexer.yychar());
            }
            else {
                lexer.getTerm(termAttr);
                setOffset(lexer.yychar(), termAttr.length());
                posIncAttr.setPositionIncrement(type == TokenType.PUNC && !first ? 0: 1);
                typeAttr.setType(type.name());
            }
            first = false;
            return true;
        }
    }

	private void bufferHostTokens(String string, Integer startOffset) {
		List<String> parts = Arrays.asList(string.split("\\."));
		if (parts.size() > 2) {
			for (int i = 1; i < parts.size() - 1; i++) {
				String token = Joiner.on(".").join(parts.subList(i, parts.size()));
				bufferedHostTokens.add(new Pair<String, Integer>(token, startOffset + string.length() - token.length()));
			}
		}

	}

	private void bufferEmailTokens(String string, Integer startOffset) {
		String[]parts = string.split("@");
		Pair<String, Integer> firstPart = new Pair<String, Integer>(parts[0], startOffset);
		bufferedEmailTokens.add(firstPart);
	}

	private TokenStream getThaiTokenizer(String text) throws IOException {
    	Tokenizer tokenizer = new ThaiTokenizer();
    	tokenizer.setReader(new StringReader(text));
    	tokenizer.reset();
    	if (thaiStopFilterFactory != null) {
    		return thaiStopFilterFactory.create(tokenizer);
    	} else {
    		return tokenizer;
    	}
	}

	@Override
    public final void end() {
      // set final offset
      int offset = lexer.yychar() + lexer.yylength();
      offsetAttr.setOffset(offset, offset);
      first = true;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        lexer.yyreset(input);
    }

    private void setOffset(int start, int len) {
        offsetAttr.setOffset(start, start + len);
    }
}
