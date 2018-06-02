/*
 *
 *    Copyright 2017 Lawrence Kesteloot
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

/**
 * A page reference to another label (see LabelSpan).
 */
public class PageRefSpan extends Span {
    private final @NotNull String mName;
    private final @NotNull FontVariantFlags mFlags;

    public PageRefSpan(String name, FontVariantFlags flags) {
        mName = name;
        mFlags = flags;
    }

    public String getName() {
        return mName;
    }

    public FontVariantFlags getFlags() {
        return mFlags;
    }
}
