# El Eco — mod de terror psicológico de 5 días (Fabric 1.21.1)

> *Nunca anochece. Tampoco amanece. Algo te mira desde la bruma.*

## Qué hace
| Sistema | Implementación |
|---|---|
| **Oscuridad eterna** | `data/minecraft/dimension_type/overworld.json` (`fixed_time: 18000`) + respaldo por código (gamerules). Niebla/color tétrico por Mixin (`BackgroundRendererMixin`), funciona **sin shaders**. |
| **5 días** | `EcoState` + `EcoDirector`: cada día dura `dayLengthTicks` (24000 = 20 min). Niebla, susurros, ojos, dobles y Observador escalan; el **Día 5** es persecución activa. |
| **Observador de la Bruma** | `ObserverEntity`: acosador días 1-4 (solo se mueve si no lo miras, huye si te acercas), persecución día 5. Modelo alto con decenas de ojos rojos emisivos. |
| **Ojos en la oscuridad** | Partícula `eleco:eyes` (emisiva) sobre hojas, paneles de vidrio y superficies oscuras; se apagan al acercarte. |
| **Dobles** | `DoppelgangerEntity`: skin (por UUID), armadura, objetos y postura del jugador. Silencioso, inmóvil, te mira. Si te acercas (<6 bloques) o apartas la vista tras haberlo visto, se desvanece y deja un **eco hueco** (silueta translúcida 5 s). Aparece en pasillos, tras árboles o junto a ventanas. |
| **Antorchas** | Las colocadas (hasta 10 bloques) y la que llevas en la mano parpadean o se apagan al azar (más rápido cada día). Antorcha apagada: bloque/ítem propio, se enciende con pedernal y acero, o crafteando con un pedernal. |
| **Estructuras** | `gothic_mansion`, `abandoned_carriage`, `ruined_chapel` (generación procedural en `EcoPiece`, JSON en `data/eleco/worldgen`). |
| **Shaderpack opcional** | Carpeta `shaderpack/` (Luma Lite adaptado). `gradle zipShaderpack` → `build/distributions/ElEco-Shaders.zip`, o en el juego `/ecoshader install`. |

## Compilar
Requisitos: **JDK 21**.
```bash
gradle wrapper --gradle-version 8.8    # solo la primera vez (genera gradlew + wrapper jar)
./gradlew build                         # -> build/libs/el-eco-1.0.0.jar
./gradlew zipShaderpack                 # -> build/distributions/ElEco-Shaders.zip
./gradlew runClient                     # probar en desarrollo
```
GitHub Actions (`.github/workflows/build.yml`) compila el jar y el zip del shaderpack en cada push.

## Comandos (op nivel 2)
`/eco day <1-5>` · `/eco status` · `/eco observer` · `/eco doppelganger`  ·  cliente: `/ecoshader install|status`

## Configuración `config/eleco.json`
`dayLengthTicks`, `torchAverageSeconds`, `doppelgangerBaseSeconds`, `whisperChancePerSecond`, `fogEndByDay` (cliente).

## Notas
- La noche eterna requiere **mundo nuevo** (el dimension_type se aplica al crearlo). En mundos existentes, el respaldo por código detiene el ciclo y fija la hora.
- Minecraft no expone el parpadeo real del jugador: "parpadear/mirar a otro lado" se implementa como apartar la vista del doble.
- Los susurros usan sonidos vanilla graves (sin audio propio); puedes sustituirlos añadiendo `sounds.json`.
- El shader lee el día del mod por el canal azul del color de niebla; si tu Iris no lo capta, desactiva *Detectar día del mod* y elige el día a mano.
