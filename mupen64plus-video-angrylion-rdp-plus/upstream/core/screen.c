#include "screen.h"

void screen_toggle_fullscreen(void)
{
    screen_set_fullscreen(!screen_get_fullscreen());
}
