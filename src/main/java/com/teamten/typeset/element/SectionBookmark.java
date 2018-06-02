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

/**
 * A bookmark for a section, such as a part or chapter.
 */
public class SectionBookmark extends Bookmark {
    private final Type mType;
    private final String mName;

    public enum Type {
        PART("Part", true),
        CHAPTER("Chapter", true),
        MINOR_SECTION("Minor Section", true),
        HALF_TITLE_PAGE("Half Title Page", false),
        TITLE_PAGE("Title Page", false),
        COPYRIGHT_PAGE("Copyright Page", false),
        TABLE_OF_CONTENTS("Table of Contents", false),
        INDEX("Index", true);

        private final String mLabel;
        private final boolean mIncludedInTableOfContents;

        Type(String label, boolean includedInTableOfContents) {
            mLabel = label;
            mIncludedInTableOfContents = includedInTableOfContents;
        }

        /**
         * Whether this section should be included in the table of contents.
         */
        public boolean isIncludedInTableOfContents() {
            return mIncludedInTableOfContents;
        }

        @Override
        public String toString() {
            return mLabel;
        }
    }

    public SectionBookmark(Type type, String name) {
        mType = type;
        mName = name;
    }

    public Type getType() {
        return mType;
    }

    public String getName() {
        return mName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SectionBookmark that = (SectionBookmark) o;

        if (mType != that.mType) {
            return false;
        }
        return mName.equals(that.mName);

    }

    @Override
    public int hashCode() {
        int result = mType.hashCode();
        result = 31*result + mName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return mType + " \"" + mName + "\"";
    }
}
