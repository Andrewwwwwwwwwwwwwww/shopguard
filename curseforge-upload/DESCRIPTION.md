# ShopGuard

**Land-claim grief protection for Fabric.** Claim land with a golden shovel and carve it to any shape,
protect it from players *and* the environment, and let admins control where claiming is allowed.
ShopGuard is **fully server-side** — players connect with a vanilla client and are protected
automatically. Works in single-player too.

## Features

- **Carve-to-fit claims.** Right-click two corners with a golden shovel to claim a rectangle, right-click
  the air to switch to carve mode, then trim it to any shape. Touching claims merge into one.
- **Real protection.** Non-owners can't break or place blocks, open containers, or disturb item frames,
  armor stands, or paintings inside a claim.
- **Shops stay open.** Visitors can still use doors, gates, trapdoors, and pressure plates to walk
  through, and trade with villagers — so a claimed shopping district keeps working. Buttons and levers
  are blocked for visitors by default (configurable).
- **Environmental protection.** Claimed blocks survive explosions (TNT, creepers, ghasts, end crystals,
  wither, beds/anchors), fire, piston pushes, and fluids flowing across the border.
- **Admin zones.** Ops mark where players are allowed to claim with a golden hoe (or a command). With no
  zones set, players can claim anywhere.
- **See what's claimed.** `/claim show` outlines everything nearby with particles — your claims in green,
  other players' in orange, and admin zones in dark red — so you always know what land is taken.
- **Trust.** Give friends full build access to your claim.
- **Configurable limits.** Cap the size of a single claim and the total per player.

## Commands

| Command | Who | What |
|---|---|---|
| `/claim` | anyone | Your claims and how much you've used |
| `/claim show` | anyone | Toggle the claim/zone outlines |
| `/claim remove` | owner | Remove the claim you're standing in |
| `/claim trust <player>` | owner | Toggle a player's build access |
| `/claim zone add \| list \| remove` | ops | Manage the zones players may claim in |

## Getting started

1. Grab a **golden shovel** and right-click two corners to make your first claim.
2. Right-click the air to toggle **carve** mode and trim the claim to fit.
3. Use `/claim show` to see claim and zone borders around you.

## Requirements

- **Fabric Loader** 0.19.3+
- **Fabric API**

Only the **server** needs ShopGuard — players can join with a vanilla client.

## Links

- **Source:** https://github.com/Andrewwwwwwwwwwwwwww/shopguard
- **Report a bug:** https://github.com/Andrewwwwwwwwwwwwwww/shopguard/issues
