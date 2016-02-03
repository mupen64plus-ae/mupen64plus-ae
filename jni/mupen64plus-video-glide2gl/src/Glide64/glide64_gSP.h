typedef struct DRAWOBJECT_t
{
  float objX;
  float objY;
  float scaleW;
  float scaleH;
  int16_t imageW;
  int16_t imageH;

  uint16_t  imageStride;
  uint16_t  imageAdrs;
  uint8_t  imageFmt;
  uint8_t  imageSiz;
  uint8_t  imagePal;
  uint8_t  imageFlags;
} DRAWOBJECT;

struct MAT2D {
  float A, B, C, D;
  float X, Y;
  float BaseScaleX;
  float BaseScaleY;
} mat_2d = {1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f};

// positional and texel coordinate clipping
#define CCLIP2(ux,lx,ut,lt,un,ln,uc,lc) \
		if (ux > lx || lx < uc || ux > lc) return; \
		if (ux < uc) { \
			float p = (uc-ux)/(lx-ux); \
			ut = p*(lt-ut)+ut; \
			un = p*(ln-un)+un; \
			ux = uc; \
		} \
		if (lx > lc) { \
			float p = (lc-ux)/(lx-ux); \
			lt = p*(lt-ut)+ut; \
			ln = p*(ln-un)+un; \
			lx = lc; \
		}

//forward decls
static void uc6_draw_polygons (VERTEX v[4]);
static void uc6_read_object_data (DRAWOBJECT *d);
static void uc6_init_tile(const DRAWOBJECT *d);
extern uint32_t dma_offset_mtx;
extern int32_t cur_mtx;
extern uint32_t dma_offset_mtx;
extern uint32_t dma_offset_vtx;
extern int32_t billboarding;

int dzdx = 0;
int deltaZ = 0;
VERTEX **org_vtx;

//software backface culling. Gonetz
// mega modifications by Dave2001

static int cull_tri(VERTEX **v) // type changed to VERTEX** [Dave2001]
{
   int i, draw, iarea;
   unsigned int mode;
   float x1, y1, x2, y2, area;

   if (v[0]->scr_off & v[1]->scr_off & v[2]->scr_off)
      return true;

   // Triangle can't be culled, if it need clipping
   draw = false;

   for (i=0; i<3; i++)
   {
      if (!v[i]->screen_translated)
      {
         v[i]->sx = rdp.view_trans[0] + v[i]->x_w * rdp.view_scale[0] + rdp.offset_x;
         v[i]->sy = rdp.view_trans[1] + v[i]->y_w * rdp.view_scale[1] + rdp.offset_y;
         v[i]->sz = rdp.view_trans[2] + v[i]->z_w * rdp.view_scale[2];
         v[i]->screen_translated = 1;
      }
      if (v[i]->w < 0.01f) //need clip_z. can't be culled now
         draw = 1;
   }

   rdp.u_cull_mode = (rdp.flags & CULLMASK);
   if (draw || rdp.u_cull_mode == 0 || rdp.u_cull_mode == CULLMASK) //no culling set
   {
      rdp.u_cull_mode >>= CULLSHIFT;
      return false;
   }

   x1 = v[0]->sx - v[1]->sx;
   y1 = v[0]->sy - v[1]->sy;
   x2 = v[2]->sx - v[1]->sx;
   y2 = v[2]->sy - v[1]->sy;
   area = y1 * x2 - x1 * y2;
   iarea = *(int*)&area;

   mode = (rdp.u_cull_mode << 19UL);
   rdp.u_cull_mode >>= CULLSHIFT;

   if ((iarea & 0x7FFFFFFF) == 0)
   {
      //LRDP (" zero area triangles\n");
      return true;
   }

   if ((rdp.flags & CULLMASK) && ((int)(iarea ^ mode)) >= 0)
   {
      //LRDP (" culled\n");
      return true;
   }

   return false;
}

static void gSPCombineMatrices(void)
{
   MulMatrices(rdp.model, rdp.proj, rdp.combined);
   g_gdp.flags ^= UPDATE_MULT_MAT;
}

/* clip_w - clips aint the z-axis */
static void clip_w (void)
{
   int i;
   int index = 0;
   int n = rdp.n_global;
   VERTEX *tmp = (VERTEX*)rdp.vtxbuf2;

   /* Swap vertex buffers */
   rdp.vtxbuf2 = rdp.vtxbuf;
   rdp.vtxbuf = tmp;
   rdp.vtx_buffer ^= 1;

   // Check the vertices for clipping
   for (i=0; i < n; i++)
   {
      bool save_inpoint = false;
      VERTEX *first, *second, *current = NULL, *current2 = NULL;
      int j = i+1;
      if (j == 3)
         j = 0;
      first = (VERTEX*)&rdp.vtxbuf2[i];
      second = (VERTEX*)&rdp.vtxbuf2[j];

      if (first->w >= 0.01f)
      {
         if (second->w >= 0.01f)    // Both are in, save the last one
         {
            save_inpoint = true;
         }
         else      // First is in, second is out, save intersection
         {
            current  = first;
            current2 = second;
         }
      }
      else
      {
         if (second->w >= 0.01f)  // First is out, second is in, save intersection & in point
         {
            current  = second;
            current2 = first;

            save_inpoint = true;
         }
      }

      if (current && current2)
      {
         float percent = (-current->w) / (current2->w - current->w);
         rdp.vtxbuf[index].not_zclipped = 0;
         rdp.vtxbuf[index].x = current->x + (current2->x - current->x) * percent;
         rdp.vtxbuf[index].y = current->y + (current2->y - current->y) * percent;
         rdp.vtxbuf[index].z = current->z + (current2->z - current->z) * percent;
         rdp.vtxbuf[index].u[0] = current->u[0] + (current2->u[0] - current->u[0]) * percent;
         rdp.vtxbuf[index].v[0] = current->v[0] + (current2->v[0] - current->v[0]) * percent;
         rdp.vtxbuf[index].u[1] = current->u[1] + (current2->u[1] - current->u[1]) * percent;
         rdp.vtxbuf[index].v[1] = current->v[1] + (current2->v[1] - current->v[1]) * percent;
         rdp.vtxbuf[index].w = settings.depth_bias * 0.01f;
         rdp.vtxbuf[index++].number = first->number | second->number;
      }

      if (save_inpoint)
      {
         // Save the in point
         rdp.vtxbuf[index] = rdp.vtxbuf2[j];
         rdp.vtxbuf[index++].not_zclipped = 1;
      }
   }
   rdp.n_global = index;
}

static void draw_tri_depth(VERTEX **vtx)
{
   float X0 = vtx[0]->sx / rdp.scale_x;
   float Y0 = vtx[0]->sy / rdp.scale_y;
   float X1 = vtx[1]->sx / rdp.scale_x;
   float Y1 = vtx[1]->sy / rdp.scale_y;
   float X2 = vtx[2]->sx / rdp.scale_x;
   float Y2 = vtx[2]->sy / rdp.scale_y;
   float diffy_02 = Y0 - Y2;
   float diffy_12 = Y1 - Y2;
   float diffx_02 = X0 - X2;
   float diffx_12 = X1 - X2;
   float denom = (diffx_02 * diffy_12 - diffx_12 * diffy_02);

   if(denom * denom > 0.0)
   {
      float diffz_02 = vtx[0]->sz - vtx[2]->sz;
      float diffz_12 = vtx[1]->sz - vtx[2]->sz;
      float fdzdx = (diffz_02 * diffy_12 - diffz_12 * diffy_02) / denom;

      if ((rdp.rm & ZMODE_DECAL) == ZMODE_DECAL)
      {
         // Calculate deltaZ per polygon for Decal z-mode
         float fdzdy = (float)((diffz_02*diffx_12 - diffz_12*diffx_02) / denom);
         float fdz = (float)(fabs(fdzdx) + fabs(fdzdy));
         deltaZ = max(8, (int)fdz);
      }
      dzdx = (int)(fdzdx * 65536.0);
   }
}

static INLINE void draw_tri_uv_calculation_update_shift(unsigned cur_tile, unsigned index, VERTEX *v)
{
   int32_t shifter = g_gdp.tile[cur_tile].shift_s;

   if (shifter)
   {
      if (shifter > 10)
         v->u[index] *= (float)(1 << (16 - shifter));
      else
         v->u[index] /= (float)(1 << shifter);
   }

   shifter = g_gdp.tile[cur_tile].shift_t;

   if (shifter)
   {
      if (shifter > 10)
         v->v[index] *= (float)(1 << (16 - shifter));
      else
         v->v[index] /= (float)(1 << shifter);
   }

   v->u[index]   -= rdp.tiles[cur_tile].f_ul_s;
   v->v[index]   -= rdp.tiles[cur_tile].f_ul_t;
   v->u[index]    = rdp.cur_cache[index]->c_off + rdp.cur_cache[index]->c_scl_x * v->u[index];
   v->v[index]    = rdp.cur_cache[index]->c_off + rdp.cur_cache[index]->c_scl_y * v->v[index];
   v->u_w[index]  = v->u[index] / v->w;
   v->v_w[index]  = v->v[index] / v->w;
}

static void draw_tri_uv_calculation(VERTEX **vtx, VERTEX *v)
{
   unsigned i;
   //FRDP(" * CALCULATING VERTEX U/V: %d\n", v->number);

   if (!(rdp.geom_mode & G_LIGHTING))
   {
      if (!(rdp.geom_mode & UPDATE_SCISSOR))
      {
         if (rdp.geom_mode & G_SHADE)
            glideSetVertexFlatShading(v, vtx, rdp.cmd1);
         else
            glideSetVertexPrimShading(v, g_gdp.prim_color.total);
      }
   }

   // Fix texture coordinates
   if (!v->uv_scaled)
   {
      v->ou *= rdp.tiles[rdp.cur_tile].s_scale;
      v->ov *= rdp.tiles[rdp.cur_tile].t_scale;
      v->uv_scaled = 1;
      if (!(rdp.othermode_h & RDP_PERSP_TEX_ENABLE))
      {
         //          v->oow = v->w = 1.0f;
         v->ou *= 0.5f;
         v->ov *= 0.5f;
      }
   }
   v->u[1] = v->u[0] = v->ou;
   v->v[1] = v->v[0] = v->ov;

   for (i = 0; i < 2; i++)
   {
      unsigned index = i+1;
      if (rdp.tex >= index && rdp.cur_cache[i])
         draw_tri_uv_calculation_update_shift(rdp.cur_tile+i, i, v);
   }

   v->uv_calculated = rdp.tex_ctr;
}

static void draw_tri (VERTEX **vtx, uint16_t linew)
{
   int i;

   org_vtx = vtx;

   for (i = 0; i < 3; i++)
   {
      VERTEX *v = (VERTEX*)vtx[i];

      if (v->uv_calculated != rdp.tex_ctr)
         draw_tri_uv_calculation(vtx, v);
      if (v->shade_mod != cmb.shade_mod_hash)
         apply_shade_mods (v);
   }

   rdp.clip = 0;

   vtx[0]->not_zclipped = vtx[1]->not_zclipped = vtx[2]->not_zclipped = 1;

   // Set vertex buffers
   rdp.vtxbuf = rdp.vtx1;  // copy from v to rdp.vtx1
   rdp.vtxbuf2 = rdp.vtx2;
   rdp.vtx_buffer = 0;
   rdp.n_global = 3;

   rdp.vtxbuf[0] = *vtx[0];
   rdp.vtxbuf[0].number = 1;
   rdp.vtxbuf[1] = *vtx[1];
   rdp.vtxbuf[1].number = 2;
   rdp.vtxbuf[2] = *vtx[2];
   rdp.vtxbuf[2].number = 4;

   if ((vtx[0]->scr_off & 16) ||
         (vtx[1]->scr_off & 16) ||
         (vtx[2]->scr_off & 16))
      clip_w();

   do_triangle_stuff (linew, false);
}

static void cull_trianglefaces(VERTEX **v, unsigned iterations, bool do_update, bool do_cull, int32_t wd)
{
   uint32_t i;
   int32_t vcount = 0;

   if (do_update)
      update();

   for (i = 0; i < iterations; i++, vcount += 3)
   {
      if (do_cull)
         if (cull_tri(v + vcount))
            continue;

      deltaZ = dzdx = 0;
      if (wd == 0 && (fb_depth_render_enabled || (rdp.rm & ZMODE_DECAL) == ZMODE_DECAL))
         draw_tri_depth(v + vcount);
      draw_tri (v + vcount, wd);
   }
}

static void pre_update(void)
{
   // This is special, not handled in update(), but here
   // Matrix Pre-multiplication idea by Gonetz (Gonetz@ngs.ru)
   if (g_gdp.flags & UPDATE_MULT_MAT)
      gSPCombineMatrices();

   if (g_gdp.flags & UPDATE_LIGHTS)
   {
      uint32_t l;
      g_gdp.flags ^= UPDATE_LIGHTS;

      // Calculate light vectors
      for (l = 0; l < rdp.num_lights; l++)
      {
         InverseTransformVector(&rdp.light[l].dir[0], rdp.light_vector[l], rdp.model);
         NormalizeVector (rdp.light_vector[l]);
      }
   }
}

static void gSPClipVertex_G64(uint32_t v)
{
   VERTEX *vtx = (VERTEX*)&rdp.vtx[v];

   vtx->scr_off = 0;
   if (vtx->x > +vtx->w)   vtx->scr_off |= 2;
   if (vtx->x < -vtx->w)   vtx->scr_off |= 1;
   if (vtx->y > +vtx->w)   vtx->scr_off |= 8;
   if (vtx->y < -vtx->w)   vtx->scr_off |= 4;
   if (vtx->w < 0.1f)      vtx->scr_off |= 16;
}

/*
 * Loads into the RSP vertex buffer the vertices that will be used by the 
 * gSP1Triangle commands to generate polygons.
 *
 * v  - Segment address of the vertex list  pointer to a list of vertices.
 * n  - Number of vertices (1 - 32).
 * v0 - Starting index in vertex buffer where vertices are to be loaded into.
 */
static void gSPVertex_G64(uint32_t v, uint32_t n, uint32_t v0)
{
   unsigned int i;
   float x, y, z;
   uint32_t iter = 16;
   void   *vertex  = (void*)(gfx_info.RDRAM + v);

   for (i=0; i < (n * iter); i+= iter)
   {
      VERTEX *vtx = (VERTEX*)&rdp.vtx[v0 + (i / iter)];
      int16_t *rdram    = (int16_t*)vertex;
      uint8_t *rdram_u8 = (uint8_t*)vertex;
      uint8_t *color = (uint8_t*)(rdram_u8 + 12);
      y                 = (float)rdram[0];
      x                 = (float)rdram[1];
      vtx->flags        = (uint16_t)rdram[2];
      z                 = (float)rdram[3];
      vtx->ov           = (float)rdram[4];
      vtx->ou           = (float)rdram[5];
      vtx->uv_scaled    = 0;
      vtx->a            = color[0];

      vtx->x = x*rdp.combined[0][0] + y*rdp.combined[1][0] + z*rdp.combined[2][0] + rdp.combined[3][0];
      vtx->y = x*rdp.combined[0][1] + y*rdp.combined[1][1] + z*rdp.combined[2][1] + rdp.combined[3][1];
      vtx->z = x*rdp.combined[0][2] + y*rdp.combined[1][2] + z*rdp.combined[2][2] + rdp.combined[3][2];
      vtx->w = x*rdp.combined[0][3] + y*rdp.combined[1][3] + z*rdp.combined[2][3] + rdp.combined[3][3];

      vtx->uv_calculated = 0xFFFFFFFF;
      vtx->screen_translated = 0;
      vtx->shade_mod = 0;

      if (fabs(vtx->w) < 0.001)
         vtx->w = 0.001f;
      vtx->oow = 1.0f / vtx->w;
      vtx->x_w = vtx->x * vtx->oow;
      vtx->y_w = vtx->y * vtx->oow;
      vtx->z_w = vtx->z * vtx->oow;
      CalculateFog (vtx);

      gSPClipVertex_G64(v0 + (i / iter));

      if (rdp.geom_mode & G_LIGHTING)
      {
         vtx->vec[0] = (int8_t)color[3];
         vtx->vec[1] = (int8_t)color[2];
         vtx->vec[2] = (int8_t)color[1];

         if (settings.ucode == 2 && rdp.geom_mode & G_POINT_LIGHTING)
         {
            float tmpvec[3] = {x, y, z};
            calc_point_light (vtx, tmpvec);
         }
         else
         {
            NormalizeVector (vtx->vec);
            calc_light (vtx);
         }

         if (rdp.geom_mode & G_TEXTURE_GEN)
         {
            if (rdp.geom_mode & G_TEXTURE_GEN_LINEAR)
               calc_linear (vtx);
            else
               calc_sphere (vtx);
         }

      }
      else
      {
         vtx->r = color[3];
         vtx->g = color[2];
         vtx->b = color[1];
      }
      vertex = (char*)vertex + iter;
   }
}

static void gSPLookAt_G64(uint32_t l, uint32_t n)
{
   int8_t  *rdram_s8  = (int8_t*) (gfx_info.RDRAM  + RSP_SegmentToPhysical(l));
   int8_t dir_x = rdram_s8[11];
   int8_t dir_y = rdram_s8[10];
   int8_t dir_z = rdram_s8[9];
   rdp.lookat[n][0] = (float)(dir_x) / 127.0f;
   rdp.lookat[n][1] = (float)(dir_y) / 127.0f;
   rdp.lookat[n][2] = (float)(dir_z) / 127.0f;
   rdp.use_lookat = (n == 0) || (n == 1 && (dir_x || dir_y));
}

static void gSPLight_G64(uint32_t l, int32_t n)
{
   int16_t *rdram     = (int16_t*)(gfx_info.RDRAM  + RSP_SegmentToPhysical(l));
   uint8_t *rdram_u8  = (uint8_t*)(gfx_info.RDRAM  + RSP_SegmentToPhysical(l));
   int8_t  *rdram_s8  = (int8_t*) (gfx_info.RDRAM  + RSP_SegmentToPhysical(l));

	--n;

	if (n < 8)
   {
      /* Get the data */
      rdp.light[n].nonblack  = rdram_u8[3];
      rdp.light[n].nonblack += rdram_u8[2];
      rdp.light[n].nonblack += rdram_u8[1];

      rdp.light[n].col[0]    = rdram_u8[3] / 255.0f;
      rdp.light[n].col[1]    = rdram_u8[2] / 255.0f;
      rdp.light[n].col[2]    = rdram_u8[1] / 255.0f;
      rdp.light[n].col[3]    = 1.0f;

      // ** Thanks to Icepir8 for pointing this out **
      // Lighting must be signed byte instead of byte
      rdp.light[n].dir[0] = (float)rdram_s8[11] / 127.0f;
      rdp.light[n].dir[1] = (float)rdram_s8[10] / 127.0f;
      rdp.light[n].dir[2] = (float)rdram_s8[9] / 127.0f;

      rdp.light[n].x = (float)rdram[5];
      rdp.light[n].y = (float)rdram[4];
      rdp.light[n].z = (float)rdram[7];
      rdp.light[n].ca = (float)rdram[0] / 16.0f;
      rdp.light[n].la = (float)rdram[4];
      rdp.light[n].qa = (float)rdram[13] / 8.0f;
      //g_gdp.flags |= UPDATE_LIGHTS;
   }
}

static void gSPViewport_G64(uint32_t v)
{
   int16_t *rdram     = (int16_t*)(gfx_info.RDRAM  + RSP_SegmentToPhysical( v ));

   int16_t scale_y = rdram[0] >> 2;
   int16_t scale_x = rdram[1] >> 2;
   int16_t scale_z = rdram[3];
   int16_t trans_x = rdram[5] >> 2;
   int16_t trans_y = rdram[4] >> 2;
   int16_t trans_z = rdram[7];
   if (settings.correct_viewport)
   {
      scale_x = abs(scale_x);
      scale_y = abs(scale_y);
   }
   rdp.view_scale[0] = scale_x * rdp.scale_x;
   rdp.view_scale[1] = -scale_y * rdp.scale_y;
   rdp.view_scale[2] = 32.0f * scale_z;
   rdp.view_trans[0] = trans_x * rdp.scale_x;
   rdp.view_trans[1] = trans_y * rdp.scale_y;
   rdp.view_trans[2] = 32.0f * trans_z;

   g_gdp.flags |= UPDATE_VIEWPORT;
}

static void gSPFogFactor_G64(int16_t fm, int16_t fo )
{
   rdp.fog_multiplier = fm;
   rdp.fog_offset     = fo;
}

static void gSPNumLights_G64(int32_t n)
{
   if (n > 12)
      return;

   rdp.num_lights = n;
   g_gdp.flags |= UPDATE_LIGHTS;
}

static void gSPForceMatrix_G64( uint32_t mptr )
{
   uint32_t address = RSP_SegmentToPhysical( mptr );

   load_matrix(rdp.combined, address);

   g_gdp.flags &= ~UPDATE_MULT_MAT;
}

static void gSPPopMatrixN_G64(uint32_t param, uint32_t num )
{
   if (rdp.model_i > num - 1)
   {
      rdp.model_i -= num;
   }
   memcpy (rdp.model, rdp.model_stack[rdp.model_i], 64);
   g_gdp.flags |= UPDATE_MULT_MAT;
}

static void gSPPopMatrix_G64(uint32_t param)
{
   switch (param)
   {
      case 0: // modelview
         if (rdp.model_i > 0)
         {
            rdp.model_i--;
            memcpy (rdp.model, rdp.model_stack[rdp.model_i], 64);
            g_gdp.flags |= UPDATE_MULT_MAT;
         }
         break;
      case 1: // projection, can't
         break;
      default:
#ifdef DEBUG
         DebugMsg( DEBUG_HIGH | DEBUG_ERROR | DEBUG_MATRIX, "// Attempting to pop matrix stack below 0\n" );
         DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_MATRIX, "gSPPopMatrix( %s );\n",
               (param == G_MTX_MODELVIEW) ? "G_MTX_MODELVIEW" :
               (param == G_MTX_PROJECTION) ? "G_MTX_PROJECTION" : "G_MTX_INVALID" );
#endif
         break;
   }
}

static void gSPLightColor_G64( uint32_t lightNum, uint32_t packedColor )
{
   lightNum--;

   if (lightNum < 8)
   {
      rdp.light[lightNum].col[0] = _SHIFTR( packedColor, 24, 8 ) * 0.0039215689f;
      rdp.light[lightNum].col[1] = _SHIFTR( packedColor, 16, 8 ) * 0.0039215689f;
      rdp.light[lightNum].col[2] = _SHIFTR( packedColor, 8, 8 )  * 0.0039215689f;
      rdp.light[lightNum].col[3] = 255;
   }
}

static void gSPDlistCount_G64(uint32_t count, uint32_t v)
{
   uint32_t address = RSP_SegmentToPhysical(v);

   if (rdp.pc_i >= 9 || address == 0)
      return;

   rdp.pc_i ++;  // go to the next PC in the stack
   rdp.pc[rdp.pc_i] = address;  // jump to the address
   rdp.dl_count = count + 1;
}

static void gSPModifyVertex_G64( uint32_t vtx, uint32_t where, uint32_t val )
{
   VERTEX *v = (VERTEX*)&rdp.vtx[vtx];

   switch (where)
   {
      case 0:
         uc6_obj_sprite(rdp.cmd0, rdp.cmd1);
         break;

      case G_MWO_POINT_RGBA:
         v->r = (uint8_t)(val >> 24);
         v->g = (uint8_t)((val >> 16) & 0xFF);
         v->b = (uint8_t)((val >> 8) & 0xFF);
         v->a = (uint8_t)(val & 0xFF);
         v->shade_mod = 0;
         break;

      case G_MWO_POINT_ST:
         {
            float scale = (rdp.othermode_h & RDP_PERSP_TEX_ENABLE) ? 0.03125f : 0.015625f;
            v->ou = (float)((int16_t)(val>>16)) * scale;
            v->ov = (float)((int16_t)(val&0xFFFF)) * scale;
            v->uv_calculated = 0xFFFFFFFF;
            v->uv_scaled = 1;
         }
         break;

      case G_MWO_POINT_XYSCREEN:
         {
            float scr_x = (float)((int16_t)(val>>16)) / 4.0f;
            float scr_y = (float)((int16_t)(val&0xFFFF)) / 4.0f;
            v->screen_translated = 2;
            v->sx = scr_x * rdp.scale_x + rdp.offset_x;
            v->sy = scr_y * rdp.scale_y + rdp.offset_y;
            if (v->w < 0.01f)
            {
               v->w = 1.0f;
               v->oow = 1.0f;
               v->z_w = 1.0f;
            }
            v->sz = rdp.view_trans[2] + v->z_w * rdp.view_scale[2];

            v->scr_off = 0;
            if (scr_x < 0) v->scr_off |= 1;
            if (scr_x > rdp.vi_width) v->scr_off |= 2;
            if (scr_y < 0) v->scr_off |= 4;
            if (scr_y > rdp.vi_height) v->scr_off |= 8;
            if (v->w < 0.1f) v->scr_off |= 16;
         }
         break;
      case G_MWO_POINT_ZSCREEN:
         {
            float scr_z = _FIXED2FLOAT((int16_t)_SHIFTR(val, 16, 16), 15);
            v->z_w = (scr_z - rdp.view_trans[2]) / rdp.view_scale[2];
            v->z = v->z_w * v->w;
         }
         break;
   }
}

static void gSPEndDisplayList_G64(void)
{
   if (rdp.pc_i > 0)
      rdp.pc_i --;
   else
   {
      //LRDP("RDP end\n");
      // Halt execution here
      rdp.halt = 1;
   }
}

static bool gSPCullVertices_G64( uint32_t v0, uint32_t vn )
{
   uint32_t i, clip = 0;
	if (vn < v0)
   {
      // Aidyn Chronicles - The First Mage seems to pass parameters in reverse order.
      const uint32_t v = v0;
      v0 = vn;
      vn = v;
   }

   /* Wipeout 64 passes vn = 512, increasing MAX_VTX to 512+ doesn't fix. */
   if (vn > MAX_VTX)
      return false;

   for (i = v0; i <= vn; i++)
   {
      VERTEX *v = (VERTEX*)&rdp.vtx[i];
      // Check if completely off the screen (quick frustrum clipping for 90 FOV)
      if (v->x >= -v->w) clip |= 0x01;
      if (v->x <= v->w)  clip |= 0x02;
      if (v->y >= -v->w) clip |= 0x04;
      if (v->y <= v->w)  clip |= 0x08;
      if (v->w >= 0.1f)  clip |= 0x10;
      if (clip == 0x1F)
         return false;
   }
   return true;
}

static void gSPCullDisplayList_G64( uint32_t v0, uint32_t vn )
{
	if (gSPCullVertices_G64( v0, vn ))
      gSPEndDisplayList_G64();
}
