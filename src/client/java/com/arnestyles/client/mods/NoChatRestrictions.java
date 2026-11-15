package com.arnestyles.client.mods;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;

import java.util.Set;

/**
 * No Chat Restrictions: Chat, Mehrspieler und Realms gehen auch, wenn das Microsoft-Konto sie sperrt
 * (z. B. durch Xbox-Datenschutz-Einstellungen). Kontosperren bleiben bestehen.
 * Nachgebaut nach No Chat Restrictions (Aizistral), WTFPL.
 */
public final class NoChatRestrictions {
    private static final Set<UserFlag> UNLOCKED = Set.of(UserFlag.CHAT_ALLOWED, UserFlag.SERVERS_ALLOWED, UserFlag.REALMS_ALLOWED);

    private NoChatRestrictions() {
    }

    /** Gibt die Kontoeinstellungen mit freigeschaltetem Chat, Servern und Realms zurueck. Wird bei jeder Abfrage neu geprueft. */
    public static UserProperties unlock(UserProperties properties) {
        if (!ModSetting.Toggle.NO_CHAT_RESTRICTIONS.isActive() || properties.flags().containsAll(UNLOCKED)) {
            return properties;
        }
        Set<UserFlag> flags = ImmutableSet.<UserFlag>builder().addAll(properties.flags()).addAll(UNLOCKED).build();
        return new UserProperties(flags, properties.bannedScopes());
    }
}
