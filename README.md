# Randomize Filters Terrain Slabs Fix

A small NeoForge compatibility mod for **Create: Randomize Filters** and
**Countered's Terrain Slabs**.

Makes mechanical rollers use Terrain Slabs' block-to-slab mappings with
**Random Filters** and **Advanced Random Filters**, including grass, dirt paths,
coarse dirt, moss, and snow.

- Uses Terrain Slabs' own mapping table (50 mappings in 3.1.2).
- Prefers Terrain Slabs when a mapping exists; otherwise keeps the original lookup.
- Leaves random selection, weights, material consumption, and paving height unchanged.
- Adds no blocks or recipes.
- Does **not** add Sequence Filter support or change Create's ordinary filters.

## Requirements

Tested with:

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| NeoForge | 21.1.252 |
| Create | 6.0.10 |
| Create: Randomize Filters | 1.0.7 |
| Countered's Terrain Slabs | 3.1.2 |
| Architectury API (required by Terrain Slabs) | 13.0.11 |

Randomize Filters 1.0.7 and Terrain Slabs 3.1.2 are pinned dependencies.
This mod patches a method added by Randomize Filters; other versions may need changes.

## Installation

Download the JAR from [Releases](https://github.com/yuutaro/randomizefilters-terrain-slabs-fix/releases)
and put it in the `mods` folder alongside the required mods.
For multiplayer, install the same version on the server and clients.

Set a Random Filter or Advanced Random Filter on a mechanical roller and supply
the original full blocks. Slabs are placed where the roller's track profile
requires them; flat sections still use full blocks.

GitHub release assets have versioned URLs suitable for packwiz.

## Development

Install JDK 21 (or run `mise install`) and place these third-party JARs in `libs/`:

- `create-1.21.1-6.0.10.jar`
- `createrandomizefilters-1.0.7.jar`
- `terrain_slabs-neoforge-3.1.2.jar`
- `architectury-13.0.11-neoforge.jar`

Dependencies are not bundled or redistributed.

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. Output: `build/libs/`.
The Gradle Wrapper downloads the project-selected Gradle version without changing
your global PATH or Java installation.

### Integration tests

With the dependencies above installed:

```sh
./gradlew runServer -PsmokeTest -PmixinDebugExport
```

Tests run in a disposable server under `run-smoketest/` and stop it afterward.
Before running, set `server-ip=127.0.0.1` in `run-smoketest/server.properties`.
Check for `TERRAIN_SLABS_FIX_SMOKE_PASS`; a Gradle success alone does not prove
that the in-game tests passed.

Tests cover all 50 native mappings, placement and consumption for 38 recipe
materials with both filter types, and unchanged lookups for unmapped blocks.
Test code and third-party JARs are excluded from the release JAR.

For local Prism testing, a symlink from the instance's `mods/` folder to the
built JAR is sufficient. Exit Minecraft before rebuilding, restart afterward,
and update the link when the version changes. Exclude this file from pack syncing.

## License

MIT. The official NeoForge MDK template notice is retained in
[TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).
Third-party mods retain their own licenses.

Template: [MDK-1.21.1-ModDevGradle](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle),
commit `e93d7f09d4eec12c76d1d7be9bf4eaad0246e9e0`.
