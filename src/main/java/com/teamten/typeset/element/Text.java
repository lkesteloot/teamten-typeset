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

package com.teamten.typeset.element;

import com.google.common.math.DoubleMath;
import com.teamten.font.SizedFont;
import com.teamten.util.CodePoints;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

/**
 * A sequence of characters.
 */
public class Text extends Box {
    private final @NotNull SizedFont mFont;
    private final @NotNull String mText;
    private boolean mContainsRightToLeftText;
    private boolean mContainsRightToLeftTextIsSet = false;

    /**
     * Constructor for string of any size.
     */
    public Text(@NotNull SizedFont font, @NotNull String text, long width, long height, long depth) {
        super(width, height, depth);
        mFont = font;
        mText = text;
    }

    /**
     * Constructor for a string.
     */
    public Text(@NotNull String text, @NotNull SizedFont font) {
        super(font.getStringMetrics(text));
        mFont = font;
        mText = text;
    }

    /**
     * Constructor for single character.
     */
    public Text(int ch, @NotNull SizedFont font) {
        super(font.getCharacterMetrics(ch));
        mFont = font;
        mText = CodePoints.toString(ch);
    }

    /**
     * The text that this element was constructed with.
     */
    @NotNull
    public String getText() {
        return mText;
    }

    /**
     * The font the text should be displayed in.
     */
    @NotNull
    public SizedFont getFont() {
        return mFont;
    }

    /**
     * Whether this text can be appended to the other text.
     */
    public boolean isCompatibleWith(Text other) {
        return mFont.getFont() == other.mFont.getFont() &&
                DoubleMath.fuzzyEquals(mFont.getSize(), other.mFont.getSize(), 0.001);
    }

    /**
     * Whether any character in the text is right-to-left.
     */
    public boolean containsRightToLeftText() {
        if (!mContainsRightToLeftTextIsSet) {
            // Cache value.
            mContainsRightToLeftText = false;

            for (int i = 0; i < mText.length(); ) {
                // Pick out the code point at this location. Could take two chars.
                int ch = mText.codePointAt(i);

                // Advance to the next code point.
                i += Character.charCount(ch);

                // See if this character is right-to-left.
                if (CharacterDirection.forChar(ch) == CharacterDirection.RIGHT_TO_LEFT) {
                    mContainsRightToLeftText = true;
                    break;
                }
            }

            mContainsRightToLeftTextIsSet = true;
        }

        return mContainsRightToLeftText;
    }

    /**
     * Return the character direction of the Text. The Text must contain
     * only a single direction throughout.
     *
     * @throws IllegalStateException if the Text contains more than one direction.
     */
    public CharacterDirection getCharacterDirection() {
        CharacterDirection characterDirection = null;

        for (int i = 0; i < mText.length(); ) {
            // Pick out the code point at this location. Could take two chars.
            int ch = mText.codePointAt(i);

            // Advance to the next code point.
            i += Character.charCount(ch);

            CharacterDirection newDirection = CharacterDirection.forChar(ch);
            if (characterDirection == null) {
                characterDirection = newDirection;
            } else {
                if (newDirection != characterDirection) {
                    throw new IllegalStateException("inconsistent direction in Text");
                }
            }
        }

        return characterDirection;
    }

    /**
     * Add one Text object for each character in this object.
     */
    public void breakUpInto(Collection<Element> list) {
        for (int i = 0; i < mText.length(); ) {
            // Pick out the code point at this location. Could take two chars.
            int ch = mText.codePointAt(i);

            // Advance to the next code point.
            i += Character.charCount(ch);

            list.add(new Text(ch, mFont));
        }
    }

    /**
     * Returns a new Text object, the text of which is the concatenation of this text and
     * the other text.
     *
     * @throws IllegalArgumentException if the two text objects are not compatible.
     */
    public Text appendedWith(Text other) {
        if (!isCompatibleWith(other)) {
            throw new IllegalArgumentException("incompatible text, cannot append");
        }

        return new Text(mText + other.mText, mFont);
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        /// drawDebugRectangle(contents, x, y);

        mFont.draw(mText, x, y, contents);

        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Text must always be in an HBox.
        throw new IllegalStateException("text should be not laid out vertically");
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.print(indent);
        stream.println(toString());
    }

    @Override
    public String toString() {
        return String.format("Text %s: “%s” in %s", getDimensionString(), mText, mFont);
    }

    @Override
    public String toTextString() {
        return mText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Text text = (Text) o;

        if (!mFont.equals(text.mFont)) {
            return false;
        }
        return mText.equals(text.mText);

    }

    @Override
    public int hashCode() {
        int result = mFont.hashCode();
        result = 31*result + mText.hashCode();
        return result;
    }

    /**
     * Encodes a character direction according to the Unicode standard.
     */
    public enum CharacterDirection {
        LEFT_TO_RIGHT, NEUTRAL, RIGHT_TO_LEFT;

        /**
         * Return the direction for the character.
         */
        public static CharacterDirection forChar(int ch) {
            switch (Character.getDirectionality(ch)) {
                case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
                case Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING:
                case Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE:
                    return LEFT_TO_RIGHT;

                case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE:
                    return RIGHT_TO_LEFT;

                default:
                    // Punctuation, etc.
                    return NEUTRAL;
            }
        }
    }
}
