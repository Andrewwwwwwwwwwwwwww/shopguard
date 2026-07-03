# ShopGuard — CurseForge & Modrinth upload kit

Everything to create both project pages and upload the first file. **Nothing here is a placeholder —
all links are live.**

**Files in this folder**
- `shopguard-0.8.1+mc26.2.jar` — the file to upload (50 KB)
- `icon-512.png` — 512×512 project icon (golden shovel)
- `DESCRIPTION.md` — the page body, ready to paste (Markdown → Modrinth directly; CF editor accepts it)
- `UPLOAD.md` — this document (summary, links, every field, checklist)

---

## Name & summary

- **Name:** `ShopGuard`
- **Slug / URL:** `shopguard`  (free on CurseForge; check on Modrinth)

**Summary / tagline** (fits CF's 255 and Modrinth's 256 character limits — paste as-is):
```
Land-claim grief protection for Fabric. Carve claims to any shape with a golden shovel; protect them from players, explosions, fire, pistons and fluids; and let admins gate where players can build. Fully server-side — vanilla clients supported.
```

**Description:** see `DESCRIPTION.md` in this folder — paste the whole file into the page body.

---

## Links (all live)

| Field | URL |
|---|---|
| **Source / Repository** | https://github.com/Andrewwwwwwwwwwwwwww/shopguard |
| **Issues / Bug tracker** | https://github.com/Andrewwwwwwwwwwwwwww/shopguard/issues |
| **Release (this build)** | https://github.com/Andrewwwwwwwwwwwwwww/shopguard/releases/tag/v0.8.1 |
| Wiki | *(none)* |
| Discord | *(none — leave blank, or add your server invite)* |

---

## Project settings

| Field | Value |
|---|---|
| **Mod loader** | Fabric |
| **Minecraft version** | 26.2 |
| **Java** | 25 (ships with MC 26.2 — nothing for users to install) |
| **License** | All Rights Reserved (on Modrinth pick "All Rights Reserved (ARR)") |
| **Icon** | `icon-512.png` |
| **CurseForge category** | Server Utility (optionally also Miscellaneous) |
| **Modrinth tags** | Management, Utility (optionally Social, Game Mechanics) |
| **Client side** | Unsupported |
| **Server side** | Required |

> **Side explained:** the mod runs entirely on the server; players join with a **vanilla client**. It
> still works in single-player because SP runs an internal server. If you'd rather SP users not see a
> "client unsupported" warning, set **Client: Optional** instead.

---

## File / version upload

| Field | Value |
|---|---|
| **File** | `shopguard-0.8.1+mc26.2.jar` |
| **Version number** | `0.8.1` |
| **Display name** | ShopGuard 0.8.1 (MC 26.2 Fabric) |
| **Release channel** | **Beta** (see "Before you publish") |
| **Game version** | 26.2 |
| **Loader** | Fabric |
| **Required dependency** | **Fabric API** (search & link it on each platform) |

**Changelog** (paste into the file's changelog box):
```
0.8.1
- Non-owners can no longer place liquids in a claim; buckets are checked where the liquid lands.
- Fluids (water/lava) can't flow across a claim border.
- Admins have no claim-size limit.

Includes explosion, fire, and piston protection for claimed blocks (0.8.0).
```

---

## Before you publish

- **This is a Beta.** Piston protection is confirmed working in-game; the explosion/fire/fluid protections
  and the full two-player protection flow are built and compile cleanly but haven't had a complete live
  test pass yet. Upload on the **Beta** channel (or run a server test first) rather than a stable Release.
- **Stay below 1.0.0.** Reserve **1.0.0** for the tested, ready release; keep shipping `0.x` betas until then.
- **All Rights Reserved:** third parties can't include ShopGuard in their modpacks without your permission
  (your own pack is fine). Modrinth requires you to pick a license — choose **All Rights Reserved (ARR)**.
- **26.2 is brand new:** if a platform's game-version dropdown doesn't list 26.2 yet, you may have to wait
  for them to add it before the file will accept that tag.

---

## Checklist

**CurseForge**
1. Create project → Name `ShopGuard`, slug `shopguard`, category **Server Utility**, upload `icon-512.png`
2. Summary (above) + paste `DESCRIPTION.md` into the description
3. Settings: License **All Rights Reserved**; add Source/Issues links above
4. Upload `shopguard-0.8.1+mc26.2.jar` → type **Beta**, game version **26.2**, loader **Fabric**
5. Add **Fabric API** as a **Required** dependency

**Modrinth**
1. Create project (type: **Mod**) → Name `ShopGuard`, slug `shopguard`, upload `icon-512.png`
2. Summary (above) + paste `DESCRIPTION.md` into the body
3. Environment: **Client Unsupported, Server Required**; Tags **Management, Utility**; License **ARR**
4. Links: Source + Issues (above)
5. Create version `0.8.1` → channel **Beta**, loader **Fabric**, game version **26.2**, paste changelog
6. Add **Fabric API** as a **Required** dependency, then publish
