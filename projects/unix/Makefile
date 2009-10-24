#/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# *   Mupen64plus - Makefile                                                *
# *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
# *   Copyright (C) 2007-2008 DarkJeztr Tillin9 Richard42                   *
# *                                                                         *
# *   This program is free software; you can redistribute it and/or modify  *
# *   it under the terms of the GNU General Public License as published by  *
# *   the Free Software Foundation; either version 2 of the License, or     *
# *   (at your option) any later version.                                   *
# *                                                                         *
# *   This program is distributed in the hope that it will be useful,       *
# *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
# *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
# *   GNU General Public License for more details.                          *
# *                                                                         *
# *   You should have received a copy of the GNU General Public License     *
# *   along with this program; if not, write to the                         *
# *   Free Software Foundation, Inc.,                                       *
# *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
# Makefile for RiceVideoLinux, in Mupen64Plus

# include pre-make file with a bunch of definitions
USES_GTK2 = true
ifeq ($(WIN32),1)
  include ../pre.mk.win32
else
  include ../pre.mk
endif

# local CFLAGS, LIBS, and LDFLAGS
CFLAGS += $(SDL_FLAGS) -fpic -DPIC -Wall
LDFLAGS	+= -lpng $(SDL_LIBS) $(LIBGL_LIBS) $(PLUGIN_LDFLAGS)

# list of object files to generate
OBJECTS = OGLGraphicsContext.o \
	Debugger.o \
	Video.o \
	Config.o \
	GraphicsContext.o \
	TextureFilters.o \
	DeviceBuilder.o \
	Render.o \
	RSP_Parser.o \
	TextureManager.o \
	RenderBase.o \
	FrameBuffer.o \
	Texture.o \
	TextureFilters_2xsai.o \
	TextureFilters_hq2x.o \
	TextureFilters_hq4x.o \
	liblinux/bmp.o \
	liblinux/BMGImage.o \
	liblinux/pngrw.o \
	OGLRender.o \
	OGLTexture.o \
	OGLFragmentShaders.o \
	OGLCombinerNV.o \
	OGLCombinerTNT2.o \
	OGLExtCombiner.o \
	OGLCombiner.o \
	OGLExtRender.o \
	blender.o \
	math.o \
	Combiner.o \
	RSP_S2DEX.o \
	RenderExt.o \
	ConvertImage.o \
	ConvertImage16.o \
	DecodedMux.o \
	RenderTexture.o \
	liblinux/BMGUtils.o \
	OGLDecodedMux.o \
	CNvTNTCombiner.o \
	GeneralCombiner.o \
	DirectXDecodedMux.o \
	CombinerTable.o \
	OGLRenderExt.o

GTKOBJECTS = \
	gui_gtk2/messagebox_gtk2.o

LDFLAGS += $(GTK_LIBS)
CFLAGS += -DUSE_GTK $(GTK_FLAGS)
OBJECTS += $(GTKOBJECTS)

# build targets
targets:
	@echo "RiceVideoLinux N64 Video plugin makefile. "
	@echo "  Targets:"
	@echo "    all        == Build RiceVideoLinux plugin"
	@echo "    clean      == remove object files"
	@echo "    rebuild    == clean and re-build all"
	@echo "  Options:"
	@echo "    BITS=32    == build 32-bit binaries on 64-bit machine"
	@echo "    PROFILE=1  == build gprof instrumentation into binaries for profiling"
	@echo "    DBG=1      == turn on debugging functions"
	@echo "    DBGSYM=1   == add debugging symbols to binaries"

all: version.h ricevideo.so

ricevideo.so: $(OBJECTS)
	$(LD) $(OBJECTS) $(LDFLAGS) $(GTK_LIBS) -o $@
	$(STRIP) $@

clean:
	$(RM) -rf ricevideo.so $(OBJECTS)

rebuild: clean all

# build rules
version.h: FORCE
	@sed 's|@MUPEN_VERSION@|\"$(MUPEN_VERSION)\"| ; s|@PLUGIN_VERSION@|\"$(PLUGIN_VERSION)\"|' \
        ../main/version.template > version.h
	@$(MV) version.h ../main/version.h

.cpp.o:
	$(CXX) -o $*.o $(CFLAGS) -c $<

.c.o:
	$(CC) -o $*.o $(CFLAGS) -c $<

FORCE:

