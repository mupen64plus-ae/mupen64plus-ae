static void fbwrite_4(struct rdp_state* rdp, uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg);
static void fbwrite_8(struct rdp_state* rdp, uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg);
static void fbwrite_16(struct rdp_state* rdp, uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg);
static void fbwrite_32(struct rdp_state* rdp, uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg);
static void fbread_4(struct rdp_state* rdp, uint32_t num, uint32_t* curpixel_memcvg);
static void fbread_8(struct rdp_state* rdp, uint32_t num, uint32_t* curpixel_memcvg);
static void fbread_16(struct rdp_state* rdp, uint32_t num, uint32_t* curpixel_memcvg);
static void fbread_32(struct rdp_state* rdp, uint32_t num, uint32_t* curpixel_memcvg);
static void fbread2_4(struct rdp_state* rdp, uint32_t num, uint32_t* curpixel_memcvg);
static void fbread2_8(struct rdp_state* rdp, uint32_t num, uint32_t* curpixel_memcvg);
static void fbread2_16(struct rdp_state* rdp, uint32_t num, uint32_t* curpixel_memcvg);
static void fbread2_32(struct rdp_state* rdp, uint32_t num, uint32_t* curpixel_memcvg);

static void (*fbread_func[4])(struct rdp_state*,uint32_t, uint32_t*) =
{
    fbread_4, fbread_8, fbread_16, fbread_32
};

static void (*fbread2_func[4])(struct rdp_state*,uint32_t, uint32_t*) =
{
    fbread2_4, fbread2_8, fbread2_16, fbread2_32
};

static void (*fbwrite_func[4])(struct rdp_state*, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t) =
{
    fbwrite_4, fbwrite_8, fbwrite_16, fbwrite_32
};

static void fbwrite_4(struct rdp_state* rdp, uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
    uint32_t fb = rdp->fb_address + curpixel;
    RWRITEADDR8(fb, 0);
}

static void fbwrite_8(struct rdp_state* rdp, uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
    uint32_t fb = rdp->fb_address + curpixel;
    PAIRWRITE8(fb, r & 0xff, (r & 1) ? 3 : 0);
}

static void fbwrite_16(struct rdp_state* rdp, uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
#undef CVG_DRAW
#ifdef CVG_DRAW
    int covdraw = (curpixel_cvg - 1) << 5;
    r=covdraw; g=covdraw; b=covdraw;
#endif

    uint32_t fb;
    uint16_t rval;
    uint8_t hval;
    fb = (rdp->fb_address >> 1) + curpixel;

    int32_t finalcvg = finalize_spanalpha(rdp->other_modes.cvg_dest, blend_en, curpixel_cvg, curpixel_memcvg);
    int16_t finalcolor;

    if (rdp->fb_format == FORMAT_RGBA)
    {
        finalcolor = ((r & ~7) << 8) | ((g & ~7) << 3) | ((b & ~7) >> 2);
    }
    else
    {
        finalcolor = (r << 8) | (finalcvg << 5);
        finalcvg = 0;
    }


    rval = finalcolor|(finalcvg >> 2);
    hval = finalcvg & 3;
    PAIRWRITE16(fb, rval, hval);
}

static void fbwrite_32(struct rdp_state* rdp, uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
    uint32_t fb = (rdp->fb_address >> 2) + curpixel;

    int32_t finalcolor;
    int32_t finalcvg = finalize_spanalpha(rdp->other_modes.cvg_dest, blend_en, curpixel_cvg, curpixel_memcvg);

    finalcolor = (r << 24) | (g << 16) | (b << 8);
    finalcolor |= (finalcvg << 5);

    PAIRWRITE32(fb, finalcolor, (g & 1) ? 3 : 0, 0);
}

static void fbfill_4(struct rdp_state* rdp, uint32_t curpixel)
{
    rdp_pipeline_crashed = 1;
}

static void fbfill_8(struct rdp_state* rdp, uint32_t curpixel)
{
    uint32_t fb = rdp->fb_address + curpixel;
    uint32_t val = (rdp->fill_color >> (((fb & 3) ^ 3) << 3)) & 0xff;
    uint8_t hval = ((val & 1) << 1) | (val & 1);
    PAIRWRITE8(fb, val, hval);
}

static void fbfill_16(struct rdp_state* rdp, uint32_t curpixel)
{
    uint16_t val;
    uint8_t hval;
    uint32_t fb = (rdp->fb_address >> 1) + curpixel;
    if (fb & 1)
        val = rdp->fill_color & 0xffff;
    else
        val = (rdp->fill_color >> 16) & 0xffff;
    hval = ((val & 1) << 1) | (val & 1);
    PAIRWRITE16(fb, val, hval);
}

static void fbfill_32(struct rdp_state* rdp, uint32_t curpixel)
{
    uint32_t fb = (rdp->fb_address >> 2) + curpixel;
    PAIRWRITE32(fb, rdp->fill_color, (rdp->fill_color & 0x10000) ? 3 : 0, (rdp->fill_color & 0x1) ? 3 : 0);
}

static void fbread_4(struct rdp_state* rdp, uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    rdp->memory_color.r = rdp->memory_color.g = rdp->memory_color.b = 0;

    *curpixel_memcvg = 7;
    rdp->memory_color.a = 0xe0;
}

static void fbread2_4(struct rdp_state* rdp, uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    rdp->pre_memory_color.r = rdp->pre_memory_color.g = rdp->pre_memory_color.b = 0;
    rdp->pre_memory_color.a = 0xe0;
    *curpixel_memcvg = 7;
}

static void fbread_8(struct rdp_state* rdp, uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    uint8_t mem;
    uint32_t addr = rdp->fb_address + curpixel;
    RREADADDR8(mem, addr);
    rdp->memory_color.r = rdp->memory_color.g = rdp->memory_color.b = mem;
    *curpixel_memcvg = 7;
    rdp->memory_color.a = 0xe0;
}

static void fbread2_8(struct rdp_state* rdp, uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    uint8_t mem;
    uint32_t addr = rdp->fb_address + curpixel;
    RREADADDR8(mem, addr);
    rdp->pre_memory_color.r = rdp->pre_memory_color.g = rdp->pre_memory_color.b = mem;
    rdp->pre_memory_color.a = 0xe0;
    *curpixel_memcvg = 7;
}

static void fbread_16(struct rdp_state* rdp, uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    uint16_t fword;
    uint8_t hbyte;
    uint32_t addr = (rdp->fb_address >> 1) + curpixel;

    uint8_t lowbits;


    if (rdp->other_modes.image_read_en)
    {
        PAIRREAD16(fword, hbyte, addr);

        if (rdp->fb_format == FORMAT_RGBA)
        {
            rdp->memory_color.r = RGBA16_R(fword);
            rdp->memory_color.g = RGBA16_G(fword);
            rdp->memory_color.b = RGBA16_B(fword);
            lowbits = ((fword & 1) << 2) | hbyte;
        }
        else
        {
            rdp->memory_color.r = rdp->memory_color.g = rdp->memory_color.b = fword >> 8;
            lowbits = (fword >> 5) & 7;
        }

        *curpixel_memcvg = lowbits;
        rdp->memory_color.a = lowbits << 5;
    }
    else
    {
        RREADIDX16(fword, addr);

        if (rdp->fb_format == FORMAT_RGBA)
        {
            rdp->memory_color.r = RGBA16_R(fword);
            rdp->memory_color.g = RGBA16_G(fword);
            rdp->memory_color.b = RGBA16_B(fword);
        }
        else
            rdp->memory_color.r = rdp->memory_color.g = rdp->memory_color.b = fword >> 8;

        *curpixel_memcvg = 7;
        rdp->memory_color.a = 0xe0;
    }
}

static void fbread2_16(struct rdp_state* rdp, uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    uint16_t fword;
    uint8_t hbyte;
    uint32_t addr = (rdp->fb_address >> 1) + curpixel;

    uint8_t lowbits;

    if (rdp->other_modes.image_read_en)
    {
        PAIRREAD16(fword, hbyte, addr);

        if (rdp->fb_format == FORMAT_RGBA)
        {
            rdp->pre_memory_color.r = RGBA16_R(fword);
            rdp->pre_memory_color.g = RGBA16_G(fword);
            rdp->pre_memory_color.b = RGBA16_B(fword);
            lowbits = ((fword & 1) << 2) | hbyte;
        }
        else
        {
            rdp->pre_memory_color.r = rdp->pre_memory_color.g = rdp->pre_memory_color.b = fword >> 8;
            lowbits = (fword >> 5) & 7;
        }

        *curpixel_memcvg = lowbits;
        rdp->pre_memory_color.a = lowbits << 5;
    }
    else
    {
        RREADIDX16(fword, addr);

        if (rdp->fb_format == FORMAT_RGBA)
        {
            rdp->pre_memory_color.r = RGBA16_R(fword);
            rdp->pre_memory_color.g = RGBA16_G(fword);
            rdp->pre_memory_color.b = RGBA16_B(fword);
        }
        else
            rdp->pre_memory_color.r = rdp->pre_memory_color.g = rdp->pre_memory_color.b = fword >> 8;

        *curpixel_memcvg = 7;
        rdp->pre_memory_color.a = 0xe0;
    }

}

static void fbread_32(struct rdp_state* rdp, uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    uint32_t mem, addr = (rdp->fb_address >> 2) + curpixel;
    RREADIDX32(mem, addr);
    rdp->memory_color.r = RGBA32_R(mem);
    rdp->memory_color.g = RGBA32_G(mem);
    rdp->memory_color.b = RGBA32_B(mem);
    if (rdp->other_modes.image_read_en)
    {
        *curpixel_memcvg = (mem >> 5) & 7;
        rdp->memory_color.a = mem & 0xe0;
    }
    else
    {
        *curpixel_memcvg = 7;
        rdp->memory_color.a = 0xe0;
    }
}

static INLINE void fbread2_32(struct rdp_state* rdp, uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    uint32_t mem, addr = (rdp->fb_address >> 2) + curpixel;
    RREADIDX32(mem, addr);
    rdp->pre_memory_color.r = RGBA32_R(mem);
    rdp->pre_memory_color.g = RGBA32_G(mem);
    rdp->pre_memory_color.b = RGBA32_B(mem);
    if (rdp->other_modes.image_read_en)
    {
        *curpixel_memcvg = (mem >> 5) & 7;
        rdp->pre_memory_color.a = mem & 0xe0;
    }
    else
    {
        *curpixel_memcvg = 7;
        rdp->pre_memory_color.a = 0xe0;
    }
}

static void rdp_set_color_image(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->fb_format   = (args[0] >> 21) & 0x7;
    rdp->fb_size     = (args[0] >> 19) & 0x3;
    rdp->fb_width    = (args[0] & 0x3ff) + 1;
    rdp->fb_address  = args[1] & 0x0ffffff;


    rdp->fbread1_ptr = fbread_func[rdp->fb_size];
    rdp->fbread2_ptr = fbread2_func[rdp->fb_size];
    rdp->fbwrite_ptr = fbwrite_func[rdp->fb_size];
}

static void rdp_set_fill_color(struct rdp_state* rdp, const uint32_t* args)
{
    rdp->fill_color = args[1];
}

static void fb_init(struct rdp_state* rdp)
{
    rdp->fb_format = FORMAT_RGBA;
    rdp->fb_size = PIXEL_SIZE_4BIT;
    rdp->fb_width = 0;
    rdp->fb_address = 0;


    rdp->fbread1_ptr = fbread_func[rdp->fb_size];
    rdp->fbread2_ptr = fbread2_func[rdp->fb_size];
    rdp->fbwrite_ptr = fbwrite_func[rdp->fb_size];
}
