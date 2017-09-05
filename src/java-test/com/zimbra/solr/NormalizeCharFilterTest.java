package com.zimbra.solr;

import static org.junit.Assert.*;
import org.junit.Test;

public class NormalizeCharFilterTest {
	
    @Test
    public void alphabet() {
        assertEquals('a', NormalizeCharFilter.normalize('\uFF21'));
        assertEquals('b', NormalizeCharFilter.normalize('\uFF22'));
        assertEquals('c', NormalizeCharFilter.normalize('\uFF23'));
        assertEquals('d', NormalizeCharFilter.normalize('\uFF24'));
        assertEquals('e', NormalizeCharFilter.normalize('\uFF25'));
        assertEquals('f', NormalizeCharFilter.normalize('\uFF26'));
        assertEquals('g', NormalizeCharFilter.normalize('\uFF27'));
        assertEquals('h', NormalizeCharFilter.normalize('\uFF28'));
        assertEquals('i', NormalizeCharFilter.normalize('\uFF29'));
        assertEquals('j', NormalizeCharFilter.normalize('\uFF2A'));
        assertEquals('k', NormalizeCharFilter.normalize('\uFF2B'));
        assertEquals('l', NormalizeCharFilter.normalize('\uFF2C'));
        assertEquals('m', NormalizeCharFilter.normalize('\uFF2D'));
        assertEquals('n', NormalizeCharFilter.normalize('\uFF2E'));
        assertEquals('o', NormalizeCharFilter.normalize('\uFF2F'));
        assertEquals('p', NormalizeCharFilter.normalize('\uFF30'));
        assertEquals('q', NormalizeCharFilter.normalize('\uFF31'));
        assertEquals('r', NormalizeCharFilter.normalize('\uFF32'));
        assertEquals('s', NormalizeCharFilter.normalize('\uFF33'));
        assertEquals('t', NormalizeCharFilter.normalize('\uFF34'));
        assertEquals('u', NormalizeCharFilter.normalize('\uFF35'));
        assertEquals('v', NormalizeCharFilter.normalize('\uFF36'));
        assertEquals('w', NormalizeCharFilter.normalize('\uFF37'));
        assertEquals('x', NormalizeCharFilter.normalize('\uFF38'));
        assertEquals('y', NormalizeCharFilter.normalize('\uFF39'));
        assertEquals('z', NormalizeCharFilter.normalize('\uFF3A'));

        assertEquals('a', NormalizeCharFilter.normalize('\uFF41'));
        assertEquals('b', NormalizeCharFilter.normalize('\uFF42'));
        assertEquals('c', NormalizeCharFilter.normalize('\uFF43'));
        assertEquals('d', NormalizeCharFilter.normalize('\uFF44'));
        assertEquals('e', NormalizeCharFilter.normalize('\uFF45'));
        assertEquals('f', NormalizeCharFilter.normalize('\uFF46'));
        assertEquals('g', NormalizeCharFilter.normalize('\uFF47'));
        assertEquals('h', NormalizeCharFilter.normalize('\uFF48'));
        assertEquals('i', NormalizeCharFilter.normalize('\uFF49'));
        assertEquals('j', NormalizeCharFilter.normalize('\uFF4A'));
        assertEquals('k', NormalizeCharFilter.normalize('\uFF4B'));
        assertEquals('l', NormalizeCharFilter.normalize('\uFF4C'));
        assertEquals('m', NormalizeCharFilter.normalize('\uFF4D'));
        assertEquals('n', NormalizeCharFilter.normalize('\uFF4E'));
        assertEquals('o', NormalizeCharFilter.normalize('\uFF4F'));
        assertEquals('p', NormalizeCharFilter.normalize('\uFF50'));
        assertEquals('q', NormalizeCharFilter.normalize('\uFF51'));
        assertEquals('r', NormalizeCharFilter.normalize('\uFF52'));
        assertEquals('s', NormalizeCharFilter.normalize('\uFF53'));
        assertEquals('t', NormalizeCharFilter.normalize('\uFF54'));
        assertEquals('u', NormalizeCharFilter.normalize('\uFF55'));
        assertEquals('v', NormalizeCharFilter.normalize('\uFF56'));
        assertEquals('w', NormalizeCharFilter.normalize('\uFF57'));
        assertEquals('x', NormalizeCharFilter.normalize('\uFF58'));
        assertEquals('y', NormalizeCharFilter.normalize('\uFF59'));
        assertEquals('z', NormalizeCharFilter.normalize('\uFF5A'));
    }

    @Test
    public void number() {
        assertEquals('0', NormalizeCharFilter.normalize('\uFF10'));
        assertEquals('1', NormalizeCharFilter.normalize('\uFF11'));
        assertEquals('2', NormalizeCharFilter.normalize('\uFF12'));
        assertEquals('3', NormalizeCharFilter.normalize('\uFF13'));
        assertEquals('4', NormalizeCharFilter.normalize('\uFF14'));
        assertEquals('5', NormalizeCharFilter.normalize('\uFF15'));
        assertEquals('6', NormalizeCharFilter.normalize('\uFF16'));
        assertEquals('7', NormalizeCharFilter.normalize('\uFF17'));
        assertEquals('8', NormalizeCharFilter.normalize('\uFF18'));
        assertEquals('9', NormalizeCharFilter.normalize('\uFF19'));
    }

    /**
     * @see http://en.wikipedia.org/wiki/Trema_(diacritic)
     */
    @Test
    public void trema() {
        assertEquals('a', NormalizeCharFilter.normalize('\u00c4'));
        assertEquals('a', NormalizeCharFilter.normalize('\u00e4'));
        assertEquals('a', NormalizeCharFilter.normalize('\u01de'));
        assertEquals('a', NormalizeCharFilter.normalize('\u01df'));
        assertEquals('e', NormalizeCharFilter.normalize('\u00cb'));
        assertEquals('e', NormalizeCharFilter.normalize('\u00eb'));
        assertEquals('h', NormalizeCharFilter.normalize('\u1e26'));
        assertEquals('h', NormalizeCharFilter.normalize('\u1e27'));
        assertEquals('i', NormalizeCharFilter.normalize('\u00cf'));
        assertEquals('i', NormalizeCharFilter.normalize('\u00ef'));
        assertEquals('i', NormalizeCharFilter.normalize('\u1e2e'));
        assertEquals('i', NormalizeCharFilter.normalize('\u1e2f'));
        assertEquals('o', NormalizeCharFilter.normalize('\u00d6'));
        assertEquals('o', NormalizeCharFilter.normalize('\u00f6'));
        assertEquals('o', NormalizeCharFilter.normalize('\u022a'));
        assertEquals('o', NormalizeCharFilter.normalize('\u022b'));
        assertEquals('o', NormalizeCharFilter.normalize('\u1e4e'));
        assertEquals('o', NormalizeCharFilter.normalize('\u1e4f'));
        assertEquals('u', NormalizeCharFilter.normalize('\u00dc'));
        assertEquals('u', NormalizeCharFilter.normalize('\u00fc'));
        assertEquals('u', NormalizeCharFilter.normalize('\u01d5'));
        assertEquals('u', NormalizeCharFilter.normalize('\u01d6'));
        assertEquals('u', NormalizeCharFilter.normalize('\u01d7'));
        assertEquals('u', NormalizeCharFilter.normalize('\u01d8'));
        assertEquals('u', NormalizeCharFilter.normalize('\u01d9'));
        assertEquals('u', NormalizeCharFilter.normalize('\u01da'));
        assertEquals('u', NormalizeCharFilter.normalize('\u01db'));
        assertEquals('u', NormalizeCharFilter.normalize('\u01dc'));
        assertEquals('u', NormalizeCharFilter.normalize('\u1e72'));
        assertEquals('u', NormalizeCharFilter.normalize('\u1e73'));
        assertEquals('u', NormalizeCharFilter.normalize('\u1e7a'));
        assertEquals('u', NormalizeCharFilter.normalize('\u1e7b'));
        assertEquals('w', NormalizeCharFilter.normalize('\u1e84'));
        assertEquals('w', NormalizeCharFilter.normalize('\u1e85'));
        assertEquals('x', NormalizeCharFilter.normalize('\u1e8c'));
        assertEquals('x', NormalizeCharFilter.normalize('\u1e8d'));
        assertEquals('y', NormalizeCharFilter.normalize('\u0178'));
        assertEquals('y', NormalizeCharFilter.normalize('\u00ff'));
    }
    
    /**
     * @see http://en.wikipedia.org/wiki/Katakana
     */
    @Test
    public void katakana() {
        assertEquals('\u3041', NormalizeCharFilter.normalize('\u30A1'));
        assertEquals('\u3042', NormalizeCharFilter.normalize('\u30A2'));
        assertEquals('\u3043', NormalizeCharFilter.normalize('\u30A3'));
        assertEquals('\u3044', NormalizeCharFilter.normalize('\u30A4'));
        assertEquals('\u3045', NormalizeCharFilter.normalize('\u30A5'));
        assertEquals('\u3046', NormalizeCharFilter.normalize('\u30A6'));
        assertEquals('\u3047', NormalizeCharFilter.normalize('\u30A7'));
        assertEquals('\u3048', NormalizeCharFilter.normalize('\u30A8'));
        assertEquals('\u3049', NormalizeCharFilter.normalize('\u30A9'));
        assertEquals('\u304A', NormalizeCharFilter.normalize('\u30AA'));
        assertEquals('\u304B', NormalizeCharFilter.normalize('\u30AB'));
        assertEquals('\u304C', NormalizeCharFilter.normalize('\u30AC'));
        assertEquals('\u304D', NormalizeCharFilter.normalize('\u30AD'));
        assertEquals('\u304E', NormalizeCharFilter.normalize('\u30AE'));
        assertEquals('\u304F', NormalizeCharFilter.normalize('\u30AF'));
        assertEquals('\u3051', NormalizeCharFilter.normalize('\u30B1'));
        assertEquals('\u3052', NormalizeCharFilter.normalize('\u30B2'));
        assertEquals('\u3053', NormalizeCharFilter.normalize('\u30B3'));
        assertEquals('\u3054', NormalizeCharFilter.normalize('\u30B4'));
        assertEquals('\u3055', NormalizeCharFilter.normalize('\u30B5'));
        assertEquals('\u3056', NormalizeCharFilter.normalize('\u30B6'));
        assertEquals('\u3057', NormalizeCharFilter.normalize('\u30B7'));
        assertEquals('\u3058', NormalizeCharFilter.normalize('\u30B8'));
        assertEquals('\u3059', NormalizeCharFilter.normalize('\u30B9'));
        assertEquals('\u305A', NormalizeCharFilter.normalize('\u30BA'));
        assertEquals('\u305B', NormalizeCharFilter.normalize('\u30BB'));
        assertEquals('\u305C', NormalizeCharFilter.normalize('\u30BC'));
        assertEquals('\u305D', NormalizeCharFilter.normalize('\u30BD'));
        assertEquals('\u305E', NormalizeCharFilter.normalize('\u30BE'));
        assertEquals('\u305F', NormalizeCharFilter.normalize('\u30BF'));
        assertEquals('\u3061', NormalizeCharFilter.normalize('\u30C1'));
        assertEquals('\u3062', NormalizeCharFilter.normalize('\u30C2'));
        assertEquals('\u3063', NormalizeCharFilter.normalize('\u30C3'));
        assertEquals('\u3064', NormalizeCharFilter.normalize('\u30C4'));
        assertEquals('\u3065', NormalizeCharFilter.normalize('\u30C5'));
        assertEquals('\u3066', NormalizeCharFilter.normalize('\u30C6'));
        assertEquals('\u3067', NormalizeCharFilter.normalize('\u30C7'));
        assertEquals('\u3068', NormalizeCharFilter.normalize('\u30C8'));
        assertEquals('\u3069', NormalizeCharFilter.normalize('\u30C9'));
        assertEquals('\u306A', NormalizeCharFilter.normalize('\u30CA'));
        assertEquals('\u306B', NormalizeCharFilter.normalize('\u30CB'));
        assertEquals('\u306C', NormalizeCharFilter.normalize('\u30CC'));
        assertEquals('\u306D', NormalizeCharFilter.normalize('\u30CD'));
        assertEquals('\u306E', NormalizeCharFilter.normalize('\u30CE'));
        assertEquals('\u306F', NormalizeCharFilter.normalize('\u30CF'));
        assertEquals('\u3071', NormalizeCharFilter.normalize('\u30D1'));
        assertEquals('\u3072', NormalizeCharFilter.normalize('\u30D2'));
        assertEquals('\u3073', NormalizeCharFilter.normalize('\u30D3'));
        assertEquals('\u3074', NormalizeCharFilter.normalize('\u30D4'));
        assertEquals('\u3075', NormalizeCharFilter.normalize('\u30D5'));
        assertEquals('\u3076', NormalizeCharFilter.normalize('\u30D6'));
        assertEquals('\u3077', NormalizeCharFilter.normalize('\u30D7'));
        assertEquals('\u3078', NormalizeCharFilter.normalize('\u30D8'));
        assertEquals('\u3079', NormalizeCharFilter.normalize('\u30D9'));
        assertEquals('\u307A', NormalizeCharFilter.normalize('\u30DA'));
        assertEquals('\u307B', NormalizeCharFilter.normalize('\u30DB'));
        assertEquals('\u307C', NormalizeCharFilter.normalize('\u30DC'));
        assertEquals('\u307D', NormalizeCharFilter.normalize('\u30DD'));
        assertEquals('\u307E', NormalizeCharFilter.normalize('\u30DE'));
        assertEquals('\u307F', NormalizeCharFilter.normalize('\u30DF'));
        assertEquals('\u3081', NormalizeCharFilter.normalize('\u30E1'));
        assertEquals('\u3082', NormalizeCharFilter.normalize('\u30E2'));
        assertEquals('\u3083', NormalizeCharFilter.normalize('\u30E3'));
        assertEquals('\u3084', NormalizeCharFilter.normalize('\u30E4'));
        assertEquals('\u3085', NormalizeCharFilter.normalize('\u30E5'));
        assertEquals('\u3086', NormalizeCharFilter.normalize('\u30E6'));
        assertEquals('\u3087', NormalizeCharFilter.normalize('\u30E7'));
        assertEquals('\u3088', NormalizeCharFilter.normalize('\u30E8'));
        assertEquals('\u3089', NormalizeCharFilter.normalize('\u30E9'));
        assertEquals('\u308A', NormalizeCharFilter.normalize('\u30EA'));
        assertEquals('\u308B', NormalizeCharFilter.normalize('\u30EB'));
        assertEquals('\u308C', NormalizeCharFilter.normalize('\u30EC'));
        assertEquals('\u308D', NormalizeCharFilter.normalize('\u30ED'));
        assertEquals('\u308E', NormalizeCharFilter.normalize('\u30EE'));
        assertEquals('\u308F', NormalizeCharFilter.normalize('\u30EF'));
        assertEquals('\u3091', NormalizeCharFilter.normalize('\u30F1'));
        assertEquals('\u3092', NormalizeCharFilter.normalize('\u30F2'));
        assertEquals('\u3093', NormalizeCharFilter.normalize('\u30F3'));
        assertEquals('\u3094', NormalizeCharFilter.normalize('\u30F4'));
        assertEquals('\u3095', NormalizeCharFilter.normalize('\u30F5'));
        assertEquals('\u3096', NormalizeCharFilter.normalize('\u30F6'));
    }

    /**
     * @see http://en.wikipedia.org/wiki/Katakana
     */
    @Test
    public void HalfWidthkatakana() {
        assertEquals('\u3042', NormalizeCharFilter.normalize('\uFF71'));
        assertEquals('\u3044', NormalizeCharFilter.normalize('\uFF72'));
        assertEquals('\u3046', NormalizeCharFilter.normalize('\uFF73'));
        assertEquals('\u3048', NormalizeCharFilter.normalize('\uFF74'));
        assertEquals('\u304A', NormalizeCharFilter.normalize('\uFF75'));
        assertEquals('\u304B', NormalizeCharFilter.normalize('\uFF76'));
        assertEquals('\u304D', NormalizeCharFilter.normalize('\uFF77'));
        assertEquals('\u304F', NormalizeCharFilter.normalize('\uFF78'));
        assertEquals('\u3051', NormalizeCharFilter.normalize('\uFF79'));
        assertEquals('\u3053', NormalizeCharFilter.normalize('\uFF7A'));
        assertEquals('\u3055', NormalizeCharFilter.normalize('\uFF7B'));
        assertEquals('\u3057', NormalizeCharFilter.normalize('\uFF7C'));
        assertEquals('\u3059', NormalizeCharFilter.normalize('\uFF7D'));
        assertEquals('\u305B', NormalizeCharFilter.normalize('\uFF7E'));
        assertEquals('\u305D', NormalizeCharFilter.normalize('\uFF7F'));
        assertEquals('\u305F', NormalizeCharFilter.normalize('\uFF80'));
        assertEquals('\u3061', NormalizeCharFilter.normalize('\uFF81'));
        assertEquals('\u3064', NormalizeCharFilter.normalize('\uFF82'));
        assertEquals('\u3066', NormalizeCharFilter.normalize('\uFF83'));
        assertEquals('\u3068', NormalizeCharFilter.normalize('\uFF84'));
        assertEquals('\u306A', NormalizeCharFilter.normalize('\uFF85'));
        assertEquals('\u306B', NormalizeCharFilter.normalize('\uFF86'));
        assertEquals('\u306C', NormalizeCharFilter.normalize('\uFF87'));
        assertEquals('\u306D', NormalizeCharFilter.normalize('\uFF88'));
        assertEquals('\u306E', NormalizeCharFilter.normalize('\uFF89'));
        assertEquals('\u306F', NormalizeCharFilter.normalize('\uFF8A'));
        assertEquals('\u3072', NormalizeCharFilter.normalize('\uFF8B'));
        assertEquals('\u3075', NormalizeCharFilter.normalize('\uFF8C'));
        assertEquals('\u3078', NormalizeCharFilter.normalize('\uFF8D'));
        assertEquals('\u307B', NormalizeCharFilter.normalize('\uFF8E'));
        assertEquals('\u307E', NormalizeCharFilter.normalize('\uFF8F'));
        assertEquals('\u307F', NormalizeCharFilter.normalize('\uFF90'));
        assertEquals('\u3080', NormalizeCharFilter.normalize('\uFF91'));
        assertEquals('\u3081', NormalizeCharFilter.normalize('\uFF92'));
        assertEquals('\u3082', NormalizeCharFilter.normalize('\uFF93'));
        assertEquals('\u3084', NormalizeCharFilter.normalize('\uFF94'));
        assertEquals('\u3086', NormalizeCharFilter.normalize('\uFF95'));
        assertEquals('\u3088', NormalizeCharFilter.normalize('\uFF96'));
        assertEquals('\u3089', NormalizeCharFilter.normalize('\uFF97'));
        assertEquals('\u308A', NormalizeCharFilter.normalize('\uFF98'));
        assertEquals('\u308B', NormalizeCharFilter.normalize('\uFF99'));
        assertEquals('\u308C', NormalizeCharFilter.normalize('\uFF9A'));
        assertEquals('\u308D', NormalizeCharFilter.normalize('\uFF9B'));
        assertEquals('\u308F', NormalizeCharFilter.normalize('\uFF9C'));
        assertEquals('\u3093', NormalizeCharFilter.normalize('\uFF9D'));
    }
}
