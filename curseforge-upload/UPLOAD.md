# ShopGuard — CurseForge & Modrinth upload kit

Everything needed to create both project pages and upload the first file. Files in this folder:

- **`shopguard-0.8.1+mc26.2.jar`** — the file to upload (50 KB)
- **`icon-512.png`** — 512×512 project icon (golden shovel)
- **`UPLOAD.md`** — this document

---

## 1. Project basics (both platforms)

| Field | Value |
|---|---|
| **Name** | ShopGuard |
| **Slug / URL** | `shopguard` (confirmed free on CurseForge; check on Modrinth) |
| **Summary / tagline** | Land-claim grief protection — carve claims to any shape, protect them from players and the environment, and let admins gate where claiming is allowed. Fully server-side. |
| **Mod loader** | Fabric |
| **Minecraft version** | 26.2 |
| **Java** | 25 (bundled with MC 26.2 — nothing for users to install) |
| **License** | All Rights Reserved |
| **Icon** | `icon-512.png` in this folder |
| **Author (in jar)** | Andrewwwwwwwwwwwwwww |

**Categories**
- **CurseForge:** Server Utility (primary) — optionally also Miscellaneous
- **Modrinth tags:** Management, Utility (optionally Social, Game Mechanics)

**Environment / side**
- **Server: Required** — the mod runs entirely on the server.
- **Client: Unsupported** — players join with a **vanilla client**; nothing is needed on their end. (It still works in single-player, because single-player runs an internal server. If you'd rather not confuse SP users, set Client to *Optional* instead.)

**Links (optional)**
- Source / Issues: none yet — the GitHub repo hasn't been pushed to a remote. Leave blank, or push the repo first if you want an issue tracker. (License is All Rights Reserved, so you can keep the repo private and still skip the source link.)

---

## 2. Version / file upload

| Field | Value |
|---|---|
| **Version number** | `0.8.1` |
| **Display name** | ShopGuard 0.8.1 (MC 26.2 Fabric) |
| **Release channel** | **Beta** (recommended for the first public build — see "Before you publish") |
| **Game version** | 26.2 |
| **Loader** | Fabric |
| **Dependencies** | **Fabric API** — *Required*. (Add it as a required dependency and link the Fabric API project on each platform. Fabric Loader 0.19.3+ is implied by the game version.) |

**Changelog for this file:**
```
0.8.1
- Non-owners can no longer place liquids in a claim; buckets are checked where the liquid lands.
- Fluids (water/lava) can't flow across a claim border.
- Admins have no claim-size limit.

Includes everything from 0.8.0: explosion, fire, and piston protection for claimed blocks.
```

---

## 3. Description (paste into the page body)

> Modrinth uses Markdown, so the block below pastes in directly. CurseForge has its own rich editor —
> paste it and it will mostly format; you may need to re-apply the headers/bullets.

```markdown
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
```

---

## 4. Before you publish — read this

- **This build has had limited live testing.** Piston protection is confirmed working in-game; the other
  environmental protections (explosion / fire / fluid, all mixin-based) and the two-player protection flow
  are built and compile cleanly but haven't had a full multiplayer test pass. **Upload as Beta**, or run a
  quick server test first, before marking anything as a stable Release.
- **Version ceiling:** we're intentionally staying below `1.0.0`. Reserve **1.0.0** for the "tested and
  ready" release; keep uploading `0.x` betas until then.
- **All Rights Reserved implications:** fine for solo distribution, but third parties can't put ShopGuard
  in their modpacks without your permission. Your own modpack is fine (you own it). Modrinth requires you
  to pick a license during setup — choose **All Rights Reserved (ARR)**.
- **26.2 in the version list:** 26.2 is brand new. If a platform's game-version dropdown doesn't list 26.2
  yet, you may have to wait for them to add it before the file will accept that tag.

## 5. Quick checklist

**CurseForge**
- [ ] Create project → Name "ShopGuard", slug `shopguard`, category **Server Utility**, upload `icon-512.png`
- [ ] Set summary + paste the description
- [ ] Upload `shopguard-0.8.1+mc26.2.jar` → type **Beta**, game version **26.2**, loader **Fabric**
- [ ] Add **Fabric API** as a required dependency
- [ ] License: **All Rights Reserved**

**Modrinth**
- [ ] Create project (type: Mod) → Name "ShopGuard", slug `shopguard`, upload `icon-512.png`
- [ ] Summary + description (Markdown pastes directly)
- [ ] Environment: **Client Unsupported, Server Required**
- [ ] Tags: **Management, Utility**; License **ARR**
- [ ] Create version `0.8.1` → channel **Beta**, loader **Fabric**, game version **26.2**
- [ ] Add **Fabric API** as a required dependency
- [ ] Paste the changelog
