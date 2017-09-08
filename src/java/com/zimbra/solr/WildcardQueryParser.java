package com.zimbra.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.StringHelper;
import org.apache.solr.search.SolrQueryParser;
import org.apache.solr.search.SyntaxError;

import com.zimbra.solr.ZimbraTokenizer.TokenType;

public class WildcardQueryParser extends QueryParser {
	private SolrQueryParser defaultParser;
	private IndexReader reader;

	private final Pattern whitespace = Pattern.compile("\\s");
	private List<String> fields;
	private boolean leading;
	private boolean trailing;
	private int maxExpansions;

	public WildcardQueryParser(String f, Analyzer a) {
		super(f, a);
	}

	@Override
	public Query parse(String queryText) throws ParseException {
		boolean hasWildcard = queryText.contains("*");
		if (queryText.startsWith("+") || queryText.startsWith("-")) {
			queryText = queryText.substring(1, queryText.length());
		}

		if (queryText.startsWith("\"") && queryText.endsWith("\"")) {
			queryText = queryText.substring(1, queryText.length() - 1);
		}

		leading = false;
		trailing = false;

		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		Query parsed = null;

		if (queryText.startsWith("*")) {
			leading = true;
		}
		if (queryText.endsWith("*")) {
			trailing = true;
		}

		if (!hasWildcard) {
			/* Non-wildcard queries fall through to standard solr parser
			 * after being reconstructed in the regular grammar
			 */
			if (hasWhitespace(queryText)) {
				queryText = "\""+queryText+"\"";
			}
			for (String f: fields) {
				buildIntermediateQuery(builder, f, queryText, null);
			}
			try {
				parsed = defaultParser.parse(builder.build().toString());
			} catch (SyntaxError shouldntHappen) {
				throw new ParseException("Syntax error: " + shouldntHappen.getMessage());
			}
		} else {
			/* If there is a wildcard, we first tokenize the query. If this results in only zero or one tokens, we can pass
			 * the query through to the SolrQueryParser with the wildcard added back.
			 * If it results in more than one token, we split the query into sections based on wildcard positions, and
			 * expand them one section at a time.
			 */
			if (leading && trailing) {
				leading = false;
			}
			/* Iterate over the specified fields and use the tokenizer for each field */
			for (String fld: fields) {
				List<TokensAtPosition> tokens = getAllTokens(fld, queryText);
				if (tokens.size() == 0) {
					/* This can happen if the textual part of the query is all stopwords, like "the*".
					 * In this case, we pass the raw text of the query to the SolrQueryParser.
					 */
					buildIntermediateQuery(builder, fld, queryText.toLowerCase(), null);
				} else if (tokens.size() == 1) {
					/* This is the most common wildcard scenario: a single-term query like "foo*" that doesnt tokenize to anything more complex.
					 * We can pass the token with the wildcard through to the SolrQueryParser.
					 * We pass the token instead of the original text because not all analyzer steps will be applied to the wildcard query,
					 * depending on if they are MultiTermAware. However, we know that the token has gone through all steps.
					 *
					 * It's also possible (but unlikely) that we have multiple tokens in the same position. In this case,
					 * we use the last non-PUNC token, or the last token if all are of type PUNC.
					 * If we add synonym indexing, this will need to be changed.
					 */
					StringBuilder sb = new StringBuilder();
					TokensAtPosition tokensAtFirstPosition = tokens.get(0);
					String tokenToUse = tokensAtFirstPosition.chooseWildcardToken();
					attachWildard(sb, tokenToUse, leading, trailing);
					buildIntermediateQuery(builder, fld, sb.toString(), null);
				} else {
					/* Multi-term wildcard queries like "foo bar*" or single-term wildcard queries in non-whitespace delimited languages
					 * that tokenize to multiple terms need to be handled with wildcard expansion.
					 * Only trailing wildcards are supported here, but that includes queries like "foo* bar".
					 * We iteratively expand sections of the query that end with a wildcard followed by a word break.
					 * We then combine it into a BooleanQuery across all desired fields.
					 */
					String[] queryParts = queryText.split("(?<=\\*)\\s");
					boolean includeField = true;
					MultiPhraseQuery.Builder mpqBuilder = new MultiPhraseQuery.Builder();
					for (int i = 0; i < queryParts.length; i++ ) {
						if (!parseQueryPart(queryParts[i], fld, mpqBuilder)) {
							includeField = false;
						};
					}
					if (includeField) {
						//the only way this is false is if a wildcard expands to nothing in the field
						buildIntermediateQuery(builder, fld, null, new BooleanClause(mpqBuilder.build(), Occur.SHOULD));
					}
				}
			}
			parsed = builder.build();
		}
		return parsed;
	}

	private void attachWildard(StringBuilder sb, String tokenToUse, boolean leading,
			boolean trailing) {
		sb.append(leading && !tokenToUse.startsWith("*") ? "*": "")
		.append(tokenToUse)
		.append(trailing && !tokenToUse.endsWith("*") ? "*": "");
	}

	private boolean parseQueryPart(String queryPart, String field, MultiPhraseQuery.Builder builder) throws ParseException {
		int curPosition;
		MultiPhraseQuery mpq = builder.build(); //TODO: does this work?
		if (mpq.getPositions().length == 0) {
			curPosition = 0;
		} else {
			curPosition = mpq.getPositions()[mpq.getPositions().length - 1] + 1;
		}
		List<TokensAtPosition> tokens = getAllTokens(this.field, queryPart);
		if (tokens.size() > 1) {
			/* Insert all pre-wildcard tokens at their appropriate positions in
			 * the phrase query */
			for (int i = 0; i < tokens.size() - 1; i++) {
				TokensAtPosition tokensAtPosition = tokens.get(i);
				if (tokensAtPosition != null) {
					for (String token : tokensAtPosition) {
						builder.add(new Term[] { new Term(field, token) },
								curPosition + i);
					}
				}
			}
		}
		if (queryPart.endsWith("*")) {
				String lastToken;
	    	if (tokens.isEmpty()) {
			  /* An empty token set means all terms got dropped during tokenization.
			   * Grab the raw query text without the wildcard. */
				lastToken = queryPart.toLowerCase().substring(0, queryPart.length() - 1);
			} else {
			  /* If there are multiple tokens at last position, grab the last token.
			   * This is to handle the edge case of PUNC tokens with wildcards.*/
  			TokensAtPosition tokensAtLastPosition = tokens.get(tokens.size() - 1);
  			lastToken = tokensAtLastPosition.chooseWildcardToken();
			}
			try {
				List<Term> expandedTerms = expandPrefix(lastToken, field);
				if (expandedTerms.size() == 0) {
					return false;
				} else {
					builder.add(expandedTerms.toArray(new Term[expandedTerms.size()]), curPosition + tokens.size() - 1);
				}
			} catch (IOException e) {
				throw new ParseException("failed expanding phrase wildcard query");
			}
		} else if (!tokens.isEmpty()){
			//no wildcard, so add last token back
			for (String token: tokens.get(tokens.size() - 1)) {
				builder.add(new Term[]{new Term(field, token)}, curPosition + tokens.size() - 1);
			}
		}
		return true;
	}

	private void buildIntermediateQuery(BooleanQuery.Builder builder, String field, String queryText, BooleanClause clause) throws ParseException {
		if (clause != null) {
			builder.add(clause);
			return;
		} else {
			if (queryText.startsWith("*") && queryText.endsWith("*")) {
				queryText = "*" + escape(queryText.substring(1, queryText.length() - 1)) + "*";
			} else if (queryText.startsWith("*")) {
				queryText = "*" + escape(queryText.substring(1));
			} else if (queryText.endsWith("*")) {
				queryText = escape(queryText.substring(0, queryText.length() - 1)) + "*";
			} else {
				queryText = escape(queryText);
			}
			Query q = new TermQuery(new Term(field, queryText));
			try {
				builder.add(defaultParser.parse(q.toString()), Occur.SHOULD);
			} catch (SyntaxError e) {
				throw new ParseException("Parse error: " + e.getMessage());
			}
			return;
		}
	}

	private boolean hasWhitespace(String queryText) {
		return whitespace.matcher(queryText).find();
	}

	private List<TokensAtPosition> getAllTokens(String field, String queryText) {
		List<TokensAtPosition> tokens = new ArrayList<TokensAtPosition>();
		try {
			TokenStream ts = getAnalyzer().tokenStream(field, queryText);
			TypeAttribute typeAttr = ts.getAttribute(TypeAttribute.class);
			CharTermAttribute termAttr = ts.getAttribute(CharTermAttribute.class);
			PositionIncrementAttribute posAttr = ts.getAttribute(PositionIncrementAttribute.class);
			ts.reset();
			ArrayList<String> curPositionTokens = null;
			ArrayList<TokenType> curTypes = null;
			boolean hasMore = ts.incrementToken();
			while (hasMore) {
				int inc = posAttr.getPositionIncrement();
				if (inc == 0) {
				  //append to current token list
				  if (curPositionTokens != null) {
					  curPositionTokens.add(termAttr.toString());
					  curTypes.add(TokenType.valueOf(typeAttr.type()));
				  }
				  hasMore = ts.incrementToken();
				  continue;
				} else {
					//push active token list to the array, start a new one, and add nulls if increment > 1
					if (curPositionTokens != null) {
						tokens.add(new TokensAtPosition(curPositionTokens, curTypes));
					}
					curPositionTokens = new ArrayList<String>();
					curPositionTokens.add(termAttr.toString());
					curTypes = new ArrayList<TokenType>();
					try {
						curTypes.add(TokenType.valueOf(typeAttr.type()));
					} catch (IllegalArgumentException e) {
						//not using the ZimbraTokenizer
					} curTypes.add(null);
					hasMore = ts.incrementToken();
					for (int i = 1; i < inc; i++) {
						tokens.add(null);
					}
				}
			}
			if (curPositionTokens != null && !curPositionTokens.isEmpty()) {
				tokens.add(new TokensAtPosition(curPositionTokens, curTypes));
			}
			ts.end();
			ts.close();
		} catch (IOException e) {}
		return tokens;
	}

	public void setFields(String fields) {
		this.fields = Arrays.asList(fields.split(" "));
	}

	public void setDefaultParser(SolrQueryParser parser) {
		defaultParser = parser;
	}

	public void setReader(IndexReader reader) {
		this.reader = reader;
	}

	/* modeled on TermsComponent.process() */
	private List<Term> expandPrefix(String prefix, String field) throws IOException {
		List<Term> expanded = new LinkedList<Term>();
		Fields lfields = ((LeafReader) reader).fields();
		if (lfields == null) {
		    return expanded;
		}
		Terms terms = lfields.terms(field);
		BytesRef prefixBytes = new BytesRef(prefix);
		try {
			TermsEnum termsEnum = terms.iterator();
			BytesRef term = null;
			if (termsEnum.seekCeil(prefixBytes) == TermsEnum.SeekStatus.END) {
		          return expanded;
		    } else {
		    	term = termsEnum.term();
		    }
			int numExpanded = 0;
			while (term != null && numExpanded < maxExpansions) {
				if (!StringHelper.startsWith(term, prefixBytes)) {
					break;
				} else {
					expanded.add(new Term(field, term.utf8ToString()));
					term = termsEnum.next();
					numExpanded++;
				}
			}
		} catch (NullPointerException noTerms) {
			//edge case - fall through to return empty list
		}
		return expanded;
	}

	public void setMaxExpansions(int maxExpansions) {
		this.maxExpansions = maxExpansions;
	}

	private class TokensAtPosition implements Iterable<String> {
		private final List<String> tokens;
		private final List<TokenType> tokenTypes;

		public TokensAtPosition(List<String> tokens, List<TokenType> types) {
			this.tokens = tokens;
			this.tokenTypes = types;
		}

		/* Given several tokens assigned to the same position,
		 * which can happen with PUNC tokens or synonyms,
		 * choose which one to use as the wildcard term.
		 * Currently, chooses the last non-PUNC token or the last token if all are PUNC.
		 */
		public String chooseWildcardToken() {
			if (tokens.size() == 1) {
				return tokens.get(0);
			} else {
				for (int i = 0; i < tokenTypes.size(); i++) {
					if (tokenTypes.get(i) != ZimbraTokenizer.TokenType.PUNC) {
						return tokens.get(i);
					}
				}
				return tokens.get(tokens.size() - 1);
			}
		}

		public List<String> getTokens() {
			return tokens;
		}

		public int size() {
			return tokens.size();
		}

		@Override
		public Iterator<String> iterator() {
			return tokens.iterator();
		}

	}
}
