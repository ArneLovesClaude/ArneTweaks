package com.arnestyles.client.essentials;

/** Ein Regler im Menue (Essentials, Mods, Sounds). */
public interface SettingSlider {
    String nameKey();

    String descriptionKey();

    /** Position des Reglers von 0 bis 1. */
    float fraction();

    /** Wert ueber die Reglerposition setzen (0 bis 1). */
    void setFraction(float fraction);

    /** Zurueck auf den Standardwert. */
    void reset();

    /** Anzeige im Menue, z. B. "45%" oder "1.5 s". */
    String display();
}
