package com.zimbra.solr;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Assert;
import org.junit.Test;


public class AddrCharTokenizerTest {
	
    @Test
    public void addrCharTokenizer() throws Exception {
        Tokenizer tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader("all-snv"));
        Assert.assertEquals(Collections.singletonList("all-snv"), toTokens(tokenizer));

        tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader("."));
        Assert.assertEquals(Collections.singletonList("."), toTokens(tokenizer));

        tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader(".. ."));
        Assert.assertEquals(Arrays.asList("..", "."),  toTokens(tokenizer));

        tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader(".abc"));
        Assert.assertEquals(Collections.singletonList(".abc"),  toTokens(tokenizer));

        tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader("a"));
        Assert.assertEquals(Collections.singletonList("a"),  toTokens(tokenizer));

        tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader("test.com"));
        Assert.assertEquals(Collections.singletonList("test.com"),  toTokens(tokenizer));

        tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader("user1@zim"));
        Assert.assertEquals(Collections.singletonList("user1@zim"),  toTokens(tokenizer));

        tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader("user1@zimbra.com"));
        Assert.assertEquals(Collections.singletonList("user1@zimbra.com"),  toTokens(tokenizer));
    }

    /**
     * Bug 79103 tab was getting included at start of a token instead of being ignored.
     */
    @Test
    public void multiLineWithTabs() throws Exception {
        Tokenizer tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader("one name <one@example.net>\n\ttwo <two@example.net>"));
        Assert.assertEquals("Token list", Arrays.asList("one", "name", "one@example.net", "two", "two@example.net"),
                toTokens(tokenizer));
    }

    @Test
    public void japanese() throws Exception {
        Tokenizer tokenizer = new AddrCharTokenizer();
        tokenizer.setReader(new StringReader("\u68ee\u3000\u6b21\u90ce"));
        Assert.assertEquals(Arrays.asList("\u68ee", "\u6b21\u90ce"), toTokens(tokenizer));
    }

    public static List<String> toTokens(TokenStream stream) throws IOException {
        List<String> result = new ArrayList<String>();
        CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            result.add(termAttr.toString());
        }
        stream.end();
        return result;
    }
}
