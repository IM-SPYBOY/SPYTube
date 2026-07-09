# SPYTube Design System — DESIGN.md

> **Purpose:** This document is the single source of truth for every visual decision in SPYTube.
> Before touching any UI file, read the relevant section below.
> When creating new screens or components, follow these tokens and patterns exactly.

---

## 1. Design Philosophy

SPYTube follows an **Apple TV + Netflix** hybrid aesthetic:

| Principle | Rule |
|-----------|------|
| **Dark-first** | Pure black backgrounds (`#0A0A0C`), no grey surfaces |
| **Glassmorphism** | Frosted glass via AGSL `safeGlass` shader (Compose) or `glass_panel.xml` (XML) |
| **Cinematic hero** | Full-bleed hero images with multi-stop gradient overlays |
| **Capsule buttons** | All primary actions use full-pill shapes (`radius ≥ 100dp` XML / `RoundedCornerShape(24.dp)` Compose) |
| **Micro-animations** | Press-bounce (0.95× scale), dot-width transitions, fade-ins |
| **Minimal chrome** | No toolbars, no dividers — content fills the screen |

---

## 2. Color Tokens

### 2.1 Dark Mode (Primary)

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#050510` | Root screen background |
| `bgGradientStart` | `#0A0A0C` | Vertical gradient top |
| `bgGradientMid` | `#070709` | Vertical gradient mid |
| `bgGradientEnd` | `#040405` | Vertical gradient bottom |
| `surface` | `#12122A` | Elevated cards, panels |
| `surfaceVariant` | `#1A1A2E` | Shimmer base, frosted glass tint |
| `cardBg` | `#141414` | Card backgrounds |
| `cardBgAlt` | `#1A1A1A` | Alternate card backgrounds |

### 2.2 Glass Surfaces

| Token | Hex | Usage |
|-------|-----|-------|
| `glassSurface` | `#80141414` (50% α) | Compose glass tint |
| `glassBorder` | `#14FFFFFF` (8% α) | Compose glass border |
| `glassSurfaceXML` | `#1AFFFFFF` | XML glass fill |
| `glassBorderXML` | `#22FFFFFF` | XML glass stroke (0.5dp) |
| `glassNavSurface` | `#1C1C1E` @ 38% α | Navbar glass tint |

### 2.3 Accent Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `primary` / `accentRed` | `#E50914` | Brand red — selected nav, badges, progress bars |
| `primaryDark` | `#B20710` | Pressed state of primary |
| `accentGold` | `#FFD700` | Star ratings |
| `accentGreen` | `#00C853` | Live indicator only |

### 2.4 Text Colors (Dark Mode)

| Token | Hex | Usage |
|-------|-----|-------|
| `textPrimary` | `#F0F0FF` | Titles, headings |
| `textSecondary` | `#B3F0F0FF` (70% α) | Subtitles, metadata |
| `textMuted` | `#E0E0E0` | Hints, captions, timestamps |
| `textDimmed` | `#8888A0` | Disabled labels, helper text |
| `metaText` | `#CCCCCC` | Hero banner metadata ("Movie · 2026") |

### 2.5 Detail Page (XML-specific)

| Element | Color |
|---------|-------|
| Page background | `#0A0A14` |
| Toolbar scrim | `#E60A0A14` |
| Content wrapper | `#0D0D1A` |
| Description text | `#CCFFFFFF` |
| Section headers | `#FFFFFF`, bold |
| Separator lines | `#10FFFFFF` |
| Genre chip border | `#33FFFFFF` |
| Genre chip text | `#CCFFFFFF` |

---

## 3. Typography

### 3.1 Font Families

| Family | Weight | File | Usage |
|--------|--------|------|-------|
| **SF Pro Display** | Black (900) | `sf_pro_display_black.otf` | Hero banner title fallback |
| **SF Pro Display** | Bold (700) | `sf_pro_display_bold.otf` | Hero metadata, nav labels |
| **Bebas Neue** | Regular | `bebas_neue.ttf` | Decorative / accent text |
| **System Default** | — | — | All XML layouts, body text |

### 3.2 Type Scale

| Element | Size | Weight | Letter Spacing | Line Height |
|---------|------|--------|----------------|-------------|
| Hero title (fallback text) | 44sp | Black | -1.5sp | 46sp |
| Hero metadata | 14sp | Bold | -0.5sp | — |
| Play button label | 16sp | Bold | — | — |
| Section header | 18sp | Bold | — | — |
| Card title (Top 10) | 13sp | Bold | — | — |
| Card meta (rating) | 12sp | Bold | — | — |
| Card meta (year) | 12sp | Normal | — | — |
| Nav label | 10sp | SemiBold (selected) / Normal | — | 10sp |
| Genre chip | 13sp | Normal | — | — |
| Description body | 15sp | Normal | — | — |
| Cast name | 12sp | Normal | — | — |
| "Server" label | 11sp | Bold | — | — |

---

## 4. Spacing & Layout

### 4.1 Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | 2dp | Icon-to-label gap in nav |
| `sm` | 4dp | Tight inner padding |
| `md` | 8dp | Card internal margins |
| `lg` | 12dp | Card gap, list item padding |
| `xl` | 16dp | Section padding, page margins |
| `2xl` | 20dp | Page horizontal padding (detail page) |
| `3xl` | 24dp | Hero content horizontal padding |
| `4xl` | 32dp | Hero-to-dots spacer |
| `5xl` | 48dp | Content bottom padding |

### 4.2 Screen Regions

```
┌─────────────────────────────┐
│ HERO (75% of screen height) │ ← Full-bleed image/trailer
│ ┌─────────────────────────┐ │
│ │ gradient overlay        │ │
│ │ [LOGO]                  │ │
│ │ Movie · 2026            │ │
│ │ [▶ Play]  [+]           │ │
│ │ ● ● ● ━━ ●             │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ CONTENT (scrollable)        │ ← 60dp overlap into hero
│ [▶ Play]  [♡]              │
│ [Cinefy|ZXC|VidRock|VidLink]│
│ [Sci-Fi] [Action] [Drama]  │
│ Description...              │
│ ─── Cast ───                │
│ ─── More Like This ───      │
└─────────────────────────────┘
┌─────────────────────────────┐
│ GLASS NAVBAR                │ ← Fixed at bottom
│ [Home|Movies|LiveTV|MyList] │
│                        [🔍]│
└─────────────────────────────┘
```

### 4.3 Key Dimensions

| Element | Dimension |
|---------|-----------|
| Hero banner height | 75% of screen height |
| Detail hero (collapsing) | 300dp |
| Navbar pill height | 58dp |
| Search circle | 58dp × 58dp |
| Play button (Compose) | 160dp × 44dp |
| Play button (XML detail) | 160dp × 44dp |
| "+" / heart button | 44dp × 44dp |
| Mute button | 36dp × 36dp |
| Poster card | 130dp × 190dp |
| Top 10 card | 140dp × 190dp |
| Continue Watching card | 200dp × 115dp |
| Cast avatar | 80dp × 80dp (circle) |
| Server radio height | 36dp |
| Nav icon | 24dp × 24dp |

---

## 5. Component Patterns

### 5.1 Primary Button (Play)

**Compose (Home Page)**
```kotlin
Row(
    modifier = Modifier
        .height(44.dp)
        .width(160.dp)
        .background(White, RoundedCornerShape(24.dp))
        .clickable { ... }
) {
    Icon(Icons.Filled.PlayArrow, tint = Color.Black, modifier = Modifier.size(22.dp))
    Spacer(Modifier.width(4.dp))
    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}
```

**XML (Detail Page)**
```xml
<!-- Drawable: btn_play_premium.xml -->
<ripple android:color="#40FFFFFF">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <corners android:radius="100dp" />  <!-- Full capsule -->
        </shape>
    </item>
</ripple>

<!-- Layout -->
<Button
    android:layout_width="160dp"
    android:layout_height="44dp"
    android:background="@drawable/btn_play_premium"
    android:text="▶  Play"
    android:textColor="#000000"
    android:textSize="16sp"
    android:textStyle="bold"
    android:stateListAnimator="@null"
    app:backgroundTint="@null" />
```

> **Rule:** All primary buttons are white capsule pills, 160dp × 44dp, black text, 16sp bold.

### 5.2 Secondary Button (Circle)

| Variant | Size | Background | Icon |
|---------|------|------------|------|
| "+" Add (Compose) | 44dp circle | `safeGlass` shader | White `+` 22dp |
| Heart/My List (XML) | 44dp circle | `btn_circle_outline.xml` | Heart 12dp padding |
| Mute (XML) | 36dp circle | `btn_circle_glass.xml` | System icon 8dp padding |

### 5.3 Media Cards

**All cards share:**
- Corner radius: `12dp`
- Press animation: `scale 0.95×` with spring damping `0.375f`
- Elevation animation: `4dp → 24dp` on press
- Refractive edge: via `refractiveEdge()` modifier
- Bottom gradient: `Transparent → #80000000` (or `#D9000000` for Top 10)

**Card variants:**

| Type | Width | Height | Extras |
|------|-------|--------|--------|
| `GlassMediaCard` | 130dp | 190dp | Star + rating + year below |
| `Top10NumberedCard` | 140dp | 190dp | Rank number inside (38sp Black), title inside |
| `ContinueWatchingCard` | 200dp | 115dp | Landscape, play circle overlay, progress bar |

### 5.4 Glass Surfaces

**Compose (AGSL shader):**
```kotlin
Modifier.safeGlass(
    backdrop = backdrop,
    shape = CircleShape,    // or RoundedCornerShape
    surfaceColor = Color(0xFF1C1C1E).copy(alpha = 0.38f),
    blurRadius = 16.dp,
    lensAmount = 24.dp
)
```

**XML (static fallback):**
```xml
<!-- glass_panel.xml -->
<layer-list>
    <!-- Gradient fill: #1AFFFFFF → #12FFFFFF → #08FFFFFF @ 135° -->
    <!-- Border: 0.5dp #22FFFFFF, 16dp radius -->
</layer-list>
```

### 5.5 Server Selector Strip

```xml
<!-- Container: server_strip_bg.xml (glass pill, 100dp radius) -->
<!-- Items: server_pill_selector.xml (state-based active/inactive) -->
<RadioGroup orientation="horizontal" padding="3dp">
    <RadioButton height="36dp" weight="1" ... />
</RadioGroup>
```

- Active server: filled red background (`#E50914`)
- Inactive: transparent, white text at 67% alpha

### 5.6 Genre Chips

```xml
<!-- genre_chip_bg.xml -->
<shape android:shape="rectangle">
    <stroke android:width="1dp" android:color="#33FFFFFF" />
    <corners android:radius="20dp" />
</shape>
```

- Height: wrap_content with 6dp vertical, 14dp horizontal padding
- Text: 13sp, `#CCFFFFFF`

### 5.7 Pagination Dots (Apple TV style)

```kotlin
// Active: 24dp wide × 6dp tall, white
// Inactive: 6dp × 6dp, white @ 40% alpha
// Shape: CircleShape (fully rounded)
// Transition: animateDpAsState 300ms tween
```

### 5.8 Hero Gradient Overlay

```kotlin
Brush.verticalGradient(
    0.0f  to Color.Transparent,
    0.3f  to Color.Black.copy(alpha = 0.2f),
    0.6f  to Color.Black.copy(alpha = 0.5f),
    0.85f to Color.Black.copy(alpha = 0.8f),
    1.0f  to Color.Black.copy(alpha = 0.95f)
)
```

### 5.9 Shimmer Loading

```kotlin
// Colors: #1A1A2E → #2A2A40 → #1A1A2E
// Speed: 1000ms linear, infinite restart
// Card skeleton: same dimensions as real cards (130×190dp, 12dp radius)
```

---

## 6. Navigation

### 6.1 Bottom Navigation Bar

| Property | Value |
|----------|-------|
| Style | Frosted glass pill + detached search circle |
| Height | 58dp |
| Shape | `CircleShape` (full pill) |
| Glass | `safeGlass` with `blurRadius=16dp`, `lensAmount=24dp` |
| Shadow | 20dp elevation, black @ 55% alpha |
| Items | Home, Movies, Live TV, My List + detached Search |
| Selected color | `#E50914` (NetflixRed) |
| Inactive color | White @ 55% alpha |
| Selection bubble | White @ 14% alpha, full capsule |
| Icon size | 24dp |
| Label size | 10sp |

### 6.2 Back Button (XML screens)

```xml
<ImageButton
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:background="@drawable/glass_panel"
    android:src="@drawable/ic_back"
    android:padding="12dp" />
```

---

## 7. Animation Tokens

| Animation | Spec | Usage |
|-----------|------|-------|
| Card press bounce | `spring(damping=0.375, stiffness=400)` | All media cards |
| Nav item bounce | `spring(MediumBouncy, StiffnessMedium)` | Navbar items |
| Title fade-in | `tween(500ms, FastOutSlowInEasing)` | Hero logo appearance |
| Dot width | `tween(300ms)` | Active dot expansion |
| Color transition | `tween(200ms)` | Nav tint color |
| Bubble alpha | `tween(250ms)` | Selection bubble |
| Hero auto-slide | 5000ms delay, `animateScrollToPage` | Banner pager |
| Shimmer sweep | 1000ms linear, infinite | Loading skeletons |

---

## 8. Screen-by-Screen Checklist

### 8.1 Home Screen (Compose)
- [x] Hero banner: 75% height, auto-sliding, logo + metadata + capsule Play + glass "+"
- [x] Apple TV pagination dots with animated width
- [x] Continue Watching row with landscape cards + progress bars
- [x] Top 10 row with rank numbers inside cards
- [x] Standard poster rows with bounce + refractive edges
- [x] Shimmer loading skeletons
- [x] Glass bottom navigation

### 8.2 Detail Page (XML)
- [x] Collapsing hero (300dp) with trailer WebView fallback
- [x] Title logo overlay on gradient
- [x] Compact capsule Play (160×44dp) + heart icon in centered row
- [x] Server selector strip below actions
- [x] Genre chips with outline style
- [x] Description, Cast (circle avatars), More Like This
- [x] 60dp content overlap into hero

### 8.3 Search (XML)
- [ ] Glass panel search input (48dp, 16dp radius)
- [ ] Glass back button
- [ ] Grid results with poster cards

### 8.4 Player (XML)
- [ ] Fullscreen with gesture overlay
- [ ] Custom controls layer

### 8.5 Splash (Compose)
- [ ] Animated logo entry
- [ ] Background matching `bgGradientStart`

---

## 9. Do's and Don'ts

### ✅ Do

- Use capsule shapes (`radius ≥ 100dp` or `RoundedCornerShape(24.dp)`) for all primary buttons
- Use `safeGlass` for any floating overlay or navigation component
- Apply `refractiveEdge()` or `safeGlass` with `lensAmount` for glass card borders
- Use multi-stop gradients for hero overlays — never a single solid overlay
- Use spring animations for press feedback — never linear
- Keep all backgrounds near-black (`#0A0A0C` to `#141414`) — never grey
- Match the capsule Play + circle secondary layout for all action rows
- Fade hero content in — never pop

### ❌ Don't

- Use system `MaterialButton` without `app:backgroundTint="@null"` and `stateListAnimator="@null"`
- Use flat/boxy corner radii (`8dp`) on primary action buttons
- Use full-width buttons where a compact centered pill would work
- Mix light-mode colors in dark-mode screens
- Use plain `Color.White` for text — use the token hierarchy (`textPrimary` → `textMuted`)
- Add toolbars or action bars — use floating glass elements instead
- Use `Modifier.blur()` on content — use `safeGlass` for backdrop blur only

---

## 10. File Reference

### Compose Components
| File | Purpose |
|------|---------|
| `Theme.kt` | Color scheme, extended colors, `frostedGlass()` modifier |
| `HeroBanner.kt` | Hero pager, logo fetching, gradient, Play+Add buttons, dots |
| `GlassCard.kt` | `GlassMediaCard`, `Top10NumberedCard`, `ContinueWatchingCard` |
| `GlassNavigation.kt` | Bottom nav pill + search circle |
| `SafeGlass.kt` | AGSL backdrop blur wrapper |
| `ShimmerCard.kt` | `ShimmerRow` loading skeleton |
| `GlassComponents.kt` | Shared glass utilities |
| `GyroGlassBorder.kt` | Refractive edge modifier |

### XML Drawables
| File | Purpose |
|------|---------|
| `btn_play_premium.xml` | White capsule play button (100dp radius) |
| `btn_circle_outline.xml` | Circle outline for heart/add |
| `btn_circle_glass.xml` | Semi-transparent glass circle (mute button) |
| `glass_panel.xml` | Standard glass panel (gradient fill + border) |
| `glass_panel_elevated.xml` | Elevated glass with more opacity |
| `server_strip_bg.xml` | Server selector container |
| `server_pill_selector.xml` | Server radio button states |
| `genre_chip_bg.xml` | Genre chip outline |
| `hero_gradient_premium.xml` | Detail page hero gradient |

### XML Layouts
| File | Purpose |
|------|---------|
| `activity_detail.xml` | Detail page — hero + content |
| `activity_search.xml` | Search — glass bar + results |
| `activity_main.xml` | Main — Compose host |
| `item_media.xml` | XML media card (search results) |
| `item_cast.xml` | Cast circle avatar + name |
| `glass_navbar.xml` | XML glass navigation bar |
