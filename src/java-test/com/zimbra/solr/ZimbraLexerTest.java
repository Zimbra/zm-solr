package com.zimbra.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.junit.Test;

import com.zimbra.solr.ZimbraTokenizer.TokenType;

public class ZimbraLexerTest {
	private ZimbraLexer lexer;
	CharTermAttribute charAttr = new CharTermAttributeImpl();

	private class Token {
		private String text;
		private TokenType type;

		public Token(String text, TokenType type) {
			this.text = text;
			this.type = type;
		}

		public TokenType getType() {
			return type;
		}

		public String getText() {
			return text;
		}
	}

	private void doTest(String in, List<Token> expected) throws IOException {
		lexer = new ZimbraLexer();
		lexer.yyreset(new StringReader(in));
		int num = 0;
		Token expectedToken = null;
		while (true) {
			TokenType tokenType = lexer.next();
			if (tokenType == null) {
				break;
			}
			try {
				expectedToken = expected.get(num);
			} catch (IndexOutOfBoundsException e) {
				fail(String.format(
						"test returned more than the expected %d tokens",
						expected.size()));
			}
			assertEquals(expectedToken.getType(), tokenType);
			lexer.getTerm(charAttr);
			assertEquals(expectedToken.getText(), charAttr.toString());
			num++;
		}
		assertEquals(expected.size(), num);
	}

	@Test
	public void testAlnum() throws IOException {
		doTest("abc", Arrays.asList(new Token("abc", TokenType.ALNUM)));
		doTest("abc-xyz", Arrays.asList(new Token("abc-xyz", TokenType.ALNUM)));
		doTest("abc+xyz", Arrays.asList(new Token("abc+xyz", TokenType.ALNUM)));
		doTest("abc<xyz>",
				Arrays.asList(new Token("abc<xyz>", TokenType.ALNUM)));
		doTest("abc/xyz", Arrays.asList(new Token("abc", TokenType.ALNUM),
				new Token("xyz", TokenType.ALNUM)));
		doTest("abc_xyz", Arrays.asList(new Token("abc_xyz", TokenType.ALNUM)));
	}

	@Test
	public void testCJK() throws IOException {
		doTest("テスト", Arrays.asList(new Token("テスト", TokenType.CJK)));
		doTest("<テ-ス+ト>", Arrays.asList(new Token("<テ-ス+ト>", TokenType.CJK)));
	}

	@Test
	public void testNum() throws IOException {
		doTest("abc-123_xyz,456.789/newtoken", Arrays.asList(
				new Token("abc-123_xyz,456.789", TokenType.NUM),
				new Token("newtoken", TokenType.ALNUM)));

	}

	@Test
	public void testApostrophe() throws IOException {
		doTest("abc's", Arrays.asList(new Token("abc's", TokenType.APOSTROPHE)));
	}

	@Test
	public void testAcronym() throws IOException {
		doTest("a.b.c.", Arrays.asList(new Token("a.b.c.", TokenType.ACRONYM)));
	}

	@Test
	public void testCompany() throws IOException {
		doTest("abc@def",
				Arrays.asList(new Token("abc@def", TokenType.COMPANY)));
		doTest("abc&def",
				Arrays.asList(new Token("abc&def", TokenType.COMPANY)));
	}

	@Test
	public void testEmail() throws IOException {
		doTest("abc.def@xyz.com",
				Arrays.asList(new Token("abc.def@xyz.com", TokenType.EMAIL)));
	}

	@Test
	public void testHost() throws IOException {
		doTest("123.345.456", Arrays.asList(new Token("123.345.456", TokenType.HOST)));
		doTest("abc-xyz.zimbra.com", Arrays.asList(new Token("abc-xyz.zimbra.com", TokenType.HOST)));
	}

	@Test
	public void testGarbage() throws IOException {
		doTest("-", Collections.EMPTY_LIST);
		doTest("+-<>", Collections.EMPTY_LIST);
	}

	@Test
	public void testUrl() throws IOException {
		String url = "http://abc.xyz.com/foo/1/";
		doTest(url, Arrays.asList(
				new Token("http", TokenType.ALNUM),
				new Token("abc.xyz.com", TokenType.HOST),
				new Token("foo", TokenType.ALNUM),
				new Token("1", TokenType.ALNUM)));
	}

	@Test
	public void testSouthEastAsianLanguages() throws IOException {
		//"how are you" in various languages
		String thai = "\u0E40\u0E1B\u0E47\u0E19\u0E2D\u0E22\u0E48\u0E32\u0E07\u0E44\u0E23\u0E1A\u0E49\u0E32\u0E07";
		String lao = "\u0EAA\u0EB0\u0E9A\u0EB2\u0E8D\u0E94\u0EB5\u0E9A";
		String myanmar = "\u1001\u1004\u103A\u1017\u103B\u102C\u1038\u1031\u1014\u1031\u1000\u102C\u1004\u103A\u1038\u101B\u1032\u1037\u101C\u102C\u1038\u104B";
		String khmer = "\u17A2\u17D2\u1793\u1780\u179F\u17BB\u1781\u179F\u1794\u17D2\u1794\u17B6\u1799\u1791\u17C1";
		doTest(thai, Arrays.asList(new Token(thai, TokenType.THAI)));
		doTest(lao, Arrays.asList(new Token(lao, TokenType.SOUTHEAST_ASIAN)));
		doTest(myanmar, Arrays.asList(new Token(myanmar, TokenType.SOUTHEAST_ASIAN)));
		doTest(khmer, Arrays.asList(new Token(khmer, TokenType.SOUTHEAST_ASIAN)));
	}

	@Test
	public void testPunctuation() throws IOException {
		String punc = "~!#$%^&*()_?/{}[];:";
		doTest(punc, Arrays.asList(
				new Token("~!#$%^&", TokenType.PUNC),
				new Token("()", TokenType.PUNC),
				new Token("?", TokenType.PUNC),
				new Token("{}[];", TokenType.PUNC)));
	}

	@Test
	public void testLineBreak() throws IOException {
		String s = "this \tshould\nbe\u2028tokenized\u2029properly\u000B\u000C\u0085";
		doTest(s, Arrays.asList(
				new Token("this", TokenType.ALNUM),
				new Token("should", TokenType.ALNUM),
				new Token("be", TokenType.ALNUM),
				new Token("tokenized", TokenType.ALNUM),
				new Token("properly", TokenType.ALNUM)));
	}
}
