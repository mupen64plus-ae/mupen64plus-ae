/*
Copyright 2007-2008 mudlord. All rights reserved. 

Redistribution and use in source and binary forms, 
with or without modification, are permitted provided that 
the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, 
    this list of conditions and the following disclaimer. 

 2. Redistributions in binary form must reproduce the above copyright notice, 
    this list of conditions and the following disclaimer in the documentation 
    and/or other materials provided with the distribution. 

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, 
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
*/

/**************************
 * Includes for this app
 **************************/
#include "gltest.h"
#define BASSDEF(f) (WINAPI *f)	// define the BASS functions as pointers
#include "Sound/bass.h"
/**************************
 * Function Declarations
 **************************/
LRESULT CALLBACK WndProc (HWND hWnd, UINT message,
WPARAM wParam, LPARAM lParam);
BOOL APIENTRY TestDlgProc (HWND hDlg, UINT message, UINT wParam, LONG lParam);
BOOL isExtensionSupported(const char *extension); 
DWORD chan;
TCHAR tempfile[MAX_PATH];	// temporary BASS.DLL
HINSTANCE bass=0;			// bass handle
void CheckExtensionsProc(HWND hDlg);
void DumpOGLDriverInformation();
void EnableOpenGL ();
void LoadBASS();
void FreeBASS();
void InitSoundSystem(HWND hwnd);
void Write( FILE* file, const char* text );


/**************************
 * Sets up Dialogs
 * as well as GUI code
 **************************/
int WINAPI WinMain (HINSTANCE hInstance,
                    HINSTANCE hPrevInstance,
                    LPSTR lpCmdLine,
                    int iCmdShow)
{
    WNDCLASS wc;
    HWND hWnd;
    HDC hDC;
    HGLRC hRC;        
    MSG msg;
    BOOL bQuit = FALSE;
    /* register window class */
    wc.style = CS_OWNDC;
    wc.lpfnWndProc = WndProc;
    wc.cbClsExtra = 0;
    wc.cbWndExtra = 0;
    wc.hInstance = hInstance;
    wc.hIcon = LoadIcon (NULL, IDI_APPLICATION);
    wc.hCursor = LoadCursor (NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH) GetStockObject (BLACK_BRUSH);
    wc.lpszMenuName = NULL;
    wc.lpszClassName = "GLTEST";
    RegisterClass (&wc);
    hWnd = CreateWindow ("GLTEST", "", WS_CAPTION | WS_POPUPWINDOW | WS_VISIBLE,
    0, 0, 0,0, //a quick kludge to stop a window being shown, only main DLG is shown
    NULL, NULL, hInstance, NULL); //as we need a OpenGL window
    EnableOpenGL ();
	LoadBASS();
	InitSoundSystem(hWnd);
    DialogBox (hInstance, MAKEINTRESOURCE(IDD_DLG1 ), hWnd, TestDlgProc);
	return true;
}


void CheckExtensionsProc(HWND hDlg)
{                            
   if (isExtensionSupported("GL_ARB_multitexture") != FALSE) //scan for multitexture support
	{SetDlgItemText(hDlg,IDC_TEXTURE, "YES");}
    else
     SetDlgItemText(hDlg,IDC_TEXTURE, "NO");
	    
	if (isExtensionSupported("GL_EXT_packed_pixels") != FALSE) //scan for compressed texture support
	{SetDlgItemText(hDlg,IDC_TEXCOMP, "YES");}
	else
     SetDlgItemText(hDlg,IDC_TEXCOMP, "NO");
        
    if (isExtensionSupported("GL_EXT_framebuffer_object") != FALSE) //scan for compressed texture support
	{SetDlgItemText(hDlg,IDC_FBO, "YES");}
    else
     SetDlgItemText(hDlg,IDC_FBO, "NO");
              
    if (isExtensionSupported("GL_ARB_shading_language_100") && isExtensionSupported("GL_ARB_shader_objects") && isExtensionSupported("GL_ARB_fragment_shader") &&
	 isExtensionSupported("GL_ARB_vertex_shader") != FALSE){
	 SetDlgItemText(hDlg,IDC_SHADERS, "YES");}
    else
     SetDlgItemText(hDlg,IDC_SHADERS, "NO");

    if (isExtensionSupported("GL_ARB_texture_non_power_of_two")  != FALSE){       
    SetDlgItemText(hDlg,IDC_NPOT, "YES"); }
    else
    SetDlgItemText(hDlg,IDC_NPOT, "NO");

    if (isExtensionSupported("GL_EXT_texture_compression_s3tc")  != FALSE){       
    SetDlgItemText(hDlg,IDC_S3TC, "YES"); }
    else
    SetDlgItemText(hDlg,IDC_S3TC, "NO");

	if (isExtensionSupported("GL_3DFX_texture_compression_FXT1")  != FALSE){       
    SetDlgItemText(hDlg,IDC_FXT1, "YES"); }
    else
    SetDlgItemText(hDlg,IDC_FXT1, "NO");
}


LRESULT CALLBACK WndProc (HWND hWnd, UINT message,
                          WPARAM wParam, LPARAM lParam)
{
    switch (message)
    {
    case WM_CREATE:
        return 0;
    case WM_CLOSE:
    PostQuitMessage (0);
    // wind the frequency down...
	BASS_ChannelSlideAttribute(chan,BASS_ATTRIB_FREQ,1000,500);
	Sleep(300);
	// ...and fade-out to avoid a "click"
	BASS_ChannelSlideAttribute(chan,BASS_ATTRIB_VOL,-1,200);
	while (BASS_ChannelIsSliding(chan,0)) Sleep(1);
	BASS_Free();
	FreeBASS();
    return 0;
    case WM_DESTROY:
        return 0;

    default:
        return DefWindowProc (hWnd, message, wParam, lParam);
    }
}
BOOL APIENTRY TestDlgProc (HWND hDlg, UINT message, UINT wParam, LONG lParam) 
        { 
    switch (message) 
        { 
    case WM_CLOSE:
		// wind the frequency down...
	BASS_ChannelSlideAttribute(chan,BASS_ATTRIB_FREQ,1000,500);
	Sleep(300);
	// ...and fade-out to avoid a "click"
	BASS_ChannelSlideAttribute(chan,BASS_ATTRIB_VOL,-1,200);
	while (BASS_ChannelIsSliding(chan,0)) Sleep(1);
	BASS_Free();
	FreeBASS();
   	EndDialog(hDlg,TRUE); 
   	break;
	case WM_INITDIALOG:
	CheckExtensionsProc(hDlg);
		 break;
	case WM_COMMAND:
   			switch(LOWORD(wParam))
   			{
   			case IDC_DUMPINFO:
			DumpOGLDriverInformation();
			break;
			case IDC_CREDITS:
			MessageBox(hDlg,"Programming and Core Design: Brad 'mudlord' Miller\nGFX:   Brad 'mudlord' Miller\nSFX:   Dubmood/Razor 1911\n\nSound system by Ian Luck\n\n(C) 2007-2008 Brad Miller (mudlord@vba-m.ngemu.com)\nhttp://vba-m.ngemu.com\nAll rights reserved\n\nThis tool is written only for Glide64 and the 'Glitch64' wrapper. Nothing else.\nThis is NOT to be distributed out of the Glide64 package without the proper written consent of the author.", "Credits",MB_TASKMODAL|MB_ICONINFORMATION);
			break;
			case IDC_TEXTUREBUTTON:
			MessageBox(hDlg,"Multitexturing is a vital feature required for the wrapper.\nMultitexturing is a video card feature that allows for two or more textures to be mapped to one 2D/3D object in one rendering pass.", "Multitexturing Information",MB_TASKMODAL|MB_ICONINFORMATION);
			break;
			case IDC_GLSLBUTTON:
			MessageBox(hDlg,"GLSL (pixel and vertex shader) support is a highly recommended feature, to be supported.\nGLSL support allows for the advanced GLSL shader-based combiner used in the wrapper. This is needed for advanced blending features and special effects such as dithered alpha rendering.\nIt is highly recommended that this is supported for optimal wrapper rendering and support.\n\nA modern video card (such as a ATI Radeon 9600 or a GeForce FX5200) is needed for GLSL combiner support.", "Pixel and Vertex Shader (GLSL) Information",MB_TASKMODAL|MB_ICONINFORMATION);
			break;
			case IDC_FBOBUTTON:
			MessageBox(hDlg,"Framebuffer object support is a recommended feature to be supported by the video card, for use in Glitch64.\n\nFramebuffer object support allows for hardware framebuffer emulation to be supported,\ndue to framebuffer objects being used to allow for hardware-accelerated render to texture, as well as for auxiliary video rendering buffer allocation.\nHowever, framebuffer object usage in the wrapper can be disabled, and glCopyTexImage2D-based render to texture can be used instead, which is supported on all OpenGL compliant video cards.\n\nA modern video card with OpenGL 1.5 support (such as a ATI Radeon 9600 or a GeForce FX5200) is needed for framebuffer object utilization.", "Framebuffer Object Information",MB_TASKMODAL|MB_ICONINFORMATION);
			break;
			case IDC_NPOTBUTTON:
			MessageBox(hDlg,"Non power of two texture support is a completely optional feature.\n\nTextures that are not in a power of two often use less space than textures that are scaled in powers of 2.\nNon-power of two texture support, allows for textures that are not in a power of two, to be used in rendering. Thus, due to the absence of texture scaling to powers of two, VRAM can be saved as textures do not need to be scaled up to a power of two.\n\nA modern video card with OpenGL 2.0 (such as a GeForce 6 6600GT)  support is needed for non power of two texture support.", "Non Power of Two Texture Information",MB_TASKMODAL|MB_ICONINFORMATION);
			break;
			case IDC_S3TCBUTTON:
			MessageBox(hDlg,"S3TC texture compression support is a optional (but recommended) feature.\n\nS3TC texture compression allows for more efficient use of video memory, due to support for textures that are being compressed. This has a quality tradeoff however, when S3TC texture compression is enabled in the texture enhancement module, 'GlideHQ'.", "S3TC Texture Compression Information",MB_TASKMODAL|MB_ICONINFORMATION);
			break;
			case IDC_FXT1BUTTON:
			MessageBox(hDlg,"FXT1 texture compression support is a completely optional feature.\n\nFXT1 texture compression is a proprietary 3dfx compression method. It is currently only supported on 3dfx and Intel-based video cards and chipsets. FXT1 texture compression is used as a alternative to S3TC texture compression.", "FXT1 Texture Compression Information",MB_TASKMODAL|MB_ICONINFORMATION);
			break;
			case IDC_PACKEDPIXELS:
			MessageBox(hDlg,"Packed pixel support is a strongly recommended feature used in Glitch64\n\nPacked pixel support is used in Glitch64 for correct texture format conversions, to assist with image quality.", "Packed Pixel Information",MB_TASKMODAL|MB_ICONINFORMATION);
			break;
   			}
 		default:
   			return FALSE;
   }   
   return TRUE;
}


/*===========================================
AUXILIARY CODE HERE
============================================*/
/**************************
 * Sound system code
 **************************/
void LoadBASS()
{
	BYTE *data;
	HRSRC hBinres;
	HGLOBAL hRes;
	HANDLE hfile;
	DWORD len,c;
	TCHAR temppath[MAX_PATH];
	/* get the BASS.DLL resource */
	hBinres=FindResource(GetModuleHandle(NULL),"BASS_DLL",RT_RCDATA);
    len=SizeofResource(NULL,hBinres);
	hRes=LoadResource(NULL,hBinres);
	data=(unsigned char*)LockResource(hRes);

	/* get a temporary filename */
	GetTempPath(MAX_PATH,temppath);
	GetTempFileName(temppath,"bas",0,tempfile);
	/* write BASS.DLL to the temporary file */
	hfile=CreateFile(tempfile,GENERIC_WRITE,0,NULL,CREATE_ALWAYS,FILE_ATTRIBUTE_TEMPORARY,NULL);
	WriteFile(hfile,data,len,&c,NULL);
	CloseHandle(hfile);

	/* load the temporary BASS.DLL library */
	if (!(bass=LoadLibrary(tempfile))) {
		ExitProcess(0);
	}
	/* "load" all the BASS functions that are to be used as function pointers */
    #define LOADBASSFUNCTION(f) *((void**)&f)=GetProcAddress(bass,#f)
	LOADBASSFUNCTION(BASS_ErrorGetCode);
	LOADBASSFUNCTION(BASS_Init);
	LOADBASSFUNCTION(BASS_Free);
	LOADBASSFUNCTION(BASS_GetCPU);
	LOADBASSFUNCTION(BASS_MusicLoad);
	LOADBASSFUNCTION(BASS_MusicFree);
	LOADBASSFUNCTION(BASS_StreamCreateFile);
	LOADBASSFUNCTION(BASS_StreamCreateURL);
	LOADBASSFUNCTION(BASS_StreamGetFilePosition);
	LOADBASSFUNCTION(BASS_StreamFree);
	LOADBASSFUNCTION(BASS_ChannelGetLength);
	LOADBASSFUNCTION(BASS_ChannelGetTags);
	LOADBASSFUNCTION(BASS_ChannelPlay);
	LOADBASSFUNCTION(BASS_ChannelStop);
	LOADBASSFUNCTION(BASS_ChannelBytes2Seconds);
	LOADBASSFUNCTION(BASS_ChannelIsActive);
	LOADBASSFUNCTION(BASS_ChannelSlideAttribute);
	LOADBASSFUNCTION(BASS_ChannelIsSliding);
	LOADBASSFUNCTION(BASS_ChannelGetPosition);
	LOADBASSFUNCTION(BASS_ChannelGetLevel);
}

void FreeBASS()
{
	if (!bass) return;
	FreeLibrary(bass);
	bass=0;
	DeleteFile(tempfile);
}

void InitSoundSystem(HWND hwnd)
{
	if (!BASS_Init(-1,44100,0,hwnd,NULL))
	ExitProcess(0);
	chan=BASS_MusicLoad(FALSE,"mario airlines (keygen edit).xm",0,0,BASS_SAMPLE_LOOP|BASS_MUSIC_RAMPS|BASS_MUSIC_PRESCAN|BASS_MUSIC_SURROUND2,0);
	BASS_ChannelPlay(chan,FALSE);
}

/**************************
 * OpenGL code
 **************************/

void DumpOGLDriverInformation()
{
    OPENFILENAME ofn;
	char szFileName[MAX_PATH] = "gltest.txt";
	ZeroMemory(&ofn, sizeof(ofn));
	ofn.lStructSize = sizeof(OPENFILENAME);
	ofn.hwndOwner = GetForegroundWindow();
	ofn.lpstrFilter = "Text Files (*.txt)\0*.txt\0";
	ofn.lpstrFile = szFileName;
	ofn.nMaxFile = MAX_PATH;
	ofn.lpstrTitle = "Save OpenGL information as...";
	ofn.Flags = OFN_EXPLORER | OFN_FILEMUSTEXIST | OFN_HIDEREADONLY;
	ofn.lpstrDefExt = "txt";

	if(GetSaveFileName(&ofn))
	{
	 HWND hEdit = GetDlgItem(GetForegroundWindow(), IDD_DLG1);
	 FILE* file = NULL;
	 TCHAR result[50];
	 int nbAuxBuffers;
	 int nbDepthBits;
	 int nbClipPlanes;
	 int nbLights;
	 int TexSize;
	 int nbTexUnits;
	 int nbViewport;
	 float largest_supported_anisotropy;
     const char* extensions = NULL;
		// Create file for dumping
    file = fopen( szFileName, "w+" );
	  // Enumerate extensions
	Write( file, "Welcome to Mudlord's Glide3x wrapper tester!\n\n" );
	Write( file, "OGL Driver Details\n\n" );
    Write( file, "Card Vendor:           " ); Write( file, (const char*)glGetString( GL_VENDOR ) ); Write( file, "\n" );
    Write( file, "OpenGL Renderer:       " ); Write( file, (const char*)glGetString( GL_RENDERER ) ); Write( file, "\n" );
    Write( file, "OpenGL ICD Version:    " ); Write( file, (const char*)glGetString( GL_VERSION ) ); Write( file, "\n" );
	glGetIntegerv(GL_MAX_DRAW_BUFFERS_ARB, &nbAuxBuffers);
	sprintf(result,"%d",nbAuxBuffers);
	Write (file, "Maximum number of auxiliary draw buffers:  ");
	Write (file, result);
	Write (file, "\n");
	glGetIntegerv(GL_DEPTH_BITS, &nbDepthBits);
	sprintf(result, "%d",nbDepthBits);
    Write (file, "Maximum number of depth buffer bitplanes:  ");
	Write (file, result);
	Write (file, "\n");
	glGetIntegerv(	GL_MAX_CLIP_PLANES, &nbClipPlanes);
	sprintf(result, "%d",nbClipPlanes);
    Write (file, "Maximum number of clipping planes:  ");
	Write (file, result);
    Write( file, "\n" );
	glGetIntegerv(	GL_MAX_LIGHTS, &nbLights);
	sprintf(result, "%d",nbLights);
    Write (file, "Maximum number of OpenGL lights:  ");
	Write (file, result);
    Write( file, "\n" );
	glGetIntegerv(	GL_MAX_TEXTURE_SIZE, &TexSize);
	sprintf(result, "%d",TexSize);
    Write (file, "Largest texture width/size your card can handle:  ");
	Write (file, result);
    Write( file, "\n" );
	glGetIntegerv(	GL_MAX_TEXTURE_UNITS_ARB, &nbTexUnits);
	sprintf(result, "%d",nbTexUnits);
    Write (file, "Number of texture units:  ");
	Write (file, result);
    Write( file, "\n" );
	glGetIntegerv(	GL_MAX_VIEWPORT_DIMS, &nbViewport);
	sprintf(result, "%d",nbViewport);
    Write (file, "Maximum viewport size:  ");
	Write (file, result);
	Write( file, "\n" );
	glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, &largest_supported_anisotropy);
	sprintf(result, "%f",largest_supported_anisotropy);
    Write (file, "Maximum level of anisotropic filtering:  ");
	Write (file, result);
    Write( file, "\n\n" );
    Write( file, "OpenGL Extensions\n" );
	Write( file, "======================\n" );
    extensions = (const char*)glGetString( GL_EXTENSIONS );
    while ( *extensions != '\0' ) {
	const char* begin = extensions;
	int i;

	while ( (*extensions != '\0') && (*extensions != ' ') ) {
	    ++extensions;
	}
	fwrite( begin, 1, extensions-begin, file );
	Write( file, "\n" );
	if ( *extensions != '\0' ) {
	    ++extensions;
	}
    }
    // Close file
    fclose( file );

	}

  
}

void Write( FILE* file, const char* text )
{
    fwrite( text, 1, strlen(text), file );
}



BOOL isExtensionSupported(const char *extension)
{
	const GLubyte *extensions = NULL;
	const GLubyte *start;
	GLubyte *where, *terminator;

	where = (GLubyte *)strchr(extension, ' ');
	if (where || *extension == '\0')
		return 0;

	extensions = glGetString(GL_EXTENSIONS);

	start = extensions;
	for (;;)
	{
		where = (GLubyte *) strstr((const char *) start, extension);
		if (!where)
			break;

		terminator = where + strlen(extension);
		if (where == start || *(where - 1) == ' ')
			if (*terminator == ' ' || *terminator == '\0')
				return TRUE;

		start = terminator;
	}

	return FALSE;
}

void EnableOpenGL ()
{
   PIXELFORMATDESCRIPTOR pfd;
   ZeroMemory (&pfd, sizeof (pfd));
   pfd.nSize = sizeof (pfd);
   SetPixelFormat (GetDC (GetForegroundWindow()), ChoosePixelFormat ( GetDC (GetForegroundWindow()), &pfd), &pfd);
   wglMakeCurrent (GetDC (GetForegroundWindow()), wglCreateContext(GetDC (GetForegroundWindow()) ) );
}
