---
projectId: '4724290021803215791'
stitchProject: 'projects/4724290021803215791'
name: Anchor Grounding System
colors:
  surface: '#fcf9f3'
  surface-dim: '#dcdad4'
  surface-bright: '#fcf9f3'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3ed'
  surface-container: '#f0eee8'
  surface-container-high: '#ebe8e2'
  surface-container-highest: '#e5e2dc'
  on-surface: '#1c1c18'
  on-surface-variant: '#424843'
  inverse-surface: '#31312d'
  inverse-on-surface: '#f3f0ea'
  outline: '#727973'
  outline-variant: '#c2c8c1'
  surface-tint: '#466552'
  primary: '#466552'
  on-primary: '#ffffff'
  primary-container: '#7a9b86'
  on-primary-container: '#133222'
  inverse-primary: '#accfb8'
  secondary: '#8e4d34'
  on-secondary: '#ffffff'
  secondary-container: '#feaa8b'
  on-secondary-container: '#783c24'
  tertiary: '#645d55'
  on-tertiary: '#ffffff'
  tertiary-container: '#9a9288'
  on-tertiary-container: '#302b24'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c8ebd3'
  primary-fixed-dim: '#accfb8'
  on-primary-fixed: '#012112'
  on-primary-fixed-variant: '#2e4d3c'
  secondary-fixed: '#ffdbcf'
  secondary-fixed-dim: '#ffb59a'
  on-secondary-fixed: '#380d00'
  on-secondary-fixed-variant: '#71361f'
  tertiary-fixed: '#ebe1d6'
  tertiary-fixed-dim: '#cec5ba'
  on-tertiary-fixed: '#1f1b14'
  on-tertiary-fixed-variant: '#4c463e'
  background: '#fcf9f3'
  on-background: '#1c1c18'
  surface-variant: '#e5e2dc'
  surface-card: '#FFFFFF'
  text-primary: '#33302B'
  text-muted: '#8A837A'
  border-subtle: '#E3DDD3'
  dark-bg: '#1C1B18'
  dark-card: '#262521'
typography:
  headline-lg:
    fontFamily: manrope
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: manrope
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: manrope
    fontSize: 20px
    fontWeight: '400'
    lineHeight: 32px
  body-md:
    fontFamily: manrope
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 28px
  label-md:
    fontFamily: manrope
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.02em
  numeral-mono:
    fontFamily: jetbrainsMono
    fontSize: 24px
    fontWeight: '400'
    lineHeight: 32px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  margin-page: 20px
  gutter-card: 16px
  stack-sm: 8px
  stack-md: 24px
  stack-lg: 40px
---

## Brand & Style

The design system is built upon the philosophy of **"Anti-KPI Minimalism."** It rejects the high-pressure, gamified patterns of modern productivity apps in favor of a "low-arousal" environment that facilitates emotional regulation and grounding. The target audience includes individuals in high-stress or low-cognitive-load states who require a digital sanctuary that feels like a physical anchor.

### Design Style: Warm Minimalism
The aesthetic combines **Minimalism** with **Tactile/Organic** elements. It prioritizes heavy whitespace (breathing room) and a grounded color palette to reduce visual noise. The goal is to evoke a sense of "quiet presence"—a UI that is functional and non-judgmental, avoiding "toxic positivity" in favor of realistic, compassionate support.

- **Atmosphere:** Tranquil, accepting, and spacious.
- **Visual Strategy:** Wide margins, soft edges, and a total absence of "urgency" cues (no badges, no streaks, no rewards).
- **Emotional Response:** A physiological sigh of relief; a feeling of being "held" by the interface rather than "pushed" by it.

## Colors

The palette is derived from nature—earth, stone, and flora—to provide a grounding effect. 

- **Primary (Quiet Green):** Used for the "emotional core" and active states. It represents growth without pressure.
- **Secondary (Terracotta):** Reserved strictly for high-priority crisis intervention and "SOS" features. Its warmth ensures visibility without the anxiety associated with "emergency red."
- **Neutral (Parchment):** The foundation of the system. It replaces clinical white with a warmer, paper-like tone to reduce eye strain and provide a tactile feel.

### Usage Guidance
- **Contrast:** Maintain a "soft-high" contrast ratio. Avoid pure black (`#000000`); use the deep charcoal-brown provided for text to keep the interface feeling organic.
- **Gradients:** Only used for the "Wave" animation, utilizing soft sine-wave transitions between the Primary and a lighter tint of the same hue.

## Typography

The system uses **Manrope** for its balance of modern geometric structure and organic warmth. It is highly legible in low-energy states.

- **Hierarchy:** We use a "flat" hierarchy. There is a minimal size difference between headings and body text to prevent visual "shouting."
- **Accessibility Mode:** For users in crisis or extreme fatigue, the `body-lg` (20px) is the default to reduce cognitive load.
- **Numerical Data:** Use a monospaced font (**JetBrains Mono**) for countdown timers and the "Wave" breathing clock to prevent layout shifting as numbers change.

## Layout & Spacing

The layout is **fluid with fixed-width safe margins**. The primary goal is "breathability."

- **The Thumb Zone:** All critical interactive elements (FABs, primary actions) must be placed in the bottom 40% of the screen to allow for effortless, single-handed use.
- **Grid:** A standard 4px base unit. 
- **Content Density:** Intentionally low. Avoid packing multiple cards on one screen. Use vertical scrolling with generous `stack-lg` gaps between different content sections to allow the user to focus on one thing at a time.

## Elevation & Depth

This system avoids heavy shadows to maintain a "flat but layered" feel.

- **Tonal Layering:** Depth is primarily communicated through color shifts. The `surface-background` is the lowest layer, with `surface-card` sitting on top.
- **Subtle Outlines:** Instead of shadows, use the `border-subtle` (1px) to define card boundaries. This creates a "stationary" feel, making the UI feel grounded and stable.
- **Glassmorphism:** Used exclusively for the Navigation Bar and Bottom Sheets to maintain context of the underlying layer without cluttering the visual field. Use a heavy (20px+) backdrop blur.

## Shapes

The shape language is "Extra Soft." 

- **Primary Cards:** Use a 16px radius. This "squircle-adjacent" roundness removes the clinical feel of digital tools.
- **Buttons:** Use 12px for interactive elements. This creates a slight visual distinction from content cards while remaining part of the soft-edge family.
- **The Wave:** Interactive grounding elements should use organic, non-perfect circular paths to mimic natural ebbs and flows.

## Components

### Buttons
- **Primary:** Filled with `brand-primary`. No shadows. Text is `surface-card`.
- **Secondary/Ghost:** `border-subtle` outline with `text-primary`.
- **SOS Button:** A distinct, floating circle using `status-emergency` (Terracotta), placed in a consistent, reachable location but separated from standard flow.

### Cards
- **Emotion/Worry Cards:** White background (light mode) or deep charcoal (dark mode). 16px padding. No shadow; 1px `border-subtle`.
- **Input Fields:** Integrated directly into cards. The active state is signaled by a 2px `brand-primary` bottom border rather than a full outline, keeping the look "open."

### Navigation
- **Bottom Bar:** High transparency with backdrop blur. Icons are soft-outline style. Labels are always present to reduce recognition effort.

### Specialized Components
- **The Wave:** A large, circular or full-width component that utilizes a slow (4-6s) sine-wave pulse animation. Interaction should feel "heavy" and slow to encourage the user to slow down their own movements.

---

## Stitch 来源

- 项目：Anchor
- Project ID：`4724290021803215791`
- 设备基准：`MOBILE`
- 最近同步：`2026-08-15T12:49:48.717502Z`
- 本文件由 Stitch 项目中的设计系统规范同步生成；页面级资源见 `design/README.md`。
