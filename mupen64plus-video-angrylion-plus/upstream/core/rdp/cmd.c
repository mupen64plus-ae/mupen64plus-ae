// maximum number of commands to buffer for parallel processing
#define CMD_BUFFER_SIZE 1024

// maximum data size of a single command in bytes
#define CMD_MAX_SIZE 176

// maximum data size of a single command in 32 bit integers
#define CMD_MAX_INTS (CMD_MAX_SIZE / sizeof(int32_t))

// extracts the command ID from a command buffer
#define CMD_ID(cmd) ((*(cmd) >> 24) & 0x3f)

// list of command IDs
#define CMD_ID_NO_OP                           0x00
#define CMD_ID_FILL_TRIANGLE                   0x08
#define CMD_ID_FILL_ZBUFFER_TRIANGLE           0x09
#define CMD_ID_TEXTURE_TRIANGLE                0x0a
#define CMD_ID_TEXTURE_ZBUFFER_TRIANGLE        0x0b
#define CMD_ID_SHADE_TRIANGLE                  0x0c
#define CMD_ID_SHADE_ZBUFFER_TRIANGLE          0x0d
#define CMD_ID_SHADE_TEXTURE_TRIANGLE          0x0e
#define CMD_ID_SHADE_TEXTURE_Z_BUFFER_TRIANGLE 0x0f
#define CMD_ID_TEXTURE_RECTANGLE               0x24
#define CMD_ID_TEXTURE_RECTANGLE_FLIP          0x25
#define CMD_ID_SYNC_LOAD                       0x26
#define CMD_ID_SYNC_PIPE                       0x27
#define CMD_ID_SYNC_TILE                       0x28
#define CMD_ID_SYNC_FULL                       0x29
#define CMD_ID_SET_KEY_GB                      0x2a
#define CMD_ID_SET_KEY_R                       0x2b
#define CMD_ID_SET_CONVERT                     0x2c
#define CMD_ID_SET_SCISSOR                     0x2d
#define CMD_ID_SET_PRIM_DEPTH                  0x2e
#define CMD_ID_SET_OTHER_MODES                 0x2f
#define CMD_ID_LOAD_TLUT                       0x30
#define CMD_ID_SET_TILE_SIZE                   0x32
#define CMD_ID_LOAD_BLOCK                      0x33
#define CMD_ID_LOAD_TILE                       0x34
#define CMD_ID_SET_TILE                        0x35
#define CMD_ID_FILL_RECTANGLE                  0x36
#define CMD_ID_SET_FILL_COLOR                  0x37
#define CMD_ID_SET_FOG_COLOR                   0x38
#define CMD_ID_SET_BLEND_COLOR                 0x39
#define CMD_ID_SET_PRIM_COLOR                  0x3a
#define CMD_ID_SET_ENV_COLOR                   0x3b
#define CMD_ID_SET_COMBINE                     0x3c
#define CMD_ID_SET_TEXTURE_IMAGE               0x3d
#define CMD_ID_SET_MASK_IMAGE                  0x3e
#define CMD_ID_SET_COLOR_IMAGE                 0x3f

static uint32_t rdp_cmd_buf[CMD_BUFFER_SIZE][CMD_MAX_INTS];
static uint32_t rdp_cmd_buf_pos;

static uint32_t rdp_cmd_pos;
static uint32_t rdp_cmd_id;
static uint32_t rdp_cmd_len;

static void rdp_invalid(struct rdp_state* rdp, const uint32_t* args);
static void rdp_noop(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tri_noshade(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tri_noshade_z(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tri_tex(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tri_tex_z(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tri_shade(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tri_shade_z(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tri_texshade(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tri_texshade_z(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tex_rect(struct rdp_state* rdp, const uint32_t* args);
static void rdp_tex_rect_flip(struct rdp_state* rdp, const uint32_t* args);
static void rdp_sync_load(struct rdp_state* rdp, const uint32_t* args);
static void rdp_sync_pipe(struct rdp_state* rdp, const uint32_t* args);
static void rdp_sync_tile(struct rdp_state* rdp, const uint32_t* args);
static void rdp_sync_full(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_key_gb(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_key_r(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_convert(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_scissor(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_prim_depth(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_other_modes(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_tile_size(struct rdp_state* rdp, const uint32_t* args);
static void rdp_load_block(struct rdp_state* rdp, const uint32_t* args);
static void rdp_load_tlut(struct rdp_state* rdp, const uint32_t* args);
static void rdp_load_tile(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_tile(struct rdp_state* rdp, const uint32_t* args);
static void rdp_fill_rect(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_fill_color(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_fog_color(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_blend_color(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_prim_color(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_env_color(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_combine(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_texture_image(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_mask_image(struct rdp_state* rdp, const uint32_t* args);
static void rdp_set_color_image(struct rdp_state* rdp, const uint32_t* args);

static const struct
{
    void (*handler)(struct rdp_state* rdp, const uint32_t*);   // command handler function pointer
    uint32_t length;                    // command data length in bytes
    bool sync;                          // synchronize all workers before execution
    char name[32];                      // descriptive name for debugging
} rdp_commands[] = {
    {rdp_noop,              8,   false, "No_Op"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_tri_noshade,       32,  false, "Fill_Triangle"},
    {rdp_tri_noshade_z,     48,  false, "Fill_ZBuffer_Triangle"},
    {rdp_tri_tex,           96,  false, "Texture_Triangle"},
    {rdp_tri_tex_z,         112, false, "Texture_ZBuffer_Triangle"},
    {rdp_tri_shade,         96,  false, "Shade_Triangle"},
    {rdp_tri_shade_z,       112, false, "Shade_ZBuffer_Triangle"},
    {rdp_tri_texshade,      160, false, "Shade_Texture_Triangle"},
    {rdp_tri_texshade_z,    176, false, "Shade_Texture_Z_Buffer_Triangle"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_tex_rect,          16,  false, "Texture_Rectangle"},
    {rdp_tex_rect_flip,     16,  false, "Texture_Rectangle_Flip"},
    {rdp_sync_load,         8,   false, "Sync_Load"},
    {rdp_sync_pipe,         8,   false, "Sync_Pipe"},
    {rdp_sync_tile,         8,   false, "Sync_Tile"},
    {rdp_sync_full,         8,   true,  "Sync_Full"},
    {rdp_set_key_gb,        8,   false, "Set_Key_GB"},
    {rdp_set_key_r,         8,   false, "Set_Key_R"},
    {rdp_set_convert,       8,   false, "Set_Convert"},
    {rdp_set_scissor,       8,   false, "Set_Scissor"},
    {rdp_set_prim_depth,    8,   false, "Set_Prim_Depth"},
    {rdp_set_other_modes,   8,   false, "Set_Other_Modes"},
    {rdp_load_tlut,         8,   false, "Load_TLUT"},
    {rdp_invalid,           8,   false, "???"},
    {rdp_set_tile_size,     8,   false, "Set_Tile_Size"},
    {rdp_load_block,        8,   false, "Load_Block"},
    {rdp_load_tile,         8,   false, "Load_Tile"},
    {rdp_set_tile,          8,   false, "Set_Tile"},
    {rdp_fill_rect,         8,   false, "Fill_Rectangle"},
    {rdp_set_fill_color,    8,   false, "Set_Fill_Color"},
    {rdp_set_fog_color,     8,   false, "Set_Fog_Color"},
    {rdp_set_blend_color,   8,   false, "Set_Blend_Color"},
    {rdp_set_prim_color,    8,   false, "Set_Prim_Color"},
    {rdp_set_env_color,     8,   false, "Set_Env_Color"},
    {rdp_set_combine,       8,   false, "Set_Combine"},
    {rdp_set_texture_image, 8,   false, "Set_Texture_Image"},
    {rdp_set_mask_image,    8,   true,  "Set_Mask_Image"},
    {rdp_set_color_image,   8,   true,  "Set_Color_Image"}
};

static void cmd_run(struct rdp_state* rdp, const uint32_t* arg)
{
    uint32_t cmd_id = CMD_ID(arg);
    rdp_commands[cmd_id].handler(rdp, arg);
}

static void cmd_run_buffered(uint32_t worker_id)
{
    for (uint32_t pos = 0; pos < rdp_cmd_buf_pos; pos++) {
        cmd_run(&rdp_states[worker_id], rdp_cmd_buf[pos]);
    }
}

static void cmd_flush(void)
{
    // only run if there's something buffered
    if (rdp_cmd_buf_pos) {
        // let workers run all buffered commands in parallel
        parallel_run(cmd_run_buffered);
        // reset buffer by starting from the beginning
        rdp_cmd_buf_pos = 0;
    }
}

static void cmd_init(void)
{
    rdp_cmd_pos = 0;
    rdp_cmd_id = 0;
    rdp_cmd_len = CMD_MAX_INTS;
}

void rdp_update(void)
{
    uint32_t** dp_reg = plugin_get_dp_registers();
    uint32_t dp_current_al = (*dp_reg[DP_CURRENT] & ~7) >> 2;
    uint32_t dp_end_al = (*dp_reg[DP_END] & ~7) >> 2;

    // don't do anything if the RDP has crashed or the registers are not set up correctly
    if (rdp_pipeline_crashed || dp_end_al <= dp_current_al) {
        return;
    }

    // while there's data in the command buffer...
    while (dp_end_al - dp_current_al > 0) {
        bool xbus_dma = (*dp_reg[DP_STATUS] & DP_STATUS_XBUS_DMA) != 0;
        uint32_t* dmem = (uint32_t*)plugin_get_dmem();
        uint32_t* cmd_buf = rdp_cmd_buf[rdp_cmd_buf_pos];

        // when reading the first int, extract the command ID and update the buffer length
        if (rdp_cmd_pos == 0) {
            if (xbus_dma) {
                cmd_buf[rdp_cmd_pos++] = dmem[dp_current_al++ & 0x3ff];
            } else {
                cmd_buf[rdp_cmd_pos++] = rdram_read_idx32(dp_current_al++);
            }

            rdp_cmd_id = CMD_ID(cmd_buf);
            rdp_cmd_len = rdp_commands[rdp_cmd_id].length >> 2;
        }

        // copy more data from the N64 to the local command buffer
        uint32_t toload = MIN(dp_end_al - dp_current_al, rdp_cmd_len - 1);

        if (xbus_dma) {
            for (uint32_t i = 0; i < toload; i++) {
                cmd_buf[rdp_cmd_pos++] = dmem[dp_current_al++ & 0x3ff];
            }
        } else {
            for (uint32_t i = 0; i < toload; i++) {
                cmd_buf[rdp_cmd_pos++] = rdram_read_idx32(dp_current_al++);
            }
        }

        // if there's enough data for the current command...
        if (rdp_cmd_pos == rdp_cmd_len) {
            // check if parallel processing is enabled
            if (config.parallel) {
                // special case: sync_full always needs to be run in main thread
                if (rdp_cmd_id == CMD_ID_SYNC_FULL) {
                    // first, run all pending commands
                    cmd_flush();

                    // parameters are unused, so NULL is fine
                    rdp_sync_full(NULL, NULL);
                } else {
                    // increment buffer position
                    rdp_cmd_buf_pos++;

                    // flush buffer when it is full or when the current command requires a sync
                    if (rdp_cmd_buf_pos >= CMD_BUFFER_SIZE || rdp_commands[rdp_cmd_id].sync) {
                        cmd_flush();
                    }
                }
            } else {
                // run command directly
                cmd_run(&rdp_states[0], cmd_buf);
            }

            // reset current command buffer to prepare for the next one
            cmd_init();
        }
    }

    // update DP registers to indicate that all bytes have been read
    *dp_reg[DP_START] = *dp_reg[DP_CURRENT] = *dp_reg[DP_END];
}
