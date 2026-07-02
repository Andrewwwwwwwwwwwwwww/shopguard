# Changelog

## 0.6.0
- **`/claim zones`** (any player): toggle admin-zone borders on/off — white end-rod particles trace the
  build-area perimeter so players can see exactly where they're allowed to claim, no tool needed.
  Turns itself off on logout. (Zone borders look distinct from the green claim outlines.)

## 0.5.1
- The shovel **mode toggle** and **"first corner set"** prompts now show above the hotbar (action bar,
  fades after a moment) instead of filling chat. Results (created/updated/carved/removed) and errors
  stay in chat.

## 0.5.0
- **Touching claims merge into one.** Adding a rectangle that touches any of your own claims fuses them
  into a single claim — extending no longer piles up separate claims, and removing one removes the whole
  thing. Non-touching claims stay separate, so multiple shops still work.
- **The shovel no longer blocks breaking.** Carve moved off left-click: **right-click the air** to
  toggle CLAIM ↔ CARVE mode, then right-click two corners. You and trusted players can break blocks
  with the shovel again.
- **Redstone control policy.** By default non-owners can't use buttons/levers in a claim (doors, gates,
  trapdoors, and pressure plates still work for walking through). Set `allowRedstoneControls: true` in
  `config/shopguard.json` to allow them.
- **Piston protection.** Pistons can't push or pull blocks across a claim boundary.
- **Zone command uses block-position corners:** `/claim zone add <corner1> <corner2>` with crosshair
  tab-completion and `~ ~ ~` support; zone IDs also tab-complete for `zone remove`.
- **Admin zone tool:** ops can hold a **golden hoe** and right-click two corners to define a claim zone.

## 0.4.1
- Claim outlines now refresh **immediately** after you add, carve, or remove a claim, instead of
  waiting up to ~0.75s for the next pulse.

## 0.4.0
- Claim outlines now only show while you're **holding the golden shovel** (no more always-on clutter).
- **Adjacent claims of the same owner merge** into one outline — the shared border between them isn't
  drawn, so they read as attached.
- **Carve by digging:** left-click two corners with the shovel to remove a rectangle from your claim
  (replaces the fiddly sneak + right-click). While it's the claim tool the golden shovel no longer digs
  real blocks — use another tool for that.

## 0.3.0
- **Claim outlines.** Claim boundaries are traced with particles for nearby players, server-side, so
  it works on any client without a client mod. (A client-rendered glowing outline was attempted first
  but deferred — Minecraft 26.2's reworked render pipeline makes it impractical to build without live
  iteration; the particle outline is a reliable stand-in.)

## 0.2.0
- **Phase 1 core.** Claim land with a golden shovel (right-click two corners to add a rectangle,
  sneak + right-click to carve to any shape); claims are protected from block break/place and
  container theft by non-owners, while doors/gates/buttons/levers/pressure plates and shop (villager)
  trading stay open so a claimed area is still shoppable.
- Admin **claim zones** gate where claiming is allowed (`/claim zone add|list|remove`, ops); with no
  zones set, players may claim anywhere.
- Configurable limits in `config/shopguard.json` (max area per claim, max total per player), plus
  no-overlap enforcement.
- Commands: `/claim info|list|remove|trust|untrust`, `/claim admin remove`.
- Not yet runtime-tested; client-rendered claim outlines and grief-hardening (explosions, mob
  griefing, fire/fluid) are still to come.

## 0.1.0
- Project scaffold: Fabric 26.2 land-claim grief-protection mod (ShopGuard).
