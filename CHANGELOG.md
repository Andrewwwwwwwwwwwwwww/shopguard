# Changelog

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
