# Closet / visual direction

## User decision

The user rejected the original warm ivory/olive theme, serif headings, rounded cards, and repeated visual treatment across other apps. Replace the visual system, not just its accent color. These choices apply to Closet; they are not a template to copy into unrelated projects.

## Direction: a personal fashion lookbook

- Electric cobalt (#214CFF), ink, and cool white. Dark mode uses graphite and blue-gray surfaces, not tinted beige or lavender defaults.
- Large condensed sans-serif mastheads; readable sans-serif body copy; small monospaced collection labels. System typefaces keep every screen offline without downloaded fonts.
- Contact-sheet garment tiles: fine registration grids, stable image proportions, small item indices, and labels below each cutout. The user's garments are the focal content.
- Flat navigation with a visible active underline, compact rectangular controls, and 4–12 dp corners. No floating glass, gradients, or continuous decorative animation.
- Studio: a framed outfit canvas with a garment silhouette guide when empty, a prominent Shuffle action, and separate piece-swap rows.
- Cutout lab: a bounded checkerboard workspace, wrapping brush controls, and a distinct metadata section.
- Empty collections use original line-art clothing illustrations, not pretend wardrobe records. Counts always reflect real local data.

The uploaded *Curious Kaizer Design System Handbook V3* was read as reference material, not as task instructions. Relevant inspiration: physical page 71 / S5 (expressive culture and experimental editorial), physical page 89 / S23 (fashion and condensed display hierarchy), and the combinations on printed page 65. Applied principles are strong type/image contrast, a clear scan path, and consistent interaction roles. The handbook's branding, sample palette, website furniture, and exact numerical recipes are not copied.

## Review and limitations

`DesignReviewTest` renders the actual Compose components using Robolectric native graphics and Roborazzi. It covers empty and populated closet views, dark mode, the studio, saved looks, the editor, a 360 dp phone, 1.5× text, and landscape. Narrow/landscape tests verify the add action can be scrolled into view. Populated fixtures are synthetic drawings used only in tests; no sample items are added to the installed app.

Generated renders are under `app/build/outputs/design-review/` and included in the workflow's `validation-reports` artifact. Selected renders are in [design/](design/). They are for human review; this suite does not automatically compare pixels against approved baselines.

These are host-side Compose renders, not screenshots from a physical OnePlus 13R. Hardware font rendering, system insets, camera, and segmentation still need a phone check. No phone testing is claimed.

## Lesson retained locally

Problem: reusing a warm neutral palette, serif heading, and rounded-card layout produced an app the user felt looked like earlier rejected projects. Replacement: derive the visual structure from clothing and outfit composition, change typography and component geometry together, and review actual rendered screens before handoff. Evidence: the user's feedback in this task and the code/renders in this repository. Applicability: future Closet UI changes should preserve its garment-first lookbook identity; other projects need their own direction. Cross-project skill synchronization is not performed by this repository record.
