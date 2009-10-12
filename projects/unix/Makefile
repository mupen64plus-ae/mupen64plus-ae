#/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# *   Mupen64plus - Makefile                                                *
# *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
# *   Copyright (C) 2009 Richard42                                          *
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
# Makefile for Mupen64plus-ui-console

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
CFLAGS = -ffast-math -funroll-loops -fexpensive-optimizations -fno-strict-aliasing -I../../src -I../../../mupen64plus-core/src/api
LDFLAGS = -ldl
ifneq ($(OS), FREEBSD)
  CFLAGS += -pipe -O3
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

# set special flags for given Makefile parameters
ifneq ($(PLUGINDIR), )
  CFLAGS += -DPLUGINDIR="$(PLUGINDIR)"
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

SRCDIR = ../../src
OBJDIR = _obj

# list of source files to compile
SOURCE = \
	$(SRCDIR)/core_interface.c \
	$(SRCDIR)/main.c \
	$(SRCDIR)/plugin.c \
	$(SRCDIR)/osal_dynamiclib_unix.c \
	$(SRCDIR)/osal_files_unix.c

# generate a list of object files build, make a temporary directory for them
OBJECTS := $(patsubst $(SRCDIR)/%.c, $(OBJDIR)/%.o, $(filter %.c, $(SOURCE)))
OBJDIRS = $(dir $(OBJECTS))
$(shell mkdir -p $(OBJDIRS))

# build targets
all: mupen64plus-cli

clean:
	rm -rf ./_obj mupen64plus-cli

# build rules
mupen64plus-cli: $(OBJECTS)
	$(CC) $^ $(LDFLAGS) $(SRC_LIBS) -o $@
	$(STRIP) $@

$(OBJDIR)/%.o: $(SRCDIR)/%.c
	$(CC) -o $@ $(CFLAGS) -c $<


