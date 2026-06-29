/* C wrapper so ae_bridge.cpp can set rcheevos internals without pulling rc_compat.h
 * into C++ (where __STDC_VERSION__ is undefined, triggering the C89 compat path). */
#include <rc_client_internal.h>

static int ra_rp_suppress(rc_client_t* client, char buffer[], size_t size) {
    (void)client;
    if (size > 0) buffer[0] = '\0';
    return 1; /* non-zero = use our buffer instead of computing RP */
}

void ra_set_rich_presence_enabled(rc_client_t* client, int enabled) {
    if (!client) return;
    client->callbacks.rich_presence_override = enabled ? NULL : ra_rp_suppress;
}
