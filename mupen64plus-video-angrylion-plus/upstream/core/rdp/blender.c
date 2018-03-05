static int32_t blenderone = 0xff;

static uint8_t bldiv_hwaccurate_table[0x8000];

static INLINE void set_blender_input(struct rdp_state* rdp, int cycle, int which, int32_t **input_r, int32_t **input_g, int32_t **input_b, int32_t **input_a, int a, int b)
{

    switch (a & 0x3)
    {
        case 0:
        {
            if (cycle == 0)
            {
                *input_r = &rdp->pixel_color.r;
                *input_g = &rdp->pixel_color.g;
                *input_b = &rdp->pixel_color.b;
            }
            else
            {
                *input_r = &rdp->blended_pixel_color.r;
                *input_g = &rdp->blended_pixel_color.g;
                *input_b = &rdp->blended_pixel_color.b;
            }
            break;
        }

        case 1:
        {
            *input_r = &rdp->memory_color.r;
            *input_g = &rdp->memory_color.g;
            *input_b = &rdp->memory_color.b;
            break;
        }

        case 2:
        {
            *input_r = &rdp->blend_color.r;      *input_g = &rdp->blend_color.g;      *input_b = &rdp->blend_color.b;
            break;
        }

        case 3:
        {
            *input_r = &rdp->fog_color.r;        *input_g = &rdp->fog_color.g;        *input_b = &rdp->fog_color.b;
            break;
        }
    }

    if (which == 0)
    {
        switch (b & 0x3)
        {
            case 0:     *input_a = &rdp->pixel_color.a; break;
            case 1:     *input_a = &rdp->fog_color.a; break;
            case 2:     *input_a = &rdp->shade_color.a; break;
            case 3:     *input_a = &zero_color; break;
        }
    }
    else
    {
        switch (b & 0x3)
        {
            case 0:     *input_a = &rdp->inv_pixel_color.a; break;
            case 1:     *input_a = &rdp->memory_color.a; break;
            case 2:     *input_a = &blenderone; break;
            case 3:     *input_a = &zero_color; break;
        }
    }
}

static STRICTINLINE int alpha_compare(struct rdp_state* rdp, int32_t comb_alpha)
{
    int32_t threshold;
    if (!rdp->other_modes.alpha_compare_en)
        return 1;
    else
    {
        if (!rdp->other_modes.dither_alpha_en)
            threshold = rdp->blend_color.a;
        else
            threshold = irand(&rdp->seed_dp) & 0xff;


        if (comb_alpha >= threshold)
            return 1;
        else
            return 0;
    }
}

static STRICTINLINE void blender_equation_cycle0(struct rdp_state* rdp, int* r, int* g, int* b)
{
    int blend1a, blend2a;
    int blr, blg, blb, sum;
    blend1a = *rdp->blender1b_a[0] >> 3;
    blend2a = *rdp->blender2b_a[0] >> 3;

    int mulb;



    if (rdp->blender2b_a[0] == &rdp->memory_color.a)
    {
        blend1a = (blend1a >> rdp->blshifta) & 0x3C;
        blend2a = (blend2a >> rdp->blshiftb) | 3;
    }

    mulb = blend2a + 1;


    blr = (*rdp->blender1a_r[0]) * blend1a + (*rdp->blender2a_r[0]) * mulb;
    blg = (*rdp->blender1a_g[0]) * blend1a + (*rdp->blender2a_g[0]) * mulb;
    blb = (*rdp->blender1a_b[0]) * blend1a + (*rdp->blender2a_b[0]) * mulb;



    if (!rdp->other_modes.force_blend)
    {





        sum = ((blend1a & ~3) + (blend2a & ~3) + 4) << 9;
        *r = bldiv_hwaccurate_table[sum | ((blr >> 2) & 0x7ff)];
        *g = bldiv_hwaccurate_table[sum | ((blg >> 2) & 0x7ff)];
        *b = bldiv_hwaccurate_table[sum | ((blb >> 2) & 0x7ff)];
    }
    else
    {
        *r = (blr >> 5) & 0xff;
        *g = (blg >> 5) & 0xff;
        *b = (blb >> 5) & 0xff;
    }
}

static STRICTINLINE void blender_equation_cycle0_2(struct rdp_state* rdp, int* r, int* g, int* b)
{
    int blend1a, blend2a;
    blend1a = *rdp->blender1b_a[0] >> 3;
    blend2a = *rdp->blender2b_a[0] >> 3;

    if (rdp->blender2b_a[0] == &rdp->memory_color.a)
    {
        blend1a = (blend1a >> rdp->pastblshifta) & 0x3C;
        blend2a = (blend2a >> rdp->pastblshiftb) | 3;
    }

    blend2a += 1;
    *r = (((*rdp->blender1a_r[0]) * blend1a + (*rdp->blender2a_r[0]) * blend2a) >> 5) & 0xff;
    *g = (((*rdp->blender1a_g[0]) * blend1a + (*rdp->blender2a_g[0]) * blend2a) >> 5) & 0xff;
    *b = (((*rdp->blender1a_b[0]) * blend1a + (*rdp->blender2a_b[0]) * blend2a) >> 5) & 0xff;
}

static STRICTINLINE void blender_equation_cycle1(struct rdp_state* rdp, int* r, int* g, int* b)
{
    int blend1a, blend2a;
    int blr, blg, blb, sum;
    blend1a = *rdp->blender1b_a[1] >> 3;
    blend2a = *rdp->blender2b_a[1] >> 3;

    int mulb;
    if (rdp->blender2b_a[1] == &rdp->memory_color.a)
    {
        blend1a = (blend1a >> rdp->blshifta) & 0x3C;
        blend2a = (blend2a >> rdp->blshiftb) | 3;
    }

    mulb = blend2a + 1;
    blr = (*rdp->blender1a_r[1]) * blend1a + (*rdp->blender2a_r[1]) * mulb;
    blg = (*rdp->blender1a_g[1]) * blend1a + (*rdp->blender2a_g[1]) * mulb;
    blb = (*rdp->blender1a_b[1]) * blend1a + (*rdp->blender2a_b[1]) * mulb;

    if (!rdp->other_modes.force_blend)
    {
        sum = ((blend1a & ~3) + (blend2a & ~3) + 4) << 9;
        *r = bldiv_hwaccurate_table[sum | ((blr >> 2) & 0x7ff)];
        *g = bldiv_hwaccurate_table[sum | ((blg >> 2) & 0x7ff)];
        *b = bldiv_hwaccurate_table[sum | ((blb >> 2) & 0x7ff)];
    }
    else
    {
        *r = (blr >> 5) & 0xff;
        *g = (blg >> 5) & 0xff;
        *b = (blb >> 5) & 0xff;
    }
}

static STRICTINLINE int blender_1cycle(struct rdp_state* rdp, uint32_t* fr, uint32_t* fg, uint32_t* fb, int dith, uint32_t blend_en, uint32_t prewrap, uint32_t curpixel_cvg, uint32_t curpixel_cvbit)
{
    int r, g, b, dontblend;


    if (alpha_compare(rdp, rdp->pixel_color.a))
    {






        if (rdp->other_modes.antialias_en ? curpixel_cvg : curpixel_cvbit)
        {

            if (!rdp->other_modes.color_on_cvg || prewrap)
            {
                dontblend = (rdp->other_modes.f.partialreject_1cycle && rdp->pixel_color.a >= 0xff);
                if (!blend_en || dontblend)
                {
                    r = *rdp->blender1a_r[0];
                    g = *rdp->blender1a_g[0];
                    b = *rdp->blender1a_b[0];
                }
                else
                {
                    rdp->inv_pixel_color.a =  (~(*rdp->blender1b_a[0])) & 0xff;





                    blender_equation_cycle0(rdp, &r, &g, &b);
                }
            }
            else
            {
                r = *rdp->blender2a_r[0];
                g = *rdp->blender2a_g[0];
                b = *rdp->blender2a_b[0];
            }

            if (rdp->other_modes.rgb_dither_sel != 3)
                rgb_dither(rdp->other_modes.rgb_dither_sel, &r, &g, &b, dith);

            *fr = r;
            *fg = g;
            *fb = b;
            return 1;
        }
        else
            return 0;
        }
    else
        return 0;
}

static STRICTINLINE int blender_2cycle(struct rdp_state* rdp, uint32_t* fr, uint32_t* fg, uint32_t* fb, int dith, uint32_t blend_en, uint32_t prewrap, uint32_t curpixel_cvg, uint32_t curpixel_cvbit, int32_t acalpha)
{
    int r, g, b, dontblend;


    if (alpha_compare(rdp, acalpha))
    {
        if (rdp->other_modes.antialias_en ? (curpixel_cvg) : (curpixel_cvbit))
        {

            rdp->inv_pixel_color.a =  (~(*rdp->blender1b_a[0])) & 0xff;
            blender_equation_cycle0_2(rdp, &r, &g, &b);


            rdp->memory_color = rdp->pre_memory_color;

            rdp->blended_pixel_color.r = r;
            rdp->blended_pixel_color.g = g;
            rdp->blended_pixel_color.b = b;
            rdp->blended_pixel_color.a = rdp->pixel_color.a;

            if (!rdp->other_modes.color_on_cvg || prewrap)
            {
                dontblend = (rdp->other_modes.f.partialreject_2cycle && rdp->pixel_color.a >= 0xff);
                if (!blend_en || dontblend)
                {
                    r = *rdp->blender1a_r[1];
                    g = *rdp->blender1a_g[1];
                    b = *rdp->blender1a_b[1];
                }
                else
                {
                    rdp->inv_pixel_color.a =  (~(*rdp->blender1b_a[1])) & 0xff;
                    blender_equation_cycle1(rdp, &r, &g, &b);
                }
            }
            else
            {
                r = *rdp->blender2a_r[1];
                g = *rdp->blender2a_g[1];
                b = *rdp->blender2a_b[1];
            }


            if (rdp->other_modes.rgb_dither_sel != 3)
                rgb_dither(rdp->other_modes.rgb_dither_sel, &r, &g, &b, dith);
            *fr = r;
            *fg = g;
            *fb = b;
            return 1;
        }
        else
        {
            rdp->memory_color = rdp->pre_memory_color;
            return 0;
        }
    }
    else
    {
        rdp->memory_color = rdp->pre_memory_color;
        return 0;
    }
}

static void blender_init_lut(void)
{
    int d = 0, n = 0, temp = 0, res = 0, invd = 0, nbit = 0;
    int ps[9];
    for (int i = 0; i < 0x8000; i++)
    {
        res = 0;
        d = (i >> 11) & 0xf;
        n = i & 0x7ff;
        invd = (~d) & 0xf;


        temp = invd + (n >> 8) + 1;
        ps[0] = temp & 7;
        for (int k = 0; k < 8; k++)
        {
            nbit = (n >> (7 - k)) & 1;
            if (res & (0x100 >> k))
                temp = invd + (ps[k] << 1) + nbit + 1;
            else
                temp = d + (ps[k] << 1) + nbit;
            ps[k + 1] = temp & 7;
            if (temp & 0x10)
                res |= (1 << (7 - k));
        }
        bldiv_hwaccurate_table[i] = res;
    }
}

static void rdp_set_fog_color(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->fog_color.r = RGBA32_R(args[1]);
    rdp->fog_color.g = RGBA32_G(args[1]);
    rdp->fog_color.b = RGBA32_B(args[1]);
    rdp->fog_color.a = RGBA32_A(args[1]);
}

static void rdp_set_blend_color(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->blend_color.r = RGBA32_R(args[1]);
    rdp->blend_color.g = RGBA32_G(args[1]);
    rdp->blend_color.b = RGBA32_B(args[1]);
    rdp->blend_color.a = RGBA32_A(args[1]);
}
