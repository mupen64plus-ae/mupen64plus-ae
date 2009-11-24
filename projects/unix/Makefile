#/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# *   Mupen64plus-audio-sdl - Makefile                                      *
# *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
# *   Copyright (C) 2007-2009 Richard Goedeken                              *
# *   Copyright (C) 2007-2008 Tillin9                                       *
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
# Makefile for SDL Audio plugin in Mupen64plus

# detect operation system
UNAME = $(shell uname -s)
ifeq ("$(UNAME)","Linux")
  OS = LINUX
  SHARED = -shared
  SO_EXTENSION = so
endif
ifeq ("$(UNAME)","linux")
  OS = LINUX
  SHARED = -shared
  SO_EXTENSION = so
endif
ifeq ("$(UNAME)","Darwin")
  OS = OSX
  SHARED = -bundle
  SO_EXTENSION = dylib
endif
ifeq ("$(UNAME)","FreeBSD")
  OS = FREEBSD
  SHARED = -shared
  SO_EXTENSION = so
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
CFLAGS = -ffast-math -funroll-loops -fexpensive-optimizations -fno-strict-aliasing -I../../src
LDFLAGS = -ldl

# Since we are building a shared library, we must compile with -fPIC for x86_64 CPUs.
# On 32-bit systems we do not want to use -fPIC because we don't have to and it has a big performance penalty on this arch
ifeq ($(ARCH_DETECTED), 64BITS)
  CFLAGS += -fpic -DPIC -fvisibility=hidden
endif
# tweak flags for 32-bit build on 64-bit system
ifeq ($(ARCH_DETECTED), 64BITS_32)
  ifeq ($(OS), FREEBSD)
    $(error Do not use the BITS=32 option with FreeBSD, use -m32 and -m elf_i386)
  endif
  CFLAGS += -m32
  LDFLAGS += -m32 -m elf_i386
endif

# set special flags per-system
ifeq ($(OS), LINUX)
  ifeq ($(CPU), X86)
    ifeq ($(ARCH_DETECTED), 64BITS)
      CFLAGS += -pipe -O3 -march=athlon64
    else
      CFLAGS += -pipe -O3 -mmmx -msse -march=i686 -mtune=pentium-m -fomit-frame-pointer
    endif
  endif
endif
ifeq ($(OS), OSX)
  ifeq ($(CPU), X86)
    ifeq ($(ARCH_DETECTED), 64BITS)
      CFLAGS += -pipe -O3 -arch x86_64 -mmacosx-version-min=10.5 -isysroot /Developer/SDKs/MacOSX10.5.sdk
      LDFLAGS += -arch x86_64
    else
      CFLAGS += -pipe -O3 -mmmx -msse -fomit-frame-pointer -arch i686 -mmacosx-version-min=10.5 -isysroot /Developer/SDKs/MacOSX10.5.sdk
      LDFLAGS += -arch i686
    endif
  endif
endif
ifeq ($(CPU), PPC)
  CFLAGS += -mcpu=powerpc
endif

# test for presence of SDL
ifeq ($(shell which sdl-config 2>/dev/null),)
  $(error No SDL development libraries found!)
endif
ifeq ($(OS),FREEBSD)
    CFLAGS  += `${SDL_CONFIG} --cflags`
    LDFLAGS += `${SDL_CONFIG} --libs`
endif
ifeq ($(OS),OSX)
    CFLAGS  += $(shell sdl-config --cflags)
    # sdl-config on mac screws up when we're trying to build a library and not an executable
    # SDL 1.3 is supposed to fix that, if it's ever released
    LDFLAGS += -L/usr/local/lib -lSDL -Wl,-framework,Cocoa
endif
ifeq ($(OS),LINUX)
    CFLAGS  += $(shell sdl-config --cflags)
    LDFLAGS += $(shell sdl-config --libs)
endif

# test for presence of libsamplerate
ifneq ($(strip $(shell pkg-config samplerate --modversion 2> /dev/null)),)
  ifneq ($(NO_RESAMP), 1)
    # set libsamplerate flags and libraries
    CFLAGS	+= $(shell pkg-config samplerate --cflags) -DUSE_SRC
    LDFLAGS += $(shell pkg-config samplerate --libs)
  endif
else
  # warn user
  $(warning No libsamplerate development libraries found.  Mupen64plus-sdl-audio will be built without Best Quality SINC resampler.)
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
	$(SRCDIR)/main.c \
	$(SRCDIR)/volume.c \
	$(SRCDIR)/osal_dynamiclib_unix.c

# generate a list of object files build, make a temporary directory for them
OBJECTS := $(patsubst $(SRCDIR)/%.c, $(OBJDIR)/%.o, $(filter %.c, $(SOURCE)))
OBJDIRS = $(dir $(OBJECTS))
$(shell mkdir -p $(OBJDIRS))

# build targets
TARGET = mupen64plus-audio-sdl.$(SO_EXTENSION)

all: $(TARGET)

clean:
	rm -rf ./_obj $(TARGET)

# build rules
$(TARGET): $(OBJECTS)
	$(LD) $(SHARED) $^ $(LDFLAGS) -o $@
	$(STRIP) $@

$(OBJDIR)/%.o: $(SRCDIR)/%.c
	$(CC) -o $@ $(CFLAGS) -c $<


