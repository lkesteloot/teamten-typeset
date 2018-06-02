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

import com.teamten.markdown.Block;
import com.teamten.typeset.element.Bookmark;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a reference from an index entry.
 */
public class IndexBookmark extends Bookmark {
    private final @NotNull List<Block> mEntries;

    public IndexBookmark(List<Block> entries) {
        mEntries = entries;
    }

    public List<Block> getEntries() {
        return mEntries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IndexBookmark that = (IndexBookmark) o;

        return mEntries.equals(that.mEntries);

    }

    @Override
    public int hashCode() {
        return mEntries.hashCode();
    }

    @Override
    public String toString() {
        String entries = mEntries.stream()
                .map(Block::toBriefString)
                .collect(Collectors.joining(", "));

        return "Index entry \"" + entries + "\"";
    }
}
