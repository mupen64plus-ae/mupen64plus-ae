#/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# *   Mupen64plus-video-rice - Makefile                                     *
# *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
# *   Copyright (C) 2007-2009 Richard Goedeken                              *
# *   Copyright (C) 2007-2008 DarkJeztr Tillin9                             *
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
# Makefile for RiceVideo plugin in Mupen64Plus

# detect operation system
UNAME = $(shell uname -s)
ifeq ("$(UNAME)","Linux")
  OS = LINUX
endif
ifeq ("$(UNAME)","linux")
  OS = LINUX
endif
ifeq ("$(UNAME)","Darwin")
  OS = OSX
  LDFLAGS += -liconv -lpng
endif
ifeq ("$(UNAME)","FreeBSD")
  OS = FREEBSD
endif

# detect system architecture
HOST_CPU ?= $(shell uname -m)
NO_ASM ?= 1
ifneq ("$(filter x86_64 amd64,$(HOST_CPU))","")
  CPU := X86
  ifeq ("$(BITS)", "32")
    ARCH_DETECTED := 64BITS_32
  else
    ARCH_DETECTED := 64BITS
  endif
endif
ifneq ("$(filter pentium i%86,$(HOST_CPU))","")
  CPU := X86
  ARCH_DETECTED := 32BITS
endif
ifneq ("$(filter ppc powerpc,$(HOST_CPU))","")
  CPU := PPC
  ARCH_DETECTED := 32BITS
endif
ifneq ("$(filter ppc64 powerpc64,$(HOST_CPU))","")
  CPU := PPC
  ARCH_DETECTED := 64BITS
endif

# base CFLAGS, LIBS, and LDFLAGS
CFLAGS = -Wall -ffast-math -funroll-loops -fexpensive-optimizations -fno-strict-aliasing -I../../src
LDFLAGS = -ldl -lpng
ifneq ($(OS), FREEBSD)
  CFLAGS += -pipe -O3
endif

# test for presence of SDL
ifeq ($(shell which sdl-config 2>/dev/null),)
  $(error No SDL development libraries found!)
endif
ifeq ($(OS),FREEBSD)
    CFLAGS  += `${SDL_CONFIG} --cflags`
    LDFLAGS += `${SDL_CONFIG} --libs`
else
    CFLAGS  += $(shell sdl-config --cflags)
    LDFLAGS += $(shell sdl-config --libs)
endif

# set mupen64plus core API header path
ifneq ("$(APIDIR)","")
  CFLAGS += "-I$(APIDIR)"
else
  TRYDIR = ../../../mupen64plus-core/src/api
  ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
    CFLAGS += -I$(TRYDIR)
  else
    TRYDIR = /usr/local/include/mupen64plus
    ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
      CFLAGS += -I$(TRYDIR)
    else
      TRYDIR = /usr/include/mupen64plus
      ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
        CFLAGS += -I$(TRYDIR)
      else
        $(error Mupen64Plus API header files not found! Use makefile parameter APIDIR to force a location.)
      endif
    endif
  endif
endif

# Since we are building a shared library, we must compile with -fPIC for x86_64 CPUs.
# On 32-bit systems we do not want to use -fPIC because we don't have to and it has a big performance penalty on this arch
ifeq ($(ARCH_DETECTED), 64BITS)
  CFLAGS += -fpic -DPIC -fvisibility=hidden
endif

# set special flags per-system
ifneq ($(OS), FREEBSD)
  ifeq ($(CPU), X86)
    ifeq ($(ARCH_DETECTED), 64BITS)
      CFLAGS += -march=athlon64
    else
      CFLAGS += -mmmx -msse -march=i686 -mtune=pentium-m -fomit-frame-pointer
    endif
    # tweak flags for 32-bit build on 64-bit system
    ifeq ($(ARCH_DETECTED), 64BITS_32)
      CFLAGS += -m32
      LDFLAGS += -m32 -m elf_i386
    endif
  endif
else
  ifeq ($(ARCH_DETECTED), 64BITS_32)
    $(error Do not use the BITS=32 option with FreeBSD, use -m32 and -m elf_i386)
  endif
endif
ifeq ($(CPU), PPC)
  CFLAGS += -mcpu=powerpc
endif

# set shell function names
CC      = gcc
CXX     = g++
LD      = g++
ifeq ($(OS),LINUX)
  STRIP	= strip -s
endif
ifeq ($(OS),OSX)
  STRIP	= strip -x 
endif

# set special flags for given Makefile parameters
ifeq ($(DEBUG),1)
  CFLAGS += -g
  STRIP = true # disable binary strip
endif

SRCDIR = ../../src
OBJDIR = _obj

# list of source files to compile
SOURCE = \
	$(SRCDIR)/OGLGraphicsContext.cpp \
	$(SRCDIR)/Debugger.cpp \
	$(SRCDIR)/Video.cpp \
	$(SRCDIR)/Config.cpp \
	$(SRCDIR)/GraphicsContext.cpp \
	$(SRCDIR)/TextureFilters.cpp \
	$(SRCDIR)/DeviceBuilder.cpp \
	$(SRCDIR)/Render.cpp \
	$(SRCDIR)/RSP_Parser.cpp \
	$(SRCDIR)/TextureManager.cpp \
	$(SRCDIR)/RenderBase.cpp \
	$(SRCDIR)/FrameBuffer.cpp \
	$(SRCDIR)/Texture.cpp \
	$(SRCDIR)/TextureFilters_2xsai.cpp \
	$(SRCDIR)/TextureFilters_hq2x.cpp \
	$(SRCDIR)/TextureFilters_hq4x.cpp \
	$(SRCDIR)/liblinux/BMGImage.c \
	$(SRCDIR)/liblinux/BMGUtils.cpp \
	$(SRCDIR)/liblinux/bmp.c \
	$(SRCDIR)/liblinux/pngrw.c \
	$(SRCDIR)/OGLRender.cpp \
	$(SRCDIR)/OGLTexture.cpp \
	$(SRCDIR)/OGLFragmentShaders.cpp \
	$(SRCDIR)/OGLCombinerNV.cpp \
	$(SRCDIR)/OGLCombinerTNT2.cpp \
	$(SRCDIR)/OGLExtCombiner.cpp \
	$(SRCDIR)/OGLCombiner.cpp \
	$(SRCDIR)/OGLExtRender.cpp \
	$(SRCDIR)/blender.cpp \
	$(SRCDIR)/math.cpp \
	$(SRCDIR)/Combiner.cpp \
	$(SRCDIR)/RSP_S2DEX.cpp \
	$(SRCDIR)/RenderExt.cpp \
	$(SRCDIR)/ConvertImage.cpp \
	$(SRCDIR)/ConvertImage16.cpp \
	$(SRCDIR)/DecodedMux.cpp \
	$(SRCDIR)/RenderTexture.cpp \
	$(SRCDIR)/OGLDecodedMux.cpp \
	$(SRCDIR)/CNvTNTCombiner.cpp \
	$(SRCDIR)/GeneralCombiner.cpp \
	$(SRCDIR)/DirectXDecodedMux.cpp \
	$(SRCDIR)/CombinerTable.cpp \
	$(SRCDIR)/OGLRenderExt.cpp

# generate a list of object files build, make a temporary directory for them
OBJECTS := $(patsubst $(SRCDIR)/%.c, $(OBJDIR)/%.o, $(filter %.c, $(SOURCE)))
OBJECTS := $(patsubst $(SRCDIR)/%.cpp, $(OBJDIR)/%.o, $(filter %.cpp, $(SOURCE)))
OBJDIRS = $(dir $(OBJECTS))
$(shell mkdir -p $(OBJDIRS))

# build targets
targets:
	@echo "Mupen64plus-video-rice N64 Graphics plugin makefile. "
	@echo "  Targets:"
	@echo "    all        == Build Mupen64plus-video-rice plugin"
	@echo "    clean      == remove object files"
	@echo "    rebuild    == clean and re-build all"
	@echo "  Options:"
	@echo "    BITS=32    == build 32-bit binaries on 64-bit machine"
	@echo "    DEBUG=1    == add debugging symbols to binaries"
	@echo "    DEBUGGER=1 == turn on debugging functions"

all: mupen64plus-video-rice.so

clean:
	rm -rf ./_obj mupen64plus-video-rice.so

rebuild: clean all

# build rules
mupen64plus-video-rice.so: $(OBJECTS)
	$(LD) -shared $^ $(LDFLAGS) -o $@
	$(STRIP) $@

$(OBJDIR)/%.o: $(SRCDIR)/%.c
	$(CC) -o $@ $(CFLAGS) -c $<

$(OBJDIR)/%.o: $(SRCDIR)/%.cpp
	$(CXX) -o $@ $(CFLAGS) -c $<


