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

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

%%

%class ZimbraLexer
%final
%unicode
%type ZimbraTokenizer.TokenType
%function next
%pack
%char

%{

int yychar() { return yychar; }

void getTerm(CharTermAttribute t) {
    t.copyBuffer(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
}

void getTerm(CharTermAttribute t, int offset, int len) {
    t.copyBuffer(zzBuffer, zzStartRead + offset, len);
}

%}

CJK_LETTER = [\u2E80-\u2FFF\u3040-\u9FFF\uAC00-\uD7FF\uFF00-\uFFEF]

/*
 * 2E80-2EFF CJK Radicals Supplement
 * 2F00-2FDF Kangxi Radicals
 * 2FF0-2FFF Ideographic Description Characters
 *------------------------------------------------------------------------------
 * 3000-303F [EXCLUDE] CJK Symbols and Punctuation
 *------------------------------------------------------------------------------
 * 3040-309F Hiragana
 * 30A0-30FF Katakana
 * 3100-312F Bopomofo
 * 3130-318F Hangul Compatibility Jamo
 * 3190-319F Kanbun
 * 31A0-31BF Bopomofo Extended
 * 31C0-31EF CJK Strokes
 * 31F0-31FF Katakana Phonetic Extensions
 * 3200-32FF Enclosed CJK Letters and Months
 * 3300-33FF CJK Compatibility
 * 3400-4DBF CJK Unified Ideographs Extension A
 * 4DC0-4DFF Yijing Hexagram Symbols
 * 4E00-9FFF CJK Unified Ideographs
 *------------------------------------------------------------------------------
 * AC00-D7AF Hangul Syllables
 * D7B0-D7FF Hangul Jamo Extended-B
 *------------------------------------------------------------------------------
 * FF00-FFEF Halfwidth and Fullwidth Forms
 *------------------------------------------------------------------------------
 */

SA_LETTER = [\u0E80-\u0EFF\u1000-\u109F\u1780-\u17FF]

/* 0E80-0EFF Lao
 * 1000-109F Myanmar
 * 1780-17FF Khmer
 */

THAI_LETTER = [\u0E00-\u0E7F]
THAI = {THAI_LETTER}+

/* There are three types of punctuation characters: 
 * 1) those treated as literals inside strings (PUNC_LITERAL)
 * 2) those used as delimiters for the NUM token type (PUNC_NUM)
 * 3) those treated as a separate token (PUNC_TOKEN)
 * Note that the dash and underscore is present in 1) and 2), which may cause some backtracking.
 */

PUNC_LITERAL = "-"|"+"|"<"|">"|"_"
PUNC_NUM = "-"|"_"|"."|","
PUNC_TOKEN_CHAR = "~"|"!"|"#"|"$"|"%"|"^"|"&"|"("|")"|"?"|"{"|"}"|"["|"]"|";"
PUNC_TOKEN = {PUNC_TOKEN_CHAR}+

LETTER = !(![:letter:]|{CJK_LETTER}|{THAI_LETTER}|{SA_LETTER})
WSPACE = [ \t\f]|\R 
ALPHA = {LETTER}+

/* We also want PUNC_LITERALs retained within alphanumeric character sequences
 * as well as CJK sequences.
 * However, we don't want just a sequence of PUNC_LITERALs to be retained.
 * For example, "--" should be skipped, but "c++" should be kept intact.
 */ 
ALNUM = ({LETTER}|[:digit:])+
ALNUM_OR_PUNC_CHAR = {LETTER}|[:digit:]|{PUNC_LITERAL}
ALNUM_WITH_PUNC_LITERALS = {ALNUM_OR_PUNC_CHAR}* {ALNUM} {ALNUM_OR_PUNC_CHAR}*
CJK_LETTER_OR_PUNC = {CJK_LETTER} | {PUNC_LITERAL}
CJK_WITH_PUNC_LITERALS = {CJK_LETTER_OR_PUNC}* {CJK_LETTER}+ {CJK_LETTER_OR_PUNC}*
SA_LETTER_OR_PUNC = {SA_LETTER} | {PUNC_LITERAL}
SA_WITH_PUNC_LITERALS = {SA_LETTER_OR_PUNC}* {SA_LETTER}+ {SA_LETTER_OR_PUNC}*
APOSTROPHE =  {ALPHA} ("'" {ALPHA})+
ACRONYM = {LETTER} "." ({LETTER} ".")+
COMPANY = {ALPHA} ("&"|"@") {ALPHA}
EMAIL = {ALNUM} (("."|"-"|"_") {ALNUM})* "@" {ALNUM} (("."|"-") {ALNUM})+
HOST = {ALNUM} ("-" {ALNUM})* ("." {ALNUM} ("-" {ALNUM})* )+
HAS_DIGIT = ({LETTER}|[:digit:])* [:digit:] ({LETTER}|[:digit:])*
// floating point, serial, model numbers, ip addresses, etc.
// every other segment must have at least one digit
NUM = ({ALNUM} {PUNC_NUM} {HAS_DIGIT}
    |  {HAS_DIGIT} {PUNC_NUM} {ALNUM}
    |  {ALNUM} ({PUNC_NUM} {HAS_DIGIT} {PUNC_NUM} {ALNUM})+
    |  {HAS_DIGIT} ({PUNC_NUM} {ALNUM} {PUNC_NUM} {HAS_DIGIT})+
    |  {ALNUM} {PUNC_NUM} {HAS_DIGIT} ({PUNC_NUM} {ALNUM} {PUNC_NUM} {HAS_DIGIT})+
    |  {HAS_DIGIT} {PUNC_NUM} {ALNUM} ({PUNC_NUM} {HAS_DIGIT} {PUNC_NUM} {ALNUM})+)

%%

{HOST}                     { return ZimbraTokenizer.TokenType.HOST; }
{NUM}                      { return ZimbraTokenizer.TokenType.NUM; }
{ALNUM_WITH_PUNC_LITERALS} { return ZimbraTokenizer.TokenType.ALNUM; }
{APOSTROPHE}               { return ZimbraTokenizer.TokenType.APOSTROPHE; }
{ACRONYM}                  { return ZimbraTokenizer.TokenType.ACRONYM; }
{COMPANY}                  { return ZimbraTokenizer.TokenType.COMPANY; }
{EMAIL}                    { return ZimbraTokenizer.TokenType.EMAIL; }
{CJK_WITH_PUNC_LITERALS}   { return ZimbraTokenizer.TokenType.CJK; }
{THAI}                     { return ZimbraTokenizer.TokenType.THAI; }
{SA_WITH_PUNC_LITERALS}    { return ZimbraTokenizer.TokenType.SOUTHEAST_ASIAN; }
{PUNC_TOKEN}               { return ZimbraTokenizer.TokenType.PUNC; }
. | {WSPACE} { /* ignore */ }
