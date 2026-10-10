# HisabNikash logo

Created 2026-10-10 using the built-in image-generation tool, with one targeted refinement. No external image API key, design dependency or third-party logo pack was added.

The mark combines an open accounting ledger with a checkmark. The brief uses the application's existing forest green and mint palette. The localized name stays real text rather than being baked into a bitmap, preserving Bangla/English layout and accessibility.

## Source and placement

- Master PNG: [`hisab_logo.png`](../../app/src/main/res/drawable-nodpi/hisab_logo.png), 1254×1254 pixels, with genuine transparent alpha.
- Dashboard: decorative image beside the localized name and existing financial heading; no duplicated accessibility description.
- Launcher: API24 legacy layered resource, API26+ adaptive foreground/background, API33+ monochrome layer for themed launchers. The modern foreground uses proportional padding so enlargement does not crop the mark outside the safe zone. The dimension-inset default remains an older-OS fallback.
- The previous small notification drawable remains separate from the adaptive launcher; adaptive icons are not substituted into notification small-icon slots.

The implementation follows [Android adaptive-icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive). The guaranteed safe area is a centered 66dp-diameter circle, as explained in [Android's app-icon codelab](https://developer.android.com/codelabs/basic-android-kotlin-compose-training-change-app-icon); the modern foreground has 10% padding on each side. Percentage insets are limited to API26+; see [InsetDrawable](https://developer.android.com/reference/android/graphics/drawable/InsetDrawable).

This is a generated raster asset, not a claimed hand-authored SVG or a trademark-clearance certificate. The original generation files remain outside the repository; the selected project asset is checked into the app resources.

Master SHA-256: `49a8e8dd0b70073b476ac252b6b00a8bdb2f8068798cc1703aac5f692690c999` (543,478 bytes). Generation mode: built-in image generation, followed by one referenced-image edit; transparent background enabled for both.

## Generation prompt

```text
Use case: logo-brand
Asset type: production logo symbol for HisabNikash, a Bangla-first offline personal income-and-expense Android app.
Primary request: an original professional minimal geometric accounting mark: a compact open ledger/book silhouette with a beautifully integrated checkmark in the negative space, communicating clarity, balanced finances and trust. One cohesive symbol, not a collection of icons. Distinctive, restrained, approachable fintech identity.
Style: precision flat vector-like artwork rendered as a high-quality square PNG, clean bold geometry and balanced optical spacing, recognizable at small app-icon sizes.
Color palette: existing brand forest green #1E6842, optional tiny mint #E9F5ED accent. The visible main mark must be forest green, not white.
Composition: one centered symbol occupying about 68 percent of the square canvas, with generous equal transparent margins for Android adaptive icon safe zones. No wordmark; the app will display its localized name using real text.
Background: genuinely transparent alpha, no colored tile or baked rounded corners.
Constraints: original design, no brand imitation, no letters or text, no currency sign, no gradients, no shadows, no bevels, no mockup, no watermark, no fine lines. Favor one strong silhouette and crisp anti-aliased edges.
```

## Targeted refinement prompt

```text
Edit this HisabNikash logo only to make its surfaces perfectly flat solid colors: forest green #1E6842 for the book, pale mint #E9F5ED for the two page shapes. Remove all gradients, shading, grain and texture. Preserve exactly the silhouette, checkmark/ledger negative space, scale, central placement and transparent margins. Keep genuinely transparent alpha, no text, no shadow, no background or tile. This is the final clean production logo asset.
```

The prompts record the requested art direction, not a claim that every pixel precisely equals the requested color; the delivered PNG remains the source of truth.
