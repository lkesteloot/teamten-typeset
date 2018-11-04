
Book typesetting system written in Java. See the
[Javadoc](https://lkesteloot.github.io/teamten-typeset/).
It supports fine typography, ligatures, kerning, automatic hyphenation, table
of contents, index, photos, cross-references, footnotes, and right-to-left text.
It converts a Markdown-inspired text file and generates a PDF. It
can be used as a stand-alone application or as a library.

This typesetting system was only used once, to typeset a
[book written by my great uncle](https://www.teamten.com/lawrence/projects/memoir-book/).
A few things are hard-coded (minor layout
spacing), but it shouldn't be too hard to get it to work for another type of
book.

# Input format.

The input format is Markdown-inspired. The support for code markup is
experimental and was never used for a real book.

    Write in *italics* or [i]italics[/i], [sc]small caps[/sc],
    and `code`. There's no bold support. You can continue a
    paragraph onto the next line.

    Separate paragraphs by blank lines.

    % Start comments with percent signs.

    1. Numbered lists. Must have a period after the number.

    2. More items.

    • Bulleted items start with a real bullet symbol.

    • Type Alt-8 on a Mac to make it.

Use `#` symbols at the start of a paragraph to mark section headers:

    # Part (for breaking the book into a few parts).

    ## Chapter (within a part).

    ### Minor section. This is like a chapter but outside a part, for things
    like the preface.

    #### Minor header within a chapter, not in the table of contents.

Indent entire sections to call out code, poetry, and signatures:

    This is a line of code:

        a = b;  // Four-space indent.

    This is for stuff you might type into a computer (user input);

       <Three spaces and a less-than symbol.

    And this is for the computer's output:

       >Three spaces and a greater-than symbol.

    Poetry has a slash:

       /Three spaces and a slash.
       /Make poems in a flash.

    Signatures have a hyphen:

       -John Smith
       
       -London, 2018

    (Signatures are just unindented lines.)

Commands to the system can be embedded in brackets:

    [SEPARATOR] Three asterisks, for separating sections.

    [VERTICAL-SPACE] Some vertical space.

    [NEW-PAGE] Start a new page.

    [ODD-PAGE] Skip to the next odd (right-hand) page.

    [HALF-TITLE] Insert the half-title page (just the book's title).

    [TITLE] Insert the full title page.

    [COPYRIGHT] Insert the copyright page.

    [TOC] Insert the table of contents.

    [INDEX] Insert the index.

    [Variable-Name: value] Set a variable's value. See below.

    [@Darwin, Charles] Add an index entry. See below.

    [^This is a footnote!] Footnote entry.

    [!darwin.jpg Charles Darwin in 1859.] Photo pathname and an optional caption.

    [LABEL id] Mark this spot with the specified id (any string). Doesn't
    generate text.

    [PAGE-OF id] Replaced with the page of the [LABEL] tag for this id.

    [TODO anything] Ignored, for making notes to oneself.

    [open-bracket] Insert an open bracket.

Generally the start of the book will have a list of variables, followed by:

    [HALF-TITLE]

    [TITLE]

    [COPYRIGHT]

    [TOC]

## Variables

These should be set at the top of the file. The format is:

    [Variable-Name: value]

For example:

    [Language: en]
    [Title: On the Origin of Species]
    [Author: Charles Darwin]
    [Body-Font: Minion, regular, 11pt]
    [Page-Width: 6in]
    [Page-Height: 9in]

The following variables have arbitrary text for values:

    Title
    Author
    Publisher-Name
    Publisher-Location (e.g., "London, England")
    Copyright (e.g., "Copyright © 1859 Charles Darwin")
    Colophon (e.g., "First edition, November 1859")
    Toc-Title (e.g., "Table of Contents")
    Index-Title (e.g., "Index")
    Language (e.g., "en_US" or "fr")

The language sets which hyphenation dictionary is used.

The following variables have font values. Font have the format
`typeface, variant, size`, where `typeface` is registered
in the `font/Typeface` class, `variant` is one of `regular`,
`bold`, `italic`, `bold italic`, or `small caps`, and
the size is a real number followed by a unit, one of
`pt`, `pc`, `in`, `cm`, `mm`, or `sp`. For example,
`[Body-Font: Minion, regular, 11pt]`.

    Body-Font
    Block-Quote-Font
    Body-Code-Font
    Caption-Font
    Part-Header-Font
    Chapter-Header-Font
    Minor-Header-Font
    Page-Number-Font
    Code-Font
    Output-Font
    Input-Font
    Headline-Font
    Half-Title-Page-Title-Font
    Title-Page-Author-Font
    Title-Page-Title-Font
    Title-Page-Publisher-Name-Font
    Title-Page-Publisher-Location-Font
    Copyright-Page-Copyright-Font
    Copyright-Page-Colophon-Font
    Toc-Page-Title-Font
    Toc-Page-Part-Font
    Toc-Page-Chapter-Font
    Footnote-Number-Font

The following variable is a typeface. The value is registered in
the `font/Typeface` class. It registers a typeface to use when
a glyph is not present in whatever typeface is supposed to be
used. This is useful for foreign-language glyphs such as
Hebrew. Times New Roman is a good typeface to use here.

    Fallback-Typeface

The following variables are distances. A distance is a real
number followed by a unit, one of `pt`, `pc`, `in`, `cm`,
`mm`, or `sp`.

    Page-Width
    Page-Height
    Page-Margin-Top
    Page-Margin-Bottom
    Page-Margin-Outer
    Page-Margin-Inner
    Footnote-Shift (how much to shift footnote references up)

See the `typeset/Config` class's method `fillWithDefaults()` for default
values for many of these variables.

Here are the values I used for the book I printed:

    % General configuration.
    [Language: fr]

    % String configuration.
    [Title: La famille Klat]
    [Author: André Klat]
    [Publisher-Name: Team Ten Press]
    [Publisher-Location: San Francisco, California]
    [Copyright: Copyright © 1989 André Klat]
    [Colophon: Première édition, mai 2017]
    [Toc-Title: Table des matières]

    % Font configuration.
    [Body-Font: Minion, regular, 11pt]
    [Block-Quote-Font: Minion, italic, 11pt]
    [Code-Font: Minion, regular, 11pt]
    [Caption-Font: Minion, italic, 9.5pt]
    [Part-Header-Font: Minion, small caps, 19pt]
    [Chapter-Header-Font: Minion, small caps, 14pt]
    [Minor-Header-Font: Minion, small caps, 11pt]
    [Page-Number-Font: Minion, regular, 8pt]
    [Headline-Font: Minion, regular, 8pt]
    [Half-Title-Page-Title-Font: Minion, small caps, 19pt]
    [Title-Page-Author-Font: Minion, small caps, 14pt]
    [Title-Page-Title-Font: Minion, small caps, 27pt]
    [Title-Page-Publisher-Name-Font: Minion, small caps, 9pt]
    [Title-Page-Publisher-Location-Font: Minion, italic, 9pt]
    [Copyright-Page-Copyright-Font: Minion, italic, 11pt]
    [Copyright-Page-Colophon-Font: Minion, small caps, 9pt]
    [Toc-Page-Title-Font: Minion, small caps, 14pt]
    [Toc-Page-Part-Font: Minion, small caps, 11pt]
    [Toc-Page-Chapter-Font: Minion, regular, 11pt]

    % Dimensions configuration.
    [Page-Width: 6in]
    [Page-Height: 9in]
    [Page-Margin-Top: 6pc]
    [Page-Margin-Bottom: 6pc]
    [Page-Margin-Outer: 6pc]
    [Page-Margin-Inner: 8pc]

## Index entries

Put index entries in-line:

    The insects in Madeira[@Madeira] which are not ground-feeders, ...

This will add an entry for "Madeira" referring to this page. Add sub-index
entries separated by vertical bars:

    The insects in Madeira[@Madeira|insects] which are not ground-feeders, ...

## Right-to-left text

Right-to-left text is only minimally supported. Be sure to set the `Fallback-Typeface`
configuration setting if your main font does not have Hebrew or Arabic glyphs.
The typesetter does not handle explicit direction codes or Arabic shaping, and
does not handle parentheses well. Simple embedded Hebrew should work fine.

# License

Copyright 2018 Lawrence Kesteloot

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
