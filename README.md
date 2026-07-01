# ShopGuard

A land-claim / grief-protection mod for Fabric (Minecraft 26.2).

Claim a region with a golden shovel and shape it to fit your build — right-click two corners to
add a rectangle, sneak + right-click to carve pieces out. Inside a claim, non-owners can't break or
place blocks or open the owner's containers, but they can still walk through (doors/gates/buttons)
and trade at shops — so a protected shopping district stays fully shoppable.

Admins can define zones that restrict where claims may be made; with no zones set, players can claim
anywhere (subject to a configurable max size and no overlap with existing claims).

Protection is fully server-side (vanilla clients are protected). An optional client module renders
claim outlines; without it you're still protected, you just don't see the glowing boundary.
