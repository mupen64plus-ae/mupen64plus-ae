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
OS := NONE
ifeq ("$(UNAME)","Linux")
  OS = LINUX
endif
ifeq ("$(UNAME)","linux")
  OS = LINUX
endif
ifneq ("$(filter GNU hurd,$(UNAME))","")
  OS = LINUX
endif
ifeq ("$(UNAME)","Darwin")
  OS = OSX
endif
ifeq ("$(UNAME)","FreeBSD")
  OS = FREEBSD
endif
ifneq ("$(filter GNU/kFreeBSD kfreebsd,$(UNAME))","")
  OS = LINUX
endif
ifeq ("$(OS)","NONE")
  $(error OS type "$(UNAME)" not supported.  Please file bug report at 'http://code.google.com/p/mupen64plus/issues')
endif

# detect system architecture
HOST_CPU ?= $(shell uname -m)
NO_ASM ?= 1
CPU := NONE
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
# PPC doesn't work yet
#ifneq ("$(filter ppc powerpc,$(HOST_CPU))","")
#  CPU := PPC
#  ARCH_DETECTED := 32BITS
#endif
#ifneq ("$(filter ppc64 powerpc64,$(HOST_CPU))","")
#  CPU := PPC
#  ARCH_DETECTED := 64BITS
#endif
ifeq ("$(CPU)","NONE")
  $(error CPU type "$(HOST_CPU)" not supported.  Please file bug report at 'http://code.google.com/p/mupen64plus/issues')
endif

# base CFLAGS, LIBS, and LDFLAGS
CFLAGS += -ffast-math -funroll-loops -fexpensive-optimizations -fno-strict-aliasing -I../../src
LDFLAGS += -lpthread

# set special flags per-system
ifeq ($(OS), LINUX)
  ifeq ($(CPU), X86)
    ifeq ($(ARCH_DETECTED), 64BITS)
      CFLAGS += -pipe -O3 -march=athlon64
      LDFLAGS += -ldl
    else
      CFLAGS += -pipe -O3 -mmmx -msse -march=i686 -mtune=pentium-m -fomit-frame-pointer
      LDFLAGS += -ldl
    endif
  endif
endif
ifeq ($(OS), OSX)
  # The mac version of SDL requires inclusion of SDL_main in the executable
  LDFLAGS += -ldl $(shell sdl-config --libs)
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

# tweak flags for 32-bit build on 64-bit system
ifeq ($(ARCH_DETECTED), 64BITS_32)
  ifeq ($(OS), FREEBSD)
    $(error Do not use the BITS=32 option with FreeBSD, use -m32 and -m elf_i386)
  endif
  CFLAGS += -m32
  LDFLAGS += -m32 -m elf_i386
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
CC      ?= gcc
CXX     ?= g++
INSTALL ?= install
ifeq ($(OS),OSX)
  STRIP	?= strip -x 
else
  STRIP	?= strip -s
endif

# set special flags for given Makefile parameters
# note: COREDIR _must_ end in a slash if you want it to work; not necessary for the others
ifneq ($(PLUGINDIR),)
  CFLAGS += '-DPLUGINDIR="$(PLUGINDIR)"'
endif
ifneq ($(COREDIR),)
  CFLAGS += '-DCOREDIR="$(COREDIR)"'
endif
ifneq ($(SHAREDIR),)
  CFLAGS += '-DSHAREDIR="$(SHAREDIR)"'
endif
ifeq ($(DEBUG),1)
  CFLAGS += -g
  STRIP = true # disable binary strip
endif

# set installation options
ifeq ($(PREFIX),)
  PREFIX := /usr/local
endif
ifeq ($(BINDIR),)
  BINDIR := $(PREFIX)/bin
endif
ifeq ($(MANDIR),)
  MANDIR := $(PREFIX)/man/man6
endif

SRCDIR = ../../src
OBJDIR = _obj

# list of source files to compile
SOURCE = \
	$(SRCDIR)/cheat.c \
	$(SRCDIR)/compare_core.c \
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
TARGET = mupen64plus

targets:
	@echo "Mupen64Plus-ui-console makefile."
	@echo "  Targets:"
	@echo "    all            == Build Mupen64Plus console front-end application"
	@echo "    clean          == remove object files and build products"
	@echo "    rebuild        == clean and re-build all"
	@echo "    install        == Install Mupen64Plus console front-end application"
	@echo "    uninstall      == Uninstall Mupen64Plus console front-end application"
	@echo "  Options:"
	@echo "    COREDIR=path   == default path to search for Mupen64Plus Core (must end with slash)"
	@echo "    PLUGINDIR=path == default path to search for plugins"
	@echo "    SHAREDIR=path  == default path to search for shared data files"
	@echo "    APIDIR=path    == path to find Mupen64Plus Core headers"
	@echo "  Install Options:"
	@echo "    PREFIX=path    == install/uninstall prefix (default: /usr/local/)"
	@echo "    BINDIR=path    == path to install mupen64plus binary (default: PREFIX/bin/)"
	@echo "    MANDIR=path    == path to install mupen64plus manual page (default: PREFIX/man/man6/)"
	@echo "    DESTDIR=path   == path to prepend to all installation paths (only for packagers)"
	@echo "  Debugging Options:"
	@echo "    DEBUG=1        == add debugging symbols to application binary"

all: $(TARGET)

clean:
	rm -rf ./_obj/* $(TARGET)
	rmdir ./_obj

rebuild: clean all

install: $(TARGET)
	$(INSTALL) -d -v "$(DESTDIR)$(BINDIR)"
	$(INSTALL) -m 0755 $(TARGET) "$(DESTDIR)$(BINDIR)"
	$(INSTALL) -d -v "$(DESTDIR)$(MANDIR)"
	$(INSTALL) -m 0644 ../../doc/mupen64plus.6.gz "$(DESTDIR)$(MANDIR)"

uninstall:
	rm -f "$(DESTDIR)$(BINDIR)/$(TARGET)" "$(DESTDIR)$(MANDIR)/mupen64plus.6.gz"

# build dependency files
CFLAGS += -MD
-include $(OBJECTS:.o=.d)

# reduced compile output when running make without V=1
ifneq ($(findstring $(MAKEFLAGS),s),s)
ifndef V
	Q_CC  = @echo '    CC  '$@;
	Q_LD  = @echo '    LD  '$@;
endif
endif

# build rules
$(TARGET): $(OBJECTS)
	$(Q_LD)$(CC) $^ $(LDFLAGS) $(SRC_LIBS) -o $@
	$(STRIP) $@

$(OBJDIR)/%.o: $(SRCDIR)/%.c
	$(Q_CC)$(CC) -o $@ $(CFLAGS) -c $<
