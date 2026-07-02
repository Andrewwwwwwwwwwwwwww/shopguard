# ShopGuard

A land-claim / grief-protection mod for Fabric (Minecraft 26.2). Fully server-side — vanilla
clients are protected and see everything (outlines are particles, not client rendering).

## Claiming (golden shovel)
- **Right-click two corners** → claim that rectangle. Touching claims you own merge into one, so you
  can build up any shape. Corners can be set from up to 64 blocks away by aiming.
- **Right-click the sky** → toggle CLAIM ↔ CARVE mode. In carve mode the two corners **remove** area
  instead — shape the claim to fit your build; carving a claim to nothing deletes it.
- Hold the shovel to see outlines: **green** = your claims, **orange** = other players'.

## Protection
Inside a claim, non-owners can't break or place blocks, open containers, or touch item frames and
armor stands — but they can still walk through (doors, gates, trapdoors, pressure plates) and trade
with villager shops, so a protected shopping district stays fully shoppable. Buttons and levers are
blocked for visitors by default (`allowRedstoneControls` in the config). Pistons can't move blocks
across a claim border. Ops bypass all protection.

## Commands
| Command | Who | Does |
|---|---|---|
| `/claim` | anyone | The claim you're standing in, your claims, and your used/max totals |
| `/claim show` | anyone | Toggle borders: zones dark red, your claims green, others orange |
| `/claim remove` | owner (ops: anyone's) | Remove the claim you're standing in |
| `/claim trust <player>` | owner | Toggle a player's build access on this claim |
| `/claim zone add|list|remove` | ops | Manage claim zones by command (tab-completes coordinates) |

## Admin zones (golden hoe, ops)
Zones define **where players may claim**: no zones = claim anywhere; once any zone exists, claims
must be fully inside one. The golden hoe works exactly like the claim shovel, but on zones —
right-click corners to add, sky-toggle to carve, touching zones merge — and zone borders show
(dark red) while an op holds it. Players use `/claim show` to see where they're allowed to build.

## Config (`config/shopguard.json`)
- `maxClaimArea` — max footprint of a single claim (default 10,000)
- `maxTotalPerPlayer` — max total footprint per player (default 40,000, ops exempt)
- `allowRedstoneControls` — let visitors use buttons/levers in claims (default false)
