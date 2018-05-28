static uint32_t special_9bit_clamptable[512];
static int32_t special_9bit_exttable[512];

static INLINE void set_suba_rgb_input(struct rdp_state* rdp, int32_t **input_r, int32_t **input_g, int32_t **input_b, int code)
{
    switch (code & 0xf)
    {
        case 0:     *input_r = &rdp->combined_color.r;   *input_g = &rdp->combined_color.g;   *input_b = &rdp->combined_color.b;   break;
        case 1:     *input_r = &rdp->texel0_color.r;     *input_g = &rdp->texel0_color.g;     *input_b = &rdp->texel0_color.b;     break;
        case 2:     *input_r = &rdp->texel1_color.r;     *input_g = &rdp->texel1_color.g;     *input_b = &rdp->texel1_color.b;     break;
        case 3:     *input_r = &rdp->prim_color.r;       *input_g = &rdp->prim_color.g;       *input_b = &rdp->prim_color.b;       break;
        case 4:     *input_r = &rdp->shade_color.r;      *input_g = &rdp->shade_color.g;      *input_b = &rdp->shade_color.b;      break;
        case 5:     *input_r = &rdp->env_color.r;        *input_g = &rdp->env_color.g;        *input_b = &rdp->env_color.b;        break;
        case 6:     *input_r = &one_color;          *input_g = &one_color;          *input_b = &one_color;      break;
        case 7:     *input_r = &rdp->noise;              *input_g = &rdp->noise;              *input_b = &rdp->noise;              break;
        case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
        {
            *input_r = &zero_color;     *input_g = &zero_color;     *input_b = &zero_color;     break;
        }
    }
}

static INLINE void set_subb_rgb_input(struct rdp_state* rdp, int32_t **input_r, int32_t **input_g, int32_t **input_b, int code)
{
    switch (code & 0xf)
    {
        case 0:     *input_r = &rdp->combined_color.r;   *input_g = &rdp->combined_color.g;   *input_b = &rdp->combined_color.b;   break;
        case 1:     *input_r = &rdp->texel0_color.r;     *input_g = &rdp->texel0_color.g;     *input_b = &rdp->texel0_color.b;     break;
        case 2:     *input_r = &rdp->texel1_color.r;     *input_g = &rdp->texel1_color.g;     *input_b = &rdp->texel1_color.b;     break;
        case 3:     *input_r = &rdp->prim_color.r;       *input_g = &rdp->prim_color.g;       *input_b = &rdp->prim_color.b;       break;
        case 4:     *input_r = &rdp->shade_color.r;      *input_g = &rdp->shade_color.g;      *input_b = &rdp->shade_color.b;      break;
        case 5:     *input_r = &rdp->env_color.r;        *input_g = &rdp->env_color.g;        *input_b = &rdp->env_color.b;        break;
        case 6:     *input_r = &rdp->key_center.r;       *input_g = &rdp->key_center.g;       *input_b = &rdp->key_center.b;       break;
        case 7:     *input_r = &rdp->k4;                 *input_g = &rdp->k4;                 *input_b = &rdp->k4;                 break;
        case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
        {
            *input_r = &zero_color;     *input_g = &zero_color;     *input_b = &zero_color;     break;
        }
    }
}

static INLINE void set_mul_rgb_input(struct rdp_state* rdp, int32_t **input_r, int32_t **input_g, int32_t **input_b, int code)
{
    switch (code & 0x1f)
    {
        case 0:     *input_r = &rdp->combined_color.r;   *input_g = &rdp->combined_color.g;   *input_b = &rdp->combined_color.b;   break;
        case 1:     *input_r = &rdp->texel0_color.r;     *input_g = &rdp->texel0_color.g;     *input_b = &rdp->texel0_color.b;     break;
        case 2:     *input_r = &rdp->texel1_color.r;     *input_g = &rdp->texel1_color.g;     *input_b = &rdp->texel1_color.b;     break;
        case 3:     *input_r = &rdp->prim_color.r;       *input_g = &rdp->prim_color.g;       *input_b = &rdp->prim_color.b;       break;
        case 4:     *input_r = &rdp->shade_color.r;      *input_g = &rdp->shade_color.g;      *input_b = &rdp->shade_color.b;      break;
        case 5:     *input_r = &rdp->env_color.r;        *input_g = &rdp->env_color.g;        *input_b = &rdp->env_color.b;        break;
        case 6:     *input_r = &rdp->key_scale.r;        *input_g = &rdp->key_scale.g;        *input_b = &rdp->key_scale.b;        break;
        case 7:     *input_r = &rdp->combined_color.a;   *input_g = &rdp->combined_color.a;   *input_b = &rdp->combined_color.a;   break;
        case 8:     *input_r = &rdp->texel0_color.a;     *input_g = &rdp->texel0_color.a;     *input_b = &rdp->texel0_color.a;     break;
        case 9:     *input_r = &rdp->texel1_color.a;     *input_g = &rdp->texel1_color.a;     *input_b = &rdp->texel1_color.a;     break;
        case 10:    *input_r = &rdp->prim_color.a;       *input_g = &rdp->prim_color.a;       *input_b = &rdp->prim_color.a;       break;
        case 11:    *input_r = &rdp->shade_color.a;      *input_g = &rdp->shade_color.a;      *input_b = &rdp->shade_color.a;      break;
        case 12:    *input_r = &rdp->env_color.a;        *input_g = &rdp->env_color.a;        *input_b = &rdp->env_color.a;        break;
        case 13:    *input_r = &rdp->lod_frac;           *input_g = &rdp->lod_frac;           *input_b = &rdp->lod_frac;           break;
        case 14:    *input_r = &rdp->primitive_lod_frac; *input_g = &rdp->primitive_lod_frac; *input_b = &rdp->primitive_lod_frac; break;
        case 15:    *input_r = &rdp->k5;                 *input_g = &rdp->k5;                 *input_b = &rdp->k5;                 break;
        case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
        case 24: case 25: case 26: case 27: case 28: case 29: case 30: case 31:
        {
            *input_r = &zero_color;     *input_g = &zero_color;     *input_b = &zero_color;     break;
        }
    }
}

static INLINE void set_add_rgb_input(struct rdp_state* rdp, int32_t **input_r, int32_t **input_g, int32_t **input_b, int code)
{
    switch (code & 0x7)
    {
        case 0:     *input_r = &rdp->combined_color.r;   *input_g = &rdp->combined_color.g;   *input_b = &rdp->combined_color.b;   break;
        case 1:     *input_r = &rdp->texel0_color.r;     *input_g = &rdp->texel0_color.g;     *input_b = &rdp->texel0_color.b;     break;
        case 2:     *input_r = &rdp->texel1_color.r;     *input_g = &rdp->texel1_color.g;     *input_b = &rdp->texel1_color.b;     break;
        case 3:     *input_r = &rdp->prim_color.r;       *input_g = &rdp->prim_color.g;       *input_b = &rdp->prim_color.b;       break;
        case 4:     *input_r = &rdp->shade_color.r;      *input_g = &rdp->shade_color.g;      *input_b = &rdp->shade_color.b;      break;
        case 5:     *input_r = &rdp->env_color.r;        *input_g = &rdp->env_color.g;        *input_b = &rdp->env_color.b;        break;
        case 6:     *input_r = &one_color;          *input_g = &one_color;          *input_b = &one_color;          break;
        case 7:     *input_r = &zero_color;         *input_g = &zero_color;         *input_b = &zero_color;         break;
    }
}

static INLINE void set_sub_alpha_input(struct rdp_state* rdp, int32_t **input, int code)
{
    switch (code & 0x7)
    {
        case 0:     *input = &rdp->combined_color.a; break;
        case 1:     *input = &rdp->texel0_color.a; break;
        case 2:     *input = &rdp->texel1_color.a; break;
        case 3:     *input = &rdp->prim_color.a; break;
        case 4:     *input = &rdp->shade_color.a; break;
        case 5:     *input = &rdp->env_color.a; break;
        case 6:     *input = &one_color; break;
        case 7:     *input = &zero_color; break;
    }
}

static INLINE void set_mul_alpha_input(struct rdp_state* rdp, int32_t **input, int code)
{
    switch (code & 0x7)
    {
        case 0:     *input = &rdp->lod_frac; break;
        case 1:     *input = &rdp->texel0_color.a; break;
        case 2:     *input = &rdp->texel1_color.a; break;
        case 3:     *input = &rdp->prim_color.a; break;
        case 4:     *input = &rdp->shade_color.a; break;
        case 5:     *input = &rdp->env_color.a; break;
        case 6:     *input = &rdp->primitive_lod_frac; break;
        case 7:     *input = &zero_color; break;
    }
}

static STRICTINLINE int32_t color_combiner_equation(int32_t a, int32_t b, int32_t c, int32_t d)
{





    a = special_9bit_exttable[a];
    b = special_9bit_exttable[b];
    c = SIGNF(c, 9);
    d = special_9bit_exttable[d];
    a = ((a - b) * c) + (d << 8) + 0x80;
    return (a & 0x1ffff);
}

static STRICTINLINE int32_t alpha_combiner_equation(int32_t a, int32_t b, int32_t c, int32_t d)
{
    a = special_9bit_exttable[a];
    b = special_9bit_exttable[b];
    c = SIGNF(c, 9);
    d = special_9bit_exttable[d];
    a = (((a - b) * c) + (d << 8) + 0x80) >> 8;
    return (a & 0x1ff);
}

static STRICTINLINE int32_t chroma_key_min(struct rdp_state* rdp, struct color* col)
{
    int32_t redkey, greenkey, bluekey, keyalpha;




    redkey = SIGN(col->r, 17);
    if (redkey > 0)
        redkey = ((redkey & 0xf) == 8) ? (-redkey + 0x10) : (-redkey);

    redkey = (rdp->key_width.r << 4) + redkey;

    greenkey = SIGN(col->g, 17);
    if (greenkey > 0)
        greenkey = ((greenkey & 0xf) == 8) ? (-greenkey + 0x10) : (-greenkey);

    greenkey = (rdp->key_width.g << 4) + greenkey;

    bluekey = SIGN(col->b, 17);
    if (bluekey > 0)
        bluekey = ((bluekey & 0xf) == 8) ? (-bluekey + 0x10) : (-bluekey);

    bluekey = (rdp->key_width.b << 4) + bluekey;

    keyalpha = (redkey < greenkey) ? redkey : greenkey;
    keyalpha = (bluekey < keyalpha) ? bluekey : keyalpha;
    keyalpha = clamp(keyalpha, 0, 0xff);
    return keyalpha;
}

static STRICTINLINE void combiner_1cycle(struct rdp_state* rdp, int adseed, uint32_t* curpixel_cvg)
{

    int32_t keyalpha, temp;
    struct color chromabypass;

    if (rdp->other_modes.key_en)
    {
        chromabypass.r = *rdp->combiner_rgbsub_a_r[1];
        chromabypass.g = *rdp->combiner_rgbsub_a_g[1];
        chromabypass.b = *rdp->combiner_rgbsub_a_b[1];
    }






    if (rdp->combiner_rgbmul_r[1] != &zero_color)
    {
















        rdp->combined_color.r = color_combiner_equation(*rdp->combiner_rgbsub_a_r[1],*rdp->combiner_rgbsub_b_r[1],*rdp->combiner_rgbmul_r[1],*rdp->combiner_rgbadd_r[1]);
        rdp->combined_color.g = color_combiner_equation(*rdp->combiner_rgbsub_a_g[1],*rdp->combiner_rgbsub_b_g[1],*rdp->combiner_rgbmul_g[1],*rdp->combiner_rgbadd_g[1]);
        rdp->combined_color.b = color_combiner_equation(*rdp->combiner_rgbsub_a_b[1],*rdp->combiner_rgbsub_b_b[1],*rdp->combiner_rgbmul_b[1],*rdp->combiner_rgbadd_b[1]);
    }
    else
    {
        rdp->combined_color.r = ((special_9bit_exttable[*rdp->combiner_rgbadd_r[1]] << 8) + 0x80) & 0x1ffff;
        rdp->combined_color.g = ((special_9bit_exttable[*rdp->combiner_rgbadd_g[1]] << 8) + 0x80) & 0x1ffff;
        rdp->combined_color.b = ((special_9bit_exttable[*rdp->combiner_rgbadd_b[1]] << 8) + 0x80) & 0x1ffff;
    }

    if (rdp->combiner_alphamul[1] != &zero_color)
        rdp->combined_color.a = alpha_combiner_equation(*rdp->combiner_alphasub_a[1],*rdp->combiner_alphasub_b[1],*rdp->combiner_alphamul[1],*rdp->combiner_alphaadd[1]);
    else
        rdp->combined_color.a = special_9bit_exttable[*rdp->combiner_alphaadd[1]] & 0x1ff;

    rdp->pixel_color.a = special_9bit_clamptable[rdp->combined_color.a];
    if (rdp->pixel_color.a == 0xff)
        rdp->pixel_color.a = 0x100;

    if (!rdp->other_modes.key_en)
    {

        rdp->combined_color.r >>= 8;
        rdp->combined_color.g >>= 8;
        rdp->combined_color.b >>= 8;
        rdp->pixel_color.r = special_9bit_clamptable[rdp->combined_color.r];
        rdp->pixel_color.g = special_9bit_clamptable[rdp->combined_color.g];
        rdp->pixel_color.b = special_9bit_clamptable[rdp->combined_color.b];
    }
    else
    {
        keyalpha = chroma_key_min(rdp, &rdp->combined_color);



        rdp->pixel_color.r = special_9bit_clamptable[chromabypass.r];
        rdp->pixel_color.g = special_9bit_clamptable[chromabypass.g];
        rdp->pixel_color.b = special_9bit_clamptable[chromabypass.b];


        rdp->combined_color.r >>= 8;
        rdp->combined_color.g >>= 8;
        rdp->combined_color.b >>= 8;
    }


    if (rdp->other_modes.cvg_times_alpha)
    {
        temp = (rdp->pixel_color.a * (*curpixel_cvg) + 4) >> 3;
        *curpixel_cvg = (temp >> 5) & 0xf;
    }

    if (!rdp->other_modes.alpha_cvg_select)
    {
        if (!rdp->other_modes.key_en)
        {
            rdp->pixel_color.a += adseed;
            if (rdp->pixel_color.a & 0x100)
                rdp->pixel_color.a = 0xff;
        }
        else
            rdp->pixel_color.a = keyalpha;
    }
    else
    {
        if (rdp->other_modes.cvg_times_alpha)
            rdp->pixel_color.a = temp;
        else
            rdp->pixel_color.a = (*curpixel_cvg) << 5;
        if (rdp->pixel_color.a > 0xff)
            rdp->pixel_color.a = 0xff;
    }

    rdp->shade_color.a += adseed;
    if (rdp->shade_color.a & 0x100)
        rdp->shade_color.a = 0xff;
}

static STRICTINLINE void combiner_2cycle(struct rdp_state* rdp, int adseed, uint32_t* curpixel_cvg, int32_t* acalpha)
{
    int32_t keyalpha, temp;
    struct color chromabypass;

    if (rdp->combiner_rgbmul_r[0] != &zero_color)
    {
        rdp->combined_color.r = color_combiner_equation(*rdp->combiner_rgbsub_a_r[0],*rdp->combiner_rgbsub_b_r[0],*rdp->combiner_rgbmul_r[0],*rdp->combiner_rgbadd_r[0]);
        rdp->combined_color.g = color_combiner_equation(*rdp->combiner_rgbsub_a_g[0],*rdp->combiner_rgbsub_b_g[0],*rdp->combiner_rgbmul_g[0],*rdp->combiner_rgbadd_g[0]);
        rdp->combined_color.b = color_combiner_equation(*rdp->combiner_rgbsub_a_b[0],*rdp->combiner_rgbsub_b_b[0],*rdp->combiner_rgbmul_b[0],*rdp->combiner_rgbadd_b[0]);
    }
    else
    {
        rdp->combined_color.r = ((special_9bit_exttable[*rdp->combiner_rgbadd_r[0]] << 8) + 0x80) & 0x1ffff;
        rdp->combined_color.g = ((special_9bit_exttable[*rdp->combiner_rgbadd_g[0]] << 8) + 0x80) & 0x1ffff;
        rdp->combined_color.b = ((special_9bit_exttable[*rdp->combiner_rgbadd_b[0]] << 8) + 0x80) & 0x1ffff;
    }

    if (rdp->combiner_alphamul[0] != &zero_color)
        rdp->combined_color.a = alpha_combiner_equation(*rdp->combiner_alphasub_a[0],*rdp->combiner_alphasub_b[0],*rdp->combiner_alphamul[0],*rdp->combiner_alphaadd[0]);
    else
        rdp->combined_color.a = special_9bit_exttable[*rdp->combiner_alphaadd[0]] & 0x1ff;



    if (rdp->other_modes.alpha_compare_en)
    {
        if (rdp->other_modes.key_en)
            keyalpha = chroma_key_min(rdp, &rdp->combined_color);

        int32_t preacalpha = special_9bit_clamptable[rdp->combined_color.a];
        if (preacalpha == 0xff)
            preacalpha = 0x100;

        if (rdp->other_modes.cvg_times_alpha)
            temp = (preacalpha * (*curpixel_cvg) + 4) >> 3;

        if (!rdp->other_modes.alpha_cvg_select)
        {
            if (!rdp->other_modes.key_en)
            {
                preacalpha += adseed;
                if (preacalpha & 0x100)
                    preacalpha = 0xff;
            }
            else
                preacalpha = keyalpha;
        }
        else
        {
            if (rdp->other_modes.cvg_times_alpha)
                preacalpha = temp;
            else
                preacalpha = (*curpixel_cvg) << 5;
            if (preacalpha > 0xff)
                preacalpha = 0xff;
        }

        *acalpha = preacalpha;
    }





    rdp->combined_color.r >>= 8;
    rdp->combined_color.g >>= 8;
    rdp->combined_color.b >>= 8;


    rdp->texel0_color = rdp->texel1_color;
    rdp->texel1_color = rdp->nexttexel_color;









    if (rdp->other_modes.key_en)
    {
        chromabypass.r = *rdp->combiner_rgbsub_a_r[1];
        chromabypass.g = *rdp->combiner_rgbsub_a_g[1];
        chromabypass.b = *rdp->combiner_rgbsub_a_b[1];
    }

    if (rdp->combiner_rgbmul_r[1] != &zero_color)
    {
        rdp->combined_color.r = color_combiner_equation(*rdp->combiner_rgbsub_a_r[1],*rdp->combiner_rgbsub_b_r[1],*rdp->combiner_rgbmul_r[1],*rdp->combiner_rgbadd_r[1]);
        rdp->combined_color.g = color_combiner_equation(*rdp->combiner_rgbsub_a_g[1],*rdp->combiner_rgbsub_b_g[1],*rdp->combiner_rgbmul_g[1],*rdp->combiner_rgbadd_g[1]);
        rdp->combined_color.b = color_combiner_equation(*rdp->combiner_rgbsub_a_b[1],*rdp->combiner_rgbsub_b_b[1],*rdp->combiner_rgbmul_b[1],*rdp->combiner_rgbadd_b[1]);
    }
    else
    {
        rdp->combined_color.r = ((special_9bit_exttable[*rdp->combiner_rgbadd_r[1]] << 8) + 0x80) & 0x1ffff;
        rdp->combined_color.g = ((special_9bit_exttable[*rdp->combiner_rgbadd_g[1]] << 8) + 0x80) & 0x1ffff;
        rdp->combined_color.b = ((special_9bit_exttable[*rdp->combiner_rgbadd_b[1]] << 8) + 0x80) & 0x1ffff;
    }

    if (rdp->combiner_alphamul[1] != &zero_color)
        rdp->combined_color.a = alpha_combiner_equation(*rdp->combiner_alphasub_a[1],*rdp->combiner_alphasub_b[1],*rdp->combiner_alphamul[1],*rdp->combiner_alphaadd[1]);
    else
        rdp->combined_color.a = special_9bit_exttable[*rdp->combiner_alphaadd[1]] & 0x1ff;

    if (!rdp->other_modes.key_en)
    {

        rdp->combined_color.r >>= 8;
        rdp->combined_color.g >>= 8;
        rdp->combined_color.b >>= 8;

        rdp->pixel_color.r = special_9bit_clamptable[rdp->combined_color.r];
        rdp->pixel_color.g = special_9bit_clamptable[rdp->combined_color.g];
        rdp->pixel_color.b = special_9bit_clamptable[rdp->combined_color.b];
    }
    else
    {
        keyalpha = chroma_key_min(rdp, &rdp->combined_color);



        rdp->pixel_color.r = special_9bit_clamptable[chromabypass.r];
        rdp->pixel_color.g = special_9bit_clamptable[chromabypass.g];
        rdp->pixel_color.b = special_9bit_clamptable[chromabypass.b];


        rdp->combined_color.r >>= 8;
        rdp->combined_color.g >>= 8;
        rdp->combined_color.b >>= 8;
    }

    rdp->pixel_color.a = special_9bit_clamptable[rdp->combined_color.a];
    if (rdp->pixel_color.a == 0xff)
        rdp->pixel_color.a = 0x100;


    if (rdp->other_modes.cvg_times_alpha)
    {
        temp = (rdp->pixel_color.a * (*curpixel_cvg) + 4) >> 3;

        *curpixel_cvg = (temp >> 5) & 0xf;


    }

    if (!rdp->other_modes.alpha_cvg_select)
    {
        if (!rdp->other_modes.key_en)
        {
            rdp->pixel_color.a += adseed;
            if (rdp->pixel_color.a & 0x100)
                rdp->pixel_color.a = 0xff;
        }
        else
            rdp->pixel_color.a = keyalpha;
    }
    else
    {
        if (rdp->other_modes.cvg_times_alpha)
            rdp->pixel_color.a = temp;
        else
            rdp->pixel_color.a = (*curpixel_cvg) << 5;
        if (rdp->pixel_color.a > 0xff)
            rdp->pixel_color.a = 0xff;
    }

    rdp->shade_color.a += adseed;
    if (rdp->shade_color.a & 0x100)
        rdp->shade_color.a = 0xff;
}

static void combiner_init_lut(void)
{
    int i;
    for(i = 0; i < 0x200; i++)
    {
        switch((i >> 7) & 3)
        {
        case 0:
        case 1:
            special_9bit_clamptable[i] = i & 0xff;
            break;
        case 2:
            special_9bit_clamptable[i] = 0xff;
            break;
        case 3:
            special_9bit_clamptable[i] = 0;
            break;
        }
    }

    for (i = 0; i < 0x200; i++)
    {
        special_9bit_exttable[i] = ((i & 0x180) == 0x180) ? (i | ~0x1ff) : (i & 0x1ff);
    }
}

static void combiner_init(struct rdp_state* rdp)
{
    rdp->combiner_rgbsub_a_r[0] = rdp->combiner_rgbsub_a_r[1] = &one_color;
    rdp->combiner_rgbsub_a_g[0] = rdp->combiner_rgbsub_a_g[1] = &one_color;
    rdp->combiner_rgbsub_a_b[0] = rdp->combiner_rgbsub_a_b[1] = &one_color;
    rdp->combiner_rgbsub_b_r[0] = rdp->combiner_rgbsub_b_r[1] = &one_color;
    rdp->combiner_rgbsub_b_g[0] = rdp->combiner_rgbsub_b_g[1] = &one_color;
    rdp->combiner_rgbsub_b_b[0] = rdp->combiner_rgbsub_b_b[1] = &one_color;
    rdp->combiner_rgbmul_r[0] = rdp->combiner_rgbmul_r[1] = &one_color;
    rdp->combiner_rgbmul_g[0] = rdp->combiner_rgbmul_g[1] = &one_color;
    rdp->combiner_rgbmul_b[0] = rdp->combiner_rgbmul_b[1] = &one_color;
    rdp->combiner_rgbadd_r[0] = rdp->combiner_rgbadd_r[1] = &one_color;
    rdp->combiner_rgbadd_g[0] = rdp->combiner_rgbadd_g[1] = &one_color;
    rdp->combiner_rgbadd_b[0] = rdp->combiner_rgbadd_b[1] = &one_color;

    rdp->combiner_alphasub_a[0] = rdp->combiner_alphasub_a[1] = &one_color;
    rdp->combiner_alphasub_b[0] = rdp->combiner_alphasub_b[1] = &one_color;
    rdp->combiner_alphamul[0] = rdp->combiner_alphamul[1] = &one_color;
    rdp->combiner_alphaadd[0] = rdp->combiner_alphaadd[1] = &one_color;
}

static void rdp_set_prim_color(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->min_level = (args[0] >> 8) & 0x1f;
    rdp->primitive_lod_frac = args[0] & 0xff;
    rdp->prim_color.r = RGBA32_R(args[1]);
    rdp->prim_color.g = RGBA32_G(args[1]);
    rdp->prim_color.b = RGBA32_B(args[1]);
    rdp->prim_color.a = RGBA32_A(args[1]);
}

static void rdp_set_env_color(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->env_color.r = RGBA32_R(args[1]);
    rdp->env_color.g = RGBA32_G(args[1]);
    rdp->env_color.b = RGBA32_B(args[1]);
    rdp->env_color.a = RGBA32_A(args[1]);
}

static void rdp_set_combine(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->combine.sub_a_rgb0  = (args[0] >> 20) & 0xf;
    rdp->combine.mul_rgb0    = (args[0] >> 15) & 0x1f;
    rdp->combine.sub_a_a0    = (args[0] >> 12) & 0x7;
    rdp->combine.mul_a0      = (args[0] >>  9) & 0x7;
    rdp->combine.sub_a_rgb1  = (args[0] >>  5) & 0xf;
    rdp->combine.mul_rgb1    = (args[0] >>  0) & 0x1f;

    rdp->combine.sub_b_rgb0  = (args[1] >> 28) & 0xf;
    rdp->combine.sub_b_rgb1  = (args[1] >> 24) & 0xf;
    rdp->combine.sub_a_a1    = (args[1] >> 21) & 0x7;
    rdp->combine.mul_a1      = (args[1] >> 18) & 0x7;
    rdp->combine.add_rgb0    = (args[1] >> 15) & 0x7;
    rdp->combine.sub_b_a0    = (args[1] >> 12) & 0x7;
    rdp->combine.add_a0      = (args[1] >>  9) & 0x7;
    rdp->combine.add_rgb1    = (args[1] >>  6) & 0x7;
    rdp->combine.sub_b_a1    = (args[1] >>  3) & 0x7;
    rdp->combine.add_a1      = (args[1] >>  0) & 0x7;


    set_suba_rgb_input(rdp, &rdp->combiner_rgbsub_a_r[0], &rdp->combiner_rgbsub_a_g[0], &rdp->combiner_rgbsub_a_b[0], rdp->combine.sub_a_rgb0);
    set_subb_rgb_input(rdp, &rdp->combiner_rgbsub_b_r[0], &rdp->combiner_rgbsub_b_g[0], &rdp->combiner_rgbsub_b_b[0], rdp->combine.sub_b_rgb0);
    set_mul_rgb_input(rdp, &rdp->combiner_rgbmul_r[0], &rdp->combiner_rgbmul_g[0], &rdp->combiner_rgbmul_b[0], rdp->combine.mul_rgb0);
    set_add_rgb_input(rdp, &rdp->combiner_rgbadd_r[0], &rdp->combiner_rgbadd_g[0], &rdp->combiner_rgbadd_b[0], rdp->combine.add_rgb0);
    set_sub_alpha_input(rdp, &rdp->combiner_alphasub_a[0], rdp->combine.sub_a_a0);
    set_sub_alpha_input(rdp, &rdp->combiner_alphasub_b[0], rdp->combine.sub_b_a0);
    set_mul_alpha_input(rdp, &rdp->combiner_alphamul[0], rdp->combine.mul_a0);
    set_sub_alpha_input(rdp, &rdp->combiner_alphaadd[0], rdp->combine.add_a0);

    set_suba_rgb_input(rdp, &rdp->combiner_rgbsub_a_r[1], &rdp->combiner_rgbsub_a_g[1], &rdp->combiner_rgbsub_a_b[1], rdp->combine.sub_a_rgb1);
    set_subb_rgb_input(rdp, &rdp->combiner_rgbsub_b_r[1], &rdp->combiner_rgbsub_b_g[1], &rdp->combiner_rgbsub_b_b[1], rdp->combine.sub_b_rgb1);
    set_mul_rgb_input(rdp, &rdp->combiner_rgbmul_r[1], &rdp->combiner_rgbmul_g[1], &rdp->combiner_rgbmul_b[1], rdp->combine.mul_rgb1);
    set_add_rgb_input(rdp, &rdp->combiner_rgbadd_r[1], &rdp->combiner_rgbadd_g[1], &rdp->combiner_rgbadd_b[1], rdp->combine.add_rgb1);
    set_sub_alpha_input(rdp, &rdp->combiner_alphasub_a[1], rdp->combine.sub_a_a1);
    set_sub_alpha_input(rdp, &rdp->combiner_alphasub_b[1], rdp->combine.sub_b_a1);
    set_mul_alpha_input(rdp, &rdp->combiner_alphamul[1], rdp->combine.mul_a1);
    set_sub_alpha_input(rdp, &rdp->combiner_alphaadd[1], rdp->combine.add_a1);

    rdp->other_modes.f.stalederivs = 1;
}

static void rdp_set_key_gb(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->key_width.g = (args[0] >> 12) & 0xfff;
    rdp->key_width.b = args[0] & 0xfff;
    rdp->key_center.g = (args[1] >> 24) & 0xff;
    rdp->key_scale.g = (args[1] >> 16) & 0xff;
    rdp->key_center.b = (args[1] >> 8) & 0xff;
    rdp->key_scale.b = args[1] & 0xff;
}

static void rdp_set_key_r(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->key_width.r = (args[1] >> 16) & 0xfff;
    rdp->key_center.r = (args[1] >> 8) & 0xff;
    rdp->key_scale.r = args[1] & 0xff;
}
