package com.openelements.crm.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SearchController#safeHighlight(String)}. The function
 * is the XSS firewall between Meilisearch's {@code _formatted} output and the
 * frontend's {@code dangerouslySetInnerHTML} call.
 */
class SearchControllerSafeHighlightTest {

    @Test
    void plainTextWithoutMarkersIsHtmlEscaped() {
        final String input = "<script>alert(1)</script>";
        final String out = SearchController.safeHighlight(input);
        assertFalse(out.contains("<script>"), "Raw <script> must not survive");
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", out);
    }

    @Test
    void markersAroundEscapedTextProduceEmWrappedSafeHtml() {
        // Meilisearch returns the private-use boundary markers around matched
        // fragments. The user-text inside the markers must be HTML-escaped
        // before the markers are converted to <em>/</em>.
        final String input = SearchController.PRE_MARK + "<script>" + SearchController.POST_MARK + " and friends";
        final String out = SearchController.safeHighlight(input);
        assertEquals("<em>&lt;script&gt;</em> and friends", out);
    }

    @Test
    void nullInputReturnsEmptyString() {
        assertEquals("", SearchController.safeHighlight(null));
    }

    @Test
    void ampersandsAndQuotesAreEscaped() {
        assertEquals("a &amp; b &quot;c&quot; &#39;d&#39;",
            SearchController.safeHighlight("a & b \"c\" 'd'"));
    }

    @Test
    void normalTextPassesThroughUnchanged() {
        assertEquals("Hello world", SearchController.safeHighlight("Hello world"));
    }
}
