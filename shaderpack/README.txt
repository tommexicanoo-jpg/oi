EL ECO SHADERS (basado en Luma Lite 1.1)
========================================
Shaderpack OPCIONAL para el mod "El Eco" (Fabric 1.21.1). El mod funciona igual sin shaders
(niebla y noche eterna se hacen desde el propio mod). Con shaders se ve mejor:

- Noche eterna fija, cielo cubierto por la bruma (sin sol ni atardeceres)
- Niebla exponencial que se espesa del Dia 1 al Dia 5 (gris verdoso -> rojo sangre)
- Colores desaturados, grano de pelicula y vineta que crece con los dias
- Antorchas con parpadeo organico + luz dinamica en la mano
- Mantiene sombras suaves, agua, viento, etc. de Luma Lite (perfiles Bajo/Medio/Alto)

INSTALACION: Iris + Sodium. Copia el .zip SIN descomprimir a /shaderpacks, o usa en el juego
el comando  /ecoshader install  (lo copia por ti). Despues elige "ElEco-Shaders".

SINCRONIZACION DE DIA: el mod codifica el dia en el canal azul del color de niebla
(b = 0.03 + 0.004*dia). Si en tu version de Iris no cambia el color con los dias, apaga
"Detectar dia del mod" en Opciones > El Eco y elige el dia a mano (/ecoshader status te dice el dia).
