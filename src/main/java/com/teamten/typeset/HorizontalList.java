/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.teamten.typeset;

import com.google.common.base.Strings;
import com.teamten.font.FontPack;
import com.teamten.font.SizedFont;
import com.teamten.hyphen.HyphenDictionary;
import com.teamten.markdown.TextSpan;
import com.teamten.typeset.element.Discretionary;
import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Footnote;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.HBox;
import com.teamten.typeset.element.Kern;
import com.teamten.typeset.element.Penalty;
import com.teamten.typeset.element.Rule;
import com.teamten.typeset.element.Text;
import com.teamten.typeset.element.VBox;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Accumulates elements in a horizontal list until a paragraph is finished, at which point a list of
 * elements is generated and added to a vertical list.
 */
public class HorizontalList extends ElementList {
    private static final boolean DEBUG_HYPHENATION = false;
    private final boolean mRaggedLeft;
    private final boolean mRaggedRight;
    private final boolean mAllowLineBreaks;

    private HorizontalList(boolean raggedLeft, boolean raggedRight, boolean allowLineBreaks) {
        mRaggedLeft = raggedLeft;
        mRaggedRight = raggedRight;
        mAllowLineBreaks = allowLineBreaks;
    }

    public HorizontalList() {
        this(false, false, true);
    }

    /**
     * Create a new object that has a ragged right edge (no justification).
     */
    public static HorizontalList raggedRight() {
        return new HorizontalList(false, true, true);
    }

    /**
     * Create a new object that's centered.
     */
    public static HorizontalList centered() {
        return new HorizontalList(true, true, true);
    }

    /**
     * Create a new object that does not permit line breaks at spaces. This implicitly is also ragged right.
     */
    public static HorizontalList noLineBreaks() {
        return new HorizontalList(false, true, false);
    }

    /**
     * Add the text span to the horizontal list, properly picking the font.
     */
    public void addTextSpan(TextSpan textSpan, FontPack fontPack, HyphenDictionary hyphenDictionary) {
        // Pick the right font.
        SizedFont font = textSpan.isSmallCaps() ? fontPack.getSmallCapsFont()
                : textSpan.isBold() && textSpan.isItalic() ? fontPack.getBoldItalicFont()
                : textSpan.isBold() ? fontPack.getBoldFont()
                : textSpan.isItalic() ? fontPack.getItalicFont()
                : textSpan.isCode() ? fontPack.getCodeFont()
                : fontPack.getRegularFont();

        String text = textSpan.getText();

        // Add the text to the current horizontal list.
        addText(text, font, hyphenDictionary);
    }

    /**
     * Get the number of footnotes in this horizontal list.
     */
    public int getFootnoteCount() {
        int count = 0;

        for (Element element : getElements()) {
            if (element instanceof Footnote) {
                count++;
            }
        }

        return count;
    }

    @Override
    protected HBox makeOutputBox(List<Element> elements, int lineNumber, long shift) {
        return new HBox(elements, shift);
    }

    @Override
    protected long getElementSize(Element element) {
        return element.getWidth();
    }

    /**
     * Return the list of elements on this line, from beginBreakpoint (inclusive) to endBreakpoint
     * (inclusive only if it's a discretionary element). All discretionary elements are turned
     * into HBoxes depending on where they are.
     */
    @Override
    protected List<Element> getElementSublist(Breakpoint beginBreakpoint, Breakpoint endBreakpoint) {
        List<Element> allElements = getElements();
        int beginIndex = beginBreakpoint.getStartIndex();
        int endIndex = endBreakpoint.getIndex();

        List<Element> elements = new ArrayList<>(Math.max(endIndex - beginIndex + 1, 10));

        if (mRaggedLeft) {
            // This 10pt is a bit arbitrary. Could make it configurable later. Its purpose is to make it
            // acceptable to leave a bunch of space on the left.
            elements.add(new Glue(0, PT.toSp(10.0), false, 0, false, true));
        }

        for (int i = beginIndex; i <= endIndex; i++) {
            Element element = allElements.get(i);

            // Include all discretionary elements, but convert them to HBoxes.
            if (element instanceof Discretionary) {
                Discretionary discretionary = (Discretionary) element;
                HBox hbox;
                if (i == beginIndex) {
                    // This is the discretionary break at the beginning of the line. Use the "post" HBox.
                    hbox = discretionary.getPostBreak();
                } else if (i == endIndex) {
                    // This is the discretionary break at the end of the line. Use the "pre" HBox.
                    hbox = discretionary.getPreBreak();
                } else {
                    // This is a discretionary in the middle of the line. Use the "no" HBox.
                    hbox = discretionary.getNoBreak();
                }
                elements.add(hbox);
            } else if (i < endIndex) {
                // The end index is normally exclusive.
                elements.add(element);
            }
        }

        if (mRaggedRight) {
            // This 10pt is a bit arbitrary. Could make it configurable later. Its purpose is to make it
            // acceptable to leave a bunch of space on the right.
            elements.add(new Glue(0, PT.toSp(10.0), false, 0, false, true));
        }

        return elements;
    }

    @Override
    protected int getChunkExtraIncrement(Chunk chunk) {
        // The auxiliary elements like images don't take up space because they're removed when processing
        // horizontal elements.
        return 0;
    }

    @Override
    protected List<Element> reverseRightToLeftText(List<Element> elements) {
        // First see if we have any right-to-left characters.
        boolean containsRightToLeft = false;
        for (Element element : elements) {
            if (element instanceof Text) {
                Text text = (Text) element;
                if (text.containsRightToLeftText()) {
                    containsRightToLeft = true;
                    break;
                }
            }
        }

        // Shortcut if we don't have any right-to-left text.
        if (!containsRightToLeft) {
            return elements;
        }

        // Split all Text objects so that no Text object contains more than
        // one character. Leave the rest alone.
        List<Element> singleChars = new ArrayList<>();
        for (Element element : elements) {
            if (element instanceof Text) {
                Text text = (Text) element;
                text.breakUpInto(singleChars);
            } else {
                singleChars.add(element);
            }
        }

        // Find sequences of elements that start with a RTL character,
        // contains only RTL characters, neutral characters, or non-Text
        // elements, and ends on a RTL character. Reverse them in-place.
        int firstRtl = -1;
        int mostRecentRtl = -1;
        for (int i = 0; i < singleChars.size(); i++) {
            Element element = singleChars.get(i);
            if (element instanceof Text) {
                Text text = (Text) element;
                Text.CharacterDirection characterDirection = text.getCharacterDirection();
                switch (characterDirection) {
                    case LEFT_TO_RIGHT:
                        // If we were in a RTL section, this ends it.
                        if (firstRtl != -1) {
                            // Reverse the RTL section.
                            reverseSection(singleChars, firstRtl, mostRecentRtl);

                            firstRtl = -1;
                            mostRecentRtl = -1;
                        }
                        break;

                    case NEUTRAL:
                        // Nothing to do, continue what we were doing.
                        break;

                    case RIGHT_TO_LEFT:
                        if (firstRtl == -1) {
                            // First RTL one we've seen, just remember it.
                            firstRtl = i;
                        }

                        // This is the most recent RTL, remember it.
                        mostRecentRtl = i;
                        break;
                }
            }
        }

        // If we ended in an RTL section, reverse it.
        if (firstRtl != -1) {
            reverseSection(singleChars, firstRtl, mostRecentRtl);
        }

        // Here we could collapse single-char Text objects into multi-char
        // Text objects, but I don't see the benefit.
        return singleChars;
    }

    /**
     * Reverse the elements between first and last inclusive.
     */
    private void reverseSection(List<Element> elements, int first, int last) {
        int half = (last - first + 1)/2;

        for (int i = 0; i < half; i++) {
            int bottom = first + i;
            int top = last - i;

            Element tmp = elements.get(bottom);
            elements.set(bottom, elements.get(top));
            elements.set(top, tmp);
        }
    }

    /**
     * Add the specified text, in the specified font, to the horizontal list.
     *
     * @param hyphenDictionary the dictionary to use for hyphenation, or null to skip hyphenation.
     */
    public void addText(String text, SizedFont font, HyphenDictionary hyphenDictionary) {
        // First, convert the single string to a sequence of elements, where each word
        // is a single Text element. There will be other elements, like glues and
        // penalties.
        List<Element> elements = textToWords(text, font);

        // Second, go through the text elements and add discretionary hyphens.
        if (hyphenDictionary != null && mAllowLineBreaks) {
            elements = hyphenate(elements, hyphenDictionary);
        }

        // Third, go through the text elements and replace the ligatures.
        elements = transformLigatures(elements, font);

        // Finally, add kerning between and within text elements.
        elements = addKerning(elements, font);

        // Add all the final elements to our horizontal list.
        elements.forEach(this::addElement);
    }

    /**
     * Take the single large string and break it into three kinds of elements: glue (for space and non-breaking
     * space); words; and sequences of non-word characters.
     */
    private List<Element> textToWords(String text, SizedFont font) {
        List<Element> elements = new ArrayList<>();

        long spaceWidth = font.getSpaceWidth();

        // Don't allow stretching or shrinking if we're not justified.
        long stretchability = mRaggedLeft || mRaggedRight ? 0 : 1;

        // Roughly copy TeX.
        Glue spaceGlue = new Glue(spaceWidth, spaceWidth/2*stretchability, spaceWidth/3*stretchability, true);
        Glue thinSpaceGlue = new Glue(spaceWidth/2, spaceWidth/4*stretchability, spaceWidth/6*stretchability, true);

        for (int i = 0; i < text.length(); ) {
            // Pick out the code point at this location. Could take two chars.
            int ch = text.codePointAt(i);

            // Advance to the next code point.
            i += Character.charCount(ch);

            if (ch == ' ' && mAllowLineBreaks) {
                elements.add(spaceGlue);
            } else if (ch == '\u00A0' && mAllowLineBreaks) {
                // Non-break space. Precede with infinite penalty.
                elements.add(new Penalty(Penalty.INFINITY));
                elements.add(spaceGlue);
            } else if (ch == '\u202F' && mAllowLineBreaks) {
                // Thin non-break space. Precede with infinite penalty.
                elements.add(new Penalty(Penalty.INFINITY));
                elements.add(thinSpaceGlue);
            } else {
                StringBuilder word = new StringBuilder();
                word.appendCodePoint(ch);

                // See what kind of word or non-word we just ran into.
                boolean isWord = isWordCharacter(ch);

                // Look forward and grab all the letters of the word (or not word).
                while (i < text.length()) {
                    ch = text.codePointAt(i);
                    if (isWord != isWordCharacter(ch) || ((ch == ' ' || ch == '\u00A0' || ch == '\u202F') && mAllowLineBreaks)) {
                        break;
                    }

                    i += Character.charCount(ch);
                    word.appendCodePoint(ch);
                }

                // Add the whole word at once.
                elements.add(new Text(word.toString(), font));
            }
        }

        return elements;
    }

    /**
     * Return a modified copy of the element list with the words hyphenated, meaning that discretionary
     * breaks have been inserted. Any word to be hyphenated must be within a single Text element.
     */
    private static List<Element> hyphenate(List<Element> elements, HyphenDictionary hyphenDictionary) {
        List<Element> newElements = new ArrayList<>();

        for (Element element : elements) {
            if (element instanceof Text) {
                Text text = (Text) element;
                String word = text.getText();

                if (word.length() > 0 && isWordCharacter(word.codePointAt(0))) {
                    List<String> syllables = hyphenDictionary.hyphenate(word);

                    for (int i = 0; i < syllables.size(); i++) {
                        String syllable = syllables.get(i);

                        // Add syllable.
                        newElements.add(new Text(syllable, text.getFont()));

                        // Add discretionary hyphen.
                        if (i < syllables.size() - 1) {
                            String preBreak;
                            if (syllable.endsWith("-")) {
                                // Hyphen already exists in word.
                                preBreak = "";
                            } else {
                                // Add implicit hyphen.
                                preBreak = "-";
                            }
                            newElements.add(new Discretionary(
                                    HBox.makeOnlyString(preBreak, text.getFont()),
                                    HBox.makeOnlyString("", text.getFont()),
                                    HBox.makeOnlyString("", text.getFont()),
                                    Discretionary.HYPHEN_PENALTY));

                        }
                    }
                } else {
                    // Not a word, leave it as-is.
                    newElements.add(element);
                }
            } else {
                // Just add any other type.
                newElements.add(element);
            }
        }

        return newElements;
    }

    /**
     * Whether the character can be part of a hyphenated word.
     */
    public static boolean isWordCharacter(int ch) {
        // Both kinds of apostrophes.
        return Character.isLetter(ch) || ch == '-' || ch == '\'' || ch == '’';
    }

    /**
     * Return a new list of elements with ligatures converted to their one-character form.
     */
    static List<Element> transformLigatures(List<Element> elements, SizedFont font) {
        // If it weren't for hyphenation, we'd just go through the elements and substitute the
        // ligatures in the Text elements. But a discretionary break can cut in the middle of
        // a ligature, such as in the word "dif-fi-cult", cutting the "ffi" ligature.
        //
        // So our plan is to:
        //
        // 1. Find patterns of Text/Discretionary/Text elements.
        // 2. Reconstruct the entire pre-break, post-break, and no-break text.
        // 3. Transform ligatures in each.
        // 4. Find common prefixes and suffixes.
        // 5. Reconstruct the Text/Discretionary/Text elements based on that.
        //
        // For example:
        //
        //   Original text: difficult
        //   Hyphenated:    dif-fi-cult
        //   As elements:   Text(dif)Discretionary(-,,)Text(fi)Discretionary(-,,)Text(cult)
        //   With "fi":     Text(dif)Discretionary(-,,)Text(`fi`)Discretionary(-,,)Text(cult)
        //   With "ffi":    Text(di)Discretionary(f-,`fi`,`ffi`)Discretionary(-,,)Text(cult)
        //
        // (The text in `backticks` above are ligature characters.

        // First, make a copy of the list. Use a linked list because we'll be popping and pushing
        // things on the front.
        Deque<Element> oldElements = new LinkedList<>(elements);

        // Clear this so we don't accidentally access it.
        elements = null;

        // Our new elements.
        List<Element> newElements = new ArrayList<>();

        // Go through each element.
        while (!oldElements.isEmpty()) {
            Element element = oldElements.removeFirst();
            if (element instanceof Text || element instanceof Discretionary) {
                // Look for a Text/Discretionary/Text pattern.
                Text beforeText;
                Discretionary discretionary;
                Text afterText;

                if (element instanceof Text) {
                    beforeText = (Text) element;
                    discretionary = (Discretionary) (oldElements.peekFirst() instanceof Discretionary ?
                            oldElements.removeFirst() : null);
                } else {
                    beforeText = null;
                    discretionary = (Discretionary) element;
                }
                afterText = (Text) (discretionary != null && oldElements.peekFirst() instanceof Text ?
                        oldElements.removeFirst() : null);

                // We now have one of the following cases:
                //
                //    text / null / null
                //    text / discretionary / null
                //    text / discretionary / text
                //    null / discretionary / null
                //    null / discretionary / text
                //
                // Sanity check.
                if (beforeText != null && afterText != null && !beforeText.getFont().equals(afterText.getFont())) {
                    throw new IllegalStateException("before and after text fonts don't match");
                }

                if (DEBUG_HYPHENATION) {
                    System.out.printf("%s, %s, %s%n", beforeText, discretionary, afterText);
                }

                // Generate the full prebreak, postbreak, and nobreak strings.
                String entirePreBreak = (beforeText == null ? "" : beforeText.getText()) +
                        (discretionary == null ? "" : discretionary.getPreBreak().getOnlyString());
                String entirePostBreak = (discretionary == null ? "" : discretionary.getPostBreak().getOnlyString()) +
                        (afterText == null ? "" : afterText.getText());
                String entireNoBreak = (beforeText == null ? "" : beforeText.getText()) +
                        (discretionary == null ? "" : discretionary.getNoBreak().getOnlyString()) +
                        (afterText == null ? "" : afterText.getText());

                // Substitute ligatures in all three.
                entirePreBreak = font.transformLigatures(entirePreBreak);
                entirePostBreak = font.transformLigatures(entirePostBreak);
                entireNoBreak = font.transformLigatures(entireNoBreak);

                // Find longest common prefix and suffix.
                String commonPrefix = Strings.commonPrefix(entirePreBreak, entireNoBreak);
                String commonSuffix = Strings.commonSuffix(entirePostBreak, entireNoBreak);

                // Find what's left, to put in the discretionary.
                if (DEBUG_HYPHENATION) {
                    System.out.printf("<%s>  <%s>  <%s>  <%s>  <%s>%n",
                            entirePreBreak, entirePostBreak, entireNoBreak, commonPrefix, commonSuffix);
                }
                String preBreak = entirePreBreak.substring(commonPrefix.length());
                String postBreak = entirePostBreak.substring(0, entirePostBreak.length() - commonSuffix.length());
                String noBreak = entireNoBreak.substring(commonPrefix.length(),
                        entireNoBreak.length() - commonSuffix.length());

                // Replace the Text elements.
                beforeText = commonPrefix.isEmpty() ? null : new Text(commonPrefix, font);
                discretionary = discretionary == null ? null : new Discretionary(
                        HBox.makeOnlyString(preBreak, font),
                        HBox.makeOnlyString(postBreak, font),
                        HBox.makeOnlyString(noBreak, font),
                        discretionary.getPenalty());
                afterText = commonSuffix.isEmpty() ? null : new Text(commonSuffix, font);

                // Add the elements to the output.
                if (beforeText != null) {
                    newElements.add(beforeText);
                }
                if (discretionary != null) {
                    newElements.add(discretionary);
                }

                // The afterText must be processed again, potentially with the next discretionary, so put
                // it back in the input. Note that this means that its string will have the ligatures
                // substituted twice. This is fine.
                if (afterText != null) {
                    oldElements.addFirst(afterText);
                }
            } else {
                // Not text or discretionary, leave it as-is.
                newElements.add(element);
            }
        }

        return newElements;
    }

    /**
     * Return a new list of elements with kerning added.
     */
    static List<Element> addKerning(List<Element> origElements, SizedFont font) {
        // Make a new list of elements.
        List<Element> newElements = new ArrayList<>(origElements.size());

        addKerningToList(origElements, newElements, 0, font);

        return newElements;
    }

    /**
     * Adds the original elements to the new list, with the given previous element.
     *
     * @return the new previous element.
     */
    static int addKerningToList(List<Element> origElements, List<Element> newElements, int previousCh, SizedFont font) {
        // Go through each element, keeping track of the previous character across them.
        for (int e = 0; e < origElements.size(); e++) {
            Element element = origElements.get(e);

            if (element instanceof Text) {
                // Go through the text one character at a time, to see if any pair requires kerning.
                Text text = (Text) element;
                String s = text.getText();
                for (int i = 0; i < s.length(); ) {
                    int ch = s.codePointAt(i);

                    // See if we need to kern.
                    long kerning = font.getKerning(previousCh, ch);
                    if (kerning != 0) {
                        // String before the kern.
                        if (i > 0) {
                            newElements.add(new Text(s.substring(0, i), font));
                            // Reset the string.
                            s = s.substring(i);
                            i = 0;
                        }
                        newElements.add(new Kern(kerning, true));
                    }
                    i += Character.charCount(ch);
                    previousCh = ch;
                }
                if (!s.isEmpty()) {
                    newElements.add(new Text(s, font));
                }
            } else if (element instanceof Glue) {
                // See if it's a space. Guess by looking at the width.
                if (((Glue) element).getSize() > 0) {
                    previousCh = ' ';
                }
                newElements.add(element);
            } else if (element instanceof Discretionary) {
                Discretionary discretionary = (Discretionary) element;
                HBox preBreak = discretionary.getPreBreak();
                HBox postBreak = discretionary.getPostBreak();
                HBox noBreak = discretionary.getNoBreak();

                // Recurse on the three sections, sending in the appropriate previous character and capturing
                // the next characters.
                List<Element> preBreakElements = new ArrayList<>();
                addKerningToList(preBreak.getElements(), preBreakElements, previousCh, font);
                List<Element> postBreakElements = new ArrayList<>();
                int postBreakCh = addKerningToList(postBreak.getElements(), postBreakElements, 0, font);
                List<Element> noBreakElements = new ArrayList<>();
                int noBreakCh = addKerningToList(noBreak.getElements(), noBreakElements, previousCh, font);

                if (postBreakCh != noBreakCh) {
                    // This is actually the most likely scenario, because it happens with simple discretionary
                    // hyphens. This is difficult to handle because the post break is empty (its previous
                    // character is not defined) and the no break is empty (its previous character is the one from
                    // before the discretionary). We're forced to put the kerning into the discretionary, in the
                    // no-break, and to do that we have to peek ahead.

                    // We'll peek ahead. We only handle the straightforward case of a Text node. If it's
                    // anything else, we throw.
                    boolean success = false;
                    Element peekElement = e + 1 < origElements.size() ? origElements.get(e + 1) : null;
                    if (peekElement instanceof Text) {
                        String s = ((Text) peekElement).getText();
                        int nextCh = s.isEmpty() ? 0 : s.codePointAt(0);
                        if (nextCh != 0) {
                            // See how our segments would kern with the next character.
                            long kerning = font.getKerning(postBreakCh, nextCh);
                            if (kerning != 0) {
                                postBreakElements.add(new Kern(kerning, true));
                            }
                            kerning = font.getKerning(noBreakCh, nextCh);
                            if (kerning != 0) {
                                noBreakElements.add(new Kern(kerning, true));
                            }
                            success = true;
                        }
                    }
                    if (success) {
                        // Disable kerning with the next character, since we've already done it here.
                        previousCh = 0;
                    } else {
                        // If this ever throws, then we weren't able to determine the next character. Don't panic.
                        // The easiest and probably safest is to just not throw here, and not kern properly.
                        // Poke around to see if we might be in a situation where kerning would be important.
                        // If not, skip it. Set previousCh to noBreakCh, since that's the most likely case.
                        if (false) {
                            Element.println(origElements, System.out, "");
                            throw new IllegalStateException("cannot resolve postBreakCh " + postBreakCh +
                                    " and noBreakCh " + noBreakCh);
                        } else {
                            // We found this case: Text(ra)Discretionary(f-,-ﬁ,ﬃ)Discretionary(-,,)Text(né).
                            // It was a discretionary with two ligatures, followed by a normal hyphen discretionary.
                            // Our model isn't powerful enough to completely represent all the various possible
                            // combinations. Punt, though in principle we could try a little harder to handle this
                            // (for example, unpack the ligatures and notice that the last characters are "i", and
                            // assume that the "i" kern will be consistent with the ligature ones.)

                            // Set previousCh to noBreakCh, since that's the most likely case.
                            previousCh = noBreakCh;
                        }
                    }
                } else {
                    // Set it to either postBreakCh or noBreakCh, they're equal.
                    previousCh = postBreakCh;
                }

                // Replace discretionary with new lists.
                element = new Discretionary(new HBox(preBreakElements), new HBox(postBreakElements),
                        new HBox(noBreakElements), discretionary.getPenalty());
                newElements.add(element);
            } else if (element instanceof Kern) {
                // We shouldn't have kerned already. In principle the user could insert these, so maybe?
                throw new IllegalArgumentException("there should not be Kern elements already in list");
            } else if (element instanceof Rule || element instanceof VBox) {
                // Reset the previous character if the element has any width. We might have a Rule of zero
                // width (like a strut in TeX), and that shouldn't affect kerning.
                if (element.getWidth() > 0) {
                    previousCh = 0;
                }
                newElements.add(element);
            } else if (element instanceof HBox) {
                // Perhaps we should recurse into the HBox here. Deal with it if it comes up.
                throw new IllegalArgumentException("can't yet handle HBox when kerning");
            } else if (element instanceof Penalty) {
                // Let through.
                newElements.add(element);
            } else {
                throw new IllegalArgumentException("unknown element type when kerning: " + element.getClass());
            }
        }

        return previousCh;
    }

    /**
     * Adds the necessary glue and penalty to end a paragraph.
     */
    public void addEndOfParagraph() {
        // Add a forced break at the end of the paragraph.

        // First prevent a break before the infinite glue, or we'd be left with a line with just the penalty afterward.
        addElement(new Penalty(Penalty.INFINITY));

        // Then add infinite glue to finish out the last line.
        addElement(new Glue(0, PT.toSp(1), true, 0, false, true));

        // And force a break.
        addElement(new Penalty(-Penalty.INFINITY));
    }
}
