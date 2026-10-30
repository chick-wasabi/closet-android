# Interface design

Closet uses a quiet rounded interface: rose, lilac, peach, bundled Nunito typography, and dark plum surfaces. Clothes carry the screen; headings and helper text stay compact.

Titles measure the actual font at the available width and system font scale. Controls stack when necessary, and grids reduce their column count for larger text. The layout does not target a particular phone model.

Motion includes short page/outfit crossfades, selected-tab transitions, card press feedback, grid changes, and an empty-state entrance. Standard Compose timing follows Android's animation-duration setting; no decoration animates indefinitely.

Public screenshots are rendered from production components using illustrated sample clothes. They are neither a separate mockup nor preloaded app data. UI commits refresh screenshots, and CI checks freshness.

## Everyday flows

Version 1.3 keeps the rounded rose/lilac/plum direction while extending the app with review, detail and Settings screens. Outfit actions appear before the image so users can reach Shuffle without scrolling past a tall stack. Secondary detail controls use compact icon buttons with accessibility labels. Long forms scroll instead of compressing text; control groups wrap at narrow widths. The appearance controls offer rose, lilac and teal accents without replacing the shared spacing and type system.
