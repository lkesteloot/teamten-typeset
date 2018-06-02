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

package com.teamten.hyphen;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A TeX-style hyphen dictionary.
 */
public class HyphenDictionary {
    private static final Splitter FIELD_SPLITTER = Splitter.on(" ").
            omitEmptyStrings().trimResults();
    private static final Joiner HYPHEN_JOINER = Joiner.on("-").skipNulls();
    private static final Pattern DIGITS_RE = Pattern.compile("[0-9]");
    // Descent defaults.
    private int mLeftHyphenMin = 2;
    private int mRightHyphenMin = 3;
    private int mCompoundLeftHyphenMin = 2;
    private int mCompoundRightHyphenMin = 3;
    private Map<String,String> mFragmentMap = new HashMap<>();
    private Map<String,String> mFragmentMapDebug = new HashMap<>();

    HyphenDictionary() {
        // Use factories.
    }

    /**
     * Reads a .dic file from a resource. Use e.g., "fr" for language.
     *
     * @throws FileNotFoundException if the file can't be found.
     * @throws IOException if the file can't be loaded.
     */
    public static HyphenDictionary fromResource(String language) throws IOException {
        String filename = "hyph_" + language + ".dic";

        // In the same resource directory as this class.
        InputStream inputStream = HyphenDictionary.class.getResourceAsStream(filename);
        if (inputStream == null) {
            throw new FileNotFoundException("no hyphenation dictionary for language \"" + language + "\"");
        }

        return fromInputStream(inputStream);
    }

    /**
     * Reads a .dic file from a UTF-8 input stream.
     */
    private static HyphenDictionary fromInputStream(InputStream inputStream) throws IOException {
        HyphenDictionary dic = new HyphenDictionary();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        dic.read(reader);
        reader.close();
        return dic;
    }

    /**
     * Read the dictionary from the reader.
     */
    private void read(BufferedReader reader) throws IOException {
        boolean started = false;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("%") || line.isEmpty()) {
                // Comment
            } else {
                if (started) {
                    // Body.
                    addPattern(line);
                } else {
                    // Header.
                    List<String> fields = FIELD_SPLITTER.splitToList(line);
                    if (!fields.isEmpty()) {
                        switch (fields.get(0)) {
                            case "LEFTHYPHENMIN":
                                setLeftHyphenMin(Integer.parseInt(fields.get(1)));
                                break;

                            case "RIGHTHYPHENMIN":
                                setRightHyphenMin(Integer.parseInt(fields.get(1)));
                                break;

                            case "COMPOUNDLEFTHYPHENMIN":
                                setCompoundLeftHyphenMin(Integer.parseInt(fields.get(1)));
                                break;

                            case "COMPOUNDRIGHTHYPHENMIN":
                                setCompoundRightHyphenMin(Integer.parseInt(fields.get(1)));
                                break;

                            case "UTF-8":
                                // Good.
                                break;

                            case "NEXTLEVEL":
                                started = true;
                                break;

                            default:
                                throw new IOException("Invalid hyphen dictionary header: " + fields.get(0));
                        }
                    }
                }
            }
        }
    }

    /**
     * Add a TeX pattern to the map.
     */
    void addPattern(String pattern) {
        String key = removeDigits(pattern);
        String value = removeNonDigits(pattern);
        mFragmentMap.put(key, value);
        if (mFragmentMapDebug != null) {
            mFragmentMapDebug.put(key, pattern);
        }
    }

    /**
     * Minimum number of letters in first fragment.
     */
    void setLeftHyphenMin(int leftHyphenMin) {
        mLeftHyphenMin = leftHyphenMin;
    }

    /**
     * Minimum number of letters in last fragment.
     */
    void setRightHyphenMin(int rightHyphenMin) {
        mRightHyphenMin = rightHyphenMin;
    }

    /**
     * I don't know what this should affect.
     */
    void setCompoundLeftHyphenMin(int compoundLeftHyphenMin) {
        mCompoundLeftHyphenMin = compoundLeftHyphenMin;
    }

    /**
     * I don't know what this should affect.
     */
    void setCompoundRightHyphenMin(int compoundRightHyphenMin) {
        mCompoundRightHyphenMin = compoundRightHyphenMin;
    }

    /**
     * Hyphenate the specified word, returning a list of word fragments between
     * which hyphenation can happen. Note that if the word already contains a hyphen,
     * it will show up at the end of one of the fragments. Don't add another hyphen
     * after that.
     */
    public List<String> hyphenate(String word) {
        // Make a sequence of possible cut points.
        char[] cutPoints = new char[word.length() + 1];
        for (int i = 0; i < cutPoints.length; i++) {
            cutPoints[i] = '0';
        }

        // Add fake periods to represent begin and end.
        word = "." + word + ".";

        // Find all sub-sequences.
        for (int seqLength = 1; seqLength <= word.length(); seqLength++) {
            for (int start = 0; start <= word.length() - seqLength; start++) {
                String seq = word.substring(start, start + seqLength);
                String value = mFragmentMap.get(seq.toLowerCase());
                if (value != null) {
                    /// System.out.printf("%s: %s %s %s %d %d%n", word, seq, value, mFragmentMapDebug.get(seq.toLowerCase()), start, seqLength);

                    // At the beginning of the word we don't count the period.
                    int offset = seq.startsWith(".") ? 0 : -1;

                    // Find the max of the new hints and the existing ones.
                    for (int i = 0; i < value.length(); i++) {
                        char c = value.charAt(i);
                        if (c > cutPoints[start + i + offset]) {
                            cutPoints[start + i + offset] = c;
                        }
                    }
                }
            }
        }

        /// System.out.printf("%s: %s%n", word, new String(cutPoints));

        // Prevent hyphenation at start and end of word.
        for (int i = 0; i < mLeftHyphenMin && i < cutPoints.length; i++) {
            cutPoints[i] = 0;
        }
        for (int i = 0; i < mRightHyphenMin && i < cutPoints.length; i++) {
            cutPoints[cutPoints.length - 1 - i] = 0;
        }

        // Remove fake periods.
        word = word.substring(1, word.length() - 1);

        // Find odd numbers and splice there.
        List<String> segments = new ArrayList<>();
        int lastStart = 0;
        for (int i = 0; i < cutPoints.length; i++) {
            if (cutPoints[i]%2 != 0) {
                segments.add(word.substring(lastStart, i));
                lastStart = i;
            }
        }
        if (lastStart < word.length()) {
            segments.add(word.substring(lastStart));
        }

        // Fix up single hyphens.
        segments = mergeSingleHyphens(segments);

        // Fix up hyphens on the wrong segment.
        segments = moveHyphenPrefixes(segments);

        return segments;
    }

    /* package */
    static String removeDigits(String line) {
        return DIGITS_RE.matcher(line).replaceAll("");
    }

    /* package */
    static String removeNonDigits(String line) {
        StringBuilder builder = new StringBuilder(line);

        // Delete periods, they don't affect this operation.
        if (builder.length() > 0 && builder.charAt(0) == '.') {
            builder.deleteCharAt(0);
        }
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == '.') {
            builder.deleteCharAt(builder.length() - 1);
        }

        // Insert missing zeros.
        for (int i = 0; i <= builder.length(); i += 2) {
            if (i == builder.length() || !Character.isDigit(builder.charAt(i))) {
                builder.insert(i, '0');
            }
        }

        // Now remove the characters.
        for (int i = builder.length() - 1; i >= 0; i--) {
            if (!Character.isDigit(builder.charAt(i))) {
                builder.deleteCharAt(i);
            }
        }

        return builder.toString();
    }

    /**
     * We once saw a problem with the word "super-confort", where the hyphen ended up
     * as its own segment. Merge it with the previous segment.
     */
    private static List<String> mergeSingleHyphens(List<String> segments) {
        // Quick exit.
        if (!segments.contains("-")) {
            return segments;
        }

        // Build up a new list.
        List<String> newSegments = new ArrayList<>(segments.size());

        for (int i = 0; i < segments.size(); i++) {
            String element = segments.get(i);

            if (i + 1 < segments.size() && segments.get(i + 1).equals("-")) {
                newSegments.add(element + "-");
                // Skip the hyphen.
                i++;
            } else {
                newSegments.add(element);
            }
        }

        return newSegments;
    }

    /**
     * We once saw a problem with the word "back-end", which was hyphenated as
     * "back" and "-end". Move the hyphen to the end of the previous segment.
     */
    private static List<String> moveHyphenPrefixes(List<String> segments) {
        // Build up a new list.
        List<String> newSegments = new ArrayList<>(segments.size());

        for (int i = 0; i < segments.size(); i++) {
            String element = segments.get(i);

            if (i + 1 < segments.size() && segments.get(i + 1).startsWith("-")) {
                newSegments.add(element + "-");
                newSegments.add(segments.get(i + 1).substring(1));
                i++;
            } else {
                newSegments.add(element);
            }
        }

        return newSegments;
    }

    /**
     * Takes a list of segments and return them separated by hyphens.
     */
    public static String segmentsToString(List<String> segments) {
        return HYPHEN_JOINER.join(segments);
    }
}
