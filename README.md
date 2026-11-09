# Highlighter

Clientseitige Fabric-Mod für **Minecraft Java 26.3** („Wilderness Bound"), die ausgewählte Spieler
leuchten lässt – inklusive der Vanilla-Umrisslinie, die man **durch Wände** sieht.

* `/highlight user <Name>` → Spieler leuchtet (Befehl erneut = aus)
* `/highlight near` → alle Spieler in der Nähe leuchten, auch solche, die erst später dazukommen (erneut = aus)
* Die Auswahl wird in `config/highlighter.json` gespeichert und **überlebt Neustarts**
* Chat-Feedback im Format `[HL] Tobi ist in der Nähe.` – Klammern grau, `HL` gelb, Rest rot

Die Mod ist **rein clientseitig**. Es werden keine Pakete gesendet, der Server merkt nichts davon.

---

## Voraussetzungen

| Was | Version |
|---|---|
| Minecraft Java | `26.3` |
| Fabric Loader | `>= 0.19.5` (entwickelt gegen `0.20.1`) |
| Fabric API | `0.165.x+26.3` (entwickelt gegen `0.165.2+26.3`) |
| Java | `25` (26.x läuft nicht mehr auf Java 21) |

## Installation

1. Fabric Loader für 26.3 installieren.
2. [Fabric API](https://modrinth.com/mod/fabric-api) in den `mods`-Ordner legen.
3. Die gebaute `highlighter-1.0.0.jar` (siehe [Bauen](#bauen)) ebenfalls in `mods` legen.
4. Spiel starten – im Log steht `[HL] Highlighter ready (...)`.

## Befehle

| Befehl | Wirkung |
|---|---|
| `/highlight user <Name>` | Schaltet die Hervorhebung eines Spielers an/aus |
| `/highlight near` | Schaltet den Near-Modus an/aus |
| `/highlight list` | Zeigt alle fest hervorgehobenen Spieler |
| `/highlight clear` | Entfernt alle festen Hervorhebungen |
| `/highlight radius <1-512>` | Ändert den Umkreis für `near` (Standard: 64 Blöcke) |
| `/highlight` | Zeigt Status und Kurzhilfe |

Tab-Completion schlägt die Namen der Spieler vor, die gerade online sind.

Beispiele:

```
/highlight user Tobi     →  [HL] Tobi wird hervorgehoben.
/highlight user Tobi     →  [HL] Tobi wird nicht mehr hervorgehoben.
/highlight near          →  [HL] Near an – alle Spieler im Umkreis von 64 Blöcken leuchten.
                             [HL] Tobi ist in der Nähe.        (sobald er in Reichweite kommt)
/highlight near          →  [HL] Near aus.
```

## Chat-Farben

`[` und `]` in Grau, `HL` in Gelb, der Text dahinter in Rot – genau wie gewünscht:

```java
Component.literal("[").withStyle(ChatFormatting.GRAY)
    .append(Component.literal("HL").withStyle(ChatFormatting.YELLOW))
    .append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
    .append(Component.literal("Tobi ist in der Nähe.").withStyle(ChatFormatting.RED));
```

## Speicherung

`config/highlighter.json` (Fabric-Konfigordner), z. B.:

```json
{
  "users": ["Tobi", "Anna"],
  "near": false,
  "radius": 64.0
}
```

Jede Änderung wird sofort geschrieben, gelesen wird beim Spielstart. Gelöschte Datei = alles aus.
Die reinen „ist in der Nähe"-Meldungen werden **nicht** gespeichert – nach einem Neustart meldet der
Near-Modus die Spieler in Reichweite erneut.

## Wie der Glow funktioniert

26.3 rendert Umrisse über den Render-State: `EntityRenderer` fragt
`Minecraft#shouldEntityAppearGlowing(entity)`, das wiederum
`Entity#isCurrentlyGlowing()` aufruft. Auf dem Client ist das nur das „glowing"-Flag (Bit 6).
Der Mixin (`EntityGlowMixin`) setzt an dieser Stelle `true` für passende Spieler:

```java
@Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
private void highlighter$forceGlow(CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof Player player && HighlightManager.isHighlighted(player)) {
        cir.setReturnValue(true);
    }
}
```

Deshalb:

* es ist exakt der Vanilla-Umriss (auch durch Wände),
* es werden keine Entity-Daten verändert und nichts an den Server geschickt,
* die Entscheidung fällt jeden Frame neu → Toggeln wirkt sofort, ohne Sync-Verzögerung.

### Besonderheit bei 26.x

Seit 26.1 ist Minecraft **unobfuskiert** (offizielle Mojang-Namen, keine Yarn-Mappings). Ein Mod für
26.x benutzt deshalb das **nicht-remappende** Loom-Plugin:

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}
dependencies {
    minecraft "com.mojang:minecraft:26.3"
    implementation "net.fabricmc:fabric-api:0.165.2+26.3"   // implementation, nicht modImplementation
}
```

Es gibt keine `mappings`-Zeile und kein `remapJar`. Ein Mod, der für 26.2 gebaut wurde, läuft auf
26.3 nicht ohne Neukompilierung – die Version in `fabric.mod.json` ist deshalb auf `~26.3` gesetzt.

## Bauen

Voraussetzungen: **JDK 25** (Gradle Wrapper 9.8.2 ist enthalten, Loom `1.19-SNAPSHOT`).

```bash
./gradlew build
# Ergebnis: build/libs/highlighter-1.0.0.jar
```

Zum Ausprobieren direkt aus der IDE/Shell:

```bash
./gradlew runClient
```

Zusätzlich liegt unter `.github/workflows/build.yml` eine GitHub Action, die bei jedem Push baut und
den Jar als Artifact hochlädt (Actions → „build" → Artifacts → `highlighter-jar`).

> **Hinweis:** Diese Quellen wurden in einer Sandbox ohne JDK und ohne Zugriff auf Maven/Fabric-Repos
> erstellt – dort konnte also nicht kompiliert werden. Alle verwendeten APIs wurden gegen echte
> 26.3-Quellen geprüft (`Entity#isCurrentlyGlowing`, `Minecraft#gui.hud.getChat()`,
> `FabricClientCommandSource#sendFeedback`, `ClientCommands`, `ChatFormatting`, `ClientLevel#players`,
> `FabricLoader#getConfigDir`). Ein `./gradlew build` bzw. der CI-Lauf ist der letzte Verifikationsschritt.

## Grenzen / Fairness

* Nur Spieler können hervorgehoben werden (keine Mobs/Items).
* Der Effekt ist rein visuell und lokal – andere Spieler sehen ihn nicht.
* Auf Servern mit Anti-Cheat kann eine solche Client-Mod als unerlaubter Vorteil gelten. Benutzung
  auf fremden Servern auf eigene Verantwortung.
* Getestet/gebaut für genau `26.3`; andere 26.x-Versionen brauchen einen Neubau.
