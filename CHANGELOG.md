# Changelog

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
