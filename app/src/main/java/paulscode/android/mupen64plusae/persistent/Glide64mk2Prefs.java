package paulscode.android.mupen64plusae.persistent;

import paulscode.android.mupen64plusae.profile.Profile;

import static paulscode.android.mupen64plusae.persistent.GamePrefs.getSafeInt;

public class Glide64mk2Prefs {

    /** The maximum frameskip in the glide64 library. */
    public final int maxFrameskip;

    /** True if auto-frameskip is enabled in the glide64 library. */
    public final boolean autoFrameskipEnabled;

    /** Filtering mode: -1=Game default, 0=automatic, 1=force bilinear, 2=force point sampled */
    public final int filtering;

    /** Fog: -1=Game default, 0=disable. 1=enable */
    public final int fog;

    /** Buffer clear on every frame: -1=Game default, 0=disable. 1=enable */
    public final int buff_clear;

    /** Buffer swapping method:
     * -1=Game default,
     * 0=swap buffers when vertical interrupt has occurred,
     * 1=swap buffers when set of conditions is satisfied. Prevents flicker on some games,
     * 2=mix of first two methods
     */
    public final int swapmode;

    /** LOD calculation: -1=Game default, 0=disable. 1=fast, 2=precise */
    public final int lodmode;

    /** Smart framebuffer: -1=Game default, 0=disable. 1=enable */
    public final int fb_smart;

    /** Hardware frame buffer emulation: -1=Game default, 0=disable. 1=enable */
    public final int fb_hires;

    /** Get frame buffer info: -1=Game default, 0=disable. 1=enable */
    public final int fb_get_info;

    /** Enable software depth render: -1=Game default, 0=disable. 1=enable */
    public final int fb_render;

    /** Set frambuffer CRC mode: -1=Game default, 0=disable CRC, 1=fast CRC, 2=safe CRC */
    public final int fb_crc_mode;

    /** Render N64 frame buffer as texture: -1=Game default, 0=disable, 1=mode1, 2=mode2 */
    public final int read_back_to_screen;

    /** Show images written directly by CPU: -1=Game default, 0=disable. 1=enable */
    public final int detect_cpu_write;

    /** Alternate texture size method: -1=Game default, 0=disable. 1=enable */
    public final int alt_tex_size;

    /** Use first SETTILESIZE only: -1=Game default, 0=disable. 1=enable */
    public final int use_sts1_only;

    /** Use fast CRC algorithm: -1=Game default, 0=disable. 1=enable */
    public final int fast_crc;

    /** Check microcode each frame: -1=Game default, 0=disable. 1=enable */
    public final int force_microcheck;

    /** Force 0xb5 command to be quad, not line 3D: -1=Game default, 0=disable. 1=enable */
    public final int force_quad3d;

    /** Fast texrect rendering with hwfbe: -1=Game default, 0=disable. 1=enable */
    public final int optimize_texrect;

    /** Clear auxiliary texture frame buffers: -1=Game default, 0=disable. 1=enable */
    public final int hires_buf_clear;

    /** Read alpha from framebuffer: -1=Game default, 0=disable. 1=enable */
    public final int fb_read_alpha;

    /** Use spheric mapping only: -1=Game default, 0=disable. 1=enable */
    public final int force_calc_sphere;

    /** Enable perspective texture correction emulation: -1=Game default, 0=disable. 1=enable */
    public final int texture_correction;

    /** Force texrect size to integral value: -1=Game default, 0=disable. 1=enable */
    public final int increase_texrect_edge;

    /** Reduce fillrect size by 1: -1=Game default, 0=disable. 1=enable */
    public final int decrease_fillrect_edge;

    /** 3DFX Dithered alpha emulation mode: -1=Game default, >=0=dithered alpha emulation mode */
    public final int stipple_mode;

    /** 3DFX Dithered alpha pattern: -1=Game default, >=0=pattern used for dithered alpha emulation */
    public final int stipple_pattern;

    /** Enable far plane clipping: -1=Game default, 0=disable. 1=enable */
    public final int clip_zmax;

    /** Enable near z clipping: -1=Game default, 0=disable. 1=enable */
    public final int clip_zmin;

    /** Adjust screen aspect for wide screen mode: -1=Game default, 0=disable. 1=enable */
    public final int adjust_aspect;

    /** Force positive viewport: -1=Game default, 0=disable. 1=enable */
    public final int correct_viewport;

    /** Aspect ratio: -1=Game default, 0=Force 4:3, 1=Force 16:9, 2=Stretch, 3=Original */
    public final int aspect;

    /** Force strict check in Depth buffer test: -1=Game default, 0=disable. 1=enable */
    public final int zmode_compare_less;

    /** Apply alpha dither regardless of alpha_dither_mode: -1=Game default, 0=disable. 1=enable */
    public final int old_style_adither;

    /** Scale vertex z value before writing to depth buffer: -1=Game default, 0=disable. 1=enable */
    public final int n64_z_scale;

    /** Set special scale for PAL games: -1=Game default, 0=disable. 1=enable */
    public final int pal230;

    /** Do not copy auxiliary frame buffers: -1=Game default, 0=disable. 1=enable */
    public final int ignore_aux_copy;

    /** Anisotropic filtering setting*/
    public final int wrpAnisotropic;

    /** Read framebuffer every frame (may be slow use only for effects that need it e.g. Banjo Kazooie, DK64 transitions):
     * -1=Game default
     * 0=disable
     * 1=enable */
    public final int fb_read_always;

    /** Handle unchanged fb: -1=Game default, 0=disable. 1=enable */
    public final int useless_is_useless;

    Glide64mk2Prefs(final Profile emulationProfile)
    {
        int setMaxFrameskip = getSafeInt( emulationProfile, "glide64Frameskip", 0 );
        autoFrameskipEnabled = setMaxFrameskip < 0;
        maxFrameskip = Math.abs( setMaxFrameskip );

        filtering = getSafeInt( emulationProfile, "glide64mk2_filtering", -1);
        fog = getSafeInt( emulationProfile, "glide64mk2_fog", -1);
        buff_clear = getSafeInt( emulationProfile, "glide64mk2_buff_clear", -1);
        swapmode = getSafeInt( emulationProfile, "glide64mk2_swapmode", -1);
        lodmode = getSafeInt( emulationProfile, "glide64mk2_lodmode", -1);
        fb_smart = getSafeInt( emulationProfile, "glide64mk2_fb_smart", -1);
        fb_hires = getSafeInt( emulationProfile, "glide64mk2_fb_hires", -1);
        fb_get_info = 0;
        fb_render = getSafeInt( emulationProfile, "glide64mk2_fb_render", -1);
        fb_crc_mode = getSafeInt( emulationProfile, "glide64mk2_fb_crc_mode", -1);
        read_back_to_screen = getSafeInt( emulationProfile, "glide64mk2_read_back_to_screen", -1);
        detect_cpu_write = getSafeInt( emulationProfile, "glide64mk2_detect_cpu_write", -1);
        alt_tex_size = getSafeInt( emulationProfile, "glide64mk2_alt_tex_size", -1);
        use_sts1_only = getSafeInt( emulationProfile, "glide64mk2_use_sts1_only", -1);
        fast_crc = getSafeInt( emulationProfile, "glide64mk2_fast_crc", -1);
        force_microcheck = getSafeInt( emulationProfile, "glide64mk2_force_microcheck", -1);
        force_quad3d = getSafeInt( emulationProfile, "glide64mk2_force_quad3d", -1);
        optimize_texrect = getSafeInt( emulationProfile, "glide64mk2_optimize_texrect", -1);
        hires_buf_clear = getSafeInt( emulationProfile, "glide64mk2_hires_buf_clear", -1);
        fb_read_alpha = getSafeInt( emulationProfile, "glide64mk2_fb_read_alpha", -1);
        force_calc_sphere = getSafeInt( emulationProfile, "glide64mk2_force_calc_sphere", -1);
        texture_correction = getSafeInt( emulationProfile, "glide64mk2_texture_correction", -1);
        increase_texrect_edge = getSafeInt( emulationProfile, "glide64mk2_increase_texrect_edge", -1);
        decrease_fillrect_edge = getSafeInt( emulationProfile, "glide64mk2_decrease_fillrect_edge", -1);
        stipple_mode = getSafeInt( emulationProfile, "glide64mk2_stipple_mode", -1);
        stipple_pattern = -1;
        clip_zmax = getSafeInt( emulationProfile, "glide64mk2_clip_zmax", -1);
        clip_zmin = getSafeInt( emulationProfile, "glide64mk2_clip_zmin", -1);
        adjust_aspect = getSafeInt( emulationProfile, "glide64mk2_adjust_aspect", -1);
        correct_viewport = getSafeInt( emulationProfile, "glide64mk2_correct_viewport", -1);
        aspect = getSafeInt( emulationProfile, "glide64mk2_aspect", -1);
        zmode_compare_less = getSafeInt( emulationProfile, "glide64mk2_zmode_compare_less", -1);
        old_style_adither = getSafeInt( emulationProfile, "glide64mk2_old_style_adither", -1);
        n64_z_scale = getSafeInt( emulationProfile, "glide64mk2_n64_z_scale", -1);
        pal230 = getSafeInt( emulationProfile, "glide64mk2_pal230", -1);
        ignore_aux_copy = getSafeInt( emulationProfile, "glide64mk2_ignore_aux_copy", -1);
        useless_is_useless = getSafeInt( emulationProfile, "glide64mk2_useless_is_useless", -1);
        fb_read_always = getSafeInt( emulationProfile, "glide64mk2_fb_read_always", 0);
        wrpAnisotropic = getSafeInt( emulationProfile, "glide64mk2_wrpAnisotropic", 0);
    }
}
