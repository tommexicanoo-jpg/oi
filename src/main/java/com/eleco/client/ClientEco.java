package com.eleco.client;

import com.eleco.EcoConfig;

/** Estado visual del cliente: día recibido del servidor y niebla suavizada. */
public final class ClientEco {
    public static boolean active = false;
    public static int day = 1;
    public static float fogEnd = 60f;
    public static float r = 0.05f, g = 0.06f, b = 0.034f;

    private ClientEco() {}

    public static void setDay(int d) {
        day = Math.max(1, Math.min(5, d));
        active = true;
    }

    public static void reset() { active = false; fogEnd = 60f; }

    /** Se llama cada tick del cliente: acerca la niebla y el color poco a poco al objetivo del día. */
    public static void tick() {
        if (!active) return;
        float[] fe = EcoConfig.get().fogEndByDay;
        float target = fe[day - 1];
        fogEnd += (target - fogEnd) * 0.02f;
        float t = (day - 1) / 4f;
        // gris verdoso -> rojo sangre. El azul codifica el dia para el shaderpack: b = 0.03 + 0.004 * dia
        float tr = 0.050f + (0.120f - 0.050f) * t;
        float tg = 0.058f + (0.012f - 0.058f) * t;
        float tb = 0.03f + 0.004f * day;
        r += (tr - r) * 0.02f; g += (tg - g) * 0.02f; b = tb;
    }
}
