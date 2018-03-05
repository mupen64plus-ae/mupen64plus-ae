static uint32_t gamma_table[0x100];
static uint32_t gamma_dither_table[0x4000];

static uint32_t vi_integer_sqrt(uint32_t a)
{
    unsigned long op = a, res = 0, one = 1 << 30;

    while (one > op)
        one >>= 2;

    while (one != 0)
    {
        if (op >= res + one)
        {
            op -= res + one;
            res += one << 1;
        }
        res >>= 1;
        one >>= 2;
    }
    return res;
}

static STRICTINLINE void gamma_filters(uint32_t* r, uint32_t* g, uint32_t* b, union vi_reg_ctrl ctrl, int32_t* seed)
{
    int cdith, dith;

    switch((ctrl.gamma_enable << 1) | ctrl.gamma_dither_enable)
    {
    case 0: // no gamma, no dithering
        return;
    case 1: // no gamma, dithering enabled
        cdith = irand(seed);
        dith = cdith & 1;
        if (*r < 255)
            *r += dith;
        dith = (cdith >> 1) & 1;
        if (*g < 255)
            *g += dith;
        dith = (cdith >> 2) & 1;
        if (*b < 255)
            *b += dith;
        break;
    case 2: // gamma enabled, no dithering
        *r = gamma_table[*r];
        *g = gamma_table[*g];
        *b = gamma_table[*b];
        break;
    case 3: // gamma and dithering enabled
        cdith = irand(seed);
        dith = cdith & 0x3f;
        *r = gamma_dither_table[((*r) << 6)|dith];
        dith = (cdith >> 6) & 0x3f;
        *g = gamma_dither_table[((*g) << 6)|dith];
        dith = ((cdith >> 9) & 0x38) | (cdith & 7);
        *b = gamma_dither_table[((*b) << 6)|dith];
        break;
    }
}

void vi_gamma_init(void)
{
    for (int i = 0; i < 256; i++)
    {
        gamma_table[i] = vi_integer_sqrt(i << 6);
        gamma_table[i] <<= 1;
    }

    for (int i = 0; i < 0x4000; i++)
    {
        gamma_dither_table[i] = vi_integer_sqrt(i);
        gamma_dither_table[i] <<= 1;
    }
}
