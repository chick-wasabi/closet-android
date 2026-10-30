# Interface design

Closet uses a quiet rounded interface: rose, lilac, peach, bundled Nunito typography, and dark plum surfaces. Clothes carry the screen; headings and helper text stay compact.

Titles measure the actual font at the available width and system font scale. Controls stack when necessary, and grids reduce their column count for larger text. The layout does not target a particular phone model.

Motion includes short page/outfit crossfades, selected-tab transitions, card press feedback, grid changes, and an empty-state entrance. Standard Compose timing follows Android's animation-duration setting; no decoration animates indefinitely.

Public screenshots are rendered from production components using illustrated sample clothes. They are neither a separate mockup nor preloaded app data. UI commits refresh screenshots, and CI checks freshness.
