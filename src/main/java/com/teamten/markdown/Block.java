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

package com.teamten.markdown;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A block is a paragraph (similar to a block in HTML DOM). It's a sequence of spans.
 */
public class Block {
    private final @NotNull BlockType mBlockType;
    private final int mLineNumber;
    private final int mCounter;
    private final boolean mInBlockQuote;
    private final @NotNull List<Span> mSpans = new ArrayList<>();

    public Block(@NotNull BlockType blockType, int lineNumber, int counter, boolean inBlockQuote) {
        mBlockType = blockType;
        mLineNumber = lineNumber;
        mCounter = counter;
        mInBlockQuote = inBlockQuote;
    }

    public BlockType getBlockType() {
        return mBlockType;
    }

    /**
     * Get line in source where this block started.
     */
    public int getLineNumber() {
        return mLineNumber;
    }

    /**
     * Get the counter for numbered lists.
     */
    public int getCounter() {
        return mCounter;
    }

    /**
     * Whether this block is inside a blockquote (indented, etc.).
     */
    public boolean isInBlockQuote() {
        return mInBlockQuote;
    }

    public List<Span> getSpans() {
        return mSpans;
    }

    /**
     * Return a string version of all text spans. Ignores non-text spans.
     */
    public String getText() {
        return mSpans.stream()
                .filter((span) -> span instanceof TextSpan)
                .map((span) -> (TextSpan) span)
                .map(TextSpan::getText)
                .collect(Collectors.joining());
    }

    public void addSpan(Span span) {
        mSpans.add(span);
    }

    /**
     * Return a copy of this block with a different block type.
     */
    public Block withBlockType(BlockType blockType) {
        Block newBlock = new Block(blockType, mLineNumber, mCounter, mInBlockQuote);
        newBlock.mSpans.addAll(mSpans);
        return newBlock;
    }

    /**
     * Convert apostrophes, quotes, etc.
     *
     * @param locale the locale being processed ("en_US", "fr", etc.).
     */
    public void postProcessText(String locale) {
        // Some rules are for French only.
        String language = locale.substring(0, Math.min(2, locale.length()));
        boolean isFrench = language.equalsIgnoreCase("fr");

        if (getBlockType() != BlockType.CODE) {
            boolean insideQuotation = false;
            int previousCh = -1;

            for (int i = 0; i < mSpans.size(); i++) {
                Span span = mSpans.get(i);
                span.postProcessText(locale);

                // Can't put this into the TextSpan class because we keep track of
                // the previous character across text spans.
                if (span instanceof TextSpan) {
                    TextSpan textSpan = (TextSpan) span;
                    StringBuilder builder = new StringBuilder();
                    String text = textSpan.getText();

                    for (int j = 0; j < text.length(); ) {
                        int ch = text.codePointAt(j);

                        // Simple character translations.
                        if (ch == '~') {
                            // No-break space.
                            builder.append('\u00A0');
                        } else if (ch == '\'') {
                            builder.append('’');
                        } else if (ch == '"') {
                            if (isFrench) {
                                builder.append(insideQuotation ? "\u00A0»" : "«\u00A0");
                            } else {
                                builder.append(insideQuotation ? '”' : '“');
                            }
                            insideQuotation = !insideQuotation;
                        } else if (isFrench && ch == '-' && i == 0 && j == 0 && text.length() >= 2 && text.codePointAt(j + 1) == ' ') {
                            // Em-dash for start of dialog.
                            builder.appendCodePoint('—');

                            // Skip space.
                            j++;
                        } else if (ch == '.' && j + 2 < text.length() && text.codePointAt(j + 1) == '.' &&
                                text.codePointAt(j + 2) == '.') {
                            // Ellipsis.
                            builder.append("\u00A0.\u00A0.\u00A0.");

                            // Skip dots.
                            j += 2;
                        } else if (isFrench && (ch == ':' || ch == ';' || ch == '!' || ch == '?')) {
                            // In French there's a space before two-part punctuation.
                            if (previousCh == '.') {
                                // After a period use a full width space (it's probably after ellipsis).
                                builder.append('\u00A0');
                            } else {
                                // Otherwise use thin non-break space.
                                builder.append('\u202F');
                            }
                            builder.appendCodePoint(ch);
                        } else {
                            builder.appendCodePoint(ch);
                        }

                        previousCh = ch;
                        j += Character.charCount(ch);
                    }

                    mSpans.set(i, new TextSpan(builder.toString(), textSpan.getFlags()));
                }
            }

            if (insideQuotation) {
                System.out.println("Warning (line " + mLineNumber + "): Block ends without closing quotation: " + this);
            }
        }
    }

    /**
     * Return just the text of the block.
     */
    public String toBriefString() {
        // Join the individual spans.
        String s = mSpans.stream()
                .map(Span::toString)
                .collect(Collectors.joining());

        // Prefix counter if it's a numbered list.
        if (mBlockType == BlockType.NUMBERED_LIST) {
            s = mCounter + ". " + s;
        }

        return s;
    }

    @Override // Object
    public String toString() {
        if (mSpans.isEmpty()) {
            return "No spans";
        } else {
            String first = getText();
            first = first.substring(0, Math.min(30, first.length()));

            return String.format("%s, %d spans, starting with: %s ...",
                    mBlockType, mSpans.size(), first);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Block block = (Block) o;

        if (mCounter != block.mCounter) {
            return false;
        }
        if (mBlockType != block.mBlockType) {
            return false;
        }
        return mSpans.equals(block.mSpans);
    }

    @Override
    public int hashCode() {
        int result = mBlockType.hashCode();
        result = 31*result + mCounter;
        result = 31*result + mSpans.hashCode();
        return result;
    }

    /**
     * Make a builder for numbered lists.
     */
    public static Builder numberedListBuilder(int lineNumber, int counter, boolean inBlockQuote) {
        return new Builder(BlockType.NUMBERED_LIST, lineNumber, counter, inBlockQuote);
    }

    /**
     * Make a plain body block from a string.
     */
    public static Block bodyBlock(String text, boolean inBlockQuote) {
        Span span = new TextSpan(text, FontVariantFlags.PLAIN);
        return new Builder(BlockType.BODY, 0, inBlockQuote).addSpan(span).build();
    }

    /**
     * Builds a Block one character at a time.
     */
    public static class Builder {
        private final Block mBlock;
        private final StringBuilder mStringBuilder = new StringBuilder();
        private FontVariantFlags mFlags = FontVariantFlags.PLAIN;

        private Builder(BlockType blockType, int lineNumber, int counter, boolean inBlockQuote) {
            mBlock = new Block(blockType, lineNumber, counter, inBlockQuote);
        }

        public Builder(BlockType blockType, int lineNumber, boolean inBlockQuote) {
            this(blockType, lineNumber, 0, inBlockQuote);
        }

        public BlockType getBlockType() {
            return mBlock.getBlockType();
        }

        /**
         * Add the character to the block.
         *
         * @param ch the character to add.
         * @param flags whether the character should be displayed in bold, italics, etc.
         */
        public void addText(char ch, FontVariantFlags flags) {
            // Switch style if necessary.
            if (!flags.equals(mFlags)) {
                emitSpan();
                mFlags = flags;
            }

            mStringBuilder.append(ch);
        }

        /**
         * Add a string. See {@link #addText(char, FontVariantFlags)} for details.
         */
        public void addText(String text, FontVariantFlags flags) {
            for (char ch : text.toCharArray()) {
                addText(ch, flags);
            }
        }

        /**
         * Add a plain string (no markup).
         */
        public void addText(String text) {
            addText(text, FontVariantFlags.PLAIN);
        }

        /**
         * Add any span to this block.
         */
        public Builder addSpan(Span span) {
            emitSpan();
            mBlock.addSpan(span);

            return this;
        }

        /**
         * Add a whole block to this block. The block's type and counter are ignored.
         */
        public Builder addBlock(Block block) {
            // Add each span.
            block.getSpans().forEach(this::addSpan);
            return this;
        }

        /**
         * Returns whether any characters have been added so far.
         */
        public boolean isEmpty() {
            return mBlock.getSpans().isEmpty() && mStringBuilder.length() == 0;
        }

        /**
         * Builds the block and returns it. Do not call this more than once for a given builder.
         */
        public Block build() {
            emitSpan();

            // Note that we can't warn if the block is built while inside bold or italic, because we
            // can't be sure that those weren't closed. If the last character of a block is the
            // character to stop bold, we'll never know about it because we won't get another
            // character that's not bold.

            return mBlock;
        }

        /**
         * Possibly emit span, if we have characters accumulated up.
         */
        private void emitSpan() {
            if (mStringBuilder.length() > 0) {
                TextSpan span = new TextSpan(mStringBuilder.toString(), mFlags);
                mStringBuilder.setLength(0);
                mBlock.addSpan(span);
            }
        }
    }
}
