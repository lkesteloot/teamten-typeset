package com.teamten.font;

import com.teamten.util.CodePoints;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Font that has a primary font and a fallback font. All operations
 * forward to the primary font, except characters and parts of
 * strings that aren't handled by the primary font, which are
 * forwarded to the fallback font. This is useful for things
 * like Hebrew that might not be handled by our preferred font.
 */
public class FailoverFont extends AbstractFont {
    private @NotNull Font mPrimaryFont;
    private @NotNull Font mFallbackFont;

    public FailoverFont(@NotNull Font primaryFont, @NotNull Font fallbackFont) {
        mPrimaryFont = primaryFont;
        mFallbackFont = fallbackFont;
    }

    @Override
    public boolean hasCharacter(int ch) {
        return mPrimaryFont.hasCharacter(ch) || mFallbackFont.hasCharacter(ch);
    }

    @Override
    public long getSpaceWidth() {
        // Always use primary font's space.
        return mPrimaryFont.getSpaceWidth();
    }

    @Override
    public Metrics getCharacterMetrics(int ch, double fontSize) {
        return mPrimaryFont.hasCharacter(ch)
                ? mPrimaryFont.getCharacterMetrics(ch, fontSize)
                : mFallbackFont.getCharacterMetrics(ch, fontSize);
    }

    @Override
    public void draw(String text, double fontSize, long x, long y, PDPageContentStream contents) throws IOException {
        // Draw one character at a time.
        int i = 0;
        while (i < text.length()) {
            int ch = text.codePointAt(i);

            // Draw the individual character.
            Font font = mPrimaryFont.hasCharacter(ch) ? mPrimaryFont
                    : mFallbackFont.hasCharacter(ch) ? mFallbackFont
                    : null;
            if (font == null) {
                throw new IOException(String.format(
                        "neither primary nor fallback font can handle %c (U+%04x)", (char) ch, ch));
            }
            font.draw(CodePoints.toString(ch), fontSize, x, y, contents);
            x += font.getCharacterMetrics(ch, fontSize).getWidth();

            // Next code point.
            i += Character.charCount(ch);
        }
    }
}
