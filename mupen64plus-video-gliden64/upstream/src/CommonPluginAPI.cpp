#ifdef OS_WINDOWS
# include <windows.h>
#else
# include "winlnxdefs.h"
#endif // OS_WINDOWS

#include "PluginAPI.h"

extern "C" {
//#include "../../../mupen64plus-core/upstream/src/main/main.h"
//#include "../../../mupen64plus-core/upstream/src/plugin/plugin.h"

//extern int l_resolutionReset;

EXPORT BOOL CALL InitiateGFX (GFX_INFO Gfx_Info)
{
	return api().InitiateGFX(Gfx_Info);
}

EXPORT void CALL MoveScreen (int xpos, int ypos)
{
	api().MoveScreen(xpos, ypos);
}

EXPORT void CALL ProcessDList(void)
{
	api().ProcessDList();
}

EXPORT void CALL ProcessRDPList(void)
{
	api().ProcessRDPList();
}

EXPORT void CALL RomClosed (void)
{
	api().RomClosed();
}

EXPORT void CALL ShowCFB (void)
{
	api().ShowCFB();
}

EXPORT void CALL UpdateScreen (void)
{
	api().UpdateScreen();
//	if(l_resolutionReset != 0){
//		if(!g_rom_pause)
//			main_toggle_pause();
//		l_resolutionReset = 0;
//	}
}

EXPORT void CALL ViStatusChanged (void)
{
	api().ViStatusChanged();
}

EXPORT void CALL ViWidthChanged (void)
{
	api().ViWidthChanged();
}

EXPORT void CALL ChangeWindow(void)
{
	api().ChangeWindow();
}

EXPORT void CALL FBWrite(unsigned int addr, unsigned int size)
{
	api().FBWrite(addr, size);
}

EXPORT void CALL FBRead(unsigned int addr)
{
	api().FBRead(addr);
}

EXPORT void CALL FBGetFrameBufferInfo(void *pinfo)
{
	api().FBGetFrameBufferInfo(pinfo);
}

#ifndef MUPENPLUSAPI
EXPORT void CALL FBWList(FrameBufferModifyEntry *plist, unsigned int size)
{
	api().FBWList(plist, size);
}
#endif
}
