package com.eleco;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuración en config/eleco.json (se crea sola). */
public class EcoConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EcoConfig instance = new EcoConfig();

    /** Duración de cada día del mod, en ticks (24000 = 20 min). Total 5 días = 100 min. */
    public int dayLengthTicks = 24000;
    /** Segundos medios de vida de una antorcha encendida (baja con los días). */
    public int torchAverageSeconds = 100;
    /** Segundos base entre dobles (se reduce cada día). */
    public int doppelgangerBaseSeconds = 110;
    /** Probabilidad base por segundo de un susurro (se multiplica por el día). */
    public double whisperChancePerSecond = 0.012;
    /** Distancia de niebla (bloques) por día en el cliente: días 1..5. */
    public float[] fogEndByDay = {52f, 36f, 26f, 17f, 10f};

    public static EcoConfig get() { return instance; }

    public static void load() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("eleco.json");
        try {
            if (Files.exists(p)) {
                EcoConfig c = GSON.fromJson(Files.readString(p), EcoConfig.class);
                if (c != null) {
                    instance = c;
                    if (instance.fogEndByDay == null || instance.fogEndByDay.length < 5) instance.fogEndByDay = new EcoConfig().fogEndByDay;
                    if (instance.dayLengthTicks < 200) instance.dayLengthTicks = 200;
                }
            }
            Files.writeString(p, GSON.toJson(instance));
        } catch (IOException | RuntimeException e) {
            ElEco.LOGGER.warn("No se pudo leer/escribir eleco.json, uso valores por defecto", e);
        }
    }
}
