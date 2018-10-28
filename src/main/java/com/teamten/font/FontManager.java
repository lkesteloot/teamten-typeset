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

package com.teamten.font;

import com.teamten.typeset.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads and manages fonts.
 */
public class FontManager {
    private final Config mConfig;
    private final Function<TypefaceVariant,Font> mFontLoader;
    private final Map<TypefaceVariant,Font> mFontCache = new HashMap<>();

    /**
     * A font manager that creates new fonts from the specified font loader. The font loader should
     * throw an IllegalArgumentException if the font cannot be loaded.
     */
    public FontManager(Config config, Function<TypefaceVariant,Font> fontLoader) {
        mConfig = config;
        mFontLoader = fontLoader;
    }

    /**
     * Fetches a font. The first time this is called for a particular font, the font is loaded.
     *
     * @throws IllegalArgumentException if the font cannot be loaded.
     */
    public Font get(TypefaceVariant typefaceVariant) {
        synchronized (mFontCache) {
            Font font = mFontCache.get(typefaceVariant);
            if (font == null) {
                font = mFontLoader.apply(typefaceVariant);
                mFontCache.put(typefaceVariant, font);
            }

            return font;
        }
    }

    /**
     * Fetches a font, returning the font and size together.
     *
     * @throws IllegalArgumentException if the font cannot be loaded.
     */
    public SizedFont get(TypefaceVariantSize typefaceVariantSize) {
        Font font = get((TypefaceVariant) typefaceVariantSize);

        // See if we should make it a failover font.
        Typeface fallbackTypeface = mConfig.getTypeface(Config.Key.FALLBACK_TYPEFACE);
        if (fallbackTypeface != null) {
            Font fallbackFont = get(typefaceVariantSize.withTypeface(fallbackTypeface));
            font = new FailoverFont(font, fallbackFont);
        }

        return new SizedFont(font, typefaceVariantSize.getSize());
    }

    /**
     * Utility method that calls {@link #get(TypefaceVariant)} with a new {@link TypefaceVariant} object
     * created from the two parameters.
     */
    public Font get(Typeface typeface, FontVariant fontVariant) {
        return get(new TypefaceVariant(typeface, fontVariant));
    }

    /**
     * Utility method that calls {@link #get(TypefaceVariantSize)} with a new {@link TypefaceVariant} object
     * created from the two parameters.
     */
    public SizedFont get(Typeface typeface, FontVariant fontVariant, double fontSize) {
        return get(new TypefaceVariantSize(typeface, fontVariant, fontSize));
    }
}
