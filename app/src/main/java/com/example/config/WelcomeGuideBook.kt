package com.example.config

import com.example.data.local.BookChapter

/**
 * Sole built-in guide book pre-loaded into Epos ("Welcome to Epos" by Bfactuals).
 * Users can freely read, highlight, translate, or delete it like any other volume.
 */
object WelcomeGuideBook {
    const val FILE_PATH: String = "builtin://welcome_to_epos"
    const val TITLE: String = "Welcome to Epos"
    const val AUTHOR: String = "Bfactuals"

    val LEGACY_BUILTIN_PATHS: List<String> = listOf(
        "builtin://featured_between_two_stops",
        "builtin://sample_cartographer"
    )

    val CHAPTERS: List<BookChapter> = listOf(
        BookChapter(
            title = "A Note For You",
            paragraphs = listOf(
                "For beautiful eyes, here is something made just for you.",
                "Your smile has always been precious—and so is your mind. This is a small surprise from me, built to give you a quiet, cozy space of your own. Now, no language or barrier stands between you and the stories you want to explore. Read freely, translate effortlessly, and open the door to an entire world of books—all at your own pace."
            )
        ),
        BookChapter(
            title = "How to Use Epos",
            paragraphs = listOf(
                "1. Adding Books: Tap + in the Library to import any PDF, EPUB, or TXT file—works 100% offline. Long-press any book to rename, change its cover, or organize it.",
                "2. Realistic Page-Flip Reading: Drag any page corner for smooth paper-curl physics, or use One-Handed Tap Mode and Auto-Flip for effortless reading.",
                "3. Instant Translation & Word Bank: Highlight or tap any word or sentence while reading to translate it across Hindi, English, Hinglish, Urdu, regional Indian, and international languages. Tap the language pill on the card to switch languages on the fly, save words to your Word Bank, and revise them anytime in Flashcard Practice mode."
            )
        ),
        BookChapter(
            title = "Comfort, Highlights & Privacy",
            paragraphs = listOf(
                "4. Reading Comfort & Focus Tools: Customize paper, sepia, or dark themes, swipe the left edge for brightness, turn on the Warmth filter, Focus Reading Ruler, Bionic Reading, or 1-tap PDF Auto-Crop & Zoom Lock. Listen hands-free with Offline Read-Aloud (TTS), Ambient Rain or Vinyl sounds, and a Sleep Timer.",
                "5. Highlights & Quote Cards: Save favorite lines, export all highlights as .txt, or turn any quote into a clean aesthetic Quote Card.",
                "6. Private Shelf (Hidden Vault): Go to ⋮ → Settings to set your own Secret Code. Type that code into the Library Search bar anytime to unlock a completely invisible Private Shelf for personal files."
            )
        ),
        BookChapter(
            title = "Happy Reading",
            paragraphs = listOf(
                "Thank you for always being so supportive.",
                "Every page turned here is a little reminder that you deserve calm, beautiful moments. Take your time, get lost in good stories, and keep smiling.",
                "Happy reading.\n— Bfactuals"
            )
        )
    )
}
