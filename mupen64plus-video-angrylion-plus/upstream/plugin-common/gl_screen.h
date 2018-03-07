#pragma once

#include "core/rdp.h"

#include <stdint.h>
#include <stdbool.h>

void gl_screen_init(struct rdp_config* config);
bool gl_screen_write(struct rdp_frame_buffer* fb, int32_t output_height);
void gl_screen_read(struct rdp_frame_buffer* fb, bool rgb);
void gl_screen_render(int32_t win_width, int32_t win_height, int32_t win_x, int32_t win_y);
void gl_screen_clear(void);
void gl_screen_close(void);
